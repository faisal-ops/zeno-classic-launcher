# Zeno Classic Launcher — AI Agent Workflow Rules

These rules apply to ALL AI coding agents (Claude Code, OpenAI Codex, Gemini CLI, etc.).
Follow them exactly. Do not skip or shortcut any step.

> See CLAUDE.md for the full rules. This file is an alias for tools that read AGENTS.md.

---

## MANDATORY: After Every Code Change

1. **Build full release APK** — always `assembleRelease`, never partial builds:
   ```bash
   export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
   ./gradlew assembleRelease --no-daemon
   ```

2. **Install on device**:
   ```bash
   adb install -r "app/build/outputs/apk/release/Zeno Classic.apk"
   ```
   Or use `./scripts/build-install.sh` (does both).

3. **Ask user to test** — wait for response before doing anything else.

4. **Commit locally ONLY after user confirms it works**:
   ```bash
   git add <specific changed files>
   git commit -m "description"
   ```

5. **Push policy**:
   - < 5 commits ahead of remote → stay local
   - ≥ 5 commits ahead → remind user to push, push only on their confirmation

## NEVER

- Never use git worktrees. Always work directly in the repo.
- Never commit before building and installing on device.
- Never commit before user confirms the build works.
- Never run partial builds (`compileReleaseKotlin`, `assembleDebug`) for device testing.
- Never use `git add .` or `git add -A`.
- Never force-push to `main`.
