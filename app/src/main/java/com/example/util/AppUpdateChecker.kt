package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object AppUpdateChecker {
    private const val TAG = "AppUpdateChecker"
    private const val GITHUB_RELEASE_URL = "https://api.github.com/repos/DhavidFebrian/SFRD-5.0/releases/latest"

    data class GithubReleaseAsset(
        val name: String,
        val browser_download_url: String
    )

    data class GithubRelease(
        val tag_name: String,
        val assets: List<GithubReleaseAsset>,
        val body: String?
    )

    data class UpdateInfo(
        val versionName: String,
        val downloadUrl: String,
        val releaseNotes: String
    )

    fun checkForUpdate(
        context: Context,
        onNewVersionFound: (UpdateInfo) -> Unit,
        onNoUpdate: () -> Unit = {}
    ) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(GITHUB_RELEASE_URL)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Update check failed", e)
                onNoUpdate()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        Log.e(TAG, "Server responded with code ${resp.code}")
                        onNoUpdate()
                        return
                    }

                    val bodyString = resp.body?.string()
                    if (bodyString.isNullOrBlank()) {
                        onNoUpdate()
                        return
                    }

                    try {
                        val moshi = Moshi.Builder()
                            .addLast(KotlinJsonAdapterFactory())
                            .build()
                        val adapter = moshi.adapter(GithubRelease::class.java)
                        val release = adapter.fromJson(bodyString)

                        if (release != null) {
                            val remoteVersion = release.tag_name
                            val currentVersion = BuildConfig.VERSION_NAME

                            if (isVersionNewer(currentVersion, remoteVersion)) {
                                // Find APK asset
                                val apkAsset = release.assets.firstOrNull { 
                                    it.name.endsWith(".apk", ignoreCase = true) 
                                }
                                if (apkAsset != null) {
                                    onNewVersionFound(
                                        UpdateInfo(
                                            versionName = remoteVersion,
                                            downloadUrl = apkAsset.browser_download_url,
                                            releaseNotes = release.body ?: ""
                                        )
                                    )
                                    return
                                }
                            }
                        }
                        onNoUpdate()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing GitHub release JSON", e)
                        onNoUpdate()
                    }
                }
            }
        })
    }

    fun isVersionNewer(current: String, remote: String): Boolean {
        val cur = current.trim().lowercase().removePrefix("v")
        val rem = remote.trim().lowercase().removePrefix("v")
        if (cur == rem) return false

        val curParts = cur.split(".").mapNotNull { it.toIntOrNull() }
        val remParts = rem.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(curParts.size, remParts.size)
        for (i in 0 until maxLen) {
            val c = curParts.getOrElse(i) { 0 }
            val r = remParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (c > r) return false
        }
        return false
    }

    fun downloadApk(
        context: Context,
        url: String,
        onProgress: (Float) -> Unit,
        onSuccess: (File) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        onFailure(IOException("Unexpected response code: ${resp.code}"))
                        return
                    }

                    val body = resp.body
                    if (body == null) {
                        onFailure(IOException("Empty response body"))
                        return
                    }

                    try {
                        val destinationFile = File(context.cacheDir, "sfrd_update.apk")
                        if (destinationFile.exists()) {
                            destinationFile.delete()
                        }

                        val contentLength = body.contentLength()
                        body.byteStream().use { input ->
                            FileOutputStream(destinationFile).use { output ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                var totalBytesRead = 0L

                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    output.write(buffer, 0, bytesRead)
                                    totalBytesRead += bytesRead
                                    if (contentLength > 0) {
                                        val progress = totalBytesRead.toFloat() / contentLength.toFloat()
                                        onProgress(progress)
                                    }
                                }
                            }
                        }
                        onSuccess(destinationFile)
                    } catch (e: Exception) {
                        onFailure(e)
                    }
                }
            }
        })
    }

    fun installApk(context: Context, file: File): Boolean {
        // 1. Check Unknown Sources permission on Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return false
            }
        }

        // 2. Trigger installation using FileProvider
        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start APK installation intent", e)
            return false
        }
    }
}
