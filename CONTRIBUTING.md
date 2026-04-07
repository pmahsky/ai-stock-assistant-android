# Contributing

## Working Style
- Treat this app as a polished POC.
- Optimize for clarity, demo value, and maintainability.
- Keep the assistant helpful for both operators and a new colleague learning the workflow.

## Before Editing
- read [AGENTS.md](/Users/prashantmahskey/Documents/StockAssistant/ai-stock-assistant-android/AGENTS.md)
- read [RUNBOOK.md](/Users/prashantmahskey/Documents/StockAssistant/ai-stock-assistant-android/RUNBOOK.md)
- confirm whether the change belongs in Android, backend, or both repos

## UX Rules
- Home screen is the operations copilot
- Transfer Assist is the structured execution screen
- recommendation questions should stay on Home
- explicit open/transfer intents may navigate into Transfer Assist
- avoid overlay controls that cover important content

## Design Rules
- keep hierarchy strong: summary first, action second, detail third
- keep labels concise
- keep responses mobile-friendly
- maintain the current warm retail-operations visual direction unless there is a strong reason to change it

## Technical Rules
- local host/IP values should come from `local.properties` through `BuildConfig`
- do not hardcode your current LAN IP in tracked source
- keep Retrofit/Ktor models aligned with backend responses
- when changing assistant behavior, verify the Home flow and Transfer Assist handoff together

## Verification
Run before handing off:
```bash
./gradlew :app:compileDebugKotlin
```

## Git Hygiene
- do not commit `.gradle/`, `.idea/`, `local.properties`, or build outputs
- keep new docs updated when the workflow or setup changes
