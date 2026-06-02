# Zeno Classic Launcher — AI Workflow Rules

These rules apply to ALL AI tools (Claude, Codex, Gemini, etc.).
Follow them exactly. Do not skip or shortcut any step.

---

## 1. NEVER USE WORKTREES

- Never use `git worktree`, `isolation: worktree`, or any worktree flag.
- Always work directly in this repository directory.
- All file edits, builds, and commits happen in the main working tree.

---

## 2. AFTER EVERY CODE CHANGE — MANDATORY WORKFLOW

Complete ALL steps in order. Never skip ahead.

### Step 1 — Full Release Build

Always run the FULL `assembleRelease`. Never run partial builds
(`compileReleaseKotlin`, `assembleDebug`, etc.) when testing on device.

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleRelease --no-daemon
```

The signed APK is at: `app/build/outputs/apk/release/Zeno Classic.apk`

### Step 2 — Install on Device

```bash
adb install -r "app/build/outputs/apk/release/Zeno Classic.apk"
```

Or use the helper script (does both steps):
```bash
./scripts/build-install.sh
```

### Step 3 — Ask User to Test

After install succeeds, say exactly:
> "App installed. Please test it on the device — does everything work correctly?"

Wait for the user's response. Do NOT commit yet.

### Step 4 — Commit Only After User Confirms

Only if the user says it works (yes / looks good / confirmed / etc.):

```bash
git add <specific files that changed>
git commit -m "Short description of what changed"
```

Never use `git add .` or `git add -A` — add only the files that were changed.

If the user says it does NOT work, fix the issue and repeat from Step 1.

### Step 5 — Check Push Reminder

After every commit, check how many commits are ahead of remote:

```bash
git rev-list --count @{u}..HEAD 2>/dev/null
```

- **< 5 commits ahead**: Stay local. Do not push.
- **≥ 5 commits ahead**: Remind the user:
  > "You now have N commits ahead of origin/main. Would you like me to push them to remote?"
  Only push if the user confirms.

---

## 3. BUILD RULES

| Rule | Detail |
|------|--------|
| Always use | `./gradlew assembleRelease --no-daemon` |
| Never use for device testing | `assembleDebug`, `compileReleaseKotlin`, or any partial task |
| JAVA_HOME | `/Applications/Android Studio.app/Contents/jbr/Contents/Home` |
| Signed APK location | `app/build/outputs/apk/release/Zeno Classic.apk` |

---

## 4. GIT RULES

- Never commit without first building and installing on device.
- Never commit without user testing confirmation.
- Never force-push to `main`.
- Never skip hooks (`--no-verify`).
- Commit message format: short imperative sentence (e.g. "Fix dock badge count on restart").

---

## 5. ANDROID VERSION COMPATIBILITY — MANDATORY CHECK

Every code change must be mentally verified against both target devices before committing:

| Device | Android | API | ROM |
|--------|---------|-----|-----|
| Primary | Android 14 | API 34 | Stock |
| Secondary | Android 16 | API 36 | LineageOS |

### Checklist for every change

Before building, run through these checks:

**API level guards**
- Any new Android SDK call → check its `minSdk`. If > 26, wrap with `if (Build.VERSION.SDK_INT >= ...)`.
- Any call using API 35+ → note it's unavailable at `compileSdk = 34`. Use reflection or skip.

**ROM-specific behaviour**
- Settings.Secure/Global keys (e.g. `bedtime_mode`, `zen_mode`) → LineageOS may not have them. Always pass a default value: `Settings.Secure.getInt(cr, "key", 0)`.
- Icon shapes on LineageOS: handled via `AppIconShape` — do not hardcode shape paths.
- Package manager queries on LineageOS may return different results — always null-check.

**AccessibilityService / lock screen**
- `GLOBAL_ACTION_LOCK_SCREEN` requires API 28+ → already guarded.
- `TYPE_KEYGUARD` (value 5) is API 35+ → always fall back to `TYPE_SYSTEM` for API 34.
- Background `startActivity` from a Service: only safe in response to user events (unlock, screen-on). Always wrap in `runCatching`.

**Compose**
- `PlatformTextStyle`, `LineHeightStyle` — Compose library, not OS API. Safe on both.
- No OS-version-specific Compose APIs in use.

**Quick commands to spot issues**
```bash
# Search for unguarded SDK calls in changed files
grep -n "Build.VERSION\|SDK_INT" <changed files>

# Verify no API 35+ symbols slipped in (compileSdk = 34 catches this at build time)
./gradlew assembleRelease --no-daemon
```

If a change only makes sense on one Android version, document why in the commit message.

---

## 6. QUICK REFERENCE

```
# Full workflow in one shot:
./scripts/build-install.sh          # build + install
# → ask user to test
# → on confirmation:
git add <files>
git commit -m "message"
git rev-list --count @{u}..HEAD     # check if ≥5, remind to push
```
