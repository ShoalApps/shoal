package io.github.shoalapps.shoal.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

@Serializable
data class Settings(
    /** 0 = follow the phone, 1 = light, 2 = dark */
    val theme: Int = 0,
    val dynamicColor: Boolean = true,
    /** "auto", "standard", "shizuku" or "root" */
    val installMethod: String = "auto",
    /** Check daily and install updates in the background. */
    val autoUpdate: Boolean = true,
    val wifiOnly: Boolean = true,
    /** "stable", "beta" or "nightly": see [Channel]. */
    val channel: String = "nightly",
)

/** Settings, kept privately on the phone. */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("manager", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _settings = MutableStateFlow(prefs.getString("settings", null)?.let { runCatching { json.decodeFromString<Settings>(it) }.getOrNull() } ?: Settings())
    val settings: StateFlow<Settings> = _settings

    fun updateSettings(change: (Settings) -> Settings) {
        _settings.update(change)
        prefs.edit().putString("settings", json.encodeToString(Settings.serializer(), _settings.value)).apply()
    }

    private val _heldPackages = MutableStateFlow(prefs.getStringSet("heldPackages", emptySet()).orEmpty().toSet())
    val heldPackages: StateFlow<Set<String>> = _heldPackages

    /** Holds follow package names, even if an app is removed or no longer in the catalog. */
    fun setHeld(packageName: String, held: Boolean) {
        _heldPackages.update { if (held) it + packageName else it - packageName }
        prefs.edit().putStringSet("heldPackages", _heldPackages.value).apply()
    }

    /** Set once root worked, so automatic mode doesn't ask the root manager on every check. */
    var rootGranted: Boolean
        get() = prefs.getBoolean("rootGranted", false)
        set(value) = prefs.edit().putBoolean("rootGranted", value).apply()

    /** Whether the setup screen has been seen. */
    var setupDone: Boolean
        get() = prefs.getBoolean("setupDone", false)
        set(value) = prefs.edit().putBoolean("setupDone", value).apply()

    var lastCheck: Long
        get() = prefs.getLong("lastCheck", 0)
        set(value) = prefs.edit().putLong("lastCheck", value).apply()

    private val historySerializer = ListSerializer(HistoryEntry.serializer())
    private val _history = MutableStateFlow(prefs.getString("history", null)?.let { runCatching { json.decodeFromString(historySerializer, it) }.getOrNull() } ?: emptyList())
    /** Installs, updates and failures, newest first. */
    val history: StateFlow<List<HistoryEntry>> = _history

    @Synchronized
    fun record(entry: HistoryEntry) {
        _history.update { UpdateHistory.add(it, entry) }
        prefs.edit().putString("history", json.encodeToString(historySerializer, _history.value)).apply()
    }

    @Synchronized
    fun clearHistory() {
        _history.value = emptyList()
        prefs.edit().remove("history").apply()
    }

    private val attemptsSerializer = MapSerializer(String.serializer(), Attempt.serializer())
    private var attemptsCache: Map<String, Attempt> = prefs.getString("attempts", null)?.let { runCatching { json.decodeFromString(attemptsSerializer, it) }.getOrNull() } ?: emptyMap()

    /** Installs that have started but not finished, by package. */
    val attempts: Map<String, Attempt> @Synchronized get() = attemptsCache

    @Synchronized
    fun setAttempt(packageName: String, attempt: Attempt?) {
        attemptsCache = if (attempt == null) attemptsCache - packageName else attemptsCache + (packageName to attempt)
        // Written straight away: Shoal updating itself is stopped by Android part-way through.
        prefs.edit().putString("attempts", json.encodeToString(attemptsSerializer, attemptsCache)).commit()
    }
}
