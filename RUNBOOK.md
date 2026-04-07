# Android Runbook

## Purpose
Use this runbook to build, run, and verify the Android app for the Stock Assistant POC.

## Dependencies
- Android Studio or Gradle
- local backend on port `8000`
- local assistant server on port `3000`

## Local Config
Create or update `local.properties` with:
```properties
sdk.dir=/Users/<you>/Library/Android/sdk
dev.host=192.168.x.x
dev.backend.port=8000
dev.assistant.port=3000
```

Notes:
- `dev.host` should be your current laptop IP for physical-device testing
- emulator fallback is `10.0.2.2` when these values are not set

## Build Check
```bash
./gradlew :app:compileDebugKotlin
```

## Run Flow
1. start backend repo services first
2. install or run the Android app
3. verify Home loads summary cards and inventory
4. test assistant prompts
5. test Transfer Assist

## Core Test Prompts
- `What can you do?`
- `How does Transfer Assist work?`
- `Suggest products for PFS 204`
- `What should I transfer to staff canteen?`
- `Show low stock in store 103`
- `Open transfer assist for canteen`

## Expected UX
- recommendation questions stay on Home with a short reply
- explicit open/transfer intents can navigate to Transfer Assist
- destination field is editable and suggestion-based
- Transfer Assist should scroll fully to the submit button
- voice input should live in the top bar and not cover content

## Troubleshooting
- app cannot reach services
  - verify `dev.host` in `local.properties`
  - verify ports `8000` and `3000`
- assistant behaves like an old version
  - restart the backend assistant server on `3000`
- recommendations are empty
  - use demo routes with seeded history such as PFS `204` or canteen `301`

## POC Guardrails
- keep UI guided and understandable for a new colleague
- keep the assistant concise
- avoid feature sprawl and production-only complexity
