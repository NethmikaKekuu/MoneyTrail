package lk.moneytrail.app.domain.model

/**
 * A category the user added, e.g. "Pet care" or "Tuition classes": the
 * users/{uid}/categories/{id} document in Firestore.
 */
data class CustomCategory(
    /** Firestore document id. Empty before it's saved. */
    val id: String,
    val name: String,
    val type: EntryType,
    /** For expense categories: whether spending on it is usually committed or discretionary. Null for income. */
    val spendingType: SpendingType?
)
