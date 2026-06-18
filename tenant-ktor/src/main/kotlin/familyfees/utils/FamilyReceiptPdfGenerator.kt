package com.example.familyfees.utils



import com.example.familyfees.dtos.responses.FamilyReceiptDto
import com.lowagie.text.*
import com.lowagie.text.pdf.*
import java.awt.Color
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date

object FamilyReceiptPdfGenerator {

    fun buildPdf(r: FamilyReceiptDto): ByteArray {
        val out = ByteArrayOutputStream()
        val doc = Document(PageSize.A4, 36f, 36f, 36f, 36f)
        PdfWriter.getInstance(doc, out)
        doc.open()

        // 🎨 Colors
        val brandBlue = Color(47, 85, 151)
        val lightGray = Color(245, 247, 251)
        val borderGray = Color(238, 242, 247)

        // 🔤 Fonts
        val titleFont = Font(Font.HELVETICA, 16f, Font.BOLD, brandBlue)
        val subFont = Font(Font.HELVETICA, 10f, Font.NORMAL, Color.DARK_GRAY)
        val labelFont = Font(Font.HELVETICA, 10f, Font.BOLD, Color.DARK_GRAY)
        val valueFont = Font(Font.HELVETICA, 10f, Font.NORMAL, Color.BLACK)
        val bigFont = Font(Font.HELVETICA, 12f, Font.BOLD, Color.BLACK)

        // ✅ Header
        doc.add(Paragraph("PHENA SOFTWARE SYSTEMS", titleFont))
        doc.add(Paragraph("Family Fees Payment Receipt", subFont))
        doc.add(Chunk.NEWLINE)

        val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm").format(Date(r.createdAt))

        // ✅ Meta row (Receipt No + Date aligned right)
        val meta = PdfPTable(2)
        meta.widthPercentage = 100f
        meta.setWidths(floatArrayOf(70f, 30f))

        meta.addCell(noBorderCell("", valueFont))
        meta.addCell(noBorderCell("Receipt No: ${r.receiptNo}", labelFont, Element.ALIGN_RIGHT))

        meta.addCell(noBorderCell("", valueFont))
        meta.addCell(noBorderCell("Date: $dateStr", valueFont, Element.ALIGN_RIGHT))

        doc.add(meta)
        doc.add(Chunk.NEWLINE)

        // ✅ Main table
        val table = PdfPTable(2)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(35f, 65f))

        table.addCell(kCell("Family", labelFont, lightGray, borderGray))
        table.addCell(vCell(r.familyName, valueFont, borderGray))

        table.addCell(kCell("Wards", labelFont, lightGray, borderGray))
        table.addCell(vCell(r.wards.joinToString(", ").ifBlank { "—" }, valueFont, borderGray))

        table.addCell(kCell("Term / Year", labelFont, lightGray, borderGray))
        table.addCell(vCell("${r.termName ?: "—"} / ${r.academicYearName ?: "—"}", valueFont, borderGray))

        table.addCell(kCell("Payment Method", labelFont, lightGray, borderGray))
        table.addCell(vCell(r.paymentMethod, valueFont, borderGray))

        table.addCell(kCell("Amount Paid", labelFont, lightGray, borderGray))
        table.addCell(vCell("GH₵ ${r.amountPaid}", bigFont, borderGray))

        table.addCell(kCell("Balance After", labelFont, lightGray, borderGray))
        table.addCell(vCell("GH₵ ${r.balanceAfter}", bigFont, borderGray))

        doc.add(table)

        doc.add(Chunk.NEWLINE)
        doc.add(Paragraph("This receipt was generated electronically. No signature required.", subFont))

        doc.close()
        return out.toByteArray()
    }

    private fun noBorderCell(text: String, font: Font, align: Int = Element.ALIGN_LEFT): PdfPCell {
        val cell = PdfPCell(Phrase(text, font))
        cell.border = Rectangle.NO_BORDER
        cell.horizontalAlignment = align
        return cell
    }

    private fun kCell(text: String, font: Font, bg: Color, border: Color): PdfPCell {
        val cell = PdfPCell(Phrase(text, font))
        cell.setBackgroundColor(bg)
        cell.borderColor = border
        cell.setPadding(8f) // ✅ OpenPDF safe
        return cell
    }

    private fun vCell(text: String, font: Font, border: Color): PdfPCell {
        val cell = PdfPCell(Phrase(text, font))
        cell.borderColor = border
        cell.setPadding(8f) // ✅ OpenPDF safe
        return cell
    }
}