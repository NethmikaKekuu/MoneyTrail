package lk.moneytrail.app.presentation

import lk.moneytrail.app.data.preferences.ThemeMode
import lk.moneytrail.app.presentation.more.MoreState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoreViewModelTest {

    @Test
    fun moreStateReady_holdsProfileAndThemeMode() {
        val state = MoreState.Ready(
            name = "Kavindu Silva",
            email = "kavindu.silva@gmail.com",
            goalProgressBadge = "2.3%",
            themeMode = ThemeMode.DARK,
            isThemeDialogOpen = false
        )

        assertTrue(state is MoreState.Ready)
        assertEquals("Kavindu Silva", state.name)
        assertEquals("kavindu.silva@gmail.com", state.email)
        assertEquals("2.3%", state.goalProgressBadge)
        assertEquals(ThemeMode.DARK, state.themeMode)
    }

    @Test
    fun moreStateLoadingAndError_workAsExpected() {
        val loading = MoreState.Loading
        val error = MoreState.Error("Failed to load")

        assertTrue(loading is MoreState.Loading)
        assertTrue(error is MoreState.Error)
        assertEquals("Failed to load", error.message)
    }
}
