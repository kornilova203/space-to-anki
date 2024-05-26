package kornilova

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import java.awt.Color
import java.util.*

val calendar: Calendar = Calendar.getInstance()
val today = LocalDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DATE))

enum class StartTimeBucket(val daysCount: Int, val tag: String, val color: Color?) {
    BUCKET_0(0, "less_than_1_year", null),
    BUCKET_1(365, "more_than_1_year", Color(56, 186, 78)),
    BUCKET_2(365 * 3, "more_than_3_years", Color(250, 231, 28)),
    BUCKET_3(365 * 5, "more_than_5_years", Color(255, 147, 39)),
    BUCKET_4(365 * 10, "more_than_10_years", Color(255, 0, 0));
}

fun chooseStartTimeBucket(startDate: LocalDate): StartTimeBucket {
    val daysCount = startDate.daysUntil(today)
    return StartTimeBucket.entries.last { it.daysCount <= daysCount }
}
