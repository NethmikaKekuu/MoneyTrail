package lk.moneytrail.app.domain.repository

import kotlinx.coroutines.flow.Flow
import lk.moneytrail.app.domain.model.Project

/**
 * Works on the signed-in user's freelance projects.
 * Failures are returned as an AppException whose message can be shown to the user.
 */
interface FreelanceRepository {

    /**
     * Observes all projects for the signed-in user. Emits again whenever a project
     * or milestone is added, updated, or removed.
     */
    fun observeProjects(): Flow<Result<List<Project>>>

    /** Gets a single project by id, or null if it doesn't exist. */
    suspend fun getProject(id: String): Result<Project?>

    /** Saves a project (creates new document if id is empty, otherwise updates existing). */
    suspend fun saveProject(project: Project): Result<Unit>

    /** Deletes a project by id. */
    suspend fun deleteProject(id: String): Result<Unit>
}
