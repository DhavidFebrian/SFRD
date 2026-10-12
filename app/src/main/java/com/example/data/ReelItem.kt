package com.example.data

data class ReelItem(
    val row: Int,
    val no: Int,
    val idListing: String,
    val driveLink: String,
    val postingIg: Boolean,
    val jadwalPosting: String,
    val namaMe: String,
    val catatan: String
) {
    val driveFileId: String
        get() = Regex("/d/([^/?]+)").find(driveLink)?.groupValues?.getOrNull(1)
            ?: Regex("[?&]id=([^&]+)").find(driveLink)?.groupValues?.getOrNull(1)
            ?: ""

    val videoUrl: String
        get() = driveFileId.takeIf { it.isNotBlank() }
            ?.let { "https://drive.usercontent.google.com/download?id=$it&export=download&confirm=t" }
            .orEmpty()

    val thumbnailUrl: String
        get() = driveFileId.takeIf { it.isNotBlank() }
            ?.let { "https://drive.google.com/thumbnail?id=$it&sz=w1000" }
            .orEmpty()

    /** Google Drive's adaptive player is substantially faster and more reliable than streaming the original MP4. */
    val previewUrl: String
        get() = driveFileId.takeIf { it.isNotBlank() }
            ?.let { "https://drive.google.com/file/d/$it/preview" }
            .orEmpty()
}
