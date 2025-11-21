📱 AI Stock Assistant – Android App

An AI-powered Android assistant that enables conversational stock operations, built with Kotlin, Jetpack Compose, OpenAI, and the Model Context Protocol (MCP).

This app seamlessly communicates with a FastAPI backend and supports real-time inventory updates, voice interaction, and barcode scanning (ML Kit prototype).

⸻

⭐ Overview

The AI Stock Assistant is designed to demonstrate how Android + AI + lightweight backend servicescan work together to automate retail workflows like stock checks, transfers, and live updates.

The focus is not on retail operations themselves, but on showing how Android expertise + AI integration can deliver modern mobile productivity solutions.

⸻

🚀 Features

🤖 Conversational AI Assistant
• Chat or voice-based interaction
• GPT-powered intent understanding
• MCP tool-calling for backend actions
• Supports natural language such as:
• “Transfer 3 milk from store 101 to 103”
• “Show low stock for store 102”

📡 Real-Time Updates
• Uses Server-Sent Events (SSE)
• Inventory UI updates instantly when backend stock changes

🏬 Store Inventory Viewer
• Dropdown selector for stores
• Displays: product, quantity, category, UoM, and expiry

🎙️ Voice Interaction
• In-app speech-to-text
• Assistant can reply via Text-To-Speech
• Mute/unmute toggle

📷 ML Kit Barcode Scanner (Prototype)
• CameraX + ML Kit barcode detection
• Scanner UI is implemented, but barcode reading is currently inconsistent
• PDA (Honeywell CT45) hardware scanner integration planned

⸻

🛠 Tech Stack

Android
• Kotlin
• Jetpack Compose
• MVVM + StateFlow
• Coroutines
• OkHttp (SSE streaming)
• SpeechRecognizer + TextToSpeech
• CameraX + ML Kit (barcode)

Backend
• FastAPI
• MCP (Model Context Protocol)
• OpenAI GPT APIs
• SQLite
• Server-Sent Events (SSE)

⸻

🧩 Architecture

Android App (Jetpack Compose)
│
├── Chat / Voice Input
│      ↓
│   /chat_stream → MCP Server
│
├── Inventory Dashboard ← /stock/overview
│
├── Live Updates (SSE): /stock/live
│
└── Barcode Scanner (CameraX, ML Kit prototype)

MCP Server (FastAPI)
├── Receives message
├── GPT model decides: normal reply or tool call
├── Executes tools:
│      ├── get_low_stock
│      ├── transfer_stock
│      ├── fetch_store_stock
│      └── overview
└── Returns streaming response

Stock Backend / SQLite DB
├── Stock updates
├── Store-level inventory
├── Realtime change notifications (SSE)
└── Business logic

📦 Project Setup

1️⃣ Clone Android App

git clone https://github.com/pmahsky/ai-stock-assistant-android.git

Open in Android Studio Hedgehog+ and run.

2️⃣ Clone & Run Backend

git clone https://github.com/pmahsky/ai-stock-backend.git
cd ai-stock-backend-main
uvicorn mcp_server:app --reload --port 3100

3️⃣ Ensure Network Access

Both phone and backend laptop must be on the same WiFi.
Update the Android BASE_URL accordingly (local IP of backend).

🧪 Testing

Chat Commands:
• “Show low stock for store 101”
• “Transfer 1 Chips from 101 to 103”
• “What is the inventory overview?”

Voice Commands

Tap the 🎤 mic icon and speak naturally.

Store Inventory

Select store from the dropdown to view stock.

🐞 Known Issues
• ML Kit barcode reading is inconsistent
• PDA hardware scanner integration is pending
• Long GPT responses may stop early when speech is muted
• If backend restarts, SSE reconnect may take a moment

⸻

📈 Roadmap
• PDA hardware scanner integration
• If Delivery (PO), returns, waste, reductions workflows can be integrated
• Offline-first capability
• On-device embeddings for product lookup (optional)
• Role-based login

⸻

🤝 Contributions

This is an ongoing innovation project.
Suggestions, issues, and PRs are welcome!

## License
This project is licensed under the MIT License.  
Copyright © 2025 Prashant Kumar Mahskey

