package com.example.data

import kotlinx.coroutines.flow.Flow

class TradeRepository(
    private val tradeDao: TradeDao,
    private val fundTransactionDao: FundTransactionDao
) {
    val allTrades: Flow<List<TradeEntity>> = tradeDao.getAllTrades()

    fun getTradeById(id: Long): Flow<TradeEntity?> = tradeDao.getTradeById(id)

    suspend fun getTradeByIdOnce(id: Long): TradeEntity? = tradeDao.getTradeByIdOnce(id)

    suspend fun insertTrade(trade: TradeEntity): Long = tradeDao.insertTrade(trade)

    suspend fun insertTrades(trades: List<TradeEntity>) = tradeDao.insertTrades(trades)

    suspend fun updateTrade(trade: TradeEntity) = tradeDao.updateTrade(trade)

    suspend fun deleteTrade(trade: TradeEntity) = tradeDao.deleteTrade(trade)

    suspend fun deleteTradeById(id: Long) = tradeDao.deleteTradeById(id)

    suspend fun deleteAllTrades() = tradeDao.deleteAllTrades()

    // Fund Transactions (Broker Credit/Debit)
    val allFundTransactions: Flow<List<FundTransactionEntity>> = fundTransactionDao.getAllTransactions()

    fun getFundTransactionsByPlatform(platform: String): Flow<List<FundTransactionEntity>> =
        fundTransactionDao.getTransactionsByPlatform(platform)

    suspend fun getFundTransactionById(id: Long): FundTransactionEntity? =
        fundTransactionDao.getTransactionById(id)

    suspend fun insertFundTransaction(transaction: FundTransactionEntity): Long =
        fundTransactionDao.insertTransaction(transaction)

    suspend fun insertFundTransactions(transactions: List<FundTransactionEntity>) =
        fundTransactionDao.insertTransactions(transactions)

    suspend fun updateFundTransaction(transaction: FundTransactionEntity) =
        fundTransactionDao.updateTransaction(transaction)

    suspend fun deleteFundTransaction(transaction: FundTransactionEntity) =
        fundTransactionDao.deleteTransaction(transaction)

    suspend fun deleteFundTransactionById(id: Long) =
        fundTransactionDao.deleteTransactionById(id)

    suspend fun deleteAllFundTransactions() =
        fundTransactionDao.deleteAllTransactions()
}
