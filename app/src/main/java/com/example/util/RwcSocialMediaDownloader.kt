package com.example.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLEncoder

data class RwcListingImage(
    val id: String,
    val imageUrl: String
)

object RwcSocialMediaDownloader {
    private const val TAG = "RwcSocialDownloader"

    private class SimpleCookieJar : CookieJar {
        private val cookieStore = mutableMapOf<String, Cookie>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            synchronized(cookieStore) {
                cookies.forEach { cookie ->
                    val key = "${cookie.name}|${cookie.domain}|${cookie.path}"
                    cookieStore[key] = cookie
                }
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            synchronized(cookieStore) {
                return ArrayList(cookieStore.values)
            }
        }
    }

    private val cookieJar = SimpleCookieJar()
    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    interface FetchImagesCallback {
        fun onProgress(message: String)
        fun onSuccess(images: List<RwcListingImage>)
        fun onFailure(error: String)
    }

    interface DownloadDesign3Callback {
        fun onProgress(message: String)
        fun onSuccess(downloadedUris: List<Uri>, firstImageUri: Uri?)
        fun onFailure(error: String)
    }

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    fun loginAndFetchImages(context: Context, listingId: String, callback: FetchImagesCallback) {
        mainHandler.post { callback.onProgress("Connecting to Ray White portal...") }

        val getReq = Request.Builder()
            .url("https://raywhitecipete.net/SocialMedia")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()

        client.newCall(getReq).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { callback.onFailure("Failed connection: ${e.localizedMessage}") }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    mainHandler.post { callback.onProgress("Logging in...") }
                    val formBody = FormBody.Builder()
                        .add("Email", "sosmedrwc@gmail.com")
                        .add("Password", "sosial123$")
                        .add("RememberMe", "true")
                        .build()

                    val loginReq = Request.Builder()
                        .url("https://raywhitecipete.net/SocialMedia")
                        .post(formBody)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .build()

                    client.newCall(loginReq).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            mainHandler.post { callback.onFailure("Login failed: ${e.localizedMessage}") }
                        }

                        override fun onResponse(call: Call, loginResp: Response) {
                            loginResp.use { resp ->
                                val finalUrl = resp.request.url.toString()
                                if (resp.isSuccessful && finalUrl.contains("/Home", ignoreCase = true)) {
                                    mainHandler.post { callback.onProgress("Login success. Fetching photos for ID $listingId...") }
                                    fetchImagesForListing(listingId, callback)
                                } else {
                                    mainHandler.post { callback.onFailure("Failed login to Ray White portal.") }
                                }
                            }
                        }
                    })
                }
            }
        })
    }

    private fun fetchImagesForListing(listingId: String, callback: FetchImagesCallback) {
        val cleanId = listingId.replace("[^0-9]".toRegex(), "")
        val url = "https://raywhitecipete.net/SocialMedia/Home/GetListingImageByIdListing?id=$cleanId"

        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { callback.onFailure("Failed fetching listing images: ${e.localizedMessage}") }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    try {
                        val bodyStr = resp.body?.string() ?: ""
                        val json = JSONObject(bodyStr)
                        if (json.optBoolean("success", false)) {
                            val entityArr = json.optJSONArray("entity") ?: json.optJSONArray("Entity")
                            val list = mutableListOf<RwcListingImage>()
                            if (entityArr != null) {
                                for (i in 0 until entityArr.length()) {
                                    val obj = entityArr.getJSONObject(i)
                                    val id = obj.optString("Id")
                                    val imgUrl = obj.optString("Image")
                                    if (id.isNotBlank() && imgUrl.isNotBlank()) {
                                        list.add(RwcListingImage(id, imgUrl))
                                    }
                                }
                            }
                            mainHandler.post { callback.onSuccess(list) }
                        } else {
                            mainHandler.post { callback.onFailure("Listing photos not found on portal for ID $listingId.") }
                        }
                    } catch (e: Exception) {
                        mainHandler.post { callback.onFailure("Error parsing photo response: ${e.localizedMessage}") }
                    }
                }
            }
        })
    }

    fun downloadDesign3Photos(
        context: Context,
        listingId: String,
        coverImageId: String,
        coverTitle: String,
        selectedImageIds: List<String>,
        callback: DownloadDesign3Callback
    ) {
        mainHandler.post { callback.onProgress("Processing Design 3 cover photo download...") }

        val downloadedUris = mutableListOf<Uri>()
        var firstImageUri: Uri? = null

        val allIdsToDownload = mutableListOf<String>()
        allIdsToDownload.add(coverImageId)
        selectedImageIds.forEach { id ->
            if (id != coverImageId && !allIdsToDownload.contains(id)) {
                allIdsToDownload.add(id)
            }
        }

        val total = allIdsToDownload.size

        Thread {
            try {
                for (idx in allIdsToDownload.indices) {
                    val imgId = allIdsToDownload[idx]
                    val isCover = idx == 0

                    val msg = if (isCover) "Downloading Design 3 Cover..." else "Downloading photo ${idx + 1} / $total..."
                    mainHandler.post { callback.onProgress(msg) }

                    val encodedTitle = URLEncoder.encode(coverTitle, "UTF-8")
                    val dlUrl = if (isCover) {
                        "https://raywhitecipete.net/SocialMedia/Home/DownloadListingSocialMediaV3?idlistingimage=$imgId&headline1=$encodedTitle"
                    } else {
                        "https://raywhitecipete.net/SocialMedia/Home/DownloadListingSocialMediaV3?idlistingimage=$imgId"
                    }

                    val req = Request.Builder()
                        .url(dlUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .build()

                    val resp = client.newCall(req).execute()
                    if (resp.isSuccessful) {
                        val bytes = resp.body?.bytes()
                        if (bytes != null && bytes.isNotEmpty()) {
                            val fileName = if (isCover) "RWC_Cover_Design3_ID${listingId}.jpg" else "RWC_ID${listingId}_img${idx + 1}.jpg"
                            val uri = saveImageToStorage(context, fileName, bytes)
                            if (uri != null) {
                                downloadedUris.add(uri)
                                if (isCover) firstImageUri = uri
                            }
                        }
                    }
                    resp.close()
                }

                mainHandler.post { callback.onSuccess(downloadedUris, firstImageUri) }

            } catch (e: Exception) {
                mainHandler.post { callback.onFailure("Download failed: ${e.localizedMessage}") }
            }
        }.start()
    }

    private fun saveImageToStorage(context: Context, fileName: String, bytes: ByteArray): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SFRD_RWC")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os ->
                        os.write(bytes)
                    }
                    uri
                } else null
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "SFRD_RWC")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                FileOutputStream(file).use { os ->
                    os.write(bytes)
                }
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Save image error: ${e.message}")
            null
        }
    }
}
