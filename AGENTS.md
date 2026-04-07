# Stock Assistant Android Context

## Purpose
This app is the Android front end for the Stock Assistant POC. The goal is to demonstrate an AI-assisted retail workflow, not a production-ready mobile platform.

## Product Direction
- Home screen acts as an operations copilot.
- Transfer Assist is the structured workflow for transfer planning and submission.
- AI should help explain, guide, and summarize.
- Deterministic logic should remain the source of truth for recommendations.

## Current UX Rules
- Recommendation questions stay on the Home assistant screen.
- Only explicit transfer/open intents should navigate to Transfer Assist.
- Transfer Assist should support both AI-recommended and manually added items.
- UI should feel guided and demo-friendly, not like a raw debug panel.

## Important Files
- `app/src/main/java/com/example/stockassistantmcpdemo/MainActivity.kt`
  Controls top-level navigation between Home and Transfer Assist.
- `app/src/main/java/com/example/stockassistantmcpdemo/assistant/AssistantScreen.kt`
  Hosts the assistant shell and top-bar actions.
- `app/src/main/java/com/example/stockassistantmcpdemo/assistant/FullScreenChat.kt`
  Main Home screen experience: hero, briefing, recommendations, inventory, conversation.
- `app/src/main/java/com/example/stockassistantmcpdemo/assistant/AssistantViewModel.kt`
  Sends chat requests and handles structured handoff into Transfer Assist.
- `app/src/main/java/com/example/stockassistantmcpdemo/assistant/StockViewModel.kt`
  Pulls inventory overview and recommendation highlights for the Home experience.
- `app/src/main/java/com/example/stockassistantmcpdemo/ui/TransferScreen.kt`
  Transfer planning screen.
- `app/src/main/java/com/example/stockassistantmcpdemo/ui/TransferViewModel.kt`
  Transfer Assist state, recommendations, manual additions, and submit flow.
- `app/src/main/java/com/example/stockassistantmcpdemo/data/NetworkModule.kt`
  Retrofit API models and backend base URL wiring.

## Local Networking
- Base URLs come from `BuildConfig`.
- `app/build.gradle.kts` reads these values from `local.properties` when present:
  - `dev.host`
  - `dev.backend.port`
  - `dev.assistant.port`
- Default host fallback is `10.0.2.2` for emulator-friendly development.
- `network_security_config.xml` allows local cleartext traffic for this POC.

## Example Prompts
- `What can you do?`
- `How does Transfer Assist work?`
- `Suggest products for PFS 204`
- `What should I transfer to staff canteen?`
- `Show low stock in store 103`
- `Where is Milk 1L?`
- `Open transfer assist for canteen`

## Design Notes
- Prefer clear, warm, operational language.
- Keep replies tight and useful.
- Avoid overlay UI elements that hide important content.
- Preserve strong hierarchy: summary first, actions second, details third.

## Git Hygiene
- Do not commit `.gradle/`, `.idea/`, `local.properties`, or build outputs.
- Local host/IP configuration should stay out of tracked source where possible.
