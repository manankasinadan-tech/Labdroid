package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileSystemItem
import com.example.data.model.Project
import com.example.data.service.ForgeEngine
import com.example.data.service.ProjectManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val projectsRoot = remember { ForgeEngine.getProjectsDir(context) }

    var currentDir by remember { mutableStateOf(projectsRoot) }
    val isAtRoot = currentDir.absolutePath == projectsRoot.absolutePath

    BackHandler(enabled = !isAtRoot) {
        currentDir.parentFile?.let { if (it.exists()) currentDir = it }
    }

    val projects by ProjectManager.projects.collectAsState()
    var currentItems by remember { mutableStateOf<List<FileSystemItem>>(emptyList()) }

    // Dialogs & selection state
    var selectedItemForAction by remember { mutableStateOf<File?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameNewName by remember { mutableStateOf("") }
    var showFileViewer by remember { mutableStateOf(false) }
    var viewingFileContent by remember { mutableStateOf("") }
    var viewingFileName by remember { mutableStateOf("") }

    LaunchedEffect(currentDir) {
        if (isAtRoot) {
            ProjectManager.refreshProjects(context)
        }
        currentItems = ProjectManager.listDirectoryContents(currentDir)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = if (isAtRoot) "PROJETS DE LA KITCHEN" else currentDir.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "/data/data/com.labdroid.kitchen/home/PROJET/${if (!isAtRoot) currentDir.relativeTo(projectsRoot).path else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            navigationIcon = {
                if (!isAtRoot) {
                    IconButton(
                        onClick = {
                            currentDir.parentFile?.let { if (it.exists()) currentDir = it }
                        },
                        modifier = Modifier.testTag("projects_up_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Dossier parent")
                    }
                }
            },
            actions = {
                // New folder button
                IconButton(
                    onClick = {
                        newFolderName = ""
                        showNewFolderDialog = true
                    },
                    modifier = Modifier.testTag("projects_btn_new_folder")
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "Nouveau dossier")
                }

                // Paste button if clipboard has item
                if (ProjectManager.clipboardInfo != null) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                val res = ProjectManager.executePaste(currentDir, context)
                                if (res.isSuccess) {
                                    Toast.makeText(context, res.getOrThrow(), Toast.LENGTH_SHORT).show()
                                    currentItems = ProjectManager.listDirectoryContents(currentDir)
                                } else {
                                    Toast.makeText(context, "Échec: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.testTag("projects_btn_paste")
                    ) {
                        Icon(
                            Icons.Default.ContentPaste,
                            contentDescription = "Coller",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(
                    onClick = {
                        scope.launch {
                            ProjectManager.refreshProjects(context)
                            currentItems = ProjectManager.listDirectoryContents(currentDir)
                        }
                    },
                    modifier = Modifier.testTag("projects_btn_refresh")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        // Clipboard indicator bar (if active)
        ProjectManager.clipboardInfo?.let { clipInfo ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = clipInfo,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    TextButton(onClick = { ProjectManager.clearClipboard() }) {
                        Text("Annuler", fontSize = 12.sp)
                    }
                }
            }
        }

        // Project Root Cards vs Folder Explorer
        if (isAtRoot) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "${projects.size} PROJET(S) DÉPAQUETÉ(S)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )
                }

                if (projects.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Aucun projet dépaqueté",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Utilisez le moteur FORGE (onglet Accueil) pour lancer un UNPACK depuis une image de partition.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(projects, key = { it.name }) { project ->
                        ProjectOverviewCard(
                            project = project,
                            onOpen = { currentDir = File(project.rootPath) },
                            onCopy = {
                                ProjectManager.setClipboard(File(project.rootPath), isCut = false)
                                Toast.makeText(context, "Projet copié dans le presse-papier", Toast.LENGTH_SHORT).show()
                            },
                            onCut = {
                                ProjectManager.setClipboard(File(project.rootPath), isCut = true)
                                Toast.makeText(context, "Projet coupé", Toast.LENGTH_SHORT).show()
                            },
                            onRename = {
                                selectedItemForAction = File(project.rootPath)
                                renameNewName = project.name
                                showRenameDialog = true
                            },
                            onDelete = {
                                selectedItemForAction = File(project.rootPath)
                                showDeleteConfirm = true
                            }
                        )
                    }
                }
            }
        } else {
            // Inside a project or folder: Real file manager
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (currentItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Dossier vide",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(currentItems, key = { it.path }) { item ->
                        ExplorerItemCard(
                            item = item,
                            onClick = {
                                if (item.isDirectory) {
                                    currentDir = File(item.path)
                                } else {
                                    // Open file viewer if text/config/sh/prop/json
                                    val f = File(item.path)
                                    if (f.length() < 500_000) { // <500KB
                                        scope.launch {
                                            viewingFileName = f.name
                                            viewingFileContent = withContext(Dispatchers.IO) {
                                                try { f.readText() } catch (e: Exception) { "Fichier binaire ou illisible: ${e.message}" }
                                            }
                                            showFileViewer = true
                                        }
                                    } else {
                                        Toast.makeText(context, "Fichier trop volumineux pour l'aperçu textuel", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onCopy = {
                                ProjectManager.setClipboard(File(item.path), isCut = false)
                                Toast.makeText(context, "Copié : ${item.name}", Toast.LENGTH_SHORT).show()
                            },
                            onCut = {
                                ProjectManager.setClipboard(File(item.path), isCut = true)
                                Toast.makeText(context, "Coupé : ${item.name}", Toast.LENGTH_SHORT).show()
                            },
                            onRename = {
                                selectedItemForAction = File(item.path)
                                renameNewName = item.name
                                showRenameDialog = true
                            },
                            onDelete = {
                                selectedItemForAction = File(item.path)
                                showDeleteConfirm = true
                            }
                        )
                    }
                }
            }
        }
    }

    // New folder dialog
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Nouveau Dossier") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Nom du dossier") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            scope.launch {
                                ProjectManager.createFolder(currentDir, newFolderName, context)
                                currentItems = ProjectManager.listDirectoryContents(currentDir)
                            }
                            showNewFolderDialog = false
                        }
                    }
                ) {
                    Text("Créer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Rename dialog
    if (showRenameDialog && selectedItemForAction != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Renommer") },
            text = {
                OutlinedTextField(
                    value = renameNewName,
                    onValueChange = { renameNewName = it },
                    label = { Text("Nouveau nom") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedItemForAction?.let { f ->
                            scope.launch {
                                ProjectManager.renameItem(f, renameNewName, context)
                                currentItems = ProjectManager.listDirectoryContents(currentDir)
                            }
                        }
                        showRenameDialog = false
                    }
                ) {
                    Text("Renommer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirm && selectedItemForAction != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Confirmer la suppression") },
            text = {
                Text("Voulez-vous vraiment supprimer définitivement '${selectedItemForAction?.name}' ? Cette opération est irréversible.")
            },
            confirmButton = {
                Button(
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        selectedItemForAction?.let { f ->
                            scope.launch {
                                val res = ProjectManager.deleteItem(f, context)
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Supprimé", Toast.LENGTH_SHORT).show()
                                    currentItems = ProjectManager.listDirectoryContents(currentDir)
                                } else {
                                    Toast.makeText(context, "Erreur de suppression", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // File viewer dialog
    if (showFileViewer) {
        AlertDialog(
            onDismissRequest = { showFileViewer = false },
            title = {
                Text(
                    text = viewingFileName,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = viewingFileContent,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showFileViewer = false }) {
                    Text("Fermer")
                }
            }
        )
    }
}

@Composable
private fun ProjectOverviewCard(
    project: Project,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen)
            .testTag("project_card_${project.name}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (project.isReadyForRepack) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (project.isReadyForRepack) Color(0xFF16A34A) else Color(0xFFD97706),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (project.isReadyForRepack) "Prêt pour REPACK" else "Métadonnées manquantes",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = if (project.isReadyForRepack) Color(0xFF16A34A) else Color(0xFFD97706)
                            )
                        }
                    }
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ouvrir") },
                            onClick = {
                                menuExpanded = false
                                onOpen()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copier") },
                            onClick = {
                                menuExpanded = false
                                onCopy()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Couper") },
                            onClick = {
                                menuExpanded = false
                                onCut()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Renommer") },
                            onClick = {
                                menuExpanded = false
                                onRename()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            // Project specs & subfolders summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "FS: ${project.metadata?.filesystemType?.uppercase() ?: "EXT4"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Taille: ${project.totalSizeBytes / (1024 * 1024)} Mo",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "rootfs/ • database/",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ExplorerItemCard(
    item: FileSystemItem,
    onClick: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("explorer_item_${item.name}"),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                    contentDescription = null,
                    tint = if (item.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (item.isDirectory) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (item.isDirectory) "${item.childCount} éléments" else "${item.sizeBytes / 1024} Ko",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", modifier = Modifier.size(18.dp))
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(text = { Text("Copier") }, onClick = { menuExpanded = false; onCopy() })
                    DropdownMenuItem(text = { Text("Couper") }, onClick = { menuExpanded = false; onCut() })
                    DropdownMenuItem(text = { Text("Renommer") }, onClick = { menuExpanded = false; onRename() })
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onDelete() }
                    )
                }
            }
        }
    }
}
