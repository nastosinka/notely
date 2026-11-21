# 🎻 Notely

Notely is a personal desktop application for organizing and reading sheet music on macOS.

The project is designed for musicians who play multiple instruments and want a simple way to keep their sheet music organized in one place.

## Features

- 📁 Create folders for different instruments with custom names and emojis
- 🎼 Import and organize PDF, JPG, and PNG sheet music
- 📖 Read sheet music directly inside the application
- 🔍 Zoom in and out, and view multiple pages side by side
- 🖼️ Preview sheet music using thumbnails of the first page
- 🎨 Minimalist interface with pastel colors
- 💾 Store files locally without a database or cloud services

## Tech Stack

- **Kotlin / JVM 21**
- **Compose Desktop**
- **Apache PDFBox**
- **Kotlinx Serialization**
- **Gradle Kotlin DSL**

## Getting Started

Clone the repository and run:

```bash
./gradlew run
```

The application stores imported sheet music in `~/Documents/Notely Library/`.

## Roadmap

- [ ] Rename and delete folders and sheet music
- [ ] Export sheet music to PDF and ZIP
- [ ] Draw annotations and add notes to PDF pages
- [ ] Support multiple pastel color themes

## Development & AI Assistance

Notely is a personal learning project developed with the assistance of **OpenAI's ChatGPT**.

I'm exploring AI-assisted software development and learning how to use large language models as coding partners.

ChatGPT helps me with architecture discussions, code generation, debugging, and UI development. I review, test, and adapt the suggested solutions while making the final decisions about the project.

The goal is to gain hands-on experience with AI-assisted development while building an application I actually want to use.

## Project Status

🚧 Work in progress — early development.