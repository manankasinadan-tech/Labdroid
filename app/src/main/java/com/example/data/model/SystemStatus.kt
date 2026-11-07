package com.example.data.model

data class SystemStatus(
    val rootPermissions: Boolean,
    val suBinaryPath: String? = null,
    val deviceModel: String,
    val operatingSystem: String,
    val cpuAbi: String,
    val daemonRunning: Boolean = true,
    val totalRamMb: Long = 0L,
    val availableRamMb: Long = 0L,
    val internalStorageFreeMb: Long = 0L
)
