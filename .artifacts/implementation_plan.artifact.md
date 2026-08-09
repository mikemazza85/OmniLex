# Fix Kotlin Compile Daemon Connection Issue

The project is encountering a "Could not connect to Kotlin compile daemon" error during the build (specifically during `kapt` tasks). This is often caused by a mismatch between the JVM version used to run Gradle and the version required by the Kotlin compiler, or by insufficient memory for the Kotlin daemon.

The research shows:
- The system `java` version is **1.8**.
- The project is configured for **Java 21** (`jvmTarget = "21"`).
- A compatible JDK 21 is available in the Android Studio installation directory: `C:\Program Files\Android\Android Studio\jbr`.

## Proposed Changes

### [Component Name] Gradle Configuration

#### [MODIFY] [gradle.properties](file:///C:/Users/Mikem/Documents/Codex/2026-07-23/referenced-chatgpt-conversation-this-is-untrusted/gradle.properties)
- Explicitly set `org.gradle.java.home` to point to the JDK 21 found in the Android Studio JBR folder.
- Increase `org.gradle.jvmargs` to allow more heap space for Gradle.
- Add `kotlin.daemon.jvm.options` to provide more memory specifically for the Kotlin compiler daemon.

## Verification Plan

### Automated Tests
1. Run `./gradlew --stop` to kill any stale daemons.
2. Run the failing task: `./gradlew :app:kaptGenerateStubsDebugKotlin --info` and verify it completes successfully.
3. Run a full build: `./gradlew assembleDebug`.

### Manual Verification
- Confirm that Android Studio can still sync and build the project.
