package com.tollscan.app.export

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.tollscan.app.data.TollReceipt
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

/**
 * Builds a PDF report using only android.graphics.pdf.PdfDocument (no external PDF library
 * required, so this always compiles regardless of network access at build time).
 *
 * Page 1..n: a summary table of all receipts in the chosen period.
 * Following pages: one attached receipt photo per page, for audit purposes.
 */
object PdfExporter {

    private const val PAGE_WIDTH = 595  // A4 @ 72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    fun export(context: Context, file: File, receipts: List<TollReceipt>, periodLabel: String) {
        val document = PdfDocument()
        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true; color = Color.rgb(20, 60, 110) }
        val subPaint = Paint().apply { textSize = 11f; color = Color.DKGRAY }
        val headerPaint = Paint().apply { textSize = 10f; isFakeBoldText = true; color = Color.WHITE }
        val bodyPaint = Paint().apply { textSize = 10f; color = Color.BLACK }
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
        val headerBgPaint = Paint().apply { color = Color.rgb(20, 60, 110) }
        val currency = NumberFormat.getNumberInstance(Locale("in", "ID"))

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = MARGIN

        canvas.drawText("Laporan Struk Tol", MARGIN, y + 18f, titlePaint)
        y += 26f
        canvas.drawText("Periode: $periodLabel", MARGIN, y, subPaint)
        y += 14f
        canvas.drawText(
            "Total Transaksi: ${receipts.size}   |   Total Tarif: Rp ${currency.format(receipts.sumOf { it.tarif })}",
            MARGIN, y, subPaint
        )
        y += 20f

        val colWidths = floatArrayOf(90f, 190f, 60f, 90f)
        val colX = floatArrayOf(
            MARGIN,
            MARGIN + colWidths[0],
            MARGIN + colWidths[0] + colWidths[1],
            MARGIN + colWidths[0] + colWidths[1] + colWidths[2]
        )
        val headers = listOf("Tanggal", "Gerbang Tol", "Jam", "Tarif")

        fun drawTableHeader() {
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f, headerBgPaint)
            headers.forEachIndexed { i, h -> canvas.drawText(h, colX[i] + 4, y + 14f, headerPaint) }
            y += 20f
        }

        drawTableHeader()

        receipts.forEach { r ->
            if (y > PAGE_HEIGHT - MARGIN - 30f) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                y = MARGIN
                drawTableHeader()
            }
            canvas.drawText(r.tanggal, colX[0] + 4, y + 14f, bodyPaint)
            canvas.drawText(r.gerbangTol.take(30), colX[1] + 4, y + 14f, bodyPaint)
            canvas.drawText(r.jam, colX[2] + 4, y + 14f, bodyPaint)
            canvas.drawText("Rp ${currency.format(r.tarif)}", colX[3] + 4, y + 14f, bodyPaint)
            canvas.drawLine(MARGIN, y + 18f, PAGE_WIDTH - MARGIN, y + 18f, linePaint)
            y += 20f
        }

        document.finishPage(page)

        // Attachment pages: one photo per receipt.
        receipts.forEach { r ->
            val bmp = try {
                BitmapFactory.decodeFile(r.imagePath)
            } catch (e: Exception) {
                null
            }
            if (bmp != null) {
                pageNumber++
                val attPage = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                val c = attPage.canvas
                c.drawText("Lampiran Foto Struk - ${r.tanggal} ${r.jam} - ${r.gerbangTol}", MARGIN, MARGIN, subPaint)

                val maxW = PAGE_WIDTH - 2 * MARGIN
                val maxH = PAGE_HEIGHT - 2 * MARGIN - 20f
                val scale = minOf(maxW / bmp.width, maxH / bmp.height)
                val drawW = bmp.width * scale
                val drawH = bmp.height * scale
                val left = (PAGE_WIDTH - drawW) / 2f
                val top = MARGIN + 20f
                val destRect = RectF(left, top, left + drawW, top + drawH)
                c.drawBitmap(bmp, null, destRect, null)

                document.finishPage(attPage)
                bmp.recycle()
            }
        }

        FileOutputStream(file).use { out -> document.writeTo(out) }
        document.close()
    }
}
