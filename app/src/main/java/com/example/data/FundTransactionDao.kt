package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FundTransactionDao {
    @Query("SELECT * FROM fund_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<FundTransactionEntity>>

    @Query("SELECT * FROM fund_transactions WHERE platform = :platform ORDER BY timestamp DESC")
    fun getTransactionsByPlatform(platform: String): Flow<List<FundTransactionEntity>>

    @Query("SELECT * FROM fund_transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): FundTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: FundTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<FundTransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: FundTransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: FundTransactionEntity)

    @Query("DELETE FROM fund_transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM fund_transactions")
    suspend fun deleteAllTransactions()
}
