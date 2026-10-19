package lk.moneytrail.app.domain.usecase

import lk.moneytrail.app.domain.model.AppException
import lk.moneytrail.app.domain.model.Categories
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.repository.EntryRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Checks a new entry and saves it. Expenses get committed or discretionary from their
 * category, so the user doesn't have to choose it while logging.
 */
class AddEntryUseCase @Inject constructor(
    private val entryRepository: EntryRepository
) {
    suspend operator fun invoke(
        type: EntryType,
        amount: Long,
        category: String,
        account: String,
        date: LocalDate,
        note: String
    ): Result<Unit> {
        val trimmedNote = note.trim()
        val error = when {
            amount <= 0 -> "Enter an amount."
            amount > MAX_AMOUNT -> "That amount is too large."
            category !in Categories.namesFor(type) -> "Pick a category."
            account.isBlank() -> "Pick an account."
            trimmedNote.length > MAX_NOTE_LENGTH -> "Keep the note under $MAX_NOTE_LENGTH characters."
            else -> null
        }
        if (error != null) return Result.failure(AppException(error))

        val entry = Entry(
            id = "",
            type = type,
            amount = amount,
            category = category,
            account = account,
            date = date,
            note = trimmedNote,
            spendingType = if (type == EntryType.EXPENSE) Categories.expense[category] else null
        )
        return entryRepository.addEntry(entry)
    }

    companion object {
        /** LKR 999,999,999: nine keypad digits. */
        const val MAX_AMOUNT = 999_999_999L
        const val MAX_NOTE_LENGTH = 200
    }
}
