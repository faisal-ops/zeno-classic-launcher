# Project-specific R8 rules live here. The default optimized Android rules are
# enabled from build.gradle.kts; keep this file intentionally small so release
# shrinking failures stay visible during CI/release builds.

# Strip Log.d/v (and their formatted args) from release builds — several call sites log internal
# diagnostic detail (e.g. lock-screen auto-unlock PIN-scan timing) that has no business existing
# in a shipped build. Log.i/w/e are left in place (worth keeping for real user-facing failures).
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}
