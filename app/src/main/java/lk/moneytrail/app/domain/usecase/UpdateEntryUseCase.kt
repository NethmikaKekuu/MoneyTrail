package lk.moneytrail.app.domain.usecase

import lk.moneytrail.app.domain.model.AppException
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.repository.EntryRepository
import javax.inject.Inject

/**
 * Checks an edited entry with the same rules as a new one and saves it. Unlike a new
 * entry, an edited expense keeps the committed or discretionary choice the user made,
 * so a category's default can be overridden. A salary may also carry a bonus, which
 * has to leave some base pay.
 */
class UpdateEntryUseCase @Inject constructor(
    private val entryRepository: EntryRepository
) {
    suspend operator fun invoke(entry: Entry): Result<Unit> {
        val trimmed = entry.copy(note = entry.note.trim())
        val error = AddEntryUseCase.entryError(trimmed.type, trimmed.amount, trimmed.category, trimmed.account, trimmed.note)
            ?: spendingTypeError(trimmed)
            ?: bonusError(trimmed)
        if (error != null) return Result.failure(AppException(error))

        return entryRepository.updateEntry(trimmed)
    }

    private fun spendingTypeError(entry: Entry): String? = when {
        entry.type == EntryType.EXPENSE && entry.spendingType == null -> "Pick committed or discretionary."
        entry.type == EntryType.INCOME && entry.spendingType != null -> "Income can't be committed or discretionary."
        else -> null
    }

    private fun bonusError(entry: Entry): String? = when {
        entry.bonus == 0L -> null
        entry.bonus < 0 -> "That bonus isn't valid."
        !entry.isSalary -> "Only a salary can have a bonus."
        entry.bonus >= entry.amount -> "Enter the base salary as well as the bonus."
        else -> null
    }
}
