package io.github.shoalapps.shoal.data

/** A Shoal app the Manager looks after. Every push to its repo publishes a `build-<n>` release. */
data class ShoalApp(
    val name: String,
    val packageName: String,
    val repo: String,
    val asset: String,
    val description: String,
)

object Catalog {
    val video = ShoalApp("Angelfish", "io.github.shoalapps.swordfish", "ShoalApps/angelfish", "Angelfish.apk", "Watch YouTube videos with SponsorBlock, DeArrow and a TV mode")
    val social = ShoalApp("Butterflyfish", "io.github.shoalapps.blobfish", "ShoalApps/butterflyfish", "Butterflyfish.apk", "Bluesky (and X) laid out like Bluesky, with rich formatting")
    val clips = ShoalApp("Clownfish", "io.github.shoalapps.clownfish", "ShoalApps/clownfish", "Clownfish.apk", "Shorts, TikTok and Reels in one place")
    val music = ShoalApp("Pufferfish", "io.github.shoalapps.pufferfish", "ShoalApps/pufferfish", "Pufferfish.apk", "YouTube Music laid out like Spotify, with offline downloads and lyrics")
    val reddit = ShoalApp("Anglerfish", "io.github.shoalapps.anglerfish", "ShoalApps/anglerfish", "Anglerfish.apk", "Reddit as an app: Material tabs, without the get-the-app nags")
    val reminders = ShoalApp("Squill", "io.github.shoalapps.squill", "ShoalApps/squill", "Squill.apk", "Notes, links and reminders, as plain files on your phone")
    val manager = ShoalApp("Shoal", "io.github.shoalapps.shoal", "ShoalApps/shoal", "Shoal.apk", "Installs and updates the Shoal apps")

    // Clownfish, Pufferfish and Anglerfish are archived for now, so Shoal doesn't list them.
    val apps = listOf(reminders, video, social, manager)

    fun of(packageName: String) = apps.firstOrNull { it.packageName == packageName }

    /**
     * SHA-256 of the certificate every Shoal release is signed with. The Manager installs
     * nothing else, so a tampered download can't be installed even with root.
     */
    const val RELEASE_CERT_SHA256 = "2BA3700D9C8733489CF865E962E0DAD438C00C62DA9B4A89B44ACB4E6325ACF4"
}
