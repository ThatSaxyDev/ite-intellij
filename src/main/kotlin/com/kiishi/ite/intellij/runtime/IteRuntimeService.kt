package com.kiishi.ite.intellij.runtime

import com.google.gson.Gson
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.kiishi.ite.intellij.settings.IteSettingsState
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.Duration
import java.util.Comparator
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.name

@Service(Service.Level.APP)
class IteRuntimeService {
    private val gson = Gson()
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    fun resolveRuntime(): IteRuntime {
        val failedMessages = mutableListOf<String>()
        for ((executable, source) in runtimeCandidates()) {
            val result = checkExecutable(executable)
            if (result.installed) {
                return result.copy(source = source)
            }
            failedMessages += result.message
        }

        return IteRuntime(
            installed = false,
            executable = systemExecutable(),
            source = "none",
            message = failedMessages.firstOrNull { it.isNotBlank() } ?: "No iTE executable was found.",
        )
    }

    fun installManagedRuntime(indicator: ProgressIndicator): String {
        indicator.text = "Checking latest iTE release"
        val release = releaseForCurrentTarget()
        val asset = release.asset
        val installRoot = managedInstallRoot()
        val stampPath = installRoot.resolve(".sha256")
        val binPath = defaultInstalledExecutable()
        val existingStamp = readFirstLine(stampPath)

        if (!asset.sha256.isNullOrBlank() && existingStamp == asset.sha256) {
            val existing = checkExecutable(binPath.absolutePathString())
            if (existing.installed) {
                return binPath.absolutePathString()
            }
        }

        val archiveType = asset.archiveType ?: "tar.gz"
        val archivePath = Files.createTempFile("ite-${release.manifest.version ?: "latest"}-${release.target}-", ".$archiveType")

        try {
            indicator.checkCanceled()
            indicator.text = "Downloading iTE ${release.manifest.version ?: "latest"}"
            downloadFile(asset.url ?: error("Release asset has no URL."), archivePath)

            if (!asset.sha256.isNullOrBlank()) {
                indicator.checkCanceled()
                indicator.text = "Verifying iTE download"
                if (sha256File(archivePath) != asset.sha256) {
                    error("Downloaded iTE archive failed checksum verification.")
                }
            }

            indicator.checkCanceled()
            indicator.text = "Installing iTE"
            return installArtifact(archivePath, asset, release.manifest.version ?: "")
        } finally {
            Files.deleteIfExists(archivePath)
        }
    }

    fun getManagedRuntimeUpdate(): IteUpdate? {
        val configuredExecutable = systemExecutable()
        val managedExecutable = defaultInstalledExecutable().absolutePathString()
        if (configuredExecutable != "ite" && configuredExecutable != managedExecutable) {
            return null
        }

        val installed = checkExecutable(managedExecutable)
        if (!installed.installed) {
            return null
        }

        val release = releaseForCurrentTarget()
        val metadata = readManagedInstallMetadata()
        val latestVersion = release.manifest.version.orEmpty()
        val latestSha = release.asset.sha256.orEmpty()
        val installedVersion = metadata.version.ifBlank { parseVersion(installed.message) }
        val updateKey = "${release.target}:$latestVersion:$latestSha"

        if (latestSha.isNotBlank() && metadata.sha256.isNotBlank()) {
            return if (latestSha == metadata.sha256) null else IteUpdate(updateKey, installedVersion, latestVersion)
        }

        if (latestVersion.isNotBlank() && installedVersion.isNotBlank() && compareVersions(latestVersion, installedVersion) > 0) {
            return IteUpdate(updateKey, installedVersion, latestVersion)
        }

        return null
    }

    private fun runtimeCandidates(): List<Pair<String, String>> {
        val configured = systemExecutable()
        val installed = defaultInstalledExecutable().absolutePathString()
        val candidates = mutableListOf(configured to if (configured == "ite") "PATH" else "configured")
        if (installed != configured) {
            candidates += installed to "installed"
        }
        return candidates
    }

    private fun systemExecutable(): String {
        return IteSettingsState.getInstance().state.executable.trim().ifEmpty { "ite" }
    }

    private fun checkExecutable(executable: String): IteRuntime {
        return try {
            val process = ProcessBuilder(executable, "--version")
                .redirectErrorStream(true)
                .start()

            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return IteRuntime(false, executable, "unknown", "Timed out while checking $executable.")
            }

            val output = process.inputStream.bufferedReader().readText().trim()
            if (process.exitValue() == 0) {
                IteRuntime(true, executable, "unknown", output)
            } else {
                IteRuntime(false, executable, "unknown", output.ifBlank { "Exited with ${process.exitValue()}." })
            }
        } catch (error: Exception) {
            IteRuntime(false, executable, "unknown", error.message ?: error.javaClass.simpleName)
        }
    }

    private fun releaseForCurrentTarget(): ReleaseForTarget {
        val manifest = downloadManifest()
        val target = targetKey()
        val asset = manifest.assets?.get(target) ?: error("No iTE build is available for $target.")
        if (asset.url.isNullOrBlank()) {
            error("No iTE download URL is available for $target.")
        }
        return ReleaseForTarget(manifest, target, asset)
    }

    private fun downloadManifest(): ReleaseManifest {
        val request = HttpRequest.newBuilder(RELEASE_MANIFEST_URI)
            .timeout(Duration.ofSeconds(30))
            .header("User-Agent", "iTE IntelliJ")
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            error("Release manifest request failed with HTTP ${response.statusCode()}.")
        }
        return gson.fromJson(response.body(), ReleaseManifest::class.java)
    }

    private fun downloadFile(url: String, destination: Path) {
        val request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(120))
            .header("User-Agent", "iTE IntelliJ")
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(destination))
        if (response.statusCode() != 200) {
            error("Download failed with HTTP ${response.statusCode()}: $url")
        }
    }

    private fun installArtifact(archivePath: Path, asset: ReleaseAsset, version: String): String {
        val archiveType = asset.archiveType ?: "tar.gz"
        if (archiveType != "tar.gz") {
            error("Unsupported iTE archive type: $archiveType")
        }

        val installRoot = managedInstallRoot()
        val appDir = installRoot.resolve("app")
        val binDir = installRoot.resolve("bin")
        val binPath = defaultInstalledExecutable()
        val stampPath = installRoot.resolve(".sha256")
        val extractDir = Files.createTempDirectory("ite-install-")

        try {
            runProcess(listOf("tar", "-xzf", archivePath.absolutePathString(), "-C", extractDir.absolutePathString()))

            val extracted = Files.list(extractDir).use { paths ->
                paths.filter { Files.isDirectory(it) }.findFirst().orElse(null)
            } ?: error("iTE archive extraction produced an unexpected layout.")

            deleteRecursively(appDir)
            Files.createDirectories(binDir)
            copyDirectoryContents(extracted, appDir)

            val executableName = asset.executable ?: if (isWindows()) "ite.exe" else "ite"
            val executable = findExecutable(appDir, executableName)
                ?: error("iTE executable was not found after extraction.")

            executable.toFile().setExecutable(true, false)
            Files.deleteIfExists(binPath)
            try {
                Files.createSymbolicLink(binPath, binDir.relativize(executable))
            } catch (_: Exception) {
                Files.copy(executable, binPath, StandardCopyOption.REPLACE_EXISTING)
                binPath.toFile().setExecutable(true, false)
            }

            Files.writeString(stampPath, "${asset.sha256.orEmpty()}\nversion=$version\n")
            return binPath.absolutePathString()
        } finally {
            deleteRecursively(extractDir)
        }
    }

    private fun copyDirectoryContents(source: Path, destination: Path) {
        Files.createDirectories(destination)
        Files.walk(source).use { paths ->
            paths.forEach { current ->
                val target = destination.resolve(source.relativize(current).toString())
                if (Files.isDirectory(current)) {
                    Files.createDirectories(target)
                } else {
                    Files.createDirectories(target.parent)
                    Files.copy(current, target, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }

    private fun findExecutable(root: Path, executableName: String): Path? {
        if (!root.exists()) {
            return null
        }
        return Files.walk(root).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.name == executableName }.findFirst().orElse(null)
        }
    }

    private fun runProcess(command: List<String>) {
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText().trim()
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            error("Command timed out: ${command.joinToString(" ")}")
        }
        if (process.exitValue() != 0) {
            error(output.ifBlank { "Command failed: ${command.joinToString(" ")}" })
        }
    }

    private fun sha256File(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun readFirstLine(path: Path): String {
        return runCatching { Files.readAllLines(path).firstOrNull()?.trim().orEmpty() }.getOrDefault("")
    }

    private fun readManagedInstallMetadata(): ManagedInstallMetadata {
        val stampPath = managedInstallRoot().resolve(".sha256")
        val lines = runCatching { Files.readAllLines(stampPath) }.getOrDefault(emptyList())
        val sha256 = lines.firstOrNull()?.trim().orEmpty()
        val version = lines.drop(1)
            .firstOrNull { it.startsWith("version=") }
            ?.substringAfter("version=")
            ?.trim()
            .orEmpty()
        return ManagedInstallMetadata(sha256, version)
    }

    private fun deleteRecursively(path: Path) {
        if (!path.exists()) {
            return
        }
        Files.walk(path).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    private fun managedInstallRoot(): Path {
        return if (isWindows()) {
            val localAppData = System.getenv("LOCALAPPDATA") ?: Path.of(System.getProperty("user.home"), "AppData", "Local").toString()
            Path.of(localAppData, "iTE")
        } else {
            Path.of(System.getProperty("user.home"), ".ite")
        }
    }

    private fun defaultInstalledExecutable(): Path {
        return managedInstallRoot().resolve("bin").resolve(if (isWindows()) "ite.exe" else "ite")
    }

    private fun targetKey(): String {
        val osName = when {
            isMac() -> "darwin"
            isLinux() -> "linux"
            isWindows() -> "win32"
            else -> error("iTE is not available for ${System.getProperty("os.name")}.")
        }
        val arch = when (System.getProperty("os.arch").lowercase()) {
            "aarch64", "arm64" -> "arm64"
            "x86_64", "amd64" -> "x64"
            else -> error("iTE is not available for ${System.getProperty("os.name")}-${System.getProperty("os.arch")}.")
        }
        return "$osName-$arch"
    }

    private fun parseVersion(text: String): String {
        return Regex("\\b(\\d+\\.\\d+\\.\\d+(?:[-+][0-9A-Za-z.-]+)?)\\b")
            .find(text)
            ?.groupValues
            ?.get(1)
            .orEmpty()
    }

    private fun compareVersions(left: String, right: String): Int {
        val a = left.split(Regex("[.-]")).map { it.toIntOrNull() ?: 0 }
        val b = right.split(Regex("[.-]")).map { it.toIntOrNull() ?: 0 }
        val length = maxOf(a.size, b.size)
        for (index in 0 until length) {
            val diff = (a.getOrNull(index) ?: 0) - (b.getOrNull(index) ?: 0)
            if (diff != 0) {
                return diff.compareTo(0)
            }
        }
        return 0
    }

    private fun isMac(): Boolean = System.getProperty("os.name").lowercase().contains("mac")

    private fun isLinux(): Boolean = System.getProperty("os.name").lowercase().contains("linux")

    private fun isWindows(): Boolean = System.getProperty("os.name").lowercase().contains("win")

    private data class ReleaseForTarget(
        val manifest: ReleaseManifest,
        val target: String,
        val asset: ReleaseAsset,
    )

    private data class ManagedInstallMetadata(
        val sha256: String,
        val version: String,
    )

    private class ReleaseManifest {
        var version: String? = null
        var assets: Map<String, ReleaseAsset>? = null
    }

    private class ReleaseAsset {
        var url: String? = null
        var sha256: String? = null
        var archiveType: String? = null
        var executable: String? = null
    }

    companion object {
        private val RELEASE_MANIFEST_URI = URI.create("https://ite.kiishi.space/releases/manifest.json")

        fun getInstance(): IteRuntimeService = service()
    }
}
