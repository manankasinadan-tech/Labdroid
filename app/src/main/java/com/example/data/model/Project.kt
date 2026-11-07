package com.example.data.model

data class ProjectMetadata(
    val projectName: String,
    val sourceImageName: String,
    val filesystemType: String, // ext4, erofs, f2fs, raw, etc.
    val partitionName: String,   // system, vendor, product, boot, etc.
    val isSparse: Boolean = true,
    val blockSize: Int = 4096,
    val originalSizeBytes: Long = 0L,
    val inodeCount: Int = 0,
    val volumeLabel: String = "",
    val mountPoint: String = "/system",
    val selinuxContextsFile: String = "file_contexts.bin",
    val fsConfigFile: String = "fs_config",
    val unpackTimestamp: Long = System.currentTimeMillis(),
    val isReadyForRepack: Boolean = true,
    val fileCount: Int = 0,
    val directoryCount: Int = 0
)

data class Project(
    val name: String,
    val rootPath: String,
    val rootfsPath: String,
    val databasePath: String,
    val tempPath: String,
    val metadata: ProjectMetadata?,
    val isReadyForRepack: Boolean,
    val totalSizeBytes: Long,
    val lastModified: Long
)
