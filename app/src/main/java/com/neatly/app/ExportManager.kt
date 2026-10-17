package com.neatly.app

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportManager {

    private fun timestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
    }

    private fun dateForFilename(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    }

    fun exportPdf(context: Context, data: Map<String, Map<String, String>>) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()
        val headerPaint = Paint()

        titlePaint.textSize = 24f
        titlePaint.isFakeBoldText = true
        titlePaint.color = Color.BLACK

        headerPaint.textSize = 16f
        headerPaint.isFakeBoldText = true
        headerPaint.color = Color.DKGRAY

        paint.textSize = 12f
        paint.color = Color.BLACK

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        var y = 50f
        canvas.drawText("Neatly Batch Record", 50f, y, titlePaint)
        y += 20f
        canvas.drawText("Date: ${timestamp()}", 50f, y, paint)
        y += 40f

        for ((section, rows) in data) {
            if (y > 750f) {
                pdfDocument.finishPage(page)
                // Note: Adding multipage support would go here. For a single batch, 1 page is usually enough.
            }
            canvas.drawText(section.uppercase(), 50f, y, headerPaint)
            y += 20f
            for ((label, value) in rows) {
                canvas.drawText(label, 60f, y, paint)
                canvas.drawText(value, 400f, y, paint)
                y += 18f
            }
            y += 20f
        }

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "Neatly_Export_${dateForFilename()}.pdf")
        try {
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            fos.close()
            pdfDocument.close()
            shareFile(context, file, "application/pdf")
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
        }
    }

    fun exportTsv(context: Context, data: Map<String, Map<String, String>>) {
        val sb = StringBuilder()
        
        // Build header row (Timestamp + all unique labels across all sections)
        val allLabels = mutableListOf("Timestamp")
        for ((_, rows) in data) {
            for (label in rows.keys) {
                if (!allLabels.contains(label)) {
                    allLabels.add(label)
                }
            }
        }
        
        sb.append(allLabels.joinToString("\t")).append("\n")
        
        // Build data row
        val dataRow = mutableListOf(timestamp())
        for (i in 1 until allLabels.size) {
            val label = allLabels[i]
            var foundValue = "0.000" // Default for unused tabs
            
            for ((_, rows) in data) {
                if (rows.containsKey(label)) {
                    foundValue = rows[label] ?: "0.000"
                    break
                }
            }
            dataRow.add(foundValue.replace(" ", "").replace("→", "").trim())
        }
        
        sb.append(dataRow.joinToString("\t")).append("\n")

        val file = File(context.cacheDir, "Neatly_Export_${dateForFilename()}.tsv")
        try {
            file.writeText(sb.toString())
            shareFile(context, file, "text/tab-separated-values")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Batch Record"))
    }
}
