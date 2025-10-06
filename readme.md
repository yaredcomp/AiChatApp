# JavaFX AI Chat (Student Contribution Starter)

This is a lightweight JavaFX desktop chat application that sends user messages to an AI model (via Ollama, OpenRouter, or Groq) and displays assistant replies. The project is intentionally compact and well-scoped so computer science students can make meaningful contributions for extra credit.

---

## What this project does

- Provides a simple chat UI built with JavaFX.
- Saves user preferences (theme, font, provider, model, API keys) and conversation history to JSON files under the user's home directory (path: `~/.javafx_ai_chat`).
- Supports multiple backend providers via an extensible `AIProvider` interface:
  - Local Ollama HTTP server (default host `http://localhost:11434`)
  - OpenRouter (requires API key)
  - Groq (requires API key)
- Settings window to change theme, font, provider, model, and provider credentials.

Code entrypoint: `com.lj.aichatapp.app.App`

UI files: `src/main/resources/views/main.fxml`, `settings.fxml`

---

## How to run (developer notes)

This project uses JavaFX and the JDK's HttpClient API. Build with Maven and run with a JDK that has JavaFX available or supply JavaFX on the module path.

Quick steps (high-level):

1. Build:

```powershell
mvn clean package
```

2. Run (example; adjust `--module-path` to your JavaFX SDK lib folder):

```powershell
# replace <path-to-javafx-lib> with the folder that contains the JavaFX jars (e.g., javafx-sdk-20/lib)
java --module-path "<path-to-javafx-lib>" --add-modules javafx.controls,javafx.fxml -jar target/*.jar
```

Notes:

- If you plan to use the default Ollama provider, run an Ollama server locally and set the host in Settings (default `http://localhost:11434`).
- For OpenRouter or Groq, place the API key in Settings → Provider Keys.

---

## Implemented features (summary)

- Main chat UI with message bubbles and auto-scroll
- Send messages and receive replies from configured AI provider
- Typing indicator while waiting for responses
- Settings UI with theme, font, provider, model, provider host/keys
- Persisted preferences and conversation in JSON files
- Providers implemented: Ollama (local), OpenRouter (HTTP), Groq (HTTP)

---

## Architecture and important classes

- `App.java` — application entry point (loads FXML and CSS)
- `MainController.java` — chat UI logic and actions
- `SettingsController.java` — settings dialog logic
- `AIServiceManager.java` — picks provider from `UserPreferences`
- `AIProvider` (interface) — implement this to add providers
- `OllamaService.java`, `OpenRouterService.java`, `GroqService.java` — provider implementations
- `PreferencesManager.java` — persists `UserPreferences` and `Conversation` as JSON
- `FxUtils.java` — small UI helpers (message bubble, fade-in)

---

## Extra-credit contribution ideas

These are concrete, well-scoped improvements students can implement for extra credit. Each item includes why it matters and suggested starting points.

1. Add a real local database for conversation history (SQLite)
   - Why: JSON is fine for small data, but a database enables efficient queries, robust storage, and easier migrations.
   - Suggested work:
     - Add SQLite (e.g., via the `org.xerial:sqlite-jdbc` Maven dependency) and a small DAO layer.
     - Create tables for conversations and messages with timestamps and roles.
     - Migrate existing JSON storage to the database on first run or provide an import tool.
     - Update `PreferencesManager` or introduce a `ConversationRepository` to use the DB for load/save.
   - Acceptance: app stores and loads conversations from SQLite and old JSON migration is available.

2. Support streaming responses from the LLM (progressive UI update)
   - Why: Streaming provides a more responsive UX for long model responses.
   - Suggested work:
     - Use non-blocking APIs (e.g., `HttpClient::sendAsync` with body handlers for streaming) or handle server-sent events/chunked responses from providers like Ollama.
     - Update `MainController` so assistant messages appear progressively (append chunks to the last bubble) and finalize when complete.
     - Ensure proper cancellation and thread-safety with `Platform.runLater`.
   - Acceptance: UI shows message content as it arrives, with a final commit at the end.

3. Format LLM responses: syntax-aware rendering (make titles bold, separate code blocks, bullets)
   - Why: LLM replies often include code snippets, lists, and headings. Clear formatting improves readability.
   - Suggested work:
     - Detect Markdown in assistant responses and render basic Markdown in the UI: bold/italic, headings, lists, and fenced code blocks.
     - Use a lightweight Markdown-to-HTML renderer and display in a JavaFX `WebView` or implement simple parsing to create JavaFX nodes (Label, TextFlow, CodeArea-like control).
     - For code blocks, render in a monospace font and provide a copy button.
   - Acceptance: assistant messages render formatted text, code blocks are visually distinct and copyable.

4. Add unit/integration tests and CI configuration
   - Why: Tests improve reliability and make the project contribution-friendly.
   - Suggested work:
     - Add unit tests for `AIServiceManager`, payload builders, and `PreferencesManager` (mock file system or use temp directories).
     - Add a GitHub Actions workflow to run `mvn -DskipTests=false test` and build on push.
   - Acceptance: Tests run in CI and are green.

5. Improve error handling and user feedback
   - Why: Right now some IO exceptions are swallowed and network errors surface as raw messages. Better user-facing errors and logs are needed.
   - Suggested work:
     - Log exceptions to a log file (SLF4J + Logback) and show friendly dialogs to users for recoverable errors.
     - Validate provider settings before sending and show clear remediation steps.
   - Acceptance: errors are logged, and users get meaningful alerts.

6. Add plugin-style provider architecture and sample provider
   - Why: Makes it easier to add new providers (OpenAI, Anthropic, etc.) without changing core code.
   - Suggested work:
     - Design a small plugin loader that discovers provider classes from a folder or classpath using a simple SPI pattern.
     - Provide documentation and a sample provider implementation.
   - Acceptance: new provider can be dropped into a providers folder or registered without modifying core classes.

7. UI improvements / accessibility / polish
   - Why: Improve UX to make the app more polished and accessible.
   - Suggested work:
     - Add keyboard shortcuts, message timestamps, clear conversation confirmation, message deletion.
     - Improve responsive layout and test with different font families/sizes.
   - Acceptance: UX improvements merged and documented.

8. Export/Import conversation and multi-conversation support
   - Why: Users may want to save or load multiple conversation sessions.
   - Suggested work:
     - Add Export/Import actions (JSON or SQLite), and implement multiple named conversations stored separately.
   - Acceptance: users can create, switch, export, and import conversations.

---

## How students should submit a contribution for extra credit

- Create a new branch and implement one or more of the items above. Keep changes focused and small.
- Add unit tests for new logic and run the test suite locally.
- Update `readme.md` with a short description of the implemented feature and any run-time setup required.
- Open a pull request with a clear description and testing steps.

Suggested rubric for instructors (example):

- Small polish / bugfix: 1–2 points
- New provider / database migration / streaming support: 4–6 points
- Major UI + Markdown renderer + tests + CI: 8–10 points

---

# End of README
