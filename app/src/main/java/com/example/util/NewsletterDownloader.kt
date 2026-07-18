package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object NewsletterDownloader {
    private const val TAG = "NewsletterDownloader"

    // Simple custom CookieJar that returns all captured cookies
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

    interface DownloadCallback {
        fun onProgress(message: String)
        fun onSuccess(pdfUri: Uri, jpgUris: List<Uri>)
        fun onFailure(error: String)
    }

    fun downloadAndConvert(
        context: Context,
        listingIds: String, // Comma separated IDs
        pdfTitle: String,
        includeCover: Boolean,
        coverPhotoUrl: String?,
        callback: DownloadCallback
    ) {
        callback.onProgress("Establishing connection...")

        val getRequest = Request.Builder()
            .url("https://raywhitecipete.net/SocialMedia")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
            .header("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
            .build()

        client.newCall(getRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure("Koneksi awal gagal: ${e.localizedMessage}")
            }

            override fun onResponse(call: Call, getResponse: Response) {
                getResponse.use {
                    callback.onProgress("Logging in to Ray White portal...")

                    val formBody = FormBody.Builder()
                        .add("Email", "sosmedrwc@gmail.com")
                        .add("Password", "sosial123$")
                        .add("RememberMe", "true")
                        .build()

                    val loginRequest = Request.Builder()
                        .url("https://raywhitecipete.net/SocialMedia")
                        .post(formBody)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Origin", "https://raywhitecipete.net")
                        .header("Referer", "https://raywhitecipete.net/SocialMedia")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                        .header("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
                        .build()

                    client.newCall(loginRequest).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            callback.onFailure("Koneksi login gagal: ${e.localizedMessage}")
                        }

                        override fun onResponse(call: Call, loginResponse: Response) {
                            loginResponse.use { resp ->
                                val finalUrl = resp.request.url.toString()
                                if (resp.isSuccessful && finalUrl.contains("/Home", ignoreCase = true)) {
                                    callback.onProgress("Login sukses. Mempersiapkan unduhan...")
                                    try {
                                        // Berikan jeda 1 detik agar server men-sinkronisasi state session di database/cache
                                        Thread.sleep(1000)
                                    } catch (e: Exception) {
                                        // ignore
                                    }
                                    downloadPdfFile(context, pdfTitle, includeCover, coverPhotoUrl, listingIds, callback)
                                } else {
                                    callback.onFailure("Gagal login ke portal (Kredensial salah atau sesi ditolak oleh server)")
                                }
                            }
                        }
                    })
                }
            }
        })
    }

    private fun downloadPdfFile(
        context: Context,
        pdfTitle: String,
        includeCover: Boolean,
        coverPhotoUrl: String?,
        listingIds: String,
        callback: DownloadCallback
    ) {
        callback.onProgress("Downloading newsletter from server...")
        val downloadUrl = "https://raywhitecipete.net/SocialMedia/Home/DownloadPdfForClient"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("titlePdfForClient", pdfTitle)
            .addQueryParameter("buyorrent", "")
            .addQueryParameter("pricemin", "0")
            .addQueryParameter("pricemax", "0")
            .addQueryParameter("minlandsize", "")
            .addQueryParameter("maxlandsize", "")
            .addQueryParameter("minbuildingsize", "")
            .addQueryParameter("maxbuildingsize", "")
            .addQueryParameter("bedrooms", "")
            .addQueryParameter("marketingagent", "")
            .addQueryParameter("address", "")
            .addQueryParameter("statusListing", "-1")
            .addQueryParameter("idListing", "")
            .addQueryParameter("hotListing", "false")
            .addQueryParameter("dibawahNJOP", "false")
            .addQueryParameter("propertytypes", "")
            .addQueryParameter("compass", "")
            .addQueryParameter("lebarjalan", "")
            .addQueryParameter("isswimmingpool", "false")
            .addQueryParameter("idListings", listingIds)
            .addQueryParameter("shortBy", "Kelurahan")
            .build()

        val pdfRequest = Request.Builder()
            .url(downloadUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Referer", "https://raywhitecipete.net/SocialMedia/Home")
            .build()

        client.newCall(pdfRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure("Gagal mengunduh PDF: ${e.localizedMessage}")
            }

            override fun onResponse(call: Call, pdfResponse: Response) {
                pdfResponse.use { pdfResp ->
                    val code = pdfResp.code
                    val path = pdfResp.request.url.encodedPath
                    
                    if (code == 302 || code == 301 || path == "/SocialMedia" || path == "/SocialMedia/") {
                        callback.onFailure("Sesi login tidak sah (di-redirect ke login).")
                        return
                    }
                    if (!pdfResp.isSuccessful) {
                        callback.onFailure("Server menolak unduhan PDF (HTTP $code)")
                        return
                    }

                    val body = pdfResp.body
                    if (body == null) {
                        callback.onFailure("Data PDF kosong dari server.")
                        return
                    }

                    val pdfBytes = body.bytes()
                    if (pdfBytes.isEmpty() || !pdfBytes.take(4).toByteArray().contentEquals(bPdfHeader())) {
                        callback.onFailure("File yang diunduh bukan PDF valid.")
                        return
                    }

                    try {
                        val tempScrapedPdf = File(context.cacheDir, "temp_scraped.pdf")
                        FileOutputStream(tempScrapedPdf).use { fos ->
                            fos.write(pdfBytes)
                        }

                        val cleanFileName = pdfTitle.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
                        val finalPdfName = "$cleanFileName.pdf"
                        
                        val tempFinalPdf = File(context.cacheDir, "temp_final.pdf")
                        callback.onProgress("Formatting PDF to 3:4 ratio & links...")
                        addCoverPageToPdf(context, tempScrapedPdf, tempFinalPdf, pdfTitle, coverPhotoUrl, includeCover, listingIds)
                        
                        val finalPdfBytes = tempFinalPdf.readBytes()
                        val publicPdfUri = savePdfToDownloads(context, finalPdfBytes, finalPdfName)
                        
                        if (publicPdfUri == null) {
                            callback.onFailure("Gagal menyimpan PDF ke folder Downloads.")
                            return
                        }

                        callback.onProgress("Converting PDF to JPG...")
                        val jpgUris = convertPdfToJpg(context, tempFinalPdf, cleanFileName)

                        // Cleanup temp files
                        try {
                            tempScrapedPdf.delete()
                            tempFinalPdf.delete()
                        } catch (e: Exception) {
                            // ignore
                        }

                        callback.onSuccess(publicPdfUri, jpgUris)
                    } catch (e: Exception) {
                        callback.onFailure("Gagal memproses file: ${e.localizedMessage}")
                    }
                }
            }
        })
    }

    private fun bPdfHeader(): ByteArray = byteArrayOf(0x25.toByte(), 0x50.toByte(), 0x44.toByte(), 0x46.toByte()) // %PDF

    private fun savePdfToDownloads(context: Context, bytes: ByteArray, fileName: String): Uri? {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(bytes)
                }
            }
            return uri
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            try {
                FileOutputStream(file).use { outputStream ->
                    outputStream.write(bytes)
                }
                return FileProvider.getUriForFile(context, "com.example.fileprovider", file)
            } catch (e: Exception) {
                Log.e(TAG, "savePdfToDownloads legacy error", e)
                return null
            }
        }
    }

    private fun convertPdfToJpg(context: Context, pdfFile: File, baseName: String): List<Uri> {
        val uris = mutableListOf<Uri>()
        var parcelFileDescriptor: ParcelFileDescriptor? = null
        var pdfRenderer: PdfRenderer? = null
        try {
            parcelFileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(parcelFileDescriptor)
            val pageCount = pdfRenderer.pageCount
            
            for (i in 0 until pageCount) {
                val page = pdfRenderer.openPage(i)
                // Scale page resolution for higher clarity
                val scale = 2
                val width = page.width * scale
                val height = page.height * scale
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                
                // Save to gallery via MediaStore
                val jpgName = "${baseName}_Page_${i + 1}.jpg"
                val jpgUri = saveImageToGallery(context, bitmap, jpgName)
                if (jpgUri != null) {
                    uris.add(jpgUri)
                }
                bitmap.recycle()
            }
        } catch (e: Exception) {
            Log.e(TAG, "convertPdfToJpg error", e)
        } finally {
            try {
                pdfRenderer?.close()
                parcelFileDescriptor?.close()
            } catch (ex: Exception) {
                Log.e(TAG, "close renderer error", ex)
            }
        }
        return uris
    }

    private fun saveImageToGallery(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/RWNewsletter")
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                }
            }
            return uri
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val dir = File(picturesDir, "RWNewsletter")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)
            try {
                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                }
                return FileProvider.getUriForFile(context, "com.example.fileprovider", file)
            } catch (e: Exception) {
                Log.e(TAG, "saveImageToGallery legacy error", e)
                return null
            }
        }
    }

    private fun drawCoverPage(
        context: Context,
        pdfTitle: String,
        coverPhotoBitmap: Bitmap?
    ): Bitmap {
        val template = BitmapFactory.decodeStream(context.assets.open("cover_template.png"))
        // Create 2x resolution HD canvas (1638 x 2048)
        val result = Bitmap.createBitmap(template.width * 2, template.height * 2, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val filterPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            isAntiAlias = true
            isDither = true
        }

        // 1. Draw property photo first (2x scale target area: 1100 x 1040)
        if (coverPhotoBitmap != null) {
            val targetWidth = 1100
            val targetHeight = 1040
            val srcWidth = coverPhotoBitmap.width
            val srcHeight = coverPhotoBitmap.height
            val srcRect = Rect()
            val destRect = Rect(0, 0, targetWidth, targetHeight)

            val srcAspect = srcWidth.toFloat() / srcHeight
            val destAspect = targetWidth.toFloat() / targetHeight

            if (srcAspect > destAspect) {
                val w = (srcHeight * destAspect).toInt()
                val offset = (srcWidth - w) / 2
                srcRect.set(offset, 0, offset + w, srcHeight)
            } else {
                val h = (srcWidth / destAspect).toInt()
                val offset = (srcHeight - h) / 2
                srcRect.set(0, offset, srcWidth, offset + h)
            }
            canvas.drawBitmap(coverPhotoBitmap, srcRect, destRect, filterPaint)
        }

        // 2. Draw template on top (masks the photo automatically!)
        val templateDest = Rect(0, 0, template.width * 2, template.height * 2)
        canvas.drawBitmap(template, null, templateDest, filterPaint)

        // 3. Prepare paints with larger sizes (2x)
        val textPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 112f // Perbesar teks judul ke 112f (dari 96f)
        }

        val subPaint = Paint().apply {
            color = Color.parseColor("#333333")
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = 60f // Perbesar teks subjudul ke 60f (dari 56f)
        }

        val cleanedPdfTitle = pdfTitle.replace("\\b(20\\d{2})\\s+\\1\\b".toRegex(), "$1")
        val titleUpper = cleanedPdfTitle.uppercase()
        var line1 = "HOT PROPERTY"
        var line2 = ""
        if (titleUpper.startsWith("HOT PROPERTY")) {
            line1 = "HOT PROPERTY"
            line2 = titleUpper.removePrefix("HOT PROPERTY").trim()
        } else if (titleUpper.startsWith("HOT LISTING")) {
            line1 = "HOT LISTING"
            line2 = titleUpper.removePrefix("HOT LISTING").trim()
        } else {
            val words = titleUpper.split(" ")
            if (words.size > 2) {
                line1 = words.take(2).joinToString(" ")
                line2 = words.drop(2).joinToString(" ")
            } else {
                line1 = titleUpper
            }
        }

        // 4. Draw title text directly on the clean template
        if (line2.isBlank()) {
            canvas.drawText(line1, 120f, 1450f, textPaint)
        } else {
            canvas.drawText(line1, 120f, 1370f, textPaint)
            canvas.drawText(line2, 120f, 1510f, textPaint)
        }

        // Ray White Cipete is already present in the template watermark - no need to draw it again

        template.recycle()
        return result
    }

    private fun addCoverPageToPdf(
        context: Context,
        srcPdfFile: File,
        destPdfFile: File,
        pdfTitle: String,
        coverPhotoUrl: String?,
        includeCover: Boolean,
        listingIds: String
    ) {
        val containerWidth = 1638
        val containerHeight = 2048
        var currentPageNumber = 1

        val newDoc = android.graphics.pdf.PdfDocument()

        // 1. Add cover page if requested
        if (includeCover) {
            var coverPhotoBitmap: Bitmap? = null
            if (!coverPhotoUrl.isNullOrBlank()) {
                try {
                    val imgRequest = Request.Builder().url(coverPhotoUrl).build()
                    val imgResponse = client.newCall(imgRequest).execute()
                    if (imgResponse.isSuccessful) {
                        imgResponse.body?.byteStream()?.use { input ->
                            coverPhotoBitmap = BitmapFactory.decodeStream(input)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Gagal mengunduh cover photo", e)
                }
            }
            val coverBitmap = drawCoverPage(context, pdfTitle, coverPhotoBitmap)
            val coverPageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(containerWidth, containerHeight, currentPageNumber++).create()
            val coverPage = newDoc.startPage(coverPageInfo)
            
            // Draw scaled HD cover with high-quality paint filter
            val destRect = Rect(0, 0, containerWidth, containerHeight)
            coverPage.canvas.drawBitmap(coverBitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
            
            newDoc.finishPage(coverPage)
            coverBitmap.recycle()
            coverPhotoBitmap?.recycle()
        }


        // 2. Parse listing IDs for link rendering
        val idList = listingIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        // 3. Process original PDF pages, formatting them to 3:4 (4:5) ratio with links
        var parcelFileDescriptor: ParcelFileDescriptor? = null
        var pdfRenderer: PdfRenderer? = null
        try {
            parcelFileDescriptor = ParcelFileDescriptor.open(srcPdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(parcelFileDescriptor)
            val pageCount = pdfRenderer.pageCount

            for (i in 0 until pageCount) {
                val page = pdfRenderer.openPage(i)

                val origWidth = page.width
                val origHeight = page.height

                val scale = 6
                val bmpWidth = origWidth * scale
                val bmpHeight = origHeight * scale
                val pageBitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)

                val canvas = Canvas(pageBitmap)
                canvas.drawColor(Color.WHITE)
                page.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                // Calculate centered aspect-ratio scaling to fit the full container
                val scaleX = containerWidth.toFloat() / origWidth.toFloat()
                val scaleY = containerHeight.toFloat() / origHeight.toFloat()
                val fitScale = Math.min(scaleX, scaleY)

                val drawWidth = (origWidth * fitScale).toInt()
                val drawHeight = (origHeight * fitScale).toInt()

                val dx = (containerWidth - drawWidth) / 2
                val dy = (containerHeight - drawHeight) / 2

                // Create new page of container size
                val newPageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(containerWidth, containerHeight, currentPageNumber++).create()
                val newPage = newDoc.startPage(newPageInfo)

                // Fill background with white
                newPage.canvas.drawColor(Color.WHITE)

                // Draw centered content
                val srcRect = Rect(0, 0, bmpWidth, bmpHeight)
                val destRect = Rect(dx, dy, dx + drawWidth, dy + drawHeight)
                val filterPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                    isAntiAlias = true
                    isDither = true
                }
                newPage.canvas.drawBitmap(pageBitmap, srcRect, destRect, filterPaint)

                // Seamless 3:4 Aspect Ratio conversion:
                // Stretch the left 1-pixel-wide edge column of the pageBitmap to cover the left empty margin (from 0 to dx)
                if (dx > 0) {
                    val srcRectLeft = Rect(0, 0, 1, bmpHeight)
                    val destRectLeft = Rect(0, dy, dx, dy + drawHeight)
                    newPage.canvas.drawBitmap(pageBitmap, srcRectLeft, destRectLeft, filterPaint)
                }

                // Stretch the right 1-pixel-wide edge column of the pageBitmap to cover the right empty margin (from dx + drawWidth to containerWidth)
                if (dx + drawWidth < containerWidth) {
                    val srcRectRight = Rect(bmpWidth - 1, 0, bmpWidth, bmpHeight)
                    val destRectRight = Rect(dx + drawWidth, dy, containerWidth, dy + drawHeight)
                    newPage.canvas.drawBitmap(pageBitmap, srcRectRight, destRectRight, filterPaint)
                }

                // Draw listing URL link transparently (recognized as clickable text by PDF viewers)
                val listingId = idList.getOrNull(i)
                if (!listingId.isNullOrBlank()) {
                    val cleanId = listingId.trim()
                    val linkUrl = "https://raywhitecipete.net/ListingView/Detail/$cleanId"
                    
                    val transparentPaint = Paint().apply {
                        color = Color.TRANSPARENT // Invisible
                        textSize = 48f
                        isAntiAlias = true
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                    }
                    
                    // Draw transparent URL link in multiple places to make the image area clickable
                    newPage.canvas.drawText(linkUrl, 200f, 500f, transparentPaint)
                    newPage.canvas.drawText(linkUrl, 200f, 800f, transparentPaint)
                    newPage.canvas.drawText(linkUrl, 200f, 1100f, transparentPaint)
                    newPage.canvas.drawText(linkUrl, 200f, 1400f, transparentPaint)
                }

                newDoc.finishPage(newPage)
                pageBitmap.recycle()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copying PDF pages", e)
        } finally {
            try {
                pdfRenderer?.close()
                parcelFileDescriptor?.close()
            } catch (ex: Exception) {
                // ignore
            }
        }

        FileOutputStream(destPdfFile).use { fos ->
            newDoc.writeTo(fos)
        }
        newDoc.close()
    }
}

