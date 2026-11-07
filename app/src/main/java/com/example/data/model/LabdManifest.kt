package com.example.data.model

data class LabdAction(
    val id: String,
    val label: String,
    val command: String,
    val requiresRoot: Boolean = false,
    val outputToConsole: Boolean = true
)

data class LabdManifest(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String = "",
    val minApi: Int = 24,
    val targetArch: String = "arm64-v8a",
    val entrypoint: String = "main.sh",
    val icon: String = "extension",
    val permissions: List<String> = emptyList(),
    val binaries: List<String> = emptyList(),
    val actions: List<LabdAction> = emptyList(),
    val installedPath: String = ""
)
