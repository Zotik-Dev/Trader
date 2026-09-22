package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.TradeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    suspend fun createJsonBackup(context: Context, trades: List<TradeEntity>): File = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())
        root.put("appName", "Trading Journal")

        val array = JSONArray()
        for (t in trades) {
            val obj = JSONObject().apply {
                put("id", t.id)
                put("instrument", t.instrument)
                put("strikeOrSymbol", t.strikeOrSymbol)
                put("optionType", t.optionType)
                put("direction", t.direction)
                put("entryPrice", t.entryPrice)
                put("slPrice", t.slPrice)
                put("target1", t.target1)
                put("target2", t.target2)
                put("target3", t.target3)
                put("exitPrice", t.exitPrice)
                put("quantity", t.quantity)
                put("grossPnL", t.grossPnL)
                put("charges", t.charges)
                put("netPnL", t.netPnL)
                put("points", t.points)
                put("riskAmount", t.riskAmount)
                put("plannedRR", t.plannedRR)
                put("actualRR", t.actualRR)
                put("status", t.status)
                put("setup", t.setup)
                
                val indArray = JSONArray()
                t.indicators.forEach { indArray.put(it) }
                put("indicators", indArray)

                put("emotion", t.emotion)
                put("mistake", t.mistake)
                put("notes", t.notes)

                val imgArray = JSONArray()
                t.imageUris.forEach { imgArray.put(it) }
                put("imageUris", imgArray)

                put("entryTimestamp", t.entryTimestamp)
                put("exitTimestamp", t.exitTimestamp)
            }
            array.put(obj)
        }
        root.put("trades", array)

        val exportDir = File(context.cacheDir, "shared_exports").apply { mkdirs() }
        val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val backupFile = File(exportDir, "Trading_Journal_Backup_$dateStamp.json")

        FileWriter(backupFile).use { writer ->
            writer.write(exportBackupToJson(trades))
        }
        backupFile
    }

    fun exportBackupToJson(trades: List<TradeEntity>): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())
        root.put("appName", "Trading Journal")

        val array = JSONArray()
        for (t in trades) {
            val obj = JSONObject().apply {
                put("id", t.id)
                put("instrument", t.instrument)
                put("strikeOrSymbol", t.strikeOrSymbol)
                put("optionType", t.optionType)
                put("direction", t.direction)
                put("entryPrice", t.entryPrice)
                put("slPrice", t.slPrice)
                put("target1", t.target1)
                put("target2", t.target2)
                put("target3", t.target3)
                put("exitPrice", t.exitPrice)
                put("quantity", t.quantity)
                put("grossPnL", t.grossPnL)
                put("charges", t.charges)
                put("netPnL", t.netPnL)
                put("points", t.points)
                put("riskAmount", t.riskAmount)
                put("plannedRR", t.plannedRR)
                put("actualRR", t.actualRR)
                put("status", t.status)
                put("setup", t.setup)

                val indArray = JSONArray()
                t.indicators.forEach { indArray.put(it) }
                put("indicators", indArray)

                put("emotion", t.emotion)
                put("mistake", t.mistake)
                put("notes", t.notes)

                val imgArray = JSONArray()
                t.imageUris.forEach { imgArray.put(it) }
                put("imageUris", imgArray)

                put("entryTimestamp", t.entryTimestamp)
                put("exitTimestamp", t.exitTimestamp)
            }
            array.put(obj)
        }
        root.put("trades", array)
        return root.toString(2)
    }

    fun shareBackupFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Trading Journal Backup - ${file.name}")
            putExtra(Intent.EXTRA_TEXT, "Trading Journal full JSON backup file.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(sendIntent, "Backup Trading Journal")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    suspend fun restoreTradesFromJsonUri(context: Context, uri: Uri): List<TradeEntity> = withContext(Dispatchers.IO) {
        val stringBuilder = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    stringBuilder.append(line)
                }
            }
        }
        parseBackupJson(stringBuilder.toString())
    }

    fun parseBackupJson(jsonString: String): List<TradeEntity> {
        val root = JSONObject(jsonString)
        val tradesArray = root.getJSONArray("trades")
        val restoredList = mutableListOf<TradeEntity>()

        for (i in 0 until tradesArray.length()) {
            val obj = tradesArray.getJSONObject(i)
            val indList = mutableListOf<String>()
            val indArray = obj.optJSONArray("indicators")
            if (indArray != null) {
                for (j in 0 until indArray.length()) {
                    indList.add(indArray.getString(j))
                }
            }

            val imgList = mutableListOf<String>()
            val imgArray = obj.optJSONArray("imageUris")
            if (imgArray != null) {
                for (k in 0 until imgArray.length()) {
                    imgList.add(imgArray.getString(k))
                }
            }

            val trade = TradeEntity(
                id = 0, // Reset ID so Room generates fresh primary keys without conflicts
                instrument = obj.optString("instrument", "NIFTY"),
                strikeOrSymbol = obj.optString("strikeOrSymbol", ""),
                optionType = obj.optString("optionType", "CE"),
                direction = obj.optString("direction", "BUY"),
                entryPrice = obj.optDouble("entryPrice", 0.0),
                slPrice = obj.optDouble("slPrice", 0.0),
                target1 = obj.optDouble("target1", 0.0),
                target2 = obj.optDouble("target2", 0.0),
                target3 = obj.optDouble("target3", 0.0),
                exitPrice = obj.optDouble("exitPrice", 0.0),
                quantity = obj.optInt("quantity", 25),
                grossPnL = obj.optDouble("grossPnL", 0.0),
                charges = obj.optDouble("charges", 0.0),
                netPnL = obj.optDouble("netPnL", 0.0),
                points = obj.optDouble("points", 0.0),
                riskAmount = obj.optDouble("riskAmount", 0.0),
                plannedRR = obj.optDouble("plannedRR", 0.0),
                actualRR = obj.optDouble("actualRR", 0.0),
                status = obj.optString("status", "CLOSED"),
                setup = obj.optString("setup", "CPR Breakout"),
                indicators = indList,
                emotion = obj.optString("emotion", "Disciplined"),
                mistake = obj.optString("mistake", "None (Followed Plan)"),
                notes = obj.optString("notes", ""),
                imageUris = imgList,
                entryTimestamp = obj.optLong("entryTimestamp", System.currentTimeMillis()),
                exitTimestamp = obj.optLong("exitTimestamp", System.currentTimeMillis())
            )
            restoredList.add(trade)
        }
        return restoredList
    }
}
