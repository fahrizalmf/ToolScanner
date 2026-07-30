package com.tollscan.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class TollRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).tollReceiptDao()

    fun getAll(): Flow<List<TollReceipt>> = dao.getAll()

    suspend fun getByDateRange(start: String, end: String): List<TollReceipt> =
        dao.getByDateRange(start, end)

    suspend fun getById(id: Long): TollReceipt? = dao.getById(id)

    suspend fun insert(receipt: TollReceipt): Long = dao.insert(receipt)

    suspend fun update(receipt: TollReceipt) = dao.update(receipt)

    suspend fun delete(receipt: TollReceipt) = dao.delete(receipt)
}
