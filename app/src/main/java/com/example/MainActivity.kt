package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.LabdManifest
import com.example.ui.screens.ExtensionRunnerScreen
import com.example.ui.screens.ForgeScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LabdroidTheme

enum class BottomTab {
    ACCUEIL,
    PROJET,
    PARAMETRES
}

enum class SubScreen {
    NONE,
    FORGE,
    TOOLS,
    EXTENSION_RUNNER
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var themeMode by remember { mutableStateOf(AppThemeMode.SYSTEM) }

            LabdroidTheme(themeMode = themeMode) {
                LabdroidApp(
                    currentThemeMode = themeMode,
                    onThemeModeChange = { themeMode = it }
                )
            }
        }
    }
}

@Composable
fun LabdroidApp(
    currentThemeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit
) {
    var currentTab by remember { mutableStateOf(BottomTab.ACCUEIL) }
    var currentSubScreen by remember { mutableStateOf(SubScreen.NONE) }
    var selectedExtensionManifest by remember { mutableStateOf<LabdManifest?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentSubScreen == SubScreen.NONE) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.testTag("bottom_navigation_bar")
                ) {
                    // 1. Accueil
                    NavigationBarItem(
                        selected = currentTab == BottomTab.ACCUEIL,
                        onClick = { currentTab = BottomTab.ACCUEIL },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == BottomTab.ACCUEIL) Icons.Default.Home else Icons.Outlined.Home,
                                contentDescription = "Accueil"
                            )
                        },
                        label = { Text("Accueil") },
                        modifier = Modifier.testTag("nav_item_accueil")
                    )

                    // 2. PROJET
                    NavigationBarItem(
                        selected = currentTab == BottomTab.PROJET,
                        onClick = { currentTab = BottomTab.PROJET },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == BottomTab.PROJET) Icons.Default.Folder else Icons.Outlined.Folder,
                                contentDescription = "PROJET"
                            )
                        },
                        label = { Text("PROJET") },
                        modifier = Modifier.testTag("nav_item_projet")
                    )

                    // 3. Paramètres
                    NavigationBarItem(
                        selected = currentTab == BottomTab.PARAMETRES,
                        onClick = { currentTab = BottomTab.PARAMETRES },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == BottomTab.PARAMETRES) Icons.Default.Settings else Icons.Outlined.Settings,
                                contentDescription = "Paramètres"
                            )
                        },
                        label = { Text("Paramètres") },
                        modifier = Modifier.testTag("nav_item_parametres")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentSubScreen) {
                SubScreen.FORGE -> {
                    ForgeScreen(
                        onNavigateBack = { currentSubScreen = SubScreen.NONE }
                    )
                }
                SubScreen.TOOLS -> {
                    ToolsScreen(
                        onNavigateBack = { currentSubScreen = SubScreen.NONE }
                    )
                }
                SubScreen.EXTENSION_RUNNER -> {
                    selectedExtensionManifest?.let { manifest ->
                        ExtensionRunnerScreen(
                            manifest = manifest,
                            onNavigateBack = {
                                selectedExtensionManifest = null
                                currentSubScreen = SubScreen.NONE
                            }
                        )
                    } ?: run {
                        currentSubScreen = SubScreen.NONE
                    }
                }
                SubScreen.NONE -> {
                    when (currentTab) {
                        BottomTab.ACCUEIL -> {
                            HomeScreen(
                                onNavigateToForge = { currentSubScreen = SubScreen.FORGE },
                                onNavigateToProjects = { currentTab = BottomTab.PROJET },
                                onNavigateToTools = { currentSubScreen = SubScreen.TOOLS },
                                onNavigateToExtension = { manifest ->
                                    selectedExtensionManifest = manifest
                                    currentSubScreen = SubScreen.EXTENSION_RUNNER
                                }
                            )
                        }
                        BottomTab.PROJET -> {
                            ProjectsScreen()
                        }
                        BottomTab.PARAMETRES -> {
                            SettingsScreen(
                                currentThemeMode = currentThemeMode,
                                onThemeModeChange = onThemeModeChange
                            )
                        }
                    }
                }
            }
        }
    }
}
