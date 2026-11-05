@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package io.github.shoalapps.shoal.ui

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.hoverable
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import io.github.shoalapps.shoal.LocalisedIcon as Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import io.github.shoalapps.shoal.LocalisedText as Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.github.shoalapps.shoal.AppState
import io.github.shoalapps.shoal.Graph
import io.github.shoalapps.shoal.data.Catalog
import io.github.shoalapps.shoal.data.Installer
import io.github.shoalapps.shoal.data.ShoalApp
import io.github.shoalapps.shoal.data.eligibleForUpdate
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(highlight: String?, onSettings: () -> Unit, onAccounts: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by Graph.store.settings.collectAsState()
    val heldPackages by Graph.store.heldPackages.collectAsState()
    var methodKey by remember { mutableIntStateOf(0) }
    val shizuku = remember(Graph.methodVersion, methodKey) { Installer.shizukuState(context) }
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) { Graph.check() }
    LaunchedEffect(highlight) {
        // Opened from another app's "Check for updates": check and bring that app into view.
        val i = Catalog.apps.indexOfFirst { it.packageName == highlight }
        if (i >= 0) { Graph.check(); listState.animateScrollToItem(i + 2) }
    }
    val updates = Catalog.apps.count { eligibleForUpdate(it.packageName, Graph.states[it.packageName]?.updateAvailable == true, heldPackages) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shoal", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { scope.launch { Graph.check() } }) { Icon(Icons.Rounded.Refresh, "Check for updates") }
                    IconButton(onClick = onAccounts) { Icon(Icons.Rounded.AccountCircle, "Accounts") }
                    IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, "Settings") }
                },
            )
        },
    ) { pad ->
        PullToRefreshBox(isRefreshing = Graph.checking, onRefresh = { scope.launch { Graph.check() } }, modifier = Modifier.padding(top = pad.calculateTopPadding())) {
            LazyColumn(
                Modifier.fillMaxSize().wrapContentWidth().widthIn(max = 720.dp),
                state = listState,
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "method") {
                    MethodCard(settings.installMethod, shizuku, methodKey + Graph.methodVersion) { methodKey++ }
                }
                item(key = "all") {
                    if (updates > 0) {
                        Button(onClick = { scope.launch { Graph.updateAll() } }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                            Text(if (updates == 1) "Update 1 app" else "Update all $updates apps")
                        }
                    }
                }
                items(Catalog.apps, key = { it.packageName }) { app ->
                    AppCard(app, Graph.states[app.packageName] ?: AppState(), app.packageName == highlight, app.packageName in heldPackages)
                }
            }
        }
    }
}

/** How installs happen on this phone, and what would make them silent. */
@Composable
private fun MethodCard(preferred: String, shizuku: Installer.ShizukuState, key: Int, onChanged: () -> Unit) {
    val context = LocalContext.current
    var method by remember { mutableStateOf<Installer.Method?>(null) }
    LaunchedEffect(preferred, shizuku, key) { method = Installer.pick(context, preferred) }
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(
                when (method) {
                    Installer.Method.SHIZUKU -> if (Installer.helperPackage(context) == Installer.SHEVERY_PACKAGE) "Silent updates with Shevery" else "Silent updates with Shizuku"
                    Installer.Method.ROOT -> "Silent updates with root"
                    else -> "Updates ask before installing"
                },
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            )
            Text(
                when (method) {
                    Installer.Method.SHIZUKU, Installer.Method.ROOT -> "New versions install in the background, checked against Shoal's signature first."
                    else -> "Android confirms each install. On Android 12 and later, apps the Manager installed can update without asking. For fully silent updates, use Shevery (a Material 3 take on Shizuku) or root."
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (method == Installer.Method.STANDARD || method == null) {
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (shizuku) {
                        Installer.ShizukuState.NEEDS_PERMISSION -> FilledTonalButton(onClick = { Installer.requestShizuku(); onChanged() }) {
                            Text(if (Installer.helperPackage(context) == Installer.SHEVERY_PACKAGE) "Allow Shevery" else "Allow Shizuku")
                        }
                        Installer.ShizukuState.NOT_RUNNING -> FilledTonalButton(onClick = {
                            Installer.helperPackage(context)?.let { pkg -> runCatching { context.startActivity(context.packageManager.getLaunchIntentForPackage(pkg)) } }
                        }) { Text(if (Installer.helperPackage(context) == Installer.SHEVERY_PACKAGE) "Start Shevery" else "Start Shizuku") }
                        Installer.ShizukuState.NOT_INSTALLED -> FilledTonalButton(onClick = {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Installer.SHEVERY_DOWNLOAD))) }
                        }) { Text("Get Shevery") }
                        Installer.ShizukuState.READY -> Unit
                    }
                    val scope = rememberCoroutineScope()
                    OutlinedButton(onClick = {
                        scope.launch {
                            if (Installer.hasRoot()) {
                                Graph.store.rootGranted = true
                                Graph.store.updateSettings { it.copy(installMethod = "auto") }
                            } else {
                                android.widget.Toast.makeText(context, "Root isn't available on this phone", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            onChanged()
                        }
                    }) { Text("Use root") }
                }
            }
        }
    }
}

@Composable
private fun AppCard(app: ShoalApp, state: AppState, highlighted: Boolean, held: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val icon: Drawable? = remember(state.installed) { runCatching { context.packageManager.getApplicationIcon(app.packageName) }.getOrNull() }
    val busy = state.progress != null
    var menu by remember { mutableStateOf(false) }
    var showNotes by rememberSaveable { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().testTag("app-${app.packageName}"),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Image(icon.toBitmap(144, 144).asImageBitmap(), null, Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)))
                } else {
                    // Each app's fish, in its own colours.
                    BubblingFish(app.repo)
                }
                Column(Modifier.padding(start = 16.dp).weight(1f)) {
                    Text(app.name, localise = false, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(app.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        versionLine(state),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (state.updateAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Spacer(Modifier.size(8.dp))
                when {
                    busy -> Unit
                    state.installed == null -> Button(onClick = { scope.launch { Graph.install(app) } }, enabled = state.latest != null) { Text("Install") }
                    state.updateAvailable -> Button(onClick = { scope.launch { Graph.install(app) } }) { Text("Update") }
                    app != Catalog.manager -> FilledTonalButton(onClick = {
                        context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { context.startActivity(it) }
                    }) { Text("Open") }
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "More for ${app.name}") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("What's new") }, onClick = {
                            menu = false
                            showNotes = !showNotes
                        })
                        DropdownMenuItem(text = { Text("Source code") }, onClick = {
                            menu = false
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/${app.repo}")))
                        })
                        if (state.installed != null || held) {
                            DropdownMenuItem(text = { Text(if (held) "Allow updates" else "Keep this version") }, enabled = !busy, onClick = {
                                menu = false
                                Graph.store.setHeld(app.packageName, !held)
                            })
                        }
                        if (state.installed != null && app != Catalog.manager) {
                            DropdownMenuItem(text = { Text("Uninstall") }, onClick = {
                                menu = false
                                context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}")))
                            })
                        }
                    }
                }
            }
            if (showNotes) ReleaseNotes(app, state.latest?.build, onClose = { showNotes = false })
            if (held) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Keeping this version", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { Graph.store.setHeld(app.packageName, false) }) { Text("Allow updates") }
                }
            }
            if (app == Catalog.manager && (held || menu)) {
                Text("Keeping Shoal on this version means it won't get fixes.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
            state.progress?.let { p ->
                Column(Modifier.padding(top = 12.dp)) {
                    if (p >= 0f) LinearWavyProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth())
                    else LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(if (p >= 0f) "Downloading ${(p * 100).toInt()}%" else "Installing…", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 6.dp))
                }
            }
            state.message?.let {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                    TextButton(onClick = { scope.launch { Graph.install(app) } }) { Text("Try again") }
                }
            }
        }
    }
}

private fun versionLine(s: AppState): String = when {
    s.installed == null && s.latest != null -> "Not installed · build ${s.latest.build} available"
    s.installed == null -> "Not installed"
    s.updateAvailable -> "Build ${s.installed} installed · build ${s.latest!!.build} available"
    s.latest != null -> "Build ${s.installed} · up to date"
    else -> "Build ${s.installed}"
}

/** Each app's icon as (tile, fish), by repository name. */
private fun fishIcon(repo: String): Pair<Int, Int> = when (repo.substringAfter('/')) {
    "angelfish" -> io.github.shoalapps.shoal.R.drawable.fish_angelfish_tile to io.github.shoalapps.shoal.R.drawable.fish_angelfish
    "butterflyfish" -> io.github.shoalapps.shoal.R.drawable.fish_butterflyfish_tile to io.github.shoalapps.shoal.R.drawable.fish_butterflyfish
    "squill" -> io.github.shoalapps.shoal.R.drawable.fish_squill_tile to io.github.shoalapps.shoal.R.drawable.fish_squill
    else -> io.github.shoalapps.shoal.R.drawable.fish_shoal_tile to io.github.shoalapps.shoal.R.drawable.fish_shoal
}

/**
 * An app's icon. It stays still: touching or hovering over it sends a shine across it and a few
 * bubbles rising out of it. Nothing happens if animations are off.
 */
@Composable
private fun BubblingFish(repo: String) {
    val (tile, fish) = fishIcon(repo)
    val moves = remember { android.animation.ValueAnimator.areAnimatorsEnabled() }
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    var burst by remember { mutableIntStateOf(0) }
    val time = remember { Animatable(1f) }
    LaunchedEffect(hovered) { if (hovered) burst++ }
    LaunchedEffect(burst) {
        if (burst > 0 && moves) {
            time.snapTo(0f)
            time.animateTo(1f, tween(1500, easing = LinearEasing))
        }
    }
    val bubbles = remember(burst) {
        val random = kotlin.random.Random(burst + repo.hashCode())
        List(7) { Bubble(.15f + .7f * random.nextFloat(), .35f * random.nextFloat(), 1.5f + 2.5f * random.nextFloat(), .35f + .65f * random.nextFloat(), random.nextFloat() * 6.28f) }
    }
    val ring = MaterialTheme.colorScheme.primary
    Box(
        Modifier.size(52.dp).hoverable(source).pointerInput(Unit) {
            // Only watches: the press still scrolls the list or reaches the card.
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                burst++
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.matchParentSize().clip(RoundedCornerShape(16.dp)).drawWithContent {
                drawContent()
                val shine = time.value / .45f
                if (shine < 1f) {
                    val x = -size.width + shine * size.width * 3f
                    drawRect(
                        Brush.linearGradient(
                            0f to Color.Transparent, .5f to Color.White.copy(alpha = .6f), 1f to Color.Transparent,
                            start = Offset(x - size.width * .3f, 0f), end = Offset(x + size.width * .3f, size.height),
                        ),
                    )
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(tile), null, Modifier.requiredSize(76.dp))
            Image(painterResource(fish), null, Modifier.requiredSize(76.dp))
        }
        Canvas(Modifier.matchParentSize()) {
            val t = time.value
            if (t < 1f) for (b in bubbles) {
                val life = ((t - b.delay) / (1f - b.delay)).coerceIn(0f, 1f)
                if (life <= 0f || life >= 1f) continue
                val y = size.height * (.7f - 1.15f * b.rise * life)
                val x = size.width * b.x + 3.dp.toPx() * kotlin.math.sin(b.wobble + life * 7f)
                val radius = b.radius.dp.toPx() * (.7f + .5f * life)
                val alpha = (1f - life) * .9f
                drawCircle(ring.copy(alpha = alpha), radius, Offset(x, y), style = Stroke(1.dp.toPx()))
                drawCircle(ring.copy(alpha = alpha * .9f), radius * .22f, Offset(x - radius * .35f, y - radius * .35f))
            }
        }
    }
}

private class Bubble(val x: Float, val delay: Float, val radius: Float, val rise: Float, val wobble: Float)
