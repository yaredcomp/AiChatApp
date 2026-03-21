package com.lj.aichatapp.service.local;

import com.lj.aichatapp.utils.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LlamaServerManager {
    private final String appDataPath;
    private Process serverProcess;
    private String currentRunningModel = null;
    
    private static final int PORT = 8081; 
    private static final String SERVER_EXE_NAME = "llama-server"; 
    private static final String SERVER_API_URL = "http://localhost:" + PORT;
    private static final String MODELS_DIR_NAME = "Local LM";

    // Comprehensive list of Windows dependencies to copy to avoid DLL missing errors
    private static final String[] WINDOWS_BINARIES = {
        "llama-server.exe",
        "llama.dll",
        "ggml.dll",
        "ggml-base.dll",
        "ggml-rpc.dll",
        "libomp140.x86_64.dll",
        "mtmd.dll", 
        
        // CPU Backends
        "ggml-cpu-x64.dll",
        "ggml-cpu-sandybridge.dll",
        "ggml-cpu-ivybridge.dll",
        "ggml-cpu-haswell.dll",
        "ggml-cpu-skylakex.dll",
        "ggml-cpu-cannonlake.dll",
        "ggml-cpu-icelake.dll",
        "ggml-cpu-cascadelake.dll",
        "ggml-cpu-cooperlake.dll",
        "ggml-cpu-sapphirerapids.dll",
        "ggml-cpu-alderlake.dll",
        "ggml-cpu-zen4.dll",
        "ggml-cpu-piledriver.dll",
        "ggml-cpu-sse42.dll"
    };

    public LlamaServerManager(String appDataPath) {
        this.appDataPath = appDataPath;
        ensureDirectories();
    }
    
    private void ensureDirectories() {
        try {
            Path modelsDir = getModelsDir();
            if (!Files.exists(modelsDir)) {
                Files.createDirectories(modelsDir);
            }
            Path binDir = Paths.get(appDataPath).resolve("bin");
            if (!Files.exists(binDir)) {
                Files.createDirectories(binDir);
            }
        } catch (IOException e) {
            Logger.error("Failed to create app directories", e);
        }
    }
    
    public Path getModelsDir() {
        return Paths.get(appDataPath).resolve(MODELS_DIR_NAME);
    }

    public List<String> listAvailableModels() {
        try (Stream<Path> walk = Files.list(getModelsDir())) {
            return walk
                .filter(Files::isRegularFile)
                .map(p -> p.getFileName().toString())
                .filter(name -> name.endsWith(".gguf"))
                .collect(Collectors.toList());
        } catch (IOException e) {
            Logger.error("Failed to list local models", e);
            return new ArrayList<>();
        }
    }

    public void startServer(String modelName) throws IOException {
        if (isServerRunning()) {
            if (modelName.equals(currentRunningModel)) {
                Logger.info("Llama server is already running with model: " + modelName);
                return;
            } else {
                Logger.info("Stopping current server to switch model from " + currentRunningModel + " to " + modelName);
                stopServer();
            }
        }

        if (modelName == null || modelName.isBlank()) {
            throw new IllegalArgumentException("Model name is not set for local Llama server.");
        }

        Path modelPath = getModelsDir().resolve(modelName);
        if (!Files.exists(modelPath)) {
            Logger.error("Local model file not found: " + modelPath);
            throw new IllegalArgumentException("Model file not found: " + modelName);
        }

        // Ensure binaries
        Path binDir = Paths.get(appDataPath).resolve("bin");
        String serverExeName = SERVER_EXE_NAME;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            serverExeName += ".exe";
            copyWindowsBinaries(binDir);
        }
        
        Path serverExePath = binDir.resolve(serverExeName);
        if (!Files.exists(serverExePath)) {
             throw new IOException("Server executable missing: " + serverExePath);
        }

        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            if (!serverExePath.toFile().setExecutable(true)) {
                Logger.warn("Could not set executable permission for " + serverExePath);
            }
        }

        ProcessBuilder pb = new ProcessBuilder(
            serverExePath.toString(),
            "--model", modelPath.toString(),
            "--port", String.valueOf(PORT),
            "--ctx-size", "2048", 
            "--n-gpu-layers", "99", 
            "--host", "0.0.0.0" 
        );
        
        pb.redirectErrorStream(true); 
        
        this.serverProcess = pb.start();
        this.currentRunningModel = modelName;
        Logger.info("Local LLM Server process started. PID: " + serverProcess.pid() + " Model: " + modelName);
        
        // Read logs
        new Thread(() -> {
            try (java.util.Scanner sc = new java.util.Scanner(serverProcess.getInputStream())) {
                while (sc.hasNextLine()) {
                    String line = sc.nextLine();
                    Logger.info("[LlamaServer] " + line);
                }
            } catch (Exception e) {
                Logger.error("Error reading server process output", e);
            }
        }, "LlamaServer-Log-Reader").start();
        
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopServer));
        
        // Block until server is ready (200 OK)
        waitForServerReady();
        
        Logger.info("Local LLM Server is ready on " + SERVER_API_URL);
    }

    private void waitForServerReady() throws IOException {
        long startTime = System.currentTimeMillis();
        long timeoutMillis = 60000; // 60 seconds timeout
        
        Logger.info("Waiting for Local LLM Server to be ready...");
        
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            // Check if process is still alive
            if (!serverProcess.isAlive()) {
                int exitCode = serverProcess.exitValue();
                String extraInfo = "";
                if (exitCode == -1073741515) { 
                    extraInfo = " (STATUS_DLL_NOT_FOUND - A required DLL is missing)";
                }
                throw new IOException("Local LLM Server exited unexpectedly during startup with code: " + exitCode + extraInfo);
            }

            try {
                URL url = new URL(SERVER_API_URL + "/health");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(500);
                connection.setReadTimeout(500);
                
                int code = connection.getResponseCode();
                if (code == 200) {
                    return; // Ready!
                }
                // 503 means still loading, so we just continue the loop
            } catch (IOException e) {
                // Connection failed (refused), likely starting up
            }

            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for server");
            }
        }
        
        throw new IOException("Timed out waiting for Local LLM Server to start after " + timeoutMillis + "ms");
    }

    private void copyWindowsBinaries(Path targetDir) {
        for (String filename : WINDOWS_BINARIES) {
            copySingleBinary(targetDir, "win", filename);
        }
    }

    private void copySingleBinary(Path targetDir, String osFolder, String filename) {
        Path targetPath = targetDir.resolve(filename);
        if (!Files.exists(targetPath)) {
            String resourcePath = "/binaries/" + osFolder + "/" + filename;
            try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    Logger.warn("Binary resource not found in jar: " + resourcePath);
                    return; 
                }
                Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                Logger.info("Copied binary: " + filename);
            } catch (IOException e) {
                Logger.error("Failed to copy binary: " + filename, e);
            }
        }
    }

    public void stopServer() {
        if (serverProcess != null && serverProcess.isAlive()) {
            Logger.info("Stopping Local LLM Server...");
            serverProcess.destroy(); 
            try {
                if (!serverProcess.waitFor(5, TimeUnit.SECONDS)) { 
                    serverProcess.destroyForcibly(); 
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                serverProcess.destroyForcibly();
            }
        }
        serverProcess = null;
        currentRunningModel = null;
    }
    
    public boolean isServerRunning() {
        return serverProcess != null && serverProcess.isAlive();
    }
    
    public String getApiUrl() {
        return SERVER_API_URL;
    }
}