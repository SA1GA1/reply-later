package app.replylater.android.ui

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeDateFormatterTest {
    @Test
    fun formatsDateForRussianInboxHeader() {
        val result = formatHomeDate(LocalDate.of(2026, 9, 12))

        assertEquals("суббота, 12 сентября", result)
    }
}

