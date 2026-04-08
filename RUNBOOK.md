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

## Recommendation Flow
Transfer Assist shows local AI picks built from seeded transfer history.

### User Flow
1. Pick the source store.
2. Pick `PFS` or `CANTEEN`.
3. Choose the destination store.
4. Tap `Refresh AI Picks`.
5. Review the cards and add the useful ones to the transfer list.

### What the App Requests
The Android app calls:

```text
GET /transfer_recommendations?from_store=<source>&to_store=<destination>&transfer_type=<type>
```

### What the UI Shows
Each recommendation card includes:
- product name
- confidence badge
- suggested quantity
- score
- how many times it moved
- a short reason

### How to Read It
Example:

```text
Milk 1L
High
Suggested qty 18 • score 0.87 • moved 5 times
frequently transferred recently
```

Meaning:
- `High` = strong repeat pattern
- `Suggested qty 18` = starting quantity based on past transfers
- `score 0.87` = internal strength from `0` to `1`
- `moved 5 times` = this route repeated 5 times in the last 30 days
- `frequently transferred recently` = simple explanation generated from the data

### Keep in Mind
- the recommendation engine is local and deterministic
- AI may be used by the assistant for conversation and guidance
- the UI label `AI picks` means assistant-driven recommendations, not a trained ML model

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
