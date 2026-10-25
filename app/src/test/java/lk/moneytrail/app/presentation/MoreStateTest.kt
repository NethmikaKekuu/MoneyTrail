package lk.moneytrail.app.presentation

import lk.moneytrail.app.presentation.more.MoreState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoreStateTest {

    @Test
    fun moreStateReady_holdsProfileAndGoalBadge() {
        val state = MoreState.Ready(
            name = "Kavindu Silva",
            email = "kavindu.silva@gmail.com",
            salaryBadge = "Due 25 Oct"
        )

        assertTrue(state is MoreState.Ready)
        assertEquals("Kavindu Silva", state.name)
        assertEquals("kavindu.silva@gmail.com", state.email)
        assertEquals("Due 25 Oct", state.salaryBadge)
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
