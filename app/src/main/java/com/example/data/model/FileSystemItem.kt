package com.example.data.model

data class FileSystemItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long = 0L,
    val lastModified: Long = 0L,
    val isReadable: Boolean = true,
    val isWritable: Boolean = true,
    val isExecutable: Boolean = false,
    val extension: String = "",
    val childCount: Int = 0
)
