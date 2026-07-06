package com.example.billing.pdf

import com.example.billing.dto.StudentBillResponse
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StudentBillPdfService {

    private const val margin = 48f
    private const val tableWidth = 500f
    private const val rowHeight = 24f

    private val regularFont = PDType1Font.HELVETICA
    private val boldFont = PDType1Font.HELVETICA_BOLD
    private val italicFont = PDType1Font.HELVETICA_OBLIQUE

    fun generateStudentBillPdf(
        bill: StudentBillResponse,
        schoolName: String = "Phena School",
        supportEmail: String = "support@phenaschool.com"
    ): ByteArray {
        val document = PDDocument()
        val output = ByteArrayOutputStream()

        try {
            var page = PDPage(PDRectangle.A4)
            document.addPage(page)

            var content = PDPageContentStream(document, page)

            var y = 785f

            y = drawHeader(
                content = content,
                bill = bill,
                schoolName = schoolName,
                supportEmail = supportEmail,
                startY = y
            )

            y -= 22f

            drawSectionTitle(
                content = content,
                text = "Bill Items",
                x = margin,
                y = y
            )

            y -= 24f

            drawTableHeader(
                content = content,
                x = margin,
                y = y,
                width = tableWidth,
                height = rowHeight
            )

            y -= rowHeight

            bill.items.forEachIndexed { index, item ->
                if (y < 125f) {
                    drawFooter(content)
                    content.close()

                    page = PDPage(PDRectangle.A4)
                    document.addPage(page)
                    content = PDPageContentStream(document, page)

                    y = 785f

                    drawContinuationHeader(
                        content = content,
                        bill = bill,
                        schoolName = schoolName,
                        startY = y
                    )

                    y -= 54f

                    drawTableHeader(
                        content = content,
                        x = margin,
                        y = y,
                        width = tableWidth,
                        height = rowHeight
                    )

                    y -= rowHeight
                }

                drawTableRow(
                    content = content,
                    x = margin,
                    y = y,
                    width = tableWidth,
                    height = rowHeight,
                    number = (index + 1).toString(),
                    itemName = item.itemName,
                    itemType = item.itemType,
                    amount = money(item.amountCedis)
                )

                y -= rowHeight
            }

            y -= 30f

            if (y < 190f) {
                drawFooter(content)
                content.close()

                page = PDPage(PDRectangle.A4)
                document.addPage(page)
                content = PDPageContentStream(document, page)

                y = 760f

                drawContinuationHeader(
                    content = content,
                    bill = bill,
                    schoolName = schoolName,
                    startY = y
                )

                y -= 70f
            }

            drawSummary(
                content = content,
                bill = bill,
                y = y
            )

            drawFooter(content)

            content.close()

            document.save(output)

            return output.toByteArray()
        } finally {
            document.close()
        }
    }

    private fun drawHeader(
        content: PDPageContentStream,
        bill: StudentBillResponse,
        schoolName: String,
        supportEmail: String,
        startY: Float
    ): Float {
        var y = startY

        drawText(
            content = content,
            text = schoolName,
            x = margin,
            y = y,
            font = boldFont,
            fontSize = 20f
        )

        y -= 22f

        drawText(
            content = content,
            text = "Student Bill / Fee Statement",
            x = margin,
            y = y,
            font = regularFont,
            fontSize = 12f
        )

        drawText(
            content = content,
            text = "Support: $supportEmail",
            x = 360f,
            y = y,
            font = regularFont,
            fontSize = 9f
        )

        y -= 24f

        drawLine(
            content = content,
            x1 = margin,
            y1 = y,
            x2 = 545f,
            y2 = y
        )

        y -= 30f

        drawSectionTitle(
            content = content,
            text = "Bill Information",
            x = margin,
            y = y
        )

        y -= 22f

        drawKeyValue(
            content = content,
            label = "Bill Number",
            value = bill.billNumber,
            x = margin,
            y = y
        )

        drawKeyValue(
            content = content,
            label = "Status",
            value = bill.status.uppercase(),
            x = 320f,
            y = y
        )

        y -= 18f

        drawKeyValue(
            content = content,
            label = "Student",
            value = bill.studentName ?: "Student ${bill.studentId}",
            x = margin,
            y = y
        )

        drawKeyValue(
            content = content,
            label = "Student ID",
            value = bill.studentId.toString(),
            x = 320f,
            y = y
        )

        y -= 18f

        drawKeyValue(
            content = content,
            label = "Class",
            value = bill.classNameSnapshot ?: "-",
            x = margin,
            y = y
        )

        drawKeyValue(
            content = content,
            label = "Category",
            value = bill.categoryName ?: "-",
            x = 320f,
            y = y
        )

        y -= 18f

        drawKeyValue(
            content = content,
            label = "Academic Year",
            value = bill.academicYearName ?: bill.academicYearId.toString(),
            x = margin,
            y = y
        )

        drawKeyValue(
            content = content,
            label = "Term",
            value = bill.academicTermName ?: bill.academicTermId.toString(),
            x = 320f,
            y = y
        )

        y -= 18f

        drawKeyValue(
            content = content,
            label = "Due Date",
            value = formatDate(bill.dueDateEpochMillis),
            x = margin,
            y = y
        )

        drawKeyValue(
            content = content,
            label = "Generated",
            value = formatDate(bill.createdAtEpochMillis),
            x = 320f,
            y = y
        )

        y -= 34f

        return y
    }

    private fun drawContinuationHeader(
        content: PDPageContentStream,
        bill: StudentBillResponse,
        schoolName: String,
        startY: Float
    ) {
        drawText(
            content = content,
            text = schoolName,
            x = margin,
            y = startY,
            font = boldFont,
            fontSize = 16f
        )

        drawText(
            content = content,
            text = "Bill Number: ${bill.billNumber}",
            x = margin,
            y = startY - 20f,
            font = regularFont,
            fontSize = 10f
        )

        drawText(
            content = content,
            text = "Continued",
            x = 460f,
            y = startY - 20f,
            font = italicFont,
            fontSize = 10f
        )

        drawLine(
            content = content,
            x1 = margin,
            y1 = startY - 34f,
            x2 = 545f,
            y2 = startY - 34f
        )
    }

    private fun drawSummary(
        content: PDPageContentStream,
        bill: StudentBillResponse,
        y: Float
    ) {
        var currentY = y

        val summaryLabelX = 330f
        val summaryValueX = 470f

        drawSectionTitle(
            content = content,
            text = "Summary",
            x = summaryLabelX,
            y = currentY
        )

        currentY -= 22f

        drawSummaryLine(
            content = content,
            label = "Subtotal",
            value = money(bill.subTotalCedis),
            labelX = summaryLabelX,
            valueX = summaryValueX,
            y = currentY
        )

        currentY -= 18f

        drawSummaryLine(
            content = content,
            label = "Arrears",
            value = money(bill.arrearsCedis),
            labelX = summaryLabelX,
            valueX = summaryValueX,
            y = currentY
        )

        currentY -= 18f

        drawSummaryLine(
            content = content,
            label = "Discount",
            value = money(bill.discountCedis),
            labelX = summaryLabelX,
            valueX = summaryValueX,
            y = currentY
        )

        currentY -= 10f

        drawLine(
            content = content,
            x1 = summaryLabelX,
            y1 = currentY,
            x2 = 545f,
            y2 = currentY
        )

        currentY -= 16f

        drawSummaryLine(
            content = content,
            label = "Total",
            value = money(bill.totalAmountCedis),
            labelX = summaryLabelX,
            valueX = summaryValueX,
            y = currentY,
            bold = true
        )

        currentY -= 20f

        drawSummaryLine(
            content = content,
            label = "Amount Paid",
            value = money(bill.amountPaidCedis),
            labelX = summaryLabelX,
            valueX = summaryValueX,
            y = currentY
        )

        currentY -= 18f

        drawSummaryLine(
            content = content,
            label = "Balance",
            value = money(bill.balanceCedis),
            labelX = summaryLabelX,
            valueX = summaryValueX,
            y = currentY,
            bold = true
        )
    }

    private fun drawFooter(
        content: PDPageContentStream
    ) {
        drawLine(
            content = content,
            x1 = margin,
            y1 = 72f,
            x2 = 545f,
            y2 = 72f
        )

        drawText(
            content = content,
            text = "This bill was generated by Phena School Management System.",
            x = margin,
            y = 54f,
            font = italicFont,
            fontSize = 9f
        )

        drawText(
            content = content,
            text = "Printed: ${formatDate(System.currentTimeMillis())}",
            x = margin,
            y = 40f,
            font = regularFont,
            fontSize = 8f
        )
    }

    private fun drawText(
        content: PDPageContentStream,
        text: String,
        x: Float,
        y: Float,
        font: PDType1Font,
        fontSize: Float
    ) {
        content.beginText()
        content.setFont(font, fontSize)
        content.newLineAtOffset(x, y)
        content.showText(safe(text))
        content.endText()
    }

    private fun drawSectionTitle(
        content: PDPageContentStream,
        text: String,
        x: Float,
        y: Float
    ) {
        drawText(
            content = content,
            text = text,
            x = x,
            y = y,
            font = boldFont,
            fontSize = 12f
        )
    }

    private fun drawKeyValue(
        content: PDPageContentStream,
        label: String,
        value: String,
        x: Float,
        y: Float
    ) {
        drawText(
            content = content,
            text = "$label:",
            x = x,
            y = y,
            font = boldFont,
            fontSize = 9f
        )

        drawText(
            content = content,
            text = value,
            x = x + 100f,
            y = y,
            font = regularFont,
            fontSize = 9f
        )
    }

    private fun drawLine(
        content: PDPageContentStream,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float
    ) {
        content.moveTo(x1, y1)
        content.lineTo(x2, y2)
        content.stroke()
    }

    private fun drawTableHeader(
        content: PDPageContentStream,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) {
        content.addRect(x, y - height + 4f, width, height)
        content.stroke()

        drawText(
            content = content,
            text = "#",
            x = x + 8f,
            y = y - 13f,
            font = boldFont,
            fontSize = 9f
        )

        drawText(
            content = content,
            text = "Item",
            x = x + 40f,
            y = y - 13f,
            font = boldFont,
            fontSize = 9f
        )

        drawText(
            content = content,
            text = "Type",
            x = x + 310f,
            y = y - 13f,
            font = boldFont,
            fontSize = 9f
        )

        drawText(
            content = content,
            text = "Amount",
            x = x + 410f,
            y = y - 13f,
            font = boldFont,
            fontSize = 9f
        )
    }

    private fun drawTableRow(
        content: PDPageContentStream,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        number: String,
        itemName: String,
        itemType: String,
        amount: String
    ) {
        content.addRect(x, y - height + 4f, width, height)
        content.stroke()

        drawText(
            content = content,
            text = number,
            x = x + 8f,
            y = y - 13f,
            font = regularFont,
            fontSize = 8.5f
        )

        drawText(
            content = content,
            text = truncate(itemName, 44),
            x = x + 40f,
            y = y - 13f,
            font = regularFont,
            fontSize = 8.5f
        )

        drawText(
            content = content,
            text = truncate(itemType, 16),
            x = x + 310f,
            y = y - 13f,
            font = regularFont,
            fontSize = 8.5f
        )

        drawText(
            content = content,
            text = amount,
            x = x + 410f,
            y = y - 13f,
            font = regularFont,
            fontSize = 8.5f
        )
    }

    private fun drawSummaryLine(
        content: PDPageContentStream,
        label: String,
        value: String,
        labelX: Float,
        valueX: Float,
        y: Float,
        bold: Boolean = false
    ) {
        val font = if (bold) {
            boldFont
        } else {
            regularFont
        }

        drawText(
            content = content,
            text = label,
            x = labelX,
            y = y,
            font = font,
            fontSize = 10f
        )

        drawText(
            content = content,
            text = value,
            x = valueX,
            y = y,
            font = font,
            fontSize = 10f
        )
    }

    private fun money(
        value: Double
    ): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US)

        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2

        return "GHS ${formatter.format(value)}"
    }

    private fun formatDate(
        epochMillis: Long?
    ): String {
        if (epochMillis == null) {
            return "-"
        }

        return SimpleDateFormat(
            "dd MMM yyyy",
            Locale.US
        ).format(Date(epochMillis))
    }

    private fun safe(
        value: String
    ): String {
        return value
            .replace("₵", "GHS ")
            .replace("–", "-")
            .replace("—", "-")
            .replace("“", "\"")
            .replace("”", "\"")
            .replace("’", "'")
            .replace("\n", " ")
            .replace("\r", " ")
    }

    private fun truncate(
        value: String,
        max: Int
    ): String {
        return if (value.length <= max) {
            value
        } else {
            value.take(max - 3) + "..."
        }
    }
}