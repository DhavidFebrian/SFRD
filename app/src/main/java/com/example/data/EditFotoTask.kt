package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "edit_foto_tasks")
data class EditFotoTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val no: Int,                      // Column L (row index offset)
    val idListing: String,            // Column M
    val namaMe: String,               // Column N
    val postingIg: Boolean,           // Column O (checkbox)
    val jadwalPosting: String,        // Column P (date)
    val editNotes: String,            // Column Q (notes)
    val done: Boolean,                // Column R (checkbox)
    val judul: String,                // Column S (title)
    val source: String,               // Column T (reference source)
    val synced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val sheetName: String = ""
) : Serializable
