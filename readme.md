# iTutor - JavaFX AI Chat Application

iTutor is a modern, lightweight JavaFX desktop chat application designed to interface with various AI models. It supports multiple backend providers including local Ollama instances, OpenRouter, and Groq, allowing for flexible and private AI interactions.

![License](https://img.shields.io/badge/license-Proprietary-red)
![Java](https://img.shields.io/badge/Java-11%2B-blue)
![JavaFX](https://img.shields.io/badge/JavaFX-19-orange)

---

## Features

- **Multi-Provider Support**: Connects to Ollama (local), OpenRouter, and Groq.
- **Modern UI**: Clean, responsive interface with Dark/Light theme support.
- **Streaming Responses**: Real-time text generation for a responsive user experience.
- **Markdown Support**: Renders formatted text, code blocks, and lists.
- **Conversation Management**: Create, delete, and search through multiple chat sessions.
- **Prompt Library**: Save and reuse commonly used prompts.
- **Customizable**: Adjustable font settings, themes, and API configurations.
- **Local Storage**: Conversation history and preferences are stored locally for privacy.

---

## Architecture

The application follows a clean MVC (Model-View-Controller) architecture:

- **Core**: `App.java` handles the application lifecycle.
- **Controllers**: `MainController`, `SettingsController`, and `PromptsController` manage UI logic.
- **Services**: `ChatService`, `PromptService` manage business logic and data persistence.
- **Providers**: Extensible `AIProvider` interface with implementations for Ollama, OpenRouter, etc.
- **Utils**: Helper classes for UI effects (`FxUtils`) and text formatting (`ResponseFormatter`).

---

## Getting Started

### Prerequisites

- JDK 11 or higher (JDK 21+ recommended for best performance).
- Maven 3.8+.
- (Optional) Local [Ollama](https://ollama.com/) installation for local model inference.

### Installation & Run

1. **Clone the repository**:
   ```bash
   git clone https://github.com/yourusername/iTutor.git
   cd iTutor
   ```

2. **Build the project**:
   ```bash
   mvn clean package
   ```

3. **Run the application**:
   ```bash
   mvn javafx:run
   ```
   *Or run the shaded jar from the target directory.*

---

## Configuration

**Providers:**
- Go to **Settings** (Gear icon).
- Select your provider (Ollama, OpenRouter, Groq).
- Enter your API Key (for cloud providers) or Host URL (for Ollama, default: `http://localhost:11434`).

**Models:**
- You can manually add model names or fetch available models from the provider in the **Models** tab.

**Preferences:**
- Customize Font Family, Font Size, and Theme (Dark/Light) in **General** settings.

---

## Roadmap

- [ ] **Database Migration**: Fully migrate local storage from JSON/SQLite to a robust schema with migration tools.
- [ ] **Export/Import**: Support exporting chats to Markdown/PDF.
- [ ] **Multi-modal Support**: Image input capabilities for supported models.
- [ ] **Voice Interface**: Speech-to-Text and Text-to-Speech integration.

---

## License

This software is provided under a proprietary End User License Agreement (EULA). See `EULA.txt` for details.
All rights reserved by the original author.
