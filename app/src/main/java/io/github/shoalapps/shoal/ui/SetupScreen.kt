package io.github.shoalapps.shoal.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.shoalapps.shoal.Graph
import io.github.shoalapps.common.ui.LocalisedText as Text
import io.github.shoalapps.shoal.data.Catalog

private fun battery(context: Context, pkg: String) =
    context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(pkg)

private fun notificationsAllowed(context: Context) = Build.VERSION.SDK_INT < 33 ||
    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

@Composable
private fun Step(title: String, why: String, done: Boolean, action: String, onAction: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(why, style = MaterialTheme.typography.bodyMedium)
            if (done) Text("Done", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            else OutlinedButton(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
private fun Choice(title: String, why: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(why, style = MaterialTheme.typography.bodySmall)
        }
        Switch(on, onChange)
    }
}

/** What Shoal needs from Android to keep the apps updated. Everything can be skipped. */
@Composable
fun SetupScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val settings by Graph.store.settings.collectAsState()
    var tick by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val watcher = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(watcher)
        onDispose { owner.lifecycle.removeObserver(watcher) }
    }
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }
    fun open(intent: Intent) { runCatching { context.startActivity(intent) } }
    val packageUri = Uri.parse("package:${context.packageName}")
    tick.let { }
    val others = Catalog.apps.filter { it != Catalog.manager && it.packageName.let { p -> runCatching { context.packageManager.getPackageInfo(p, 0) }.isSuccess } }
    Scaffold { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 620.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Set up Shoal", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("A few things Android needs you to allow, so your apps stay up to date by themselves. You can change any of them later.", style = MaterialTheme.typography.bodyLarge)
                Step("Install and update apps", "Lets Shoal install the apps and their updates. Without this, it can't update anything.", context.packageManager.canRequestPackageInstalls(), "Allow") {
                    open(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageUri))
                }
                Step("Stop battery optimisation", "Android can otherwise put off Shoal's daily update check, sometimes for days.", battery(context, context.packageName), "Allow") {
                    open(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri))
                }
                Step("Notifications", "Tells you when updates are ready, or when one needs a tap.", notificationsAllowed(context), "Allow") {
                    if (Build.VERSION.SDK_INT >= 33) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Updates in the background", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Choice("Update automatically", "Check once a day and download anything new", settings.autoUpdate) { v -> Graph.store.updateSettings { it.copy(autoUpdate = v) }; io.github.shoalapps.shoal.data.UpdateWorker.schedule(context) }
                        Choice("Wi-Fi only", "Turn off to allow updates on mobile data too", settings.wifiOnly) { v -> Graph.store.updateSettings { it.copy(wifiOnly = v) }; io.github.shoalapps.shoal.data.UpdateWorker.schedule(context) }
                        Text("Android lets Shoal update apps it installed without asking, on Android 12 and later. Some updates still need one tap, unless you use Shevery, Shizuku or root.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (others.isNotEmpty()) {
                    Text("Your other Shoal apps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Worth doing if an app's reminders or notifications arrive late. Open its page, then find Battery and choose Unrestricted.", style = MaterialTheme.typography.bodyMedium)
                    others.forEach { app ->
                        Step(app.name, if (battery(context, app.packageName)) "Battery optimisation is off for this app." else "Battery optimisation is on for this app.", battery(context, app.packageName), "Open ${app.name}'s page") {
                            open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}")))
                        }
                    }
                }
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Finish") }
            }
        }
    }
}
