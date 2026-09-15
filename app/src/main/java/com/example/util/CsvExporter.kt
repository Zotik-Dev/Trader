package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.TradeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    suspend fun exportTradesToCsv(context: Context, trades: List<TradeEntity>): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "shared_exports").apply { mkdirs() }
        val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val csvFile = File(exportDir, "Trading_Journal_$dateStamp.csv")

        FileWriter(csvFile).use { writer ->
            // Header
            writer.append("Trade ID,Date,Time,Instrument,Symbol / Strike,Option Type,Direction,Entry Price,Exit Price,Quantity,Gross PnL,Charges,Net PnL,Points,Stop Loss,Target 1,Target 2,Target 3,Risk Amount,Planned RR,Actual RR,Status,Setup,Indicators,Emotion,Mistake,Notes,Images Count\n")

            for (t in trades) {
                val dateStr = dateFormatter.format(Date(t.entryTimestamp))
                val timeStr = timeFormatter.format(Date(t.entryTimestamp))
                val indicatorsStr = t.indicators.joinToString(separator = "; ")
                
                fun escape(str: String): String {
                    var s = str.replace("\"", "\"\"")
                    if (s.contains(",") || s.contains("\n") || s.contains("\"")) {
                        s = "\"$s\""
                    }
                    return s
                }

                writer.append("${t.id},")
                writer.append("$dateStr,")
                writer.append("$timeStr,")
                writer.append("${escape(t.instrument)},")
                writer.append("${escape(t.strikeOrSymbol)},")
                writer.append("${t.optionType},")
                writer.append("${t.direction},")
                writer.append("${t.entryPrice},")
                writer.append("${t.exitPrice},")
                writer.append("${t.quantity},")
                writer.append("${t.grossPnL},")
                writer.append("${t.charges},")
                writer.append("${t.netPnL},")
                writer.append("${t.points},")
                writer.append("${t.slPrice},")
                writer.append("${t.target1},")
                writer.append("${t.target2},")
                writer.append("${t.target3},")
                writer.append("${t.riskAmount},")
                writer.append("${t.plannedRR},")
                writer.append("${t.actualRR},")
                writer.append("${escape(t.status)},")
                writer.append("${escape(t.setup)},")
                writer.append("${escape(indicatorsStr)},")
                writer.append("${escape(t.emotion)},")
                writer.append("${escape(t.mistake)},")
                writer.append("${escape(t.notes)},")
                writer.append("${t.imageUris.size}\n")
            }
        }
        csvFile
    }

    fun shareCsvFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Trading Journal Export - ${file.name}")
            putExtra(Intent.EXTRA_TEXT, "Here is the export of my trading journal.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(sendIntent, "Export Trading Journal CSV")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
