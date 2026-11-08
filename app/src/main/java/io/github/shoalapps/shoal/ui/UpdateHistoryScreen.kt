@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.shoalapps.shoal.ui

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.shoalapps.shoal.AppLanguage
import io.github.shoalapps.shoal.Graph
import io.github.shoalapps.shoal.LocalisedIcon as Icon
import io.github.shoalapps.shoal.LocalisedText as Text
import io.github.shoalapps.shoal.data.HistoryEntry

/** Every install, update and failure Shoal has recorded on this phone, newest first. */
@Composable
fun UpdateHistoryScreen(onBack: () -> Unit) {
    val history by Graph.store.history.collectAsState()
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Update history") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
            actions = {
                if (history.isNotEmpty()) IconButton(onClick = { confirmClear = true }) { Icon(Icons.Rounded.Delete, "Clear history") }
            },
        )
    }) { pad ->
        if (history.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad).padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Installs, updates and failures will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(top = pad.calculateTopPadding()).wrapContentWidth().widthIn(max = 720.dp),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                itemsIndexed(history, key = { i, e -> "${e.time}-${e.packageName}-$i" }) { i, entry ->
                    if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    HistoryRow(entry)
                }
            }
        }
    }

    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("Clear update history?") },
        text = { Text("This only removes the list. Your apps stay as they are.") },
        confirmButton = { TextButton(onClick = { Graph.store.clearHistory(); confirmClear = false }) { Text("Clear") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
    )
}

@Composable
private fun HistoryRow(entry: HistoryEntry) {
    val failed = entry.kind == HistoryEntry.Kind.FAILED
    val status = AppLanguage.text(
        when (entry.kind) {
            HistoryEntry.Kind.INSTALLED -> "Installed"
            HistoryEntry.Kind.UPDATED -> "Updated"
            HistoryEntry.Kind.FAILED -> if (entry.fromBuild == null) "Couldn't install" else "Couldn't update"
        },
    )
    val builds = when {
        entry.fromBuild != null && entry.toBuild != null && entry.fromBuild != entry.toBuild -> "${entry.fromBuild} → ${entry.toBuild}"
        entry.toBuild != null -> "${entry.toBuild}"
        else -> null
    }
    val details = listOfNotNull(
        status,
        builds?.let { "${AppLanguage.text("Build")} $it" },
        if (entry.background) AppLanguage.text("Automatic") else null,
    ).joinToString(" · ")
    val ago = DateUtils.getRelativeTimeSpanString(entry.time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()

    ListItem(
        leadingContent = {
            Icon(
                when (entry.kind) {
                    HistoryEntry.Kind.INSTALLED -> Icons.Rounded.Download
                    HistoryEntry.Kind.UPDATED -> Icons.Rounded.CheckCircle
                    HistoryEntry.Kind.FAILED -> Icons.Rounded.ErrorOutline
                },
                null,
                tint = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        },
        headlineContent = { Text(entry.name, localise = false) },
        supportingContent = {
            androidx.compose.foundation.layout.Column {
                Text(details, localise = false)
                entry.message?.let { Text(AppLanguage.text(it), localise = false, color = MaterialTheme.colorScheme.error) }
            }
        },
        trailingContent = { Text(ago, localise = false, style = MaterialTheme.typography.labelMedium) },
    )
}
