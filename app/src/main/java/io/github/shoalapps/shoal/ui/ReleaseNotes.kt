package io.github.shoalapps.shoal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.shoalapps.shoal.Graph
import io.github.shoalapps.shoal.LocalisedText as Text
import io.github.shoalapps.shoal.data.Releases
import io.github.shoalapps.shoal.data.ShoalApp
import kotlinx.coroutines.CancellationException

@Composable
internal fun ReleaseNotes(app: ShoalApp, latestBuild: Int?, onClose: () -> Unit) {
    var notes by remember(app, latestBuild) { mutableStateOf<List<Releases.Note>?>(null) }
    var failed by remember(app, latestBuild) { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    var history by rememberSaveable(app.packageName, latestBuild) { mutableStateOf(false) }

    LaunchedEffect(app, latestBuild, attempt) {
        failed = false
        try {
            notes = Releases.notes(Graph.http, app)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            failed = true
        }
    }

    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("What's new", style = MaterialTheme.typography.titleMedium)
        val entries = notes
        when {
            failed -> {
                Text("Couldn't load release notes")
                TextButton(onClick = { attempt++ }) { Text("Try again") }
            }
            entries == null -> Text("Loading release notes…")
            entries.isEmpty() -> Text("No releases yet")
            else -> {
                ReleaseNote(entries.first())
                if (entries.size > 1) {
                    TextButton(onClick = { history = !history }) {
                        Text(if (history) "Hide history" else "Show more history")
                    }
                    if (history) entries.drop(1).forEach { ReleaseNote(it) }
                }
            }
        }
        TextButton(onClick = onClose) { Text("Close") }
    }
}

@Composable
private fun ReleaseNote(note: Releases.Note) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Build ${note.build}", style = MaterialTheme.typography.labelLarge)
        if (note.message == null) Text("Minor bug fixes")
        else Text(note.message, localise = false)
    }
}
