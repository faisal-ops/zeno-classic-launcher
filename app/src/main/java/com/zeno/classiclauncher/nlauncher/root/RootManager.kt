package com.zeno.classiclauncher.nlauncher.root

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.DataOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

// ── Result types ──────────────────────────────────────────────────────────────

enum class RootSignalId {
    MAGISK_ARTIFACTS,     // Magisk install dir, modules, Shamiko, TrickyStore, daemon socket
    KSU_ARTIFACTS,        // KernelSU install dir, /proc/kernelsu, /dev/ksu
    APATCH_ARTIFACTS,     // APatch install dir and device node
    LSPOSED_ARTIFACTS,    // LSPosed native file traces (lspd dir, lsposed.jar, rc file)
    ROOT_PROCESS_CAPS,    // /proc/<pid>/status: uid=0 + CapEff=ffffffffffffffff (magiskd)
    MAGISK_UNIX_SOCKET,   // /proc/net/unix: Magisk daemon socket (global kernel view)
    ZYGISK_MOUNT,         // /proc/self/mounts: zygisk tmpfs visible in our namespace
    SU_EXEC,              // su -c id returned uid=0 (most definitive, needs grant)
    SU_BINARY_ON_PATH,    // su binary on known filesystem paths
    BUSYBOX_FOUND,        // BusyBox binary found on filesystem or via su
    DEBUGGABLE_BUILD,     // ro.debuggable=1 via reflection (not patchable by DenyList)
}

data class RootSignal(
    val id: RootSignalId,
    val detected: Boolean,
    val label: String,
    val detail: String,
)

data class RootDetectionResult(
    val signals: List<RootSignal>,
) {
    // DEBUGGABLE_BUILD is a ROM property (always true on LineageOS userdebug), not a root signal.
    // Exclude it from isRooted so a freshly unrooted LineageOS device doesn't report rooted.
    val isRooted: Boolean get() = signals
        .filter { it.id != RootSignalId.DEBUGGABLE_BUILD }
        .any { it.detected }

    // High-confidence = process/socket/mount-level evidence that can't come from ROM alone.
    val highConfidence: Boolean get() = signals.filter {
        it.id !in setOf(RootSignalId.SU_BINARY_ON_PATH, RootSignalId.BUSYBOX_FOUND, RootSignalId.DEBUGGABLE_BUILD)
    }.any { it.detected }
}

// ── RootManager ───────────────────────────────────────────────────────────────

object RootManager {

    suspend fun detectRoot(): RootDetectionResult = coroutineScope {
        val magisk   = async { probeMagiskArtifacts() }
        val ksu      = async { probeKernelSU() }
        val apatch   = async { probeAPatch() }
        val lsposed  = async { probeLSPosed() }
        val caps     = async { probeRootProcessCaps() }
        val socket   = async { probeMagiskUnixSocket() }
        val zygisk   = async { probeZygiskMount() }
        val suExec   = async { probeSuExec() }
        val suBin    = async { probeSuBinary() }
        val busybox  = async { probeBusyBox() }
        val debug    = async { probeDebuggableBuild() }

        RootDetectionResult(
            signals = listOf(
                magisk.await(), ksu.await(), apatch.await(), lsposed.await(),
                caps.await(), socket.await(), zygisk.await(),
                suExec.await(), suBin.await(), busybox.await(), debug.await(),
            ),
        )
    }

    // ── Probe 1: Magisk artifacts ─────────────────────────────────────────────
    // Sources: FOSS-Root-Checker path list + our proc-level checks.
    // /data/adb is 0711 (world-execute) — stat() works for all processes.
    // DenyList only patches per-process mount namespaces, not global filesystem.

    private suspend fun probeMagiskArtifacts(): RootSignal = withContext(Dispatchers.IO) {
        val paths = listOf(
            "/data/adb/magisk",           // Core Magisk install
            "/data/adb/magisk.db",        // Magisk database
            "/data/adb/magisk.img",       // Legacy Magisk image
            "/data/adb/modules",          // Magisk modules directory
            "/data/adb/magisk/su",        // Magisk su binary
            "/data/adb/post-fs-data.d",   // Magisk early-mount scripts
            "/data/adb/service.d",        // Magisk service scripts
            "/data/adb/env",              // Magisk environment
            "/data/adb/shamiko",          // Shamiko root-hide module
            "/data/adb/tricky_store",     // TrickyStore integrity spoof
            "/data/adb/zygisk_next",      // ZygiskNext (for KSU/APatch)
            "/data/adb/riru",             // Riru legacy plugin framework
            "/sbin/.magisk/mirror",       // Legacy Magisk mirror mount
            "/dev/com.topjohnwu.magisk.daemon",  // Magisk daemon device socket
            "/cache/magisk.log",          // Magisk boot log
            "/data/resource-cache/magisk.apk",   // Cached Magisk APK
        )
        val found = paths.any { pathExists(it) }
        RootSignal(
            id = RootSignalId.MAGISK_ARTIFACTS,
            detected = found,
            label = "Magisk install artifacts",
            detail = "/data/adb/magisk*, modules, shamiko, tricky_store, daemon socket",
        )
    }

    // ── Probe 2: KernelSU artifacts ──────────────────────────────────────────
    // /proc/kernelsu and /dev/ksu are kernel-level — not patchable from userspace.

    private suspend fun probeKernelSU(): RootSignal = withContext(Dispatchers.IO) {
        val paths = listOf(
            "/data/adb/ksu",         // KernelSU install dir
            "/data/adb/ksu/bin/su",  // KernelSU su binary
            "/proc/kernelsu",        // KernelSU kernel proc entry
            "/dev/ksu",              // KernelSU device node
        )
        val found = paths.any { pathExists(it) }
        RootSignal(
            id = RootSignalId.KSU_ARTIFACTS,
            detected = found,
            label = "KernelSU artifacts",
            detail = "/data/adb/ksu, /proc/kernelsu, /dev/ksu — kernel-level root",
        )
    }

    // ── Probe 3: APatch artifacts ─────────────────────────────────────────────

    private suspend fun probeAPatch(): RootSignal = withContext(Dispatchers.IO) {
        val paths = listOf(
            "/data/adb/apatch",         // APatch core dir
            "/data/adb/apatch/bin/su",  // APatch su binary
            "/data/adb/ap/bin/su",      // Alternative APatch layout
            "/data/adb/ap/patch",       // APatch kernel patch
            "/dev/apatch",              // APatch device node
            "/sys/kernel/debug/tracing/su", // APatch kernel trace
        )
        val found = paths.any { pathExists(it) }
        RootSignal(
            id = RootSignalId.APATCH_ARTIFACTS,
            detected = found,
            label = "APatch artifacts",
            detail = "/data/adb/apatch, /dev/apatch — KernelPatch-based root",
        )
    }

    // ── Probe 4: LSPosed native file traces ───────────────────────────────────
    // Package-name check alone misses LSPosed installed as a hidden module.
    // These file paths come from FOSS-Root-Checker's SystemInfo check.

    private suspend fun probeLSPosed(): RootSignal = withContext(Dispatchers.IO) {
        val paths = listOf(
            "/data/adb/lspd",                          // LSPosed daemon dir
            "/system/framework/lsposed.jar",           // LSPosed framework jar
            "/system/etc/init/hw/init.lsposed.rc",     // LSPosed init script
            "/data/adb/lspd.db",                       // LSPosed database
        )
        // Also check /proc/net/unix for lspd socket
        val fileFsFound = paths.any { pathExists(it) }
        val socketFound = runCatching {
            File("/proc/net/unix").readLines().any { line ->
                line.contains("lspd", ignoreCase = true) ||
                    line.contains("lsposed", ignoreCase = true)
            }
        }.getOrDefault(false)
        RootSignal(
            id = RootSignalId.LSPOSED_ARTIFACTS,
            detected = fileFsFound || socketFound,
            label = "LSPosed framework traces",
            detail = "/data/adb/lspd, lsposed.jar, lspd socket — Zygisk hook framework",
        )
    }

    // ── Probe 5: Root process with full Linux capabilities ────────────────────
    // /proc/<pid>/status is world-readable. magiskd runs uid=0 with CapEff=full.
    // Shamiko can blank comm/cmdline but cannot forge CapEff without kernel mod.

    private suspend fun probeRootProcessCaps(): RootSignal = withContext(Dispatchers.IO) {
        val found = runCatching {
            File("/proc").listFiles()
                ?.filter { it.isDirectory && it.name.all(Char::isDigit) }
                ?.any { proc ->
                    runCatching {
                        val status = File(proc, "status").readText()
                        val hasFullCaps = status.contains("CapEff:\t0000003fffffffff") ||
                            status.contains("CapEff:\tffffffffffffffff")
                        val isRootUid = status.lines().any { l ->
                            l.startsWith("Uid:") && l.split("\t").getOrNull(1) == "0"
                        }
                        hasFullCaps && isRootUid
                    }.getOrDefault(false)
                } ?: false
        }.getOrDefault(false)
        RootSignal(
            id = RootSignalId.ROOT_PROCESS_CAPS,
            detected = found,
            label = "Root process (full capabilities)",
            detail = "/proc/<pid>/status — CapEff=ffffffffffffffff + uid=0 (magiskd)",
        )
    }

    // ── Probe 6: Magisk daemon Unix socket ────────────────────────────────────
    // /proc/net/unix is a global kernel view — not per mount namespace.
    // Shamiko blanks the socket path but the SOCK_STREAM uid=0 entry remains.

    private suspend fun probeMagiskUnixSocket(): RootSignal = withContext(Dispatchers.IO) {
        val found = runCatching {
            File("/proc/net/unix").readLines().drop(1).any { line ->
                val cols = line.trim().split("\\s+".toRegex())
                val path = cols.getOrNull(7) ?: ""
                // Only match sockets with an explicit magisk path — the empty-path fallback
                // (path.isEmpty && state==0001) fires on ANY abstract socket on any Android device.
                path.contains("magisk", ignoreCase = true)
            }
        }.getOrDefault(false)
        RootSignal(
            id = RootSignalId.MAGISK_UNIX_SOCKET,
            detected = found,
            label = "Magisk daemon socket",
            detail = "/proc/net/unix — global kernel view, survives Shamiko path blanking",
        )
    }

    // ── Probe 7: Zygisk mount in /proc/self/mounts ───────────────────────────
    // Zygisk injects a tmpfs into the zygote namespace.
    // Visible in our own /proc/self/mounts even with DenyList active.

    private suspend fun probeZygiskMount(): RootSignal = withContext(Dispatchers.IO) {
        val found = runCatching {
            File("/proc/self/mounts").readText().contains("zygisk", ignoreCase = true)
        }.getOrDefault(false)
        RootSignal(
            id = RootSignalId.ZYGISK_MOUNT,
            detected = found,
            label = "Zygisk mount",
            detail = "/proc/self/mounts — Zygisk tmpfs visible in our own process namespace",
        )
    }

    // ── Probe 8: su -c id execution ───────────────────────────────────────────
    // Most definitive test. Fails if app is on Magisk DenyList or su not granted.

    private suspend fun probeSuExec(): RootSignal {
        val detected = withTimeoutOrNull(5_000L) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val p = ProcessBuilder("su", "-c", "id")
                        .redirectErrorStream(true)
                        .start()
                    val out = p.inputStream.bufferedReader().readText()
                    p.waitFor(4, TimeUnit.SECONDS)
                    out.contains("uid=0")
                }.getOrDefault(false)
            }
        } ?: false
        return RootSignal(
            id = RootSignalId.SU_EXEC,
            detected = detected,
            label = "su exec (uid=0)",
            detail = "Requires Magisk SuperUser grant — go to Magisk → SuperUser → Allow",
        )
    }

    // ── Probe 9: su binary on filesystem ─────────────────────────────────────
    // Comprehensive list from FOSS-Root-Checker.
    // Will always fail on Magisk DenyList (mount namespace hides su paths).
    // Uses both File.exists() and ls exec fallback for maximum coverage.
    //
    // NOTE: /system/bin/su and /system/xbin/su are ROM-shipped on LineageOS userdebug builds.
    // On userdebug ROMs we skip those system paths to avoid a permanent false positive — only
    // non-system paths (data, sbin, dev) indicate a third-party su install.

    private suspend fun probeSuBinary(): RootSignal = withContext(Dispatchers.IO) {
        val isUserdebug = runCatching {
            val cls = Class.forName("android.os.SystemProperties")
            val get = cls.getMethod("get", String::class.java, String::class.java)
            val buildType = get.invoke(null, "ro.build.type", "user") as String
            buildType == "userdebug" || buildType == "eng"
        }.getOrDefault(false)

        // Paths that LineageOS / AOSP userdebug ships as stock ROM files — not root artifacts.
        val romSuPaths = setOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/system/sbin/su",
        )

        val suPaths = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/system/sd/xbin/su", "/system/bin/failsafe/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/data/local/su",
            "/su/bin/su", "/system/sbin/su",
            "/system/usr/we-need-root/su-backup",
            "/system/xbin/mu",            // SuperUser variant
            "/system/bin/.ext/.su",
            "/system/app/Superuser.apk",
            "/system/app/SuperSU",
            "/system/etc/init.d/99SuperSUDaemon",
            "/dev/com.koushikdutta.superuser.daemon",
            "/data/data/com.noshufou.android.su",
        )

        val pathsToCheck = if (isUserdebug) suPaths.filter { it !in romSuPaths } else suPaths
        val found = pathsToCheck.any { pathExists(it) }
        RootSignal(
            id = RootSignalId.SU_BINARY_ON_PATH,
            detected = found,
            label = "su binary on filesystem",
            detail = "Hidden by Magisk DenyList. Detects SuperSU, legacy roots, unmasked Magisk.",
        )
    }

    // ── Probe 10: BusyBox ────────────────────────────────────────────────────
    // Modern root frameworks (Magisk/KSU/APatch) bundle busybox in /data/adb.
    // Also check via su shell in case the binary is hidden from untrusted_app.

    private suspend fun probeBusyBox(): RootSignal = withContext(Dispatchers.IO) {
        val busyboxPaths = listOf(
            "/system/xbin/busybox", "/system/bin/busybox",
            "/vendor/bin/busybox", "/sbin/busybox",
            "/data/local/busybox", "/data/local/xbin/busybox",
            "/data/adb/magisk/busybox",   // Magisk bundled busybox
            "/data/adb/ksu/bin/busybox",  // KernelSU bundled busybox
            "/data/adb/ap/bin/busybox",   // APatch bundled busybox
        )
        val fsFound = busyboxPaths.any { pathExists(it) }

        // Fallback: ask su shell for the path (works when binary is visible to root but not to us)
        val suFound = if (!fsFound) {
            withTimeoutOrNull(4_000L) {
                runCatching {
                    val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "which busybox"))
                    val out = p.inputStream.bufferedReader().readLine()?.trim() ?: ""
                    p.waitFor(3, TimeUnit.SECONDS)
                    out.isNotEmpty() && out.startsWith("/")
                }.getOrDefault(false)
            } ?: false
        } else false

        RootSignal(
            id = RootSignalId.BUSYBOX_FOUND,
            detected = fsFound || suFound,
            label = "BusyBox binary",
            detail = "BusyBox found on filesystem or via `su -c which busybox`",
        )
    }

    // ── Probe 11: ro.debuggable via reflection ────────────────────────────────
    // Magisk can hook Java-layer SystemProperties.get() but we call via reflection
    // to the native layer, bypassing that hook.

    private suspend fun probeDebuggableBuild(): RootSignal = withContext(Dispatchers.IO) {
        val detected = runCatching {
            val cls = Class.forName("android.os.SystemProperties")
            val get = cls.getMethod("get", String::class.java, String::class.java)
            val debuggable = get.invoke(null, "ro.debuggable", "0") as String
            val buildType = get.invoke(null, "ro.build.type", "user") as String
            debuggable == "1" || buildType == "userdebug" || buildType == "eng"
        }.getOrDefault(false)
        RootSignal(
            id = RootSignalId.DEBUGGABLE_BUILD,
            detected = detected,
            label = "Debuggable/userdebug build (ROM info only)",
            detail = "ro.debuggable=1 or ro.build.type=userdebug — permanent on LineageOS; does not indicate root is installed",
        )
    }

    // ── Shared path-existence helper ──────────────────────────────────────────
    // Primary: File.exists() (fast, no subprocess).
    // Fallback: `ls <path>` exec — can bypass SELinux restrictions on stat()
    // that block untrusted_app from accessing certain directories, because the
    // child process may receive different SELinux policy treatment.

    private fun pathExists(path: String): Boolean {
        if (runCatching { File(path).exists() }.getOrDefault(false)) return true
        return runCatching {
            Runtime.getRuntime().exec(arrayOf("ls", path)).waitFor() == 0
        }.getOrDefault(false)
    }

    // ── Root type detection ───────────────────────────────────────────────────

    enum class RootType { MAGISK, KERNELSU, APATCH, UNKNOWN }

    fun detectRootType(): RootType = when {
        pathExists("/data/adb/magisk") || pathExists("/data/adb/magisk.db") -> RootType.MAGISK
        pathExists("/data/adb/ksu") || pathExists("/proc/kernelsu") || pathExists("/dev/ksu") -> RootType.KERNELSU
        pathExists("/data/adb/apatch") || pathExists("/dev/apatch") -> RootType.APATCH
        else -> RootType.UNKNOWN
    }

    // ── Revoke SuperUser grant from the root manager ──────────────────────────
    // Returns true if the root manager entry was successfully cleared.
    // Magisk: delete from policies table via magisk --sqlite
    // KernelSU: delete from app profile DB via ksud or direct sqlite3
    // APatch: best-effort via apd CLI

    suspend fun revokeRootManagerGrant(pkg: String): Boolean = withContext(Dispatchers.IO) {
        when (detectRootType()) {
            RootType.MAGISK -> runCatching {
                // Write command via stdin to avoid shell quoting issues with Runtime.exec()
                val cmd = "DELETE FROM policies WHERE package_name='$pkg'"
                val via1 = execute("magisk --sqlite \"$cmd\"")
                // Fallback: direct sqlite3 on the Magisk database file
                val via2 = execute("sqlite3 /data/adb/magisk.db \"$cmd\"")
                via1 || via2
            }.getOrDefault(false)

            RootType.KERNELSU -> runCatching {
                // KSU stores policies keyed by UID, not package name.
                // Get the app UID first, then delete that UID's policy row.
                val uidProc = Runtime.getRuntime().exec(
                    arrayOf("su", "-c", "stat -c %u /data/data/$pkg 2>/dev/null || dumpsys package $pkg | grep userId | head -1 | grep -o '[0-9]*'")
                )
                val uid = uidProc.inputStream.bufferedReader().readLine()?.trim()?.toIntOrNull()
                uidProc.waitFor(4, TimeUnit.SECONDS)

                if (uid != null && uid > 0) {
                    // Try ksud profile CLI first (KSU >= 0.9.x)
                    val ksudProc = Runtime.getRuntime().exec(
                        arrayOf("su", "-c", "ksud profile set-app-profile --uid $uid --allow false 2>/dev/null; echo done")
                    )
                    ksudProc.waitFor(4, TimeUnit.SECONDS)

                    // Also try direct sqlite3 on known KSU DB paths (works on older KSU)
                    val ksuDbPaths = listOf(
                        "/data/adb/ksu/db/sudb.db",
                        "/data/adb/ksud_db",
                        "/data/adb/ksu/sudb.db",
                    )
                    ksuDbPaths.forEach { db ->
                        runCatching {
                            Runtime.getRuntime().exec(
                                arrayOf("su", "-c", "sqlite3 $db \"UPDATE uid_policy SET allow=0 WHERE uid=$uid\" 2>/dev/null")
                            ).waitFor(3, TimeUnit.SECONDS)
                        }
                    }
                    true
                } else false
            }.getOrDefault(false)

            RootType.APATCH -> runCatching {
                val p = Runtime.getRuntime().exec(
                    arrayOf("su", "-c", "apd superuser revoke $pkg 2>/dev/null; echo done")
                )
                p.waitFor(4, TimeUnit.SECONDS) == true
            }.getOrDefault(false)

            RootType.UNKNOWN -> false
        }
    }

    // ── Grant / execute ───────────────────────────────────────────────────────

    suspend fun requestRootGrant(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val process = Runtime.getRuntime().exec("su")
            DataOutputStream(process.outputStream).use { os ->
                os.writeBytes("id\n")
                os.writeBytes("exit\n")
                os.flush()
            }
            val exitCode = process.waitFor()
            val output = process.inputStream.bufferedReader().readText()
            exitCode == 0 && output.contains("uid=0")
        }.getOrDefault(false)
    }

    suspend fun execute(vararg commands: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val process = Runtime.getRuntime().exec("su")
            DataOutputStream(process.outputStream).use { os ->
                for (cmd in commands) { os.writeBytes("$cmd\n") }
                os.writeBytes("exit\n")
                os.flush()
            }
            process.waitFor() == 0
        }.getOrDefault(false)
    }

    suspend fun readLine(command: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val process = Runtime.getRuntime().exec("su")
            DataOutputStream(process.outputStream).use { os ->
                os.writeBytes("$command\n")
                os.writeBytes("exit\n")
                os.flush()
            }
            val output = process.inputStream.bufferedReader().readLine()
            process.waitFor()
            output?.trim()
        }.getOrNull()
    }

    // Returns "ssid|securityType|passphrase" from saved WifiConfigStoreSoftAp.xml, or null.
    suspend fun readHotspotConfig(): Triple<String, String, String>? = withContext(Dispatchers.IO) {
        val raw = readLine(
            "F=/data/misc/apexdata/com.android.wifi/WifiConfigStoreSoftAp.xml;" +
            "S=\$(grep -oE '[^>]+</string>' \$F | head -1 | sed 's/<\\/string>//;s/\"//g;s/&quot;//g');" +
            "T=\$(grep -oE 'SecurityType\" value=\"[0-9]+' \$F | grep -oE '[0-9]+\$');" +
            "P=\$(grep -oE 'Passphrase\">[^<]+' \$F | sed 's/Passphrase\">//');" +
            "printf '%s|%s|%s' \"\$S\" \"\$T\" \"\$P\""
        ) ?: return@withContext null
        val parts = raw.split("|")
        val ssid = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return@withContext null
        val secType = parts.getOrNull(1)?.trim() ?: "2"
        val pass = parts.getOrNull(2) ?: ""
        val secStr = when (secType) {
            "0" -> "open"
            "3" -> "wpa3"
            "4" -> "wpa3_transition"
            "5" -> "owe"
            "6" -> "owe_transition"
            else -> "wpa2"
        }
        Triple(ssid, secStr, pass)
    }
}
