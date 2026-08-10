# Fix Build Errors: Migrate Kapt to KSP & Resolve JVM Signature Mismatch

Following the upgrade to Gradle 9.5 and AGP 9.3.1, the standard `kotlin-kapt` plugin is experiencing conflicts. Furthermore, Kotlin 2.2.10 with KSP2 introduces a JVM signature mismatch in Room (`unexpected jvm signature V`). This plan migrates to KSP and upgrades Room to a version that supports Kotlin 2.2.10.

## User Review Required

> [!IMPORTANT]
> **Room Upgrade**: To fix the `unexpected jvm signature V` error, Room must be upgraded to `2.8.4`. This version includes the necessary fixes for Kotlin 2.2.10 and KSP2.

## Proposed Changes

### 1. Build Configuration (Build Engine)

#### [MODIFY] [build.gradle.kts](file:///C:/Users/Mikem/Documents/Codex/2026-07-23/OmniLex Phase 1/build.gradle.kts)
- Add the KSP plugin declaration with version `2.2.10-2.0.2`.

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/Mikem/Documents/Codex/2026-07-23/OmniLex Phase 1/app/build.gradle.kts)
- Apply the `com.google.devtools.ksp` plugin.
- Replace `kapt` with `ksp`.
- Update Room dependencies to `2.8.4`.

#### [MODIFY] [gradle.properties](file:///C:/Users/Mikem/Documents/Codex/2026-07-23/OmniLex Phase 1/gradle.properties)
- Add `ksp.useKSP2=true` to ensure KSP2 is used with Kotlin 2.x.

## Verification Plan

### Automated Tests
- Run `app:assembleDebug` to verify that the build completes successfully.
- Verify that Room schemas are generated in the `$projectDir/schemas` directory.

### Manual Verification
- Launch the app to ensure the database initializes correctly and the Phase 2 FTS search still works.
