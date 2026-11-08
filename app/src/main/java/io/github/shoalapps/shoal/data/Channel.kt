package io.github.shoalapps.shoal.data

/**
 * Which builds an app updates to. Every push to an app's main branch publishes a build; the
 * slower channels wait until a build has been out for a while, so problems are found and fixed
 * on Nightly first. Marking a release as a pre-release on GitHub keeps it off Beta and Stable.
 */
enum class Channel(val key: String, val label: String, val minAgeDays: Int) {
    STABLE("stable", "Stable", 7),
    BETA("beta", "Beta", 2),
    NIGHTLY("nightly", "Nightly", 0);

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key } ?: NIGHTLY
    }
}

/** A published build, as GitHub's release list describes it. */
data class PublishedBuild(val build: Int, val publishedAt: Long, val prerelease: Boolean, val apkUrl: String?, val pageUrl: String)

object Channels {
    private const val DAY = 24 * 60 * 60 * 1000L

    /**
     * The build a channel offers: the newest one old enough for it. If every build in the list is
     * too new (a busy week), the oldest listed one is offered, so the channel never stalls.
     */
    fun pick(builds: List<PublishedBuild>, channel: Channel, now: Long, listIsFull: Boolean): PublishedBuild? {
        val eligible = builds.filter { channel == Channel.NIGHTLY || !it.prerelease }.sortedByDescending { it.build }
        if (channel.minAgeDays == 0) return eligible.firstOrNull()
        val aged = eligible.firstOrNull { now - it.publishedAt >= channel.minAgeDays * DAY }
        return aged ?: if (listIsFull) eligible.lastOrNull() else null
    }
}
