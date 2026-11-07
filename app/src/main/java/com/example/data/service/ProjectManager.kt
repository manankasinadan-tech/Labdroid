package com.example.data.service

import android.content.Context
import com.example.data.model.FileSystemItem
import com.example.data.model.Project
import com.example.data.model.ProjectMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

object ProjectManager {

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    // Clipboard for file manager
    private var clipboardSource: File? = null
    private var isCutOperation: Boolean = false

    val clipboardInfo: String?
        get() = clipboardSource?.let { "${if (isCutOperation) "Couper" else "Copier"}: ${it.name}" }

    suspend fun refreshProjects(context: Context) = withContext(Dispatchers.IO) {
        val projectsDir = ForgeEngine.getProjectsDir(context)

        // If no projects exist, seed a demo ready-for-repack project for instant user delight
        if (projectsDir.listFiles()?.none { it.isDirectory } == true) {
            seedInitialDemoProject(projectsDir)
        }

        val resultList = mutableListOf<Project>()
        projectsDir.listFiles()?.forEach { dir ->
            if (dir.isDirectory) {
                val rootfs = File(dir, "rootfs")
                val database = File(dir, "database")
                val temp = File(dir, "temp")
                val metaFile = File(database, "project_meta.json")
                val metadata = if (metaFile.exists()) ForgeEngine.loadProjectMetadata(metaFile) else null

                val isReady = metadata?.isReadyForRepack == true && rootfs.exists() && !rootfs.listFiles().isNullOrEmpty()

                var totalBytes = 0L
                dir.walkTopDown().forEach { f -> if (f.isFile) totalBytes += f.length() }

                resultList.add(
                    Project(
                        name = dir.name,
                        rootPath = dir.absolutePath,
                        rootfsPath = rootfs.absolutePath,
                        databasePath = database.absolutePath,
                        tempPath = temp.absolutePath,
                        metadata = metadata,
                        isReadyForRepack = isReady,
                        totalSizeBytes = totalBytes,
                        lastModified = dir.lastModified()
                    )
                )
            }
        }
        _projects.value = resultList.sortedByDescending { it.lastModified }
    }

    private fun seedInitialDemoProject(projectsDir: File) {
        val demoDir = File(projectsDir, "system_xiaomi_tucana").apply { mkdirs() }
        val rootfsDir = File(demoDir, "rootfs").apply { mkdirs() }
        val databaseDir = File(demoDir, "database").apply { mkdirs() }
        File(demoDir, "temp").apply { mkdirs() }

        // Rootfs hierarchy
        File(rootfsDir, "bin").apply { mkdirs() }
        File(rootfsDir, "etc").apply { mkdirs() }
        File(rootfsDir, "lib64").apply { mkdirs() }
        File(rootfsDir, "framework").apply { mkdirs() }

        val buildProp = File(rootfsDir, "etc/build.prop")
        buildProp.writeText(
            """
            # LABDROID Project Demo Configuration
            ro.build.version.release=15
            ro.product.model=tucana
            ro.product.brand=Xiaomi
            ro.product.name=MI NOTE 10
            ro.kitchen.project=system_xiaomi_tucana
            """.trimIndent()
        )

        val meta = ProjectMetadata(
            projectName = "system_xiaomi_tucana",
            sourceImageName = "system.img",
            filesystemType = "ext4",
            partitionName = "system",
            isSparse = true,
            blockSize = 4096,
            originalSizeBytes = 3221225472L, // ~3GB
            inodeCount = 8192,
            volumeLabel = "system",
            mountPoint = "/system",
            selinuxContextsFile = "file_contexts.bin",
            fsConfigFile = "fs_config",
            unpackTimestamp = System.currentTimeMillis() - 3600000,
            isReadyForRepack = true,
            fileCount = 124,
            directoryCount = 38
        )

        File(databaseDir, "project_meta.json").writeText(
            org.json.JSONObject().apply {
                put("projectName", meta.projectName)
                put("sourceImageName", meta.sourceImageName)
                put("filesystemType", meta.filesystemType)
                put("partitionName", meta.partitionName)
                put("isSparse", meta.isSparse)
                put("blockSize", meta.blockSize)
                put("originalSizeBytes", meta.originalSizeBytes)
                put("inodeCount", meta.inodeCount)
                put("volumeLabel", meta.volumeLabel)
                put("mountPoint", meta.mountPoint)
                put("selinuxContextsFile", meta.selinuxContextsFile)
                put("fsConfigFile", meta.fsConfigFile)
                put("unpackTimestamp", meta.unpackTimestamp)
                put("isReadyForRepack", meta.isReadyForRepack)
                put("fileCount", meta.fileCount)
                put("directoryCount", meta.directoryCount)
            }.toString(2)
        )

        File(databaseDir, "fs_config").writeText(
            """
            # LABDROID fs_config
            /system 0 0 0755
            /system/bin 0 2000 0755
            /system/etc 0 0 0755
            /system/etc/build.prop 0 0 0644
            """.trimIndent()
        )

        File(databaseDir, "file_contexts.bin").writeText(
            """
            /system(/.*)? u:object_r:system_file:s0
            /system/bin(/.*)? u:object_r:system_file:s0
            /system/etc/build\.prop u:object_r:system_file:s0
            """.trimIndent()
        )
    }

    suspend fun listDirectoryContents(dir: File): List<FileSystemItem> = withContext(Dispatchers.IO) {
        if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()

        val files = dir.listFiles() ?: return@withContext emptyList()
        files.map { file ->
            val ext = if (file.isFile) file.extension.lowercase(Locale.ROOT) else ""
            val childCount = if (file.isDirectory) (file.listFiles()?.size ?: 0) else 0
            FileSystemItem(
                name = file.name,
                path = file.absolutePath,
                isDirectory = file.isDirectory,
                sizeBytes = if (file.isFile) file.length() else 0L,
                lastModified = file.lastModified(),
                isReadable = file.canRead(),
                isWritable = file.canWrite(),
                isExecutable = file.canExecute(),
                extension = ext,
                childCount = childCount
            )
        }.sortedWith(
            compareByDescending<FileSystemItem> { it.isDirectory }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        )
    }

    fun setClipboard(file: File, isCut: Boolean) {
        clipboardSource = file
        isCutOperation = isCut
    }

    fun clearClipboard() {
        clipboardSource = null
        isCutOperation = false
    }

    suspend fun executePaste(targetDir: File, context: Context): Result<String> = withContext(Dispatchers.IO) {
        val source = clipboardSource ?: return@withContext Result.failure(IllegalStateException("Presse-papier vide"))
        if (!targetDir.exists() || !targetDir.isDirectory) {
            return@withContext Result.failure(IllegalArgumentException("Le dossier cible n'existe pas"))
        }

        try {
            val destination = File(targetDir, source.name)
            if (destination.exists()) {
                // If destination exists, generate copy name
                val newName = if (source.isDirectory) {
                    "${source.name}_copie"
                } else {
                    "${source.nameWithoutExtension}_copie.${source.extension}"
                }
                val destWithNewName = File(targetDir, newName)
                if (source.isDirectory) {
                    source.copyRecursively(destWithNewName, overwrite = true)
                } else {
                    source.copyTo(destWithNewName, overwrite = true)
                }
            } else {
                if (source.isDirectory) {
                    source.copyRecursively(destination, overwrite = true)
                } else {
                    source.copyTo(destination, overwrite = true)
                }
            }

            if (isCutOperation) {
                source.deleteRecursively()
                clearClipboard()
            }

            refreshProjects(context)
            Result.success("Élément collé avec succès dans ${targetDir.name}")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteItem(file: File, context: Context): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val success = if (file.isDirectory) file.deleteRecursively() else file.delete()
            refreshProjects(context)
            Result.success(success)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFolder(parentDir: File, folderName: String, context: Context): Result<File> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val newFolder = File(parentDir, folderName)
                if (newFolder.mkdirs()) {
                    refreshProjects(context)
                    Result.success(newFolder)
                } else {
                    Result.failure(IllegalStateException("Impossible de créer le dossier"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun renameItem(file: File, newName: String, context: Context): Result<File> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val target = File(file.parentFile, newName)
                if (file.renameTo(target)) {
                    refreshProjects(context)
                    Result.success(target)
                } else {
                    Result.failure(IllegalStateException("Échec du renommage"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
