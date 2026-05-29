package com.example.fees.utils


import com.example.fees.dtos.responses.ReceiptResponse
import com.lowagie.text.*
import com.lowagie.text.pdf.*

import java.util.*


import com.lowagie.text.*
import com.lowagie.text.pdf.*
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.awt.Color

object ReceiptPdfGenerator {

    fun buildPdf(r: ReceiptResponse): ByteArray {
        val out = ByteArrayOutputStream()
        val document = Document(PageSize.A4, 36f, 36f, 36f, 36f)
        PdfWriter.getInstance(document, out)
        document.open()

        val brandBlue = Color(47, 85, 151)
        val lightGray = Color(245, 247, 251)

        val titleFont = Font(Font.HELVETICA, 16f, Font.BOLD, brandBlue)
        val subFont = Font(Font.HELVETICA, 10f, Font.NORMAL, Color.DARK_GRAY)
        val labelFont = Font(Font.HELVETICA, 10f, Font.BOLD, Color.DARK_GRAY)
        val valueFont = Font(Font.HELVETICA, 10f, Font.NORMAL, Color.BLACK)
        val bigFont = Font(Font.HELVETICA, 12f, Font.BOLD, Color.BLACK)

        // Header
        document.add(Paragraph("King Of Glory School", titleFont))
        document.add(Paragraph("Official Payment Receipt", subFont))
        document.add(Chunk.NEWLINE)

        val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm").format(Date(r.createdAt))

        // Meta (right aligned)
        val meta = PdfPTable(2)
        meta.widthPercentage = 100f
        meta.setWidths(floatArrayOf(70f, 30f))

        meta.addCell(noBorderCell("", valueFont))
        meta.addCell(noBorderCell("Receipt No: ${r.receiptNo}", labelFont, Element.ALIGN_RIGHT))

        meta.addCell(noBorderCell("", valueFont))
        meta.addCell(noBorderCell("Date: $dateStr", valueFont, Element.ALIGN_RIGHT))

        document.add(meta)
        document.add(Chunk.NEWLINE)

        // Info table
        val table = PdfPTable(2)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(35f, 65f))

        table.addCell(kCell("Student", labelFont, lightGray))
        table.addCell(vCell(r.studentName, valueFont))

        table.addCell(kCell("Class", labelFont, lightGray))
        table.addCell(vCell(r.className, valueFont))

        table.addCell(kCell("Term / Year", labelFont, lightGray))
        table.addCell(vCell("${r.termName} / ${r.academicYearName}", valueFont))

        table.addCell(kCell("Payment Method", labelFont, lightGray))
        table.addCell(vCell(r.paymentMethod ?: "—", valueFont))

        table.addCell(kCell("Amount Paid", labelFont, lightGray))
        table.addCell(vCell("GH₵ ${r.amountPaid}", bigFont))

        table.addCell(kCell("Balance After", labelFont, lightGray))
        table.addCell(vCell("GH₵ ${r.balanceAfter}", bigFont))

        document.add(table)

        document.add(Chunk.NEWLINE)
        document.add(Paragraph("This receipt was generated electronically. No signature required.", subFont))

        document.close()
        return out.toByteArray()
    }

    private fun noBorderCell(text: String, font: Font, align: Int = Element.ALIGN_LEFT): PdfPCell {
        val cell = PdfPCell(Phrase(text, font))
        cell.border = Rectangle.NO_BORDER
        cell.horizontalAlignment = align
        return cell
    }

    private fun kCell(text: String, font: Font, bg: Color): PdfPCell {
        val cell = PdfPCell(Phrase(text, font))
        cell.setBackgroundColor(bg)
        cell.setPadding(8f)                 // ✅ FIX: method, not property
        cell.borderColor = Color(238, 242, 247)
        return cell
    }

    private fun vCell(text: String, font: Font): PdfPCell {
        val cell = PdfPCell(Phrase(text, font))
        cell.setPadding(8f)                 // ✅ FIX: method, not property
        cell.borderColor = Color(238, 242, 247)
        return cell
    }
}