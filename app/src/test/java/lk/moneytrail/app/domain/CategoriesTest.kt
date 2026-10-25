package lk.moneytrail.app.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import lk.moneytrail.app.domain.model.AccountKind
import lk.moneytrail.app.domain.model.AccountTemplates
import lk.moneytrail.app.domain.model.Categories
import lk.moneytrail.app.domain.model.CustomCategory
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.domain.repository.CategoryRepository
import lk.moneytrail.app.domain.usecase.AddCategoryUseCase
import lk.moneytrail.app.domain.usecase.CompleteOnboardingUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class CategoriesTest {

    /** Keeps what would have been saved, instead of talking to Firestore. */
    private class FakeCategoryRepository : CategoryRepository {
        val added = mutableListOf<CustomCategory>()

        override fun observeCategories(): Flow<Result<List<CustomCategory>>> = emptyFlow()

        override suspend fun addCategory(category: CustomCategory): Result<Unit> {
            added += category
            return Result.success(Unit)
        }

        override suspend fun deleteCategory(id: String): Result<Unit> = Result.success(Unit)
    }

    private val petCare = CustomCategory("c1", "Pet care", EntryType.EXPENSE, SpendingType.COMMITTED)
    private val tuition = CustomCategory("c2", "Tuition classes", EntryType.INCOME, null)

    @Test
    fun namesFor_putsTheUsersOwnAfterTheBuiltInOnesOfThatType() {
        val expense = Categories.namesFor(EntryType.EXPENSE, listOf(petCare, tuition))
        val income = Categories.namesFor(EntryType.INCOME, listOf(petCare, tuition))

        assertEquals("Pet care", expense.last())
        assertFalse("Tuition classes" in expense)
        assertEquals("Salary", income.first())
        assertEquals("Tuition classes", income.last())
    }

    @Test
    fun defaultSpending_comesFromTheBuiltInOrTheUsersCategory() {
        assertEquals(SpendingType.DISCRETIONARY, Categories.defaultSpending("Coffee", listOf(petCare)))
        assertEquals(SpendingType.COMMITTED, Categories.defaultSpending("Pet care", listOf(petCare)))
        assertNull(Categories.defaultSpending("Tuition classes", listOf(tuition)))
    }

    @Test
    fun categoryChecks() {
        val existing = listOf(petCare)
        fun error(name: String, type: EntryType = EntryType.EXPENSE, spending: SpendingType? = SpendingType.COMMITTED) =
            AddCategoryUseCase.categoryError(name, type, spending, existing)

        assertEquals("Give the category a name.", error("  "))
        assertEquals("Keep the name under 30 characters.", error("A".repeat(31)))
        assertEquals("There's already a category called coffee.", error("coffee"))
        assertEquals("There's already a category called PET CARE.", error("PET CARE"))
        assertEquals("Pick committed or discretionary.", error("Gym", spending = null))
        assertNull(error("Gym"))
        assertNull(error("Tuition classes", EntryType.INCOME, null))
    }

    @Test
    fun addCategory_savesTheNameTrimmedAndIncomeWithoutASpendingType() = runBlocking {
        val repository = FakeCategoryRepository()

        AddCategoryUseCase(repository)("  Tuition classes ", EntryType.INCOME, SpendingType.COMMITTED, emptyList())

        assertEquals(CustomCategory("", "Tuition classes", EntryType.INCOME, null), repository.added.single())
    }

    @Test
    fun customPlace_getsAUniqueSlugId() {
        val sampath = AccountTemplates.custom(" Sampath Bank ", AccountKind.BANK, emptySet())

        assertEquals("sampath-bank", sampath.id)
        assertEquals("Sampath Bank", sampath.name)
        assertEquals("sampath-bank-2", AccountTemplates.custom("Sampath bank", AccountKind.BANK, setOf("sampath-bank")).id)
        assertEquals("place", AccountTemplates.custom("මුදල්", AccountKind.CASH, emptySet()).id)
    }

    @Test
    fun placeChecks() {
        assertEquals("Give the place a name.", CompleteOnboardingUseCase.placeError(" ", AccountTemplates.all))
        assertEquals(
            "There's already a place called main bank.",
            CompleteOnboardingUseCase.placeError("main bank", AccountTemplates.all)
        )
        assertNull(CompleteOnboardingUseCase.placeError("Sampath Bank", AccountTemplates.all))
    }
}
