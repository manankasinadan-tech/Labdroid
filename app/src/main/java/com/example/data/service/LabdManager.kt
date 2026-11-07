package com.example.data.service

import android.content.Context
import com.example.data.model.LabdAction
import com.example.data.model.LabdManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object LabdManager {

    private val _installedExtensions = MutableStateFlow<List<LabdManifest>>(emptyList())
    val installedExtensions: StateFlow<List<LabdManifest>> = _installedExtensions.asStateFlow()

    fun getExtensionsDir(context: Context): File {
        val direct = File("/data/data/com.labdroid.kitchen/extensions")
        return if (direct.exists() || direct.mkdirs()) {
            direct
        } else {
            File(context.filesDir, "extensions").apply { mkdirs() }
        }
    }

    fun getBinDir(context: Context): File {
        val direct = File("/data/data/com.labdroid.kitchen/bin")
        return if (direct.exists() || direct.mkdirs()) {
            direct
        } else {
            File(context.filesDir, "bin").apply { mkdirs() }
        }
    }

    suspend fun refreshExtensions(context: Context) = withContext(Dispatchers.IO) {
        val extDir = getExtensionsDir(context)
        val binDir = getBinDir(context)

        // Ensure default skeleton extension exists
        val skeletonDir = File(extDir, "skeleton_module")
        if (!skeletonDir.exists() || !File(skeletonDir, "manifest.json").exists()) {
            createSkeletonExtension(skeletonDir, binDir)
        }

        val list = mutableListOf<LabdManifest>()
        extDir.listFiles()?.forEach { dir ->
            if (dir.isDirectory) {
                val manifestFile = File(dir, "manifest.json")
                if (manifestFile.exists()) {
                    parseManifest(manifestFile, dir.absolutePath)?.let { list.add(it) }
                }
            }
        }
        _installedExtensions.value = list
    }

    private fun createSkeletonExtension(targetDir: File, binDir: File) {
        targetDir.mkdirs()
        val manifestJson = JSONObject().apply {
            put("id", "skeleton_module")
            put("name", "Module Squelette .labd")
            put("version", "1.0.0")
            put("author", "LABDROID Core Team")
            put("description", "Extension modèle officielle démontrant l'architecture modulaire native .labd.")
            put("minApi", 24)
            put("targetArch", "arm64-v8a")
            put("entrypoint", "main.sh")
            put("icon", "extension")

            val actionsArray = org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "status_check")
                    put("label", "Tester le moteur du module")
                    put("command", "echo '[SQUELETTE] Test opérationnel du module .labd'; uname -a; date")
                    put("requiresRoot", false)
                    put("outputToConsole", true)
                })
                put(JSONObject().apply {
                    put("id", "selinux_audit")
                    put("label", "Audit SELinux & Permissions")
                    put("command", "getenforce; ls -la /data/data/com.labdroid.kitchen/")
                    put("requiresRoot", true)
                    put("outputToConsole", true)
                })
                put(JSONObject().apply {
                    put("id", "io_benchmark")
                    put("label", "Benchmark I/O Kitchen")
                    put("command", "dd if=/dev/zero of=/data/data/com.labdroid.kitchen/test_io.tmp bs=1M count=10; rm -f /data/data/com.labdroid.kitchen/test_io.tmp; echo '[SQUELETTE] I/O Benchmark terminé!'")
                    put("requiresRoot", false)
                    put("outputToConsole", true)
                })
            }
            put("actions", actionsArray)
        }

        File(targetDir, "manifest.json").writeText(manifestJson.toString(2))

        val scriptFile = File(targetDir, "main.sh")
        scriptFile.writeText(
            """
            #!/system/bin/sh
            echo "============================================="
            echo "LABDROID Module Squelette .labd actif"
            echo "Version: 1.0.0 - Arch: arm64-v8a"
            echo "============================================="
            """.trimIndent()
        )
        scriptFile.setExecutable(true)

        // Install sample binary/symlink in central binDir
        val binStub = File(binDir, "labd_skeleton_tool")
        binStub.writeText("#!/system/bin/sh\necho 'Tool executable binary from .labd extension'\n")
        binStub.setExecutable(true)
    }

    private fun parseManifest(manifestFile: File, installedPath: String): LabdManifest? {
        return try {
            val json = JSONObject(manifestFile.readText())
            val actionsList = mutableListOf<LabdAction>()
            val actionsArray = json.optJSONArray("actions")
            if (actionsArray != null) {
                for (i in 0 until actionsArray.length()) {
                    val act = actionsArray.getJSONObject(i)
                    actionsList.add(
                        LabdAction(
                            id = act.optString("id", "action_$i"),
                            label = act.optString("label", "Action $i"),
                            command = act.optString("command", "echo 'Action'"),
                            requiresRoot = act.optBoolean("requiresRoot", false),
                            outputToConsole = act.optBoolean("outputToConsole", true)
                        )
                    )
                }
            }

            LabdManifest(
                id = json.optString("id", File(installedPath).name),
                name = json.optString("name", "Extension sans nom"),
                version = json.optString("version", "1.0"),
                author = json.optString("author", "Inconnu"),
                description = json.optString("description", ""),
                minApi = json.optInt("minApi", 24),
                targetArch = json.optString("targetArch", "arm64-v8a"),
                entrypoint = json.optString("entrypoint", "main.sh"),
                icon = json.optString("icon", "extension"),
                actions = actionsList,
                installedPath = installedPath
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun installLabdPackage(context: Context, archiveFile: File): Result<LabdManifest> =
        withContext(Dispatchers.IO) {
            if (!archiveFile.exists() || !archiveFile.name.endsWith(".labd", ignoreCase = true) && !archiveFile.name.endsWith(".zip", ignoreCase = true)) {
                return@withContext Result.failure(IllegalArgumentException("Format .labd invalide"))
            }

            try {
                val tempUnpack = File(context.cacheDir, "labd_unpack_${System.currentTimeMillis()}").apply { mkdirs() }
                unzipArchive(archiveFile, tempUnpack)

                val manifestFile = File(tempUnpack, "manifest.json")
                if (!manifestFile.exists()) {
                    tempUnpack.deleteRecursively()
                    return@withContext Result.failure(IllegalStateException("Archive .labd invalide: 'manifest.json' manquant."))
                }

                val tempManifest = parseManifest(manifestFile, tempUnpack.absolutePath)
                    ?: return@withContext Result.failure(IllegalStateException("Impossible de parser manifest.json"))

                val extDir = getExtensionsDir(context)
                val targetDir = File(extDir, tempManifest.id)
                if (targetDir.exists()) {
                    targetDir.deleteRecursively()
                }
                targetDir.mkdirs()

                // Copy unpacked contents to permanent extension directory
                tempUnpack.copyRecursively(targetDir, overwrite = true)
                tempUnpack.deleteRecursively()

                // Centralize and deploy binaries to /data/data/com.labdroid.kitchen/bin/
                val binDir = getBinDir(context)
                val extBinDir = File(targetDir, "bin")
                if (extBinDir.exists() && extBinDir.isDirectory) {
                    extBinDir.listFiles()?.forEach { binFile ->
                        val destBin = File(binDir, binFile.name)
                        binFile.copyTo(destBin, overwrite = true)
                        destBin.setExecutable(true, false)
                    }
                }

                val finalManifestFile = File(targetDir, "manifest.json")
                val finalManifest = parseManifest(finalManifestFile, targetDir.absolutePath)!!

                refreshExtensions(context)
                Result.success(finalManifest)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun unzipArchive(zipFile: File, targetDir: File) {
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry: ZipEntry?
            while (zis.nextEntry.also { entry = it } != null) {
                val file = File(targetDir, entry!!.name)
                if (entry!!.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
            }
        }
    }
}
