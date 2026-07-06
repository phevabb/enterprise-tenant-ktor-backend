package com.example.student.services

import com.example.academics.dtos.response.StudentReportCardResponse
import com.example.academics.dtos.response.SubjectScoreInlineResponse
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.io.ByteArrayOutputStream
import java.net.URL
import java.text.Normalizer
import java.util.Locale

class StudentReportPdfService {

    fun generateReportPack(
        schoolName: String,
        records: List<StudentReportCardResponse>
    ): ByteArray {
        val document = PDDocument()

        try {
            if (records.isEmpty()) {
                addEmptyPage(
                    document = document,
                    schoolName = schoolName
                )
            } else {
                records.forEach { record ->
                    addReportPage(
                        document = document,
                        schoolName = schoolName,
                        record = record
                    )
                }
            }

            val output = ByteArrayOutputStream()
            document.save(output)

            return output.toByteArray()
        } finally {
            document.close()
        }
    }

    private fun addEmptyPage(
        document: PDDocument,
        schoolName: String
    ) {
        val page = PDPage(PDRectangle.A4)
        document.addPage(page)

        PDPageContentStream(document, page).use { content ->
            drawPageBackground(content, page)

            writeText(
                content = content,
                text = schoolName.ifBlank { "School Name" },
                x = 40f,
                y = 760f,
                font = PDType1Font.HELVETICA_BOLD,
                fontSize = 18f
            )

            writeText(
                content = content,
                text = "No report card records found.",
                x = 40f,
                y = 720f,
                font = PDType1Font.HELVETICA,
                fontSize = 12f
            )
        }
    }

    private fun addReportPage(
        document: PDDocument,
        schoolName: String,
        record: StudentReportCardResponse
    ) {
        val page = PDPage(PDRectangle.A4)
        document.addPage(page)

        PDPageContentStream(document, page).use { content ->
            val pageWidth = page.mediaBox.width
            val pageHeight = page.mediaBox.height
            val margin = 36f
            var y = pageHeight - 38f

            drawPageBackground(content, page)

            drawHeader(
                document = document,
                content = content,
                schoolName = schoolName,
                record = record,
                pageWidth = pageWidth,
                margin = margin,
                y = y
            )

            y -= 122f

            writeText(
                content = content,
                text = "Academic Performance",
                x = margin,
                y = y,
                font = PDType1Font.HELVETICA_BOLD,
                fontSize = 13f
            )

            y -= 18f

            y = drawSubjectTable(
                content = content,
                record = record,
                margin = margin,
                y = y
            )

            y -= 20f

            y = drawSummaryBoxes(
                content = content,
                record = record,
                margin = margin,
                y = y,
                pageWidth = pageWidth
            )

            y -= 24f

            y = drawRemarksAndPromotionSection(
                content = content,
                record = record,
                margin = margin,
                y = y,
                pageWidth = pageWidth
            )

            drawFooter(
                content = content,
                margin = margin,
                pageWidth = pageWidth
            )
        }
    }

    private fun drawPageBackground(
        content: PDPageContentStream,
        page: PDPage
    ) {
        content.setNonStrokingColor(248, 250, 252)
        content.addRect(
            0f,
            0f,
            page.mediaBox.width,
            page.mediaBox.height
        )
        content.fill()
        content.setNonStrokingColor(15, 23, 42)
    }

    private fun drawHeader(
        document: PDDocument,
        content: PDPageContentStream,
        schoolName: String,
        record: StudentReportCardResponse,
        pageWidth: Float,
        margin: Float,
        y: Float
    ) {
        val headerHeight = 98f
        val headerX = margin
        val headerY = y - headerHeight + 4f
        val headerWidth = pageWidth - (margin * 2)

        // White header background
        content.setNonStrokingColor(255, 255, 255)
        content.addRect(
            headerX,
            headerY,
            headerWidth,
            headerHeight
        )
        content.fill()

// Soft premium border
        content.setStrokingColor(226, 232, 240)
        content.setLineWidth(0.8f)
        content.addRect(
            headerX,
            headerY,
            headerWidth,
            headerHeight
        )
        content.stroke()

        content.setNonStrokingColor(15, 23, 42)

        writeText(
            content = content,
            text = schoolName.ifBlank { "School Name" },
            x = margin + 15f,
            y = y - 18f,
            font = PDType1Font.HELVETICA_BOLD,
            fontSize = 18f
        )

        writeText(
            content = content,
            text = "ACADEMIC REPORT",
            x = margin + 15f,
            y = y - 39f,
            font = PDType1Font.HELVETICA_BOLD,
            fontSize = 10f
        )

        writeText(
            content = content,
            text = "Academic Year: ${record.academicYear.name} | Term: ${record.term.name} | Class: ${record.classLevel.name}",
            x = margin + 15f,
            y = y - 59f,
            font = PDType1Font.HELVETICA,
            fontSize = 9f
        )

        val avatarSize = 48f
        val avatarX = pageWidth - margin - 72f
        val avatarY = y - 62f

        drawStudentAvatar(
            document = document,
            content = content,
            imageUrl = getStudentProfilePictureUrl(record),
            studentName = record.student.name,
            x = avatarX,
            y = avatarY,
            size = avatarSize
        )

        writeRightText(
            content = content,
            text = record.student.name,
            rightX = avatarX - 10f,
            y = y - 23f,
            font = PDType1Font.HELVETICA_BOLD,
            fontSize = 12f
        )

        val position = record.overallPosition?.let { ordinal(it) } ?: "-"

        writeRightText(
            content = content,
            text = "Overall Position: $position / ${record.numberOnRoll}",
            rightX = avatarX - 10f,
            y = y - 45f,
            font = PDType1Font.HELVETICA,
            fontSize = 9f
        )

        writeRightText(
            content = content,
            text = "Student ID: ${record.student.id}",
            rightX = avatarX - 10f,
            y = y - 63f,
            font = PDType1Font.HELVETICA,
            fontSize = 8.5f
        )
    }

    private fun drawStudentAvatar(
        document: PDDocument,
        content: PDPageContentStream,
        imageUrl: String?,
        studentName: String,
        x: Float,
        y: Float,
        size: Float
    ) {
        content.setNonStrokingColor(255, 255, 255)
        content.addRect(x, y, size, size)
        content.fill()

        content.setStrokingColor(59, 130, 246)
        content.setLineWidth(1.2f)
        content.addRect(x, y, size, size)
        content.stroke()

        val imageBytes = imageUrl
            ?.takeIf { it.isNotBlank() }
            ?.let { downloadImageBytes(it) }

        if (imageBytes != null) {
            try {
                val image = PDImageXObject.createFromByteArray(
                    document,
                    imageBytes,
                    "student-profile-picture"
                )

                content.drawImage(
                    image,
                    x + 3f,
                    y + 3f,
                    size - 6f,
                    size - 6f
                )

                return
            } catch (_: Exception) {
                // fall back to initials
            }
        }

        drawInitialsAvatar(
            content = content,
            studentName = studentName,
            x = x,
            y = y,
            size = size
        )
    }

    private fun drawInitialsAvatar(
        content: PDPageContentStream,
        studentName: String,
        x: Float,
        y: Float,
        size: Float
    ) {
        content.setNonStrokingColor(37, 99, 235)
        content.addRect(x + 3f, y + 3f, size - 6f, size - 6f)
        content.fill()

        val initials = studentName
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "ST" }

        content.setNonStrokingColor(255, 255, 255)

        writeCenteredText(
            content = content,
            text = initials,
            centerX = x + (size / 2),
            y = y + 19f,
            font = PDType1Font.HELVETICA_BOLD,
            fontSize = 15f
        )

        content.setNonStrokingColor(15, 23, 42)
    }

    private fun drawSubjectTable(
        content: PDPageContentStream,
        record: StudentReportCardResponse,
        margin: Float,
        y: Float
    ): Float {
        var currentY = y
        val rowHeight = 20f

        val widths = listOf(
            25f,
            130f,
            50f,
            50f,
            50f,
            45f,
            115f,
            45f
        )

        drawTableRow(
            content = content,
            x = margin,
            y = currentY,
            height = rowHeight,
            widths = widths,
            values = listOf(
                "#",
                "Subject",
                "Class",
                "Exam",
                "Total",
                "Grade",
                "Interpretation",
                "Position"
            ),
            header = true
        )

        currentY -= rowHeight

        record.subjects.forEachIndexed { index, subject ->
            drawTableRow(
                content = content,
                x = margin,
                y = currentY,
                height = rowHeight,
                widths = widths,
                values = listOf(
                    (index + 1).toString(),
                    subject.subjectName,
                    show(subject.classScore),
                    show(subject.examScore),
                    show(subject.totalScore),
                    subject.gradeCode ?: "-",
                    subject.interpretation ?: "-",
                    subject.position?.let { ordinal(it) } ?: "-"
                ),
                header = false
            )

            currentY -= rowHeight
        }

        val classTotal = record.subjects.sumOf { it.classScore ?: 0 }
        val examTotal = record.subjects.sumOf { it.examScore ?: 0 }
        val total = record.rawScoreTotal ?: record.subjects.sumOf { it.totalScore ?: 0 }
        val average = averageTotal(record.subjects)

        drawTableRow(
            content = content,
            x = margin,
            y = currentY,
            height = rowHeight,
            widths = widths,
            values = listOf(
                "",
                "Totals / Average",
                classTotal.toString(),
                examTotal.toString(),
                total.toString(),
                "-",
                "Avg: ${"%.1f".format(Locale.US, average)}",
                "-"
            ),
            header = true
        )

        return currentY - rowHeight
    }

    private fun drawTableRow(
        content: PDPageContentStream,
        x: Float,
        y: Float,
        height: Float,
        widths: List<Float>,
        values: List<String>,
        header: Boolean
    ) {
        val totalWidth = widths.sum()

        if (header) {
            content.setNonStrokingColor(239, 246, 255)
        } else {
            content.setNonStrokingColor(255, 255, 255)
        }

        content.addRect(
            x,
            y - height + 4f,
            totalWidth,
            height
        )
        content.fill()

        content.setStrokingColor(226, 232, 240)
        content.setLineWidth(0.6f)
        content.addRect(
            x,
            y - height + 4f,
            totalWidth,
            height
        )
        content.stroke()

        var currentX = x

        values.forEachIndexed { index, value ->
            content.setNonStrokingColor(15, 23, 42)

            writeText(
                content = content,
                text = truncate(value, getColumnMaxChars(index)),
                x = currentX + 4f,
                y = y - 12f,
                font = if (header) PDType1Font.HELVETICA_BOLD else PDType1Font.HELVETICA,
                fontSize = if (header) 7.6f else 7.2f
            )

            currentX += widths[index]
        }

        content.setNonStrokingColor(15, 23, 42)
    }

    private fun drawSummaryBoxes(
        content: PDPageContentStream,
        record: StudentReportCardResponse,
        margin: Float,
        y: Float,
        pageWidth: Float
    ): Float {
        val boxGap = 10f
        val boxHeight = 50f
        val boxWidth = (pageWidth - (margin * 2) - (boxGap * 3)) / 4

        val total = record.rawScoreTotal ?: record.subjects.sumOf { it.totalScore ?: 0 }
        val average = averageTotal(record.subjects)
        val position = record.overallPosition?.let { ordinal(it) } ?: "-"
        val conduct = record.conduct ?: "-"

        val items = listOf(
            "Total Score" to total.toString(),
            "Average" to "%.1f".format(Locale.US, average),
            "Position" to position,
            "Conduct" to conduct
        )

        items.forEachIndexed { index, item ->
            val x = margin + (index * (boxWidth + boxGap))

            drawMetricCard(
                content = content,
                label = item.first,
                value = item.second,
                x = x,
                y = y,
                width = boxWidth,
                height = boxHeight
            )
        }

        return y - boxHeight
    }

    private fun drawMetricCard(
        content: PDPageContentStream,
        label: String,
        value: String,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) {
        content.setNonStrokingColor(255, 255, 255)
        content.addRect(x, y - height + 4f, width, height)
        content.fill()

        content.setStrokingColor(226, 232, 240)
        content.setLineWidth(0.7f)
        content.addRect(x, y - height + 4f, width, height)
        content.stroke()

        content.setNonStrokingColor(100, 116, 139)

        writeText(
            content = content,
            text = label,
            x = x + 8f,
            y = y - 16f,
            font = PDType1Font.HELVETICA_BOLD,
            fontSize = 7.5f
        )

        content.setNonStrokingColor(15, 23, 42)

        writeText(
            content = content,
            text = truncate(value, 18),
            x = x + 8f,
            y = y - 35f,
            font = PDType1Font.HELVETICA_BOLD,
            fontSize = 11f
        )
    }

    private fun drawRemarksAndPromotionSection(
        content: PDPageContentStream,
        record: StudentReportCardResponse,
        margin: Float,
        y: Float,
        pageWidth: Float
    ): Float {
        val gap = 14f
        val leftWidth = ((pageWidth - (margin * 2)) * 0.64f) - (gap / 2)
        val rightWidth = ((pageWidth - (margin * 2)) * 0.36f) - (gap / 2)
        val boxHeight = 118f

        val leftX = margin
        val rightX = margin + leftWidth + gap

        drawInfoPanel(
            content = content,
            title = "Remarks",
            rows = listOf(
                "Attitude" to (record.attitude ?: "-"),
                "Interest" to (record.interest ?: "-"),
                "Teacher's Remarks" to (record.teacherRemarks ?: "-"),
                "Head Teacher's Remarks" to (record.headTeacherRemarks ?: "-")
            ),
            x = leftX,
            y = y,
            width = leftWidth,
            height = boxHeight
        )

        drawInfoPanel(
            content = content,
            title = "Promotion / Next Term",
            rows = listOf(
                "Promoted To" to (record.promotedTo ?: "-"),
                "Next Term Begins" to (record.nextTermBegins ?: "-")
            ),
            x = rightX,
            y = y,
            width = rightWidth,
            height = boxHeight
        )

        return y - boxHeight
    }

    private fun drawInfoPanel(
        content: PDPageContentStream,
        title: String,
        rows: List<Pair<String, String>>,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) {
        content.setNonStrokingColor(255, 255, 255)
        content.addRect(x, y - height + 4f, width, height)
        content.fill()

        content.setStrokingColor(226, 232, 240)
        content.setLineWidth(0.8f)
        content.addRect(x, y - height + 4f, width, height)
        content.stroke()

        content.setNonStrokingColor(239, 246, 255)
        content.addRect(x, y - 24f, width, 28f)
        content.fill()

        content.setNonStrokingColor(30, 64, 175)

        writeText(
            content = content,
            text = title,
            x = x + 10f,
            y = y - 14f,
            font = PDType1Font.HELVETICA_BOLD,
            fontSize = 9.5f
        )

        var currentY = y - 40f

        rows.forEach { row ->
            content.setNonStrokingColor(71, 85, 105)

            writeText(
                content = content,
                text = "${row.first}:",
                x = x + 10f,
                y = currentY,
                font = PDType1Font.HELVETICA_BOLD,
                fontSize = 8.2f
            )

            content.setNonStrokingColor(15, 23, 42)

            currentY = writeWrappedLine(
                content = content,
                text = row.second,
                x = x + 118f,
                y = currentY,
                maxWidth = width - 128f,
                font = PDType1Font.HELVETICA,
                fontSize = 8.2f,
                lineHeight = 10f
            )

            currentY -= 3f
        }

        content.setNonStrokingColor(15, 23, 42)
    }

    private fun drawFooter(
        content: PDPageContentStream,
        margin: Float,
        pageWidth: Float
    ) {
        content.setStrokingColor(226, 232, 240)
        content.moveTo(margin, 44f)
        content.lineTo(pageWidth - margin, 44f)
        content.stroke()

        writeText(
            content = content,
            text = "Generated by Phena Systems",
            x = margin,
            y = 29f,
            font = PDType1Font.HELVETICA_OBLIQUE,
            fontSize = 8f
        )
    }

    private fun writeText(
        content: PDPageContentStream,
        text: String,
        x: Float,
        y: Float,
        font: PDFont,
        fontSize: Float
    ) {
        content.beginText()
        content.setFont(font, fontSize)
        content.newLineAtOffset(x, y)
        content.showText(safe(text))
        content.endText()
    }

    private fun writeRightText(
        content: PDPageContentStream,
        text: String,
        rightX: Float,
        y: Float,
        font: PDFont,
        fontSize: Float
    ) {
        val cleanedText = safe(text)
        val textWidth = font.getStringWidth(cleanedText) / 1000f * fontSize
        val x = rightX - textWidth

        writeText(
            content = content,
            text = cleanedText,
            x = x,
            y = y,
            font = font,
            fontSize = fontSize
        )
    }

    private fun writeCenteredText(
        content: PDPageContentStream,
        text: String,
        centerX: Float,
        y: Float,
        font: PDFont,
        fontSize: Float
    ) {
        val cleanedText = safe(text)
        val textWidth = font.getStringWidth(cleanedText) / 1000f * fontSize
        val x = centerX - (textWidth / 2)

        writeText(
            content = content,
            text = cleanedText,
            x = x,
            y = y,
            font = font,
            fontSize = fontSize
        )
    }

    private fun writeWrappedLine(
        content: PDPageContentStream,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        font: PDFont = PDType1Font.HELVETICA,
        fontSize: Float = 9f,
        lineHeight: Float = 13f
    ): Float {
        val words = safe(text).split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        words.forEach { word ->
            val candidate = if (currentLine.isBlank()) {
                word
            } else {
                "$currentLine $word"
            }

            val candidateWidth = font.getStringWidth(candidate) / 1000f * fontSize

            if (candidateWidth <= maxWidth) {
                currentLine = candidate
            } else {
                if (currentLine.isNotBlank()) {
                    lines += currentLine
                }

                currentLine = word
            }
        }

        if (currentLine.isNotBlank()) {
            lines += currentLine
        }

        var currentY = y

        lines.forEach { line ->
            writeText(
                content = content,
                text = line,
                x = x,
                y = currentY,
                font = font,
                fontSize = fontSize
            )

            currentY -= lineHeight
        }

        return currentY
    }

    private fun downloadImageBytes(
        imageUrl: String
    ): ByteArray? {
        return try {
            val connection = URL(imageUrl).openConnection()
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.getInputStream().use { input ->
                input.readBytes()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getStudentProfilePictureUrl(
        record: StudentReportCardResponse
    ): String? {
        /**
         * This assumes your StudentReportCardResponse.student DTO has:
         *
         * val profilePictureUrl: String?
         *
         * If your DTO uses another name, change this line.
         */
        return record.student.profilePictureUrl
    }

    private fun show(
        value: Int?
    ): String {
        return value?.toString() ?: "-"
    }

    private fun averageTotal(
        subjects: List<SubjectScoreInlineResponse>
    ): Double {
        val validScores = subjects.mapNotNull { it.totalScore }

        if (validScores.isEmpty()) {
            return 0.0
        }

        return validScores.average()
    }

    private fun ordinal(
        number: Int
    ): String {
        val suffix = if (number % 100 in 11..13) {
            "th"
        } else {
            when (number % 10) {
                1 -> "st"
                2 -> "nd"
                3 -> "rd"
                else -> "th"
            }
        }

        return "$number$suffix"
    }

    private fun getColumnMaxChars(
        index: Int
    ): Int {
        return when (index) {
            0 -> 3
            1 -> 24
            2 -> 8
            3 -> 8
            4 -> 8
            5 -> 8
            6 -> 18
            7 -> 8
            else -> 12
        }
    }

    private fun truncate(
        value: String,
        max: Int
    ): String {
        val cleaned = safe(value)

        return if (cleaned.length <= max) {
            cleaned
        } else {
            cleaned.take(max - 3) + "..."
        }
    }

    private fun safe(
        value: String
    ): String {
        val normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")

        return normalized
            .replace("₵", "GHS ")
            .replace("–", "-")
            .replace("—", "-")
            .replace("“", "\"")
            .replace("”", "\"")
            .replace("’", "'")
            .replace("\n", " ")
            .replace("\r", " ")
    }
}