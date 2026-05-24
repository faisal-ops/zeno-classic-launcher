# Zeno Classic Launcher — Copilot Workflow Instructions

These rules apply to all AI coding assistants working in this repo.

## MANDATORY: After Every Code Change

1. **Full release build** — always `assembleRelease`, never partial builds:
   ```bash
   export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
   ./gradlew assembleRelease --no-daemon
   ```

2. **Install on device**:
   ```bash
   adb install -r "app/build/outputs/apk/release/Zeno Classic.apk"
   ```
   Or use `./scripts/build-install.sh` (does both).

3. **Ask user to test** — say "App installed. Please test it — does everything work correctly?" and wait.

4. **Commit ONLY after user confirms** it works:
   ```bash
   git add <specific changed files>
   git commit -m "description"
   ```

5. **Push policy** — check `git rev-list --count @{u}..HEAD`:
   - < 5 commits ahead: keep local
   - ≥ 5 commits ahead: remind user to push to remote

## NEVER

- Never use git worktrees — always work directly in the repo
- Never run partial builds (`compileReleaseKotlin`, `assembleDebug`) for device testing
- Never commit before user confirms the build works on device
- Never use `git add .` or `git add -A`
- Never force-push to `main`

## Build Reference

| | |
|---|---|
| Build command | `./gradlew assembleRelease --no-daemon` |
| JAVA_HOME | `/Applications/Android Studio.app/Contents/jbr/Contents/Home` |
| APK path | `app/build/outputs/apk/release/Zeno Classic.apk` |
| Signing | Requires `key.properties` at repo root |
