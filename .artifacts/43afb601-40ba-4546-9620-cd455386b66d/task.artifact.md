# Build Fix: Kapt to KSP Migration Task List

- `[x]` **1. Build Engine Configuration**
    - `[x]` Update top-level `build.gradle.kts` with KSP plugin.
    - `[x]` Update `app/build.gradle.kts` to apply KSP and update dependencies.
    - `[x]` Update Room to `2.8.4` and enable `ksp.useKSP2=true`.
- `[x]` **2. Verification**
    - `[x]` Run `gradle_build("app:assembleDebug")`.
    - `[x]` Verify application launch on device.
    - `[ ]` Run `gradle_build("app:assembleDebug")`.
    - `[ ]` Verify application launch on device.
