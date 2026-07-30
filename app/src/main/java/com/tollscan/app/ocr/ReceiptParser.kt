package com.tollscan.app.ocr

import java.util.Calendar
import java.util.Locale

/**
 * Result of parsing raw OCR text from a toll receipt.
 * [confidence] is a rough 0-100 heuristic score, useful to warn the user
 * when a field probably needs manual correction.
 */
data class ParsedReceipt(
    val gerbangTol: String,
    val tanggal: String, // yyyy-MM-dd
    val jam: String,     // HH:mm
    val tarif: Long,
    val confidence: Int
)

/**
 * Parses raw OCR text coming from several common Indonesian toll receipt layouts
 * (Jasa Marga, Astra Tol, Jasamarga Related Business, various e-toll printers, etc).
 * Instead of relying on one fixed template, a list of candidate patterns is tried
 * for every field and the first plausible match wins.
 */
object ReceiptParser {

    private val gerbangPatterns = listOf(
        Regex("""(?i)(?:GERBANG\s*TOL|GERBANG|GT\.?)\s*[:\-]?\s*([A-Z0-9ÀÁ\s./\-]{3,40})"""),
        Regex("""(?i)(?:GARDU|GATE)\s*[:\-]?\s*([A-Z0-9\s./\-]{3,40})"""),
        Regex("""(?i)^\s*(?:KELUAR|MASUK|EXIT|ENTRY)\s*[:\-]?\s*([A-Z0-9\s./\-]{3,40})""", RegexOption.MULTILINE),
        Regex("""(?i)RUAS\s*[:\-]?\s*([A-Z0-9\s./\-]{3,40})""")
    )

    private val tanggalPatterns = listOf(
        Regex("""(\d{1,2})[/\-](\d{1,2})[/\-](\d{2,4})"""),
        Regex("""(\d{4})[/\-](\d{1,2})[/\-](\d{1,2})""")
    )

    private val jamPattern = Regex("""(\d{1,2}):(\d{2})(?::(\d{2}))?""")

    private val tarifPatterns = listOf(
        Regex("""(?i)(?:TARIF|TOTAL|BAYAR|JUMLAH)\s*[:\-]?\s*(?:RP\.?)?\s*([\d.,]{3,12})"""),
        Regex("""(?i)RP\.?\s*([\d.,]{3,12})""")
    )

    fun parse(rawText: String): ParsedReceipt {
        val gerbang = firstMatch(gerbangPatterns, rawText)
            ?.trim()
            ?.trim('.', '-', ':')
            ?.uppercase(Locale.getDefault())
            ?.takeIf { it.length in 3..40 }
            ?: "TIDAK TERDETEKSI"

        var tanggal = ""
        for (pattern in tanggalPatterns) {
            val match = pattern.find(rawText) ?: continue
            val normalized = normalizeDate(match.groupValues[1], match.groupValues[2], match.groupValues[3])
            if (normalized.isNotEmpty()) {
                tanggal = normalized
                break
            }
        }

        val jamMatch = jamPattern.find(rawText)
        val jam = if (jamMatch != null) {
            val h = jamMatch.groupValues[1].padStart(2, '0')
            val m = jamMatch.groupValues[2].padStart(2, '0')
            "$h:$m"
        } else ""

        var tarif = 0L
        outer@ for (pattern in tarifPatterns) {
            for (match in pattern.findAll(rawText)) {
                val cleaned = match.groupValues[1].replace(".", "").replace(",", "").trim()
                val value = cleaned.toLongOrNull() ?: continue
                if (value in 500..2_000_000) {
                    tarif = value
                    break@outer
                }
            }
        }

        var confidence = 0
        if (gerbang != "TIDAK TERDETEKSI") confidence += 30
        if (tanggal.isNotEmpty()) confidence += 25
        if (jam.isNotEmpty()) confidence += 20
        if (tarif > 0) confidence += 25

        return ParsedReceipt(
            gerbangTol = gerbang,
            tanggal = tanggal.ifEmpty { today() },
            jam = jam.ifEmpty { "00:00" },
            tarif = tarif,
            confidence = confidence
        )
    }

    private fun firstMatch(patterns: List<Regex>, text: String): String? {
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1]
            }
        }
        return null
    }

    private fun normalizeDate(p1: String, p2: String, p3: String): String {
        return try {
            val n1 = p1.toInt()
            val n2 = p2.toInt()
            val n3 = p3.toInt()
            val (year, month, day) = if (p1.length == 4) {
                Triple(n1, n2, n3)
            } else {
                val yr = if (n3 < 100) 2000 + n3 else n3
                Triple(yr, n2, n1)
            }
            if (month in 1..12 && day in 1..31 && year in 2000..2100) {
                String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
            } else ""
        } catch (e: NumberFormatException) {
            ""
        }
    }

    private fun today(): String {
        val cal = Calendar.getInstance()
        return String.format(
            Locale.US, "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }
}
