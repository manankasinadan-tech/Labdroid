package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LogEntry
import com.example.data.model.LogLevel
import com.example.ui.theme.ConsoleDarkBackground
import com.example.ui.theme.LogErrorColor
import com.example.ui.theme.LogInfoColor
import com.example.ui.theme.LogSuccessColor
import com.example.ui.theme.LogWarningColor
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ForgeConsole(
    logs: List<LogEntry>,
    isProcessing: Boolean = false,
    modifier: Modifier = Modifier,
    onClear: () -> Unit = {}
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var autoScrollEnabled by remember { mutableStateOf(true) }

    // Auto-scroll logic when new logs arrive, unless user paused it
    LaunchedEffect(logs.size, autoScrollEnabled) {
        if (autoScrollEnabled && logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("forge_log_console"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ConsoleDarkBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Console Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (isProcessing) LogWarningColor else LogSuccessColor
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CONSOLE MOTEUR FORGE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    if (isProcessing) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = LogWarningColor
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Auto-scroll toggle badge
                    IconButton(
                        onClick = {
                            autoScrollEnabled = !autoScrollEnabled
                            if (autoScrollEnabled && logs.isNotEmpty()) {
                                scope.launch { listState.scrollToItem(logs.size - 1) }
                            }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("console_autoscroll_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Défilement automatique",
                            tint = if (autoScrollEnabled) LogSuccessColor else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Copy logs button
                    IconButton(
                        onClick = {
                            if (logs.isNotEmpty()) {
                                val fullText = logs.joinToString("\n") { entry ->
                                    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(entry.timestamp))
                                    "[$time][${entry.level}][${entry.tag}] ${entry.message}"
                                }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("LABDROID FORGE Logs", fullText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Journaux copiés dans le presse-papier", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("console_copy_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copier les logs",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Clear logs button
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("console_clear_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Effacer la console",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Console output viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color(0xFF040711), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                if (logs.isEmpty()) {
                    Text(
                        text = "En attente d'une opération (UNPACK ou REPACK)...\nLes journaux de traitement s'afficheront en temps réel.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(logs, key = { it.id }) { logEntry ->
                            LogItemRow(entry = logEntry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogItemRow(entry: LogEntry) {
    val time = remember(entry.timestamp) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(entry.timestamp))
    }

    val (levelColor, levelPrefix) = when (entry.level) {
        LogLevel.INFO -> Pair(LogInfoColor, "INFO")
        LogLevel.SUCCESS -> Pair(LogSuccessColor, "OK  ")
        LogLevel.WARNING -> Pair(LogWarningColor, "WARN")
        LogLevel.ERROR -> Pair(LogErrorColor, "ERR ")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$time ",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = Color(0xFF64748B)
        )
        Text(
            text = "[$levelPrefix] ",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = levelColor
        )
        Text(
            text = entry.message,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = when (entry.level) {
                LogLevel.ERROR -> LogErrorColor
                LogLevel.SUCCESS -> Color(0xFFE2E8F0)
                LogLevel.WARNING -> Color(0xFFFDE68A)
                LogLevel.INFO -> Color(0xFFCBD5E1)
            }
        )
    }
}
