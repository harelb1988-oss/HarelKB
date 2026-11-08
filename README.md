# HarelKB – Android IME

Kotlin Android keyboard starter project.

## Current scope
- Real `InputMethodService` (not HTML wrapper), package `com.harelkb.keyboard`.
- Hebrew/English key layout, prediction row, toolbar, private/hybrid toggle.
- Local fallback engine and configurable remote chat-completions adapter.
- Ask sheet with Insert action.
- `wa:` contact lookup via Android Contacts Provider and WhatsApp `wa.me` deep link.
- Launch intents for Teams, Outlook and OneDrive.
- GitHub Actions debug APK build.

## Build
Requirements: JDK 17, Android SDK, Gradle 8.7.

```bash
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub Actions
Push this folder to GitHub. The workflow builds a debug APK and uploads it as an artifact.
Optional repository secrets:
- `AI_ENDPOINT`: endpoint for a provider you are authorized to use, compatible with the chat-completions request format.
- `AI_API_KEY`: corresponding API key.

These secrets are injected as Gradle project properties. Do not commit API keys.

## AI provider note
`MetaAIEngine` is an adapter with the requested class name and system prompt. It does not connect to the consumer Meta AI app and does not claim an official Meta AI API exists. It sends requests only to the explicitly configured endpoint. With no endpoint/key, or on request failure, callers can use the local engine. Private mode in the keyboard routes Ask requests to local processing only.

## Privacy
- Contacts are queried only when `wa:` is invoked and `READ_CONTACTS` is granted.
- No contacts are uploaded.
- Private mode is local-only for the implemented Ask action.
- This starter does not integrate Google services other than the Android Contacts Provider permission requested by the spec.
- Internet permission is present for the optional configured AI endpoint.

## Commands
- `wa:<contact> <message>` – contact lookup and WhatsApp deep link.
- `ts:` – open Teams.
- `em:` – open Outlook.
- `od:` – open OneDrive.
- `cal:` – opens Outlook as the Microsoft calendar entry point; a dedicated Microsoft Calendar app package is not assumed.

## Important limitations before production
This is a buildable starter, not a certified production keyboard and cannot honestly be described as “0 bugs” without compiling/testing on target devices.
- Verify installed package IDs for Microsoft apps on target devices; package IDs can vary by app version/region.
- `cal:` currently opens Outlook because Microsoft Calendar is commonly surfaced through Outlook; no separate Microsoft Calendar Android package is assumed.
- The current `handleCommand` dispatcher is exposed for command integration, but a production command-entry UI/parser still needs to be wired to an explicit command mode.
- Voice input is a placeholder; it does not record audio or invoke speech recognition.
- Numeric/symbol keyboard page, emoji picker, clipboard history, rich language switching, autocorrect, gesture typing, and full suggestion insertion behavior need further implementation.
- IME keyboard height is requested at approximately 40% of display height; Android/OEM window insets and device behavior may affect actual visible height.
- The provided key order follows the requested rows literally; review the physical Hebrew layout before release.
- Remote AI responses depend on the configured provider's schema, network, model and terms.
- For release, add automated unit/instrumentation tests, signing configuration, accessibility review, Play policy review, and device QA.
