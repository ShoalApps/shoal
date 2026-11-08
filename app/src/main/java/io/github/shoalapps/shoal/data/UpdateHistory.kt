package io.github.shoalapps.shoal.data

import kotlinx.serialization.Serializable

/** One install, update or failure, kept on the phone for the update history. */
@Serializable
data class HistoryEntry(
    val time: Long,
    val packageName: String,
    val name: String,
    val kind: Kind,
    /** The build installed before, if there was one. */
    val fromBuild: Int? = null,
    /** The build installed afterwards, or the one that failed to go in. */
    val toBuild: Int? = null,
    /** Why it failed. */
    val message: String? = null,
    /** Started by the daily check rather than by you. */
    val background: Boolean = false,
) {
    enum class Kind { INSTALLED, UPDATED, FAILED }
}

/** An install that has started but not yet finished, kept so its result can still be recorded if Shoal restarts. */
@Serializable
data class Attempt(val fromBuild: Int?, val toBuild: Int?, val started: Long, val background: Boolean = false)

object UpdateHistory {
    /** The history keeps this many entries, newest first. */
    const val LIMIT = 200

    /** How long an unfinished install is remembered before it's recorded as not finishing. */
    const val ATTEMPT_EXPIRY_MILLIS = 24 * 60 * 60 * 1000L

    fun add(entries: List<HistoryEntry>, entry: HistoryEntry, limit: Int = LIMIT): List<HistoryEntry> =
        if (isRepeat(entries, entry)) entries else (listOf(entry) + entries).take(limit)

    /**
     * Whether this success was already recorded: Shoal can notice a finished install when it
     * starts again, and Android can then report the same install a moment later.
     */
    fun isRepeat(entries: List<HistoryEntry>, entry: HistoryEntry): Boolean {
        if (entry.kind == HistoryEntry.Kind.FAILED) return false
        val last = entries.firstOrNull { it.packageName == entry.packageName } ?: return false
        return last.kind != HistoryEntry.Kind.FAILED && last.toBuild == entry.toBuild
    }

    /** The entry for an install that went in. Installing over an existing build counts as an update. */
    fun success(app: ShoalApp, attempt: Attempt?, installedBuild: Int?, now: Long) = HistoryEntry(
        time = now,
        packageName = app.packageName,
        name = app.name,
        kind = if (attempt?.fromBuild != null) HistoryEntry.Kind.UPDATED else HistoryEntry.Kind.INSTALLED,
        fromBuild = attempt?.fromBuild,
        toBuild = installedBuild ?: attempt?.toBuild,
        background = attempt?.background ?: false,
    )

    fun failure(app: ShoalApp, attempt: Attempt?, message: String, now: Long) = HistoryEntry(
        time = now,
        packageName = app.packageName,
        name = app.name,
        kind = HistoryEntry.Kind.FAILED,
        fromBuild = attempt?.fromBuild,
        toBuild = attempt?.toBuild,
        message = message,
        background = attempt?.background ?: false,
    )

    /**
     * What to do with an unfinished install when Shoal starts again, for example after Shoal
     * updated itself and Android restarted it: record it as installed once the new build is on
     * the phone, give up on it after a day, and otherwise keep waiting.
     */
    fun settle(app: ShoalApp, attempt: Attempt, installedBuild: Int?, now: Long): HistoryEntry? = when {
        installedBuild != null && attempt.toBuild != null && installedBuild >= attempt.toBuild -> success(app, attempt, installedBuild, now)
        now - attempt.started > ATTEMPT_EXPIRY_MILLIS -> failure(app, attempt, "The install didn't finish", now)
        else -> null
    }
}
