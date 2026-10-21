package lk.moneytrail.app.domain.usecase

import lk.moneytrail.app.domain.model.AppException
import lk.moneytrail.app.domain.model.Categories
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryStatus
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.domain.repository.EntryRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Checks a new entry and saves it. Expenses get committed or discretionary from their
 * category, so the user doesn't have to choose it while logging; for a category the
 * user made, the caller passes its setting as [spendingType]. [isExpected] saves money
 * planned for its date, which only counts once the user confirms it.
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
        note: String,
        isExpected: Boolean = false,
        spendingType: SpendingType? = Categories.expense[category],
        fromAccount: String = "",
        toAccount: String = ""
    ): Result<Unit> {
        val trimmedNote = note.trim()
        val error = entryError(type, amount, category, account, trimmedNote)
            ?: (if (type == EntryType.EXPENSE && spendingType == null) "Pick committed or discretionary." else null)
            ?: (if (type == EntryType.TRANSFER && fromAccount.isNotBlank() && fromAccount == toAccount) "Pick two different accounts." else null)
        if (error != null) return Result.failure(AppException(error))

        val entry = Entry(
            id = "",
            type = type,
            amount = amount,
            category = if (type == EntryType.TRANSFER) "Transfer" else category,
            account = if (type == EntryType.TRANSFER && fromAccount.isNotBlank()) fromAccount else account,
            date = date,
            note = trimmedNote,
            spendingType = if (type == EntryType.EXPENSE) spendingType else null,
            status = if (isExpected) EntryStatus.EXPECTED else EntryStatus.CONFIRMED,
            fromAccountId = if (type == EntryType.TRANSFER) fromAccount.ifEmpty { account } else "",
            toAccountId = if (type == EntryType.TRANSFER) toAccount else ""
        )
        return entryRepository.addEntry(entry)
    }

    companion object {
        /** LKR 999,999,999: nine keypad digits. */
        const val MAX_AMOUNT = 999_999_999L
        const val MAX_NOTE_LENGTH = 200
        const val MAX_CATEGORY_LENGTH = 50

        /** What's wrong with an entry's fields, or null when it can be saved. Editing uses the same checks. */
        fun entryError(type: EntryType, amount: Long, category: String, account: String, note: String): String? = when {
            amount <= 0 -> "Enter an amount."
            amount > MAX_AMOUNT -> "That amount is too large."
            category.isBlank() || category.length > MAX_CATEGORY_LENGTH -> "Pick a category."
            account.isBlank() -> "Pick an account."
            note.length > MAX_NOTE_LENGTH -> "Keep the note under $MAX_NOTE_LENGTH characters."
            else -> null
        }
    }
}
