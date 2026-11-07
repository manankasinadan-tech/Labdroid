package com.example.data.service

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.example.data.model.SystemStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object RootService {

    suspend fun getSystemStatus(context: Context): SystemStatus = withContext(Dispatchers.IO) {
        val rootInfo = checkRootAccess()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        val totalRamMb = (memoryInfo.totalMem / (1024 * 1024))
        val availRamMb = (memoryInfo.availMem / (1024 * 1024))

        val internalFreeMb = try {
            val stat = StatFs(context.filesDir.absolutePath)
            (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024)
        } catch (_: Exception) {
            0L
        }

        val deviceName = if (Build.MANUFACTURER.equals("unknown", ignoreCase = true)) {
            Build.MODEL
        } else {
            "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} - ${Build.DEVICE}"
        }

        val osName = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        val cpuArch = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"

        SystemStatus(
            rootPermissions = rootInfo.first,
            suBinaryPath = rootInfo.second,
            deviceModel = deviceName,
            operatingSystem = osName,
            cpuAbi = cpuArch,
            daemonRunning = true,
            totalRamMb = totalRamMb,
            availableRamMb = availRamMb,
            internalStorageFreeMb = internalFreeMb
        )
    }

    private fun checkRootAccess(): Pair<Boolean, String?> {
        val commonSuPaths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su",
            "/su/bin/su"
        )

        for (path in commonSuPaths) {
            val file = File(path)
            if (file.exists()) {
                return Pair(true, path)
            }
        }

        // Try executing su command check
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            process.waitFor()
            if (line != null && line.contains("uid=0")) {
                Pair(true, "su (active)")
            } else {
                Pair(false, null)
            }
        } catch (_: Exception) {
            Pair(false, null)
        }
    }

    suspend fun executeCommand(
        command: String,
        useRoot: Boolean = true,
        workingDir: File? = null,
        onOutputLine: (String) -> Unit = {}
    ): Int = withContext(Dispatchers.IO) {
        val cmdArray = if (useRoot) {
            arrayOf("su", "-c", command)
        } else {
            arrayOf("sh", "-c", command)
        }

        try {
            val processBuilder = ProcessBuilder(*cmdArray)
            workingDir?.let { processBuilder.directory(it) }
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { onOutputLine(it) }
            }
            process.waitFor()
        } catch (e: Exception) {
            onOutputLine("Execution error: ${e.message}")
            -1
        }
    }
}
