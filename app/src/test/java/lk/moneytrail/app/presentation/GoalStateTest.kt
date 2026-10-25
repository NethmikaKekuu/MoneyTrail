package lk.moneytrail.app.presentation

import lk.moneytrail.app.domain.model.GoalModel
import lk.moneytrail.app.presentation.goal.GoalUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GoalStateTest {

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

        val state: GoalUiState = GoalUiState.Success(
            goals = listOf(model1, model2),
            totalAllocationPool = 39_900L,
            isNewGoalDialogOpen = false
        )

        assertTrue(state is GoalUiState.Success)
        val success = state as GoalUiState.Success
        assertEquals(2, success.goals.size)
        assertEquals(690_000L, success.totalTargetAmount)
        assertEquals(61_200L, success.totalSavedAmount)
        assertEquals("MacBook Pro M4", success.primaryGoal.name)
        assertFalse(success.isNewGoalDialogOpen)
    }

    @Test
    fun goalUiState_asksToMoveSavingsUntilDoneThisMonth() {
        val goal = GoalModel("MacBook Pro M4", 490_000L, 11_200L, LocalDate.of(2027, 10, 19), id = "mac")
        val toDo = GoalUiState.Success(goals = listOf(goal), totalAllocationPool = 39_900L)

        assertTrue(toDo.isSavingsToDo)
        assertFalse(toDo.copy(isSavingsMovedThisMonth = true).isSavingsToDo)
        assertFalse(toDo.areAllGoalsReached)
    }

    @Test
    fun goalUiState_seesWhenEveryGoalIsReached() {
        val reached = GoalModel("Headphones", 30_000L, 30_000L, LocalDate.of(2026, 12, 1), id = "phones")
        val state = GoalUiState.Success(goals = listOf(reached))

        assertTrue(state.areAllGoalsReached)
        assertFalse(state.isSavingsToDo)
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
