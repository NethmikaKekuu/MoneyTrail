package lk.moneytrail.app.domain.usecase

import lk.moneytrail.app.domain.model.AppException
import lk.moneytrail.app.domain.model.Categories
import lk.moneytrail.app.domain.model.CustomCategory
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.domain.repository.CategoryRepository
import javax.inject.Inject

/**
 * Checks and saves a category the user made. Used from Settings and the new entry
 * screen; onboarding uses [categoryError] for categories it saves at the end.
 */
class AddCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    /** [existing] is the user's categories so far, to stop the same name being added twice. */
    suspend operator fun invoke(
        name: String,
        type: EntryType,
        spendingType: SpendingType?,
        existing: List<CustomCategory>
    ): Result<Unit> {
        val error = categoryError(name, type, spendingType, existing)
        if (error != null) return Result.failure(AppException(error))

        return categoryRepository.addCategory(
            CustomCategory(
                id = "",
                name = name.trim(),
                type = type,
                spendingType = if (type == EntryType.EXPENSE) spendingType else null
            )
        )
    }

    companion object {
        const val MAX_NAME_LENGTH = 30

        fun categoryError(name: String, type: EntryType, spendingType: SpendingType?, existing: List<CustomCategory>): String? {
            val trimmed = name.trim()
            return when {
                trimmed.isEmpty() -> "Give the category a name."
                trimmed.length > MAX_NAME_LENGTH -> "Keep the name under $MAX_NAME_LENGTH characters."
                Categories.namesFor(type, existing).any { it.equals(trimmed, ignoreCase = true) } ->
                    "There's already a category called $trimmed."
                type == EntryType.EXPENSE && spendingType == null -> "Pick committed or discretionary."
                else -> null
            }
        }
    }
}
