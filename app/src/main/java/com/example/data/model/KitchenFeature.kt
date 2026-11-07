package com.example.data.model

import androidx.compose.ui.graphics.vector.ImageVector

enum class FeatureType {
    NATIVE,
    EXTENSION
}

data class KitchenFeature(
    val id: String,
    val title: String,
    val type: FeatureType,
    val iconName: String = "build", // mapped to standard icon or icon pack
    val isExtension: Boolean = false,
    val extensionManifest: LabdManifest? = null,
    val route: String = id
)
