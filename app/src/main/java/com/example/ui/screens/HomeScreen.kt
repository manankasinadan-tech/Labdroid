package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FeatureType
import com.example.data.model.KitchenFeature
import com.example.data.model.LabdManifest
import com.example.data.model.SystemStatus
import com.example.data.service.LabdManager
import com.example.data.service.RootService
import com.example.ui.components.FeatureCard
import com.example.ui.components.FileSelectorMode
import com.example.ui.components.PopupFileSelector
import com.example.ui.components.TelemetryCard
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToForge: () -> Unit,
    onNavigateToProjects: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToExtension: (LabdManifest) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var systemStatus by remember {
        mutableStateOf(
            SystemStatus(
                rootPermissions = false,
                deviceModel = "Détection...",
                operatingSystem = "Android",
                cpuAbi = "arm64-v8a"
            )
        )
    }

    val installedExtensions by LabdManager.installedExtensions.collectAsState()

    var showInstallPicker by remember { mutableStateOf(false) }
    var showDaemonDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        systemStatus = RootService.getSystemStatus(context)
        LabdManager.refreshExtensions(context)
    }

    // Build the unified features list: Natives followed by installed extensions
    val unifiedFeatures = remember(installedExtensions) {
        val list = mutableListOf<KitchenFeature>()

        // 1. Native FORGE
        list.add(
            KitchenFeature(
                id = "forge",
                title = "FORGE (Le Moteur Absolu)",
                type = FeatureType.NATIVE,
                iconName = "forge",
                isExtension = false
            )
        )

        // 2. Kitchen Explorer
        list.add(
            KitchenFeature(
                id = "explorer",
                title = "Kitchen Explorer",
                type = FeatureType.NATIVE,
                iconName = "explorer",
                isExtension = false
            )
        )

        // 3. Native Tools
        list.add(
            KitchenFeature(
                id = "tools",
                title = "Native Tools (Fastboot & Flash)",
                type = FeatureType.NATIVE,
                iconName = "tools",
                isExtension = false
            )
        )

        // 4. Installed .labd extensions with the exact same visual card styling
        installedExtensions.forEach { ext ->
            list.add(
                KitchenFeature(
                    id = ext.id,
                    title = ext.name,
                    type = FeatureType.EXTENSION,
                    iconName = ext.icon,
                    isExtension = true,
                    extensionManifest = ext
                )
            )
        }

        list
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // En-tête (Top App Bar)
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "LABDROID",
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "v2.0",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            },
            actions = {
                // Bouton + : Ouvre instantanément le sélecteur popup pour installer un module .labd à la volée
                IconButton(
                    onClick = { showInstallPicker = true },
                    modifier = Modifier.testTag("topbar_btn_add_labd")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Installer un module .labd",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Bouton Alimentation : État / redémarrage du démon moteur
                IconButton(
                    onClick = { showDaemonDialog = true },
                    modifier = Modifier.testTag("topbar_btn_daemon_power")
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Démon moteur",
                        tint = if (systemStatus.daemonRunning) Color(0xFF16A34A) else Color(0xFFEF4444)
                    )
                }

                // Bouton Info (i) : Crédits et documentation
                IconButton(
                    onClick = { showInfoDialog = true },
                    modifier = Modifier.testTag("topbar_btn_info")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Informations",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Carte de Statut Système (Bloc supérieur)
            item {
                TelemetryCard(status = systemStatus)
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "FONCTIONNALITÉS & EXTENSIONS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }

            // Liste Unifiée des Fonctionnalités et Extensions sur l'Accueil
            items(unifiedFeatures, key = { it.id }) { feature ->
                FeatureCard(
                    feature = feature,
                    onClick = {
                        when (feature.id) {
                            "forge" -> onNavigateToForge()
                            "explorer" -> onNavigateToProjects()
                            "tools" -> onNavigateToTools()
                            else -> {
                                feature.extensionManifest?.let { onNavigateToExtension(it) }
                            }
                        }
                    }
                )
            }
        }
    }

    // Popup File Selector for .labd installation
    PopupFileSelector(
        isOpen = showInstallPicker,
        title = "Installer une Extension .labd",
        mode = FileSelectorMode.LABD_EXTENSIONS,
        onDismiss = { showInstallPicker = false },
        onFileSelected = { selectedFile ->
            scope.launch {
                Toast.makeText(context, "Installation de ${selectedFile.name}...", Toast.LENGTH_SHORT).show()
                val result = LabdManager.installLabdPackage(context, selectedFile)
                if (result.isSuccess) {
                    val manifest = result.getOrThrow()
                    Toast.makeText(context, "Module '${manifest.name}' installé avec succès!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Erreur: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    )

    // Daemon Power Dialog
    if (showDaemonDialog) {
        AlertDialog(
            onDismissRequest = { showDaemonDialog = false },
            title = {
                Text("Démon Moteur LABDROID", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Statut du démon : ${if (systemStatus.daemonRunning) "En cours d'exécution" else "Arrêté"}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Le démon gère les processus en tâche de fond, la surveillance de l'espace de travail et l'isolation des processus root.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            systemStatus = systemStatus.copy(daemonRunning = !systemStatus.daemonRunning)
                            Toast.makeText(
                                context,
                                if (systemStatus.daemonRunning) "Démon redémarré avec succès" else "Démon arrêté",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        showDaemonDialog = false
                    }
                ) {
                    Text(if (systemStatus.daemonRunning) "Redémarrer le démon" else "Démarrer le démon")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDaemonDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }

    // Info & Documentation Dialog
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Text("À propos de LABDROID", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("LABDROID Kitchen v2.0 (M3)")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Environnement autonome de manipulation et reconstruction de partitions Android (ext4, EROFS, boot.img).",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Espace de travail : /data/data/com.labdroid.kitchen/",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Crédits : Conçu pour les développeurs et mainteneurs de ROMs Android.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}
