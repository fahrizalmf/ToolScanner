package com.tollscan.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single scanned toll receipt (struk tol).
 *
 * [tanggal] is stored as "yyyy-MM-dd" so it sorts and range-filters correctly as plain text.
 * [jam] is stored as "HH:mm".
 * [tarif] is stored in whole Rupiah (no decimals).
 */
@Entity(tableName = "toll_receipts")
data class TollReceipt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gerbangTol: String,
    val tanggal: String,
    val jam: String,
    val tarif: Long,
    val imagePath: String,
    val rawOcrText: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
