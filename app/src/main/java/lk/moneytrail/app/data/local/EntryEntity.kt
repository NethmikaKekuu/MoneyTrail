package lk.moneytrail.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import lk.moneytrail.app.domain.model.Entry
import lk.moneytrail.app.domain.model.EntryStatus
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.SpendingType
import java.time.LocalDate

/**
 * Room's copy of one users/{uid}/entries/{entryId} document. The fields match Firestore,
 * plus the owner's [uid], so one user never sees another's rows on a shared phone.
 */
@Entity(tableName = "entries", indices = [Index("uid", "date")])
data class EntryEntity(
    /** The Firestore document id. */
    @PrimaryKey val id: String,
    val uid: String,
    val status: String,
    val type: String,
    val amount: Long,
    val category: String,
    val account: String,
    /** yyyy-MM-dd, as in Firestore, so a month is a BETWEEN on text. */
    val date: String,
    val note: String,
    val spendingType: String?,
    val bonus: Long,
    val usdCents: Long,
    val effectiveRate: Long,
    val feeLkr: Long,
    /** Milliseconds; orders entries within a day. */
    val createdAt: Long
)

fun Entry.toEntity(uid: String, createdAt: Long): EntryEntity = EntryEntity(
    id = id,
    uid = uid,
    status = status.name,
    type = type.name,
    amount = amount,
    category = category,
    account = account,
    date = date.toString(),
    note = note,
    spendingType = spendingType?.name,
    bonus = bonus,
    usdCents = usdCents,
    effectiveRate = effectiveRate,
    feeLkr = feeLkr,
    createdAt = createdAt
)

/** Null for a row with an unknown type or an unreadable date, which is skipped. */
fun EntryEntity.toEntry(): Entry? {
    val entryType = EntryType.entries.find { it.name == type } ?: return null
    val day = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
    return Entry(
        id = id,
        type = entryType,
        amount = amount,
        category = category,
        account = account,
        date = day,
        note = note,
        spendingType = SpendingType.entries.find { it.name == spendingType },
        status = EntryStatus.entries.find { it.name == status } ?: EntryStatus.CONFIRMED,
        bonus = bonus,
        usdCents = usdCents,
        effectiveRate = effectiveRate,
        feeLkr = feeLkr
    )
}
