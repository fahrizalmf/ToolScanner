package com.tollscan.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TollReceiptDao {

    @Query("SELECT * FROM toll_receipts ORDER BY tanggal DESC, jam DESC")
    fun getAll(): Flow<List<TollReceipt>>

    @Query("SELECT * FROM toll_receipts WHERE tanggal BETWEEN :startDate AND :endDate ORDER BY tanggal ASC, jam ASC")
    suspend fun getByDateRange(startDate: String, endDate: String): List<TollReceipt>

    @Query("SELECT * FROM toll_receipts WHERE id = :id")
    suspend fun getById(id: Long): TollReceipt?

    @Insert
    suspend fun insert(receipt: TollReceipt): Long

    @Update
    suspend fun update(receipt: TollReceipt)

    @Delete
    suspend fun delete(receipt: TollReceipt)
}
