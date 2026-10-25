package lk.moneytrail.app.presentation

import lk.moneytrail.app.domain.model.GoalModel
import lk.moneytrail.app.presentation.goal.GoalUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GoalViewModelTest {

    @Test
    fun goalUiState_handlesMultiGoalSuccessState() {
        val model1 = GoalModel(
            name = "MacBook Pro M4",
            targetAmount = 490_000L,
            savedAmount = 11_200L,
            targetDate = LocalDate.of(2027, 10, 19)
        )
        val model2 = GoalModel(
            name = "Emergency Fund",
            targetAmount = 200_000L,
            savedAmount = 50_000L,
            targetDate = LocalDate.of(2027, 6, 1)
        )

        val state = GoalUiState.Success(
            goals = listOf(model1, model2),
            totalAllocationPool = 39_900L,
            isNewGoalDialogOpen = false
        )

        assertEquals(2, state.goals.size)
        assertEquals(690_000L, state.totalTargetAmount)
        assertEquals(61_200L, state.totalSavedAmount)
        assertEquals("MacBook Pro M4", state.primaryGoal.name)
        assertFalse(state.isNewGoalDialogOpen)
        assertFalse(state.isAllocationDialogOpen)
    }

    @Test
    fun goalUiState_overallProgressCalculation() {
        val model1 = GoalModel(
            name = "MacBook Pro M4",
            targetAmount = 400_000L,
            savedAmount = 100_000L,
            targetDate = LocalDate.of(2027, 10, 19)
        )
        val model2 = GoalModel(
            name = "Emergency Fund",
            targetAmount = 100_000L,
            savedAmount = 50_000L,
            targetDate = LocalDate.of(2027, 6, 1)
        )

        val state = GoalUiState.Success(
            goals = listOf(model1, model2)
        )

        assertEquals(500_000L, state.totalTargetAmount)
        assertEquals(150_000L, state.totalSavedAmount)
        assertEquals(0.30f, state.overallProgressFraction, 0.01f)
        assertEquals("30.0%", state.overallPercentageText)
    }

    @Test
    fun goalUiState_dialogStateToggles() {
        val model = GoalModel(
            name = "MacBook Pro M4",
            targetAmount = 490_000L,
            savedAmount = 11_200L,
            targetDate = LocalDate.of(2027, 10, 19)
        )

        val initialState = GoalUiState.Success(
            goals = listOf(model)
        )

        val newGoalDialogState = initialState.copy(isNewGoalDialogOpen = true)
        assertTrue(newGoalDialogState.isNewGoalDialogOpen)

        val allocationDialogState = initialState.copy(isAllocationDialogOpen = true)
        assertTrue(allocationDialogState.isAllocationDialogOpen)

        val bannerState = initialState.copy(bannerMessage = "Goal updated successfully!")
        assertEquals("Goal updated successfully!", bannerState.bannerMessage)
    }

    @Test
    fun goalUiState_emptyGoalsPrimaryFallback() {
        val state = GoalUiState.Success(
            goals = emptyList()
        )

        assertEquals("Goal", state.primaryGoal.name)
        assertEquals(0L, state.totalTargetAmount)
        assertEquals(0L, state.totalSavedAmount)
        assertEquals(0.0f, state.overallProgressFraction, 0.001f)
        assertEquals("0.0%", state.overallPercentageText)
    }

    @Test
    fun goalUiState_handlesLoadingAndErrorStates() {
        val loadingState: GoalUiState = GoalUiState.Loading
        val emptyState: GoalUiState = GoalUiState.Empty
        val errorState: GoalUiState = GoalUiState.Error("Network error")

        assertTrue(loadingState is GoalUiState.Loading)
        assertTrue(emptyState is GoalUiState.Empty)
        assertTrue(errorState is GoalUiState.Error)
        assertEquals("Network error", (errorState as GoalUiState.Error).message)
    }
}
