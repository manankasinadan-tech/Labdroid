package com.example.ui.components

import android.os.Environment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.FileSystemItem
import com.example.data.service.ForgeEngine
import com.example.data.service.LabdManager
import com.example.data.service.ProjectManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FileSelectorMode {
    IMAGES,      // .img, .bin, .raw
    LABD_EXTENSIONS, // .labd, .zip
    ANY_FILE,
    DIRECTORY_ONLY
}

@Composable
fun PopupFileSelector(
    isOpen: Boolean,
    title: String = "Sélecteur de Fichiers LABDROID",
    mode: FileSelectorMode = FileSelectorMode.ANY_FILE,
    initialPath: String? = null,
    onDismiss: () -> Unit,
    onFileSelected: (File) -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current

    // Locations shortcuts
    val sdcardRoot = Environment.getExternalStorageDirectory().absolutePath
    val kitchenHome = ForgeEngine.getKitchenHomeDir(context).absolutePath
    val projectsRoot = ForgeEngine.getProjectsDir(context).absolutePath
    val extRoot = LabdManager.getExtensionsDir(context).absolutePath

    var currentDir by remember {
        mutableStateOf(
            File(
                initialPath?.takeIf { File(it).exists() }
                    ?: sdcardRoot
            )
        )
    }

    var items by remember { mutableStateOf<List<FileSystemItem>>(emptyList()) }
    var selectedItem by remember { mutableStateOf<FileSystemItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    LaunchedEffect(currentDir) {
        items = ProjectManager.listDirectoryContents(currentDir)
        selectedItem = null
    }

    val filteredItems = remember(items, searchQuery, mode) {
        items.filter { item ->
            // Search filter
            val matchesSearch = searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true)
            if (!matchesSearch) return@filter false

            // Mode filter
            if (item.isDirectory) {
                true
            } else {
                when (mode) {
                    FileSelectorMode.IMAGES -> {
                        item.extension in listOf("img", "bin", "raw", "sparse", "iso")
                    }
                    FileSelectorMode.LABD_EXTENSIONS -> {
                        item.extension in listOf("labd", "zip")
                    }
                    FileSelectorMode.ANY_FILE -> true
                    FileSelectorMode.DIRECTORY_ONLY -> false
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("popup_file_selector_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (mode) {
                                    FileSelectorMode.IMAGES -> Icons.Default.Storage
                                    FileSelectorMode.LABD_EXTENSIONS -> Icons.Default.Extension
                                    FileSelectorMode.DIRECTORY_ONLY -> Icons.Default.Folder
                                    else -> Icons.Default.Description
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when (mode) {
                                    FileSelectorMode.IMAGES -> "Fichiers images (.img, .bin, .raw)"
                                    FileSelectorMode.LABD_EXTENSIONS -> "Packages d'extension (.labd, .zip)"
                                    FileSelectorMode.DIRECTORY_ONLY -> "Sélectionner un dossier cible"
                                    else -> "Tous les fichiers et dossiers"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = { isSearchActive = !isSearchActive },
                            modifier = Modifier.testTag("file_picker_search_toggle")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Rechercher")
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("file_picker_close_btn")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer")
                        }
                    }
                }

                // Search field (animated)
                AnimatedVisibility(visible = isSearchActive) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .testTag("file_picker_search_input"),
                        placeholder = { Text("Filtrer par nom de fichier...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Effacer")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Shortcuts row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentDir.absolutePath.startsWith(sdcardRoot),
                        onClick = { currentDir = File(sdcardRoot) },
                        label = { Text("Sdcard", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.SdCard, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = currentDir.absolutePath == projectsRoot,
                        onClick = { currentDir = File(projectsRoot) },
                        label = { Text("PROJET", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = currentDir.absolutePath == kitchenHome,
                        onClick = { currentDir = File(kitchenHome) },
                        label = { Text("Kitchen Home", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = currentDir.absolutePath == extRoot,
                        onClick = { currentDir = File(extRoot) },
                        label = { Text("Extensions", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Extension, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Current path breadcrumb & UP button
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                currentDir.parentFile?.let { if (it.exists()) currentDir = it }
                            },
                            enabled = currentDir.parentFile != null && currentDir.parentFile?.canRead() == true,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("file_picker_up_btn")
                        ) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = "Dossier parent",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = currentDir.absolutePath,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Files list
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (filteredItems.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Dossier vide ou aucun fichier correspondant",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredItems, key = { it.path }) { item ->
                                val isSelected = selectedItem?.path == item.path
                                FileItemRow(
                                    item = item,
                                    isSelected = isSelected,
                                    onItemClick = {
                                        if (item.isDirectory) {
                                            currentDir = File(item.path)
                                        } else {
                                            selectedItem = item
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                // Footer action bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("file_picker_cancel_btn")
                    ) {
                        Text("Annuler")
                    }

                    val canConfirm = if (mode == FileSelectorMode.DIRECTORY_ONLY) {
                        true
                    } else {
                        selectedItem != null && !selectedItem!!.isDirectory
                    }

                    Button(
                        onClick = {
                            val targetFile = if (mode == FileSelectorMode.DIRECTORY_ONLY) {
                                currentDir
                            } else {
                                selectedItem?.path?.let { File(it) } ?: currentDir
                            }
                            onFileSelected(targetFile)
                            onDismiss()
                        },
                        enabled = canConfirm,
                        modifier = Modifier.testTag("file_picker_select_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (mode == FileSelectorMode.DIRECTORY_ONLY) "Sélectionner ce dossier" else "Choisir ce fichier"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileItemRow(
    item: FileSystemItem,
    isSelected: Boolean,
    onItemClick: () -> Unit
) {
    val dateStr = remember(item.lastModified) {
        if (item.lastModified > 0) {
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(item.lastModified))
        } else ""
    }

    val sizeStr = remember(item.sizeBytes, item.isDirectory, item.childCount) {
        if (item.isDirectory) {
            "${item.childCount} éléments"
        } else {
            val kb = item.sizeBytes / 1024.0
            val mb = kb / 1024.0
            if (mb >= 1.0) "%.1f Mo".format(mb) else "%.1f Ko".format(kb)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onItemClick)
            .testTag("file_item_${item.name}"),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    item.isDirectory -> Icons.Default.Folder
                    item.extension in listOf("img", "raw", "bin") -> Icons.Default.Storage
                    item.extension in listOf("labd", "zip") -> Icons.Default.Extension
                    else -> Icons.Default.Description
                },
                contentDescription = null,
                tint = when {
                    item.isDirectory -> MaterialTheme.colorScheme.primary
                    item.extension in listOf("img", "raw", "bin") -> MaterialTheme.colorScheme.tertiary
                    item.extension in listOf("labd", "zip") -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (item.isDirectory || isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = sizeStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (dateStr.isNotBlank()) {
                        Text(
                            text = "•  $dateStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
