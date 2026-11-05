package io.github.shoalapps.shoal

import io.github.shoalapps.shoal.data.Catalog
import io.github.shoalapps.shoal.data.Releases
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReleasesTest {
    @Test fun buildNumbersComeFromReleaseTags() {
        assertEquals(42, Releases.buildOf("https://github.com/ShoalApps/angelfish/releases/tag/build-42"))
        assertEquals(7, Releases.buildOf("build-7"))
        assertNull(Releases.buildOf("v1.0"))
    }

    private fun github(vararg routes: Pair<String, String>) = okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
        val request = chain.request()
        val location = routes.toMap()[request.url.toString()]
        okhttp3.Response.Builder().request(request).protocol(okhttp3.Protocol.HTTP_2)
            .code(if (location == null) 404 else 302).message("").body(ByteArray(0).toResponseBody())
            .apply { if (location != null) header("Location", location) }.build()
    }.build()

    @Test fun aRenamedRepositoryStillFindsItsLatestRelease() {
        val http = github(
            "https://github.com/ShoalApps/swordfish/releases/latest" to "https://github.com/ShoalApps/angelfish/releases/latest",
            "https://github.com/ShoalApps/angelfish/releases/latest" to "https://github.com/ShoalApps/angelfish/releases/tag/build-66",
        )
        assertEquals("build-66", Releases.latestTag(http, "ShoalApps/swordfish")?.first)
    }

    @Test fun anEndlessRedirectGivesUpAndAMissingOneIsNoRelease() {
        val loop = github("https://github.com/A/b/releases/latest" to "https://github.com/A/b/releases/latest")
        assertNull(Releases.latestTag(loop, "A/b"))
        assertNull(Releases.latestTag(github(), "A/b"))
    }

    private fun apk() = java.io.File.createTempFile("test", ".apk").apply { writeText("apk"); deleteOnExit() }

    @Test fun aHungInstallIsStopped() {
        val process = ProcessBuilder("sleep", "60").start()
        val outcome = io.github.shoalapps.shoal.data.Installer.feed(process, apk(), 300)
        assert(outcome is io.github.shoalapps.shoal.data.Installer.Outcome.Failed)
        assert(!process.isAlive || process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS))
    }

    @Test fun anInstallReportsWhatHappened() {
        val installed = ProcessBuilder("sh", "-c", "cat >/dev/null; echo Success").start()
        assertEquals(io.github.shoalapps.shoal.data.Installer.Outcome.Installed, io.github.shoalapps.shoal.data.Installer.feed(installed, apk(), 5000))
        val failed = ProcessBuilder("sh", "-c", "cat >/dev/null; echo Failure no space >&2").start()
        assertEquals("Failure no space", (io.github.shoalapps.shoal.data.Installer.feed(failed, apk(), 5000) as io.github.shoalapps.shoal.data.Installer.Outcome.Failed).message)
    }

    @Test fun fingerprintsAreUppercaseHexLikeTheCatalogs() {
        assertEquals("BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD", Releases.sha256("abc".toByteArray()))
        assertEquals(64, Catalog.RELEASE_CERT_SHA256.length)
    }

    @Test fun everyAppHasItsOwnRepoAndApk() {
        assertEquals(Catalog.apps.size, Catalog.apps.map { it.packageName }.toSet().size)
        Catalog.apps.forEach { assert(it.asset.endsWith(".apk") && it.repo.startsWith("ShoalApps/")) }
    }
}
