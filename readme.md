# DU Academia ChatAI

**DU Academia ChatAI** is a professional, high-performance JavaFX desktop application designed to bridge the gap between academic workflows and modern Artificial Intelligence. Developed by the **Dilla University Computer Science Department**, this tool provides a secure, flexible, and feature-rich environment for interacting with cutting-edge AI models.

![License](https://img.shields.io/badge/license-Proprietary-red)
![Java](https://img.shields.io/badge/Java-21%2B-blue)
![JavaFX](https://img.shields.io/badge/JavaFX-21-orange)

---

## 🌟 Core Capabilities

*   **Intelligent Multi-Provider Integration**: Seamlessly switch between local inference (via Ollama or Embedded Llama) and cloud-based providers like Groq and OpenRouter.
*   **Privacy-First Local Storage**: All conversation histories, system prompts, and preferences are stored securely on your local machine using an optimized SQLite database.
*   **Modern, Reactive UI**: Features a sleek, "ultra-modern" interface with native Dark and Light mode support, center-cropped circular branding, and responsive typography.
*   **Advanced Prompt Engineering**: A dedicated Prompt Library allows users to curate, manage, and instantly deploy complex system instructions with variable support.
*   **Real-Time Streaming Inference**: Optimized text generation streaming for a low-latency, conversational experience.
*   **Comprehensive Markdown Rendering**: High-fidelity rendering of mathematical formulas, formatted code blocks with syntax highlighting, and complex academic tables.
*   **Session Management**: Intuitive navigation with search capabilities, renaming, and persistent conversation history.

---

## 🏗️ Architecture & Development

The project is built on a robust, modular architecture (`edu.du.et.chatapp`) that prioritizes extensibility and maintainability:

*   **Modular System**: Leverages the Java Module System (JPMS) for enhanced security and runtime efficiency.
*   **Service-Oriented Design**: Business logic is encapsulated in dedicated services (`ChatService`, `AIServiceManager`) for clear separation from the UI.
*   **Infrastructure Abstraction**: Database and preference management are abstracted behind clean repository interfaces.
*   **Theme Management**: A centralized `ThemeManager` reactively handles global styling and synchronization between JavaFX and embedded WebKit components.

---

## 🚀 Getting Started

### Prerequisites

*   **JDK 21 or higher** (JDK 25 recommended).
*   **Maven 3.8+**.
*   (Optional) **Ollama** for local model serving.

### Installation & Run

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/Dilla-University-CS/DUAcademiaAIChat.git
    cd DUAcademiaAIChat
    ```

2.  **Build the project**:
    ```bash
    mvn clean package
    ```

3.  **Run the application**:
    ```bash
    mvn javafx:run
    ```

---

## 🛠️ Configuration

**Provider Setup:**
1.  Access **Settings** (Gear icon).
2.  Choose your preferred provider (Ollama, Local LM, Groq, or OpenRouter).
3.  Configure API keys or server hosts as required.

**Model Management:**
*   Automatically fetch available models from providers or manually import GGUF files for local inference.

**Personalization:**
*   Adjust Font Family, Font Size, and toggle between Light/Dark themes. Theme changes propagate instantly across the entire application, including the chat interface.

---

## 🏛️ Credits

Developed with excellence by the **Dilla University Computer Science Department**. 

---

## 📄 License

This software is provided under a proprietary End User License Agreement (EULA). See `EULA.txt` for details.
All rights reserved by **Dilla University**.
