package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Project
import com.example.data.service.ForgeEngine
import com.example.data.service.ProjectManager
import com.example.ui.components.FileSelectorMode
import com.example.ui.components.ForgeConsole
import com.example.ui.components.PopupFileSelector
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgeScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0 = UNPACK, 1 = REPACK

    // UNPACK state
    var imagePath by remember { mutableStateOf("") }
    var projectNameOverride by remember { mutableStateOf("") }
    var showImagePicker by remember { mutableStateOf(false) }

    // REPACK state
    val projects by ProjectManager.projects.collectAsState()
    var selectedProject by remember { mutableStateOf<Project?>(null) }
    var isProjectDropdownExpanded by remember { mutableStateOf(false) }

    // Engine logs & state
    val logs by ForgeEngine.logs.collectAsState()
    val isProcessing by ForgeEngine.isProcessing.collectAsState()

    LaunchedEffect(Unit) {
        ProjectManager.refreshProjects(context)
    }

    LaunchedEffect(projects) {
        if (selectedProject == null && projects.isNotEmpty()) {
            selectedProject = projects.firstOrNull { it.isReadyForRepack } ?: projects.first()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "FORGE",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Moteur Absolu",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("forge_back_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        // Tab Row: UNPACK / REPACK
        PrimaryTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("UNPACK (Extraction)", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("REPACK (Reconstruction)", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (selectedTabIndex == 0) {
                // ==================== UNPACK SECTION ====================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forge_unpack_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Extraction Intelligente de Partition",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sélectionnez une image de partition Android (ext4, EROFS, boot.img). Le moteur analysera l'en-tête binaire, créera un projet autonome et collectera les métadonnées de reconstruction.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Path input & file selector popup trigger
                        OutlinedTextField(
                            value = imagePath,
                            onValueChange = { imagePath = it },
                            label = { Text("Chemin de l'image partition") },
                            placeholder = { Text("/sdcard/.../system.img") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("forge_image_path_input"),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(
                                    onClick = { showImagePicker = true },
                                    modifier = Modifier.testTag("forge_browse_image_btn")
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = "Parcourir")
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Optional project name override
                        OutlinedTextField(
                            value = projectNameOverride,
                            onValueChange = { projectNameOverride = it },
                            label = { Text("Nom du projet (optionnel)") },
                            placeholder = { Text("Laissez vide pour auto-détection") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("forge_project_name_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Unpack Button
                        Button(
                            onClick = {
                                if (imagePath.isNotBlank()) {
                                    scope.launch {
                                        val result = ForgeEngine.unpackImage(
                                            context = context,
                                            imageFilePath = imagePath,
                                            projectNameOverride = projectNameOverride.takeIf { it.isNotBlank() }
                                        )
                                        if (result.isSuccess) {
                                            ProjectManager.refreshProjects(context)
                                            Toast.makeText(context, "UNPACK réussi! Projet créé.", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Erreur: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Veuillez choisir un fichier image", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isProcessing && imagePath.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("forge_btn_unpack"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Extraction en cours...")
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("LANCER L'UNPACK", fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = "Règle absolue : L'image originale n'est pas conservée. 1 UNPACK = 1 rootfs + 1 base de métadonnées autonome.",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            } else {
                // ==================== REPACK SECTION ====================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forge_repack_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Reconstruction Intelligente (REPACK)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Reconstruit la partition image exclusivement à partir du rootfs/ et des métadonnées stockées dans database/. Sortie garantie dans /sdcard/LABDROID/REPACKED/.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Project selector dropdown
                        Column {
                            Text(
                                text = "Projet cible :",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { isProjectDropdownExpanded = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("forge_project_dropdown_btn"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedProject?.name ?: "Aucun projet disponible",
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                                    }
                                }

                                DropdownMenu(
                                    expanded = isProjectDropdownExpanded,
                                    onDismissRequest = { isProjectDropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    if (projects.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("Aucun projet dépaqueté") },
                                            onClick = { isProjectDropdownExpanded = false }
                                        )
                                    } else {
                                        projects.forEach { proj ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = if (proj.isReadyForRepack) Icons.Default.CheckCircle else Icons.Default.Warning,
                                                            contentDescription = null,
                                                            tint = if (proj.isReadyForRepack) Color(0xFF16A34A) else Color(0xFFD97706),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = proj.name,
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    selectedProject = proj
                                                    isProjectDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Project Technical Metadata Card
                        selectedProject?.let { proj ->
                            OutlinedCard(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "État du projet :",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (proj.isReadyForRepack) "Prêt pour reconstruction" else "Incomplet (métadonnées manquantes)",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (proj.isReadyForRepack) Color(0xFF16A34A) else Color(0xFFEF4444)
                                        )
                                    }
                                    proj.metadata?.let { meta ->
                                        Text(
                                            text = "• FS Type: ${meta.filesystemType.uppercase()} (${if (meta.isSparse) "Sparse" else "RAW"})",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "• Partition: ${meta.partitionName} | Bloc: ${meta.blockSize}B",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "• SELinux: ${meta.selinuxContextsFile} | fs_config: ${meta.fsConfigFile}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        // Repack Button
                        Button(
                            onClick = {
                                selectedProject?.let { proj ->
                                    scope.launch {
                                        val result = ForgeEngine.repackProject(context, proj.name)
                                        if (result.isSuccess) {
                                            val outFile = result.getOrThrow()
                                            Toast.makeText(context, "Image reconstruite: ${outFile.name}", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Échec REPACK: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            enabled = !isProcessing && selectedProject != null && selectedProject?.isReadyForRepack == true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("forge_btn_repack"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reconstruction en cours...")
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("LANCER LE REPACK", fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = "Sortie : /sdcard/LABDROID/REPACKED/ (fichier unique sans écrasement)",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Real-Time Console (Integrated seamlessly in both UNPACK and REPACK)
            ForgeConsole(
                logs = logs,
                isProcessing = isProcessing,
                onClear = { ForgeEngine.clearLogs() }
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Popup File Selector for selecting .img
    PopupFileSelector(
        isOpen = showImagePicker,
        title = "Sélectionner une Image de Partition",
        mode = FileSelectorMode.IMAGES,
        onDismiss = { showImagePicker = false },
        onFileSelected = { file ->
            imagePath = file.absolutePath
            if (projectNameOverride.isBlank()) {
                projectNameOverride = file.nameWithoutExtension
            }
        }
    )
}
