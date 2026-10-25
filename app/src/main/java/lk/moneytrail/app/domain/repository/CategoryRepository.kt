package lk.moneytrail.app.domain.repository

import kotlinx.coroutines.flow.Flow
import lk.moneytrail.app.domain.model.CustomCategory

// Works on the signed-in user's own categories. Failures are returned as an
// lk.moneytrail.app.domain.model.AppException whose message can be shown to the user.
interface CategoryRepository {

    /** The categories the user added, sorted by name. Updates live. */
    fun observeCategories(): Flow<Result<List<CustomCategory>>>

    /** Saves a new category. The id of [category] is ignored; Firestore picks one. */
    suspend fun addCategory(category: CustomCategory): Result<Unit>

    /** Removes a category. Entries already filed under it keep its name. */
    suspend fun deleteCategory(id: String): Result<Unit>
}
