package app.replylater.android.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RussianDateFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("ru-RU"))

fun formatHomeDate(date: LocalDate): String = date.format(RussianDateFormatter)

