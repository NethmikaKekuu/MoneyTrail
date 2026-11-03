package lk.moneytrail.app.data

import lk.moneytrail.app.data.reminders.WorkManagerReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

class ReminderScheduleTest {

    @Test
    fun beforeNine_theCheckIsThisMorning() {
        val now = LocalDateTime.of(2026, 10, 25, 7, 30)

        assertEquals(Duration.ofMinutes(90), WorkManagerReminderScheduler.untilNextCheck(now))
    }

    @Test
    fun afterNine_theCheckIsTomorrowMorning() {
        val now = LocalDateTime.of(2026, 10, 25, 21, 0)

        assertEquals(Duration.ofHours(12), WorkManagerReminderScheduler.untilNextCheck(now))
    }
}
