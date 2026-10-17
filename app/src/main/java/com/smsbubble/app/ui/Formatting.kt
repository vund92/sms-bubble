package com.smsbubble.app.ui

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.time.format.TextStyle
import java.util.Locale

val VI: Locale = Locale("vi", "VN")
private val HM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DMY: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val DMY_SHORT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yy")

/** Định dạng hiển thị trong ô nhập tay. */
val INPUT_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

fun Long.toLdt(): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())

fun LocalDateTime.toMillis(): Long =
    atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun weekday(d: LocalDate): String =
    d.dayOfWeek.getDisplayName(TextStyle.FULL, VI).replaceFirstChar { it.uppercase(VI) }

/** "Thứ 2" … "Thứ 7", "Chủ nhật". */
private fun weekdayShort(d: LocalDate): String =
    if (d.dayOfWeek.value == 7) "Chủ nhật" else "Thứ ${d.dayOfWeek.value + 1}"

/** Nhãn thời gian ở giữa khung chat, ví dụ "15:23 Thứ 2, 13 thg 7". */
fun headerLabel(ts: Long): String {
    val t = ts.toLdt()
    val d = t.toLocalDate()
    return "${t.format(HM)} ${weekdayShort(d)}, ${d.dayOfMonth} thg ${d.monthValue}"
}

/** Nhãn ngắn ở danh sách cuộc trò chuyện. */
fun listLabel(ts: Long): String {
    val t = ts.toLdt()
    val d = t.toLocalDate()
    val today = LocalDate.now()
    return when {
        d == today -> t.format(HM)
        d == today.minusDays(1) -> "Hôm qua"
        d.isAfter(today.minusDays(7)) && d.isBefore(today) -> weekday(d)
        else -> d.format(DMY_SHORT)
    }
}

/** Ví dụ: "Thứ Bảy, 17/10/2026 lúc 16:24". */
fun fullLabel(ts: Long): String {
    val t = ts.toLdt()
    return "${weekday(t.toLocalDate())}, ${t.format(DMY)} lúc ${t.format(HM)}"
}

fun shortTime(ts: Long): String = ts.toLdt().format(HM)

/**
 * Đọc ngày giờ người dùng gõ tay. Chấp nhận:
 *  17/10/2026 16:24 · 17-10-2026 16:24 · 17.10.2026 16:24 · 16:24 17/10/2026
 *  hoặc chỉ ngày (17/10/2026) — khi đó giữ nguyên giờ [fallbackTime].
 */
private fun strict(p: String): DateTimeFormatter =
    DateTimeFormatter.ofPattern(p).withResolverStyle(ResolverStyle.STRICT)

fun parseUserDateTime(input: String, fallbackTime: LocalTime): LocalDateTime? {
    val s = input.trim().replace(Regex("\\s+"), " ")
    if (s.isEmpty()) return null
    val dateTimePatterns = listOf("d/M/uuuu H:mm", "d-M-uuuu H:mm", "d.M.uuuu H:mm", "H:mm d/M/uuuu")
    for (p in dateTimePatterns) {
        try {
            return LocalDateTime.parse(s, strict(p))
        } catch (e: DateTimeParseException) {
        }
    }
    for (p in listOf("d/M/uuuu", "d-M-uuuu", "d.M.uuuu")) {
        try {
            return LocalDate.parse(s, strict(p)).atTime(fallbackTime)
        } catch (e: DateTimeParseException) {
        }
    }
    return null
}

/** Chữ cái đầu cho avatar: "Nguyễn Văn An" -> "NA". Trả về "" nếu tên không có chữ (vd: chỉ là số). */
fun initialsOf(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() && it[0].isLetter() }
    return when (words.size) {
        0 -> ""
        1 -> words[0].take(1).uppercase(VI)
        else -> (words.first().take(1) + words.last().take(1)).uppercase(VI)
    }
}

/** Tên gọi ngắn cho nút chọn người gửi: "Nguyễn Văn An" -> "An". */
fun shortNameOf(name: String): String {
    val last = name.trim().split(Regex("\\s+")).lastOrNull { it.any(Char::isLetter) }
    return last?.take(14) ?: "Họ"
}
