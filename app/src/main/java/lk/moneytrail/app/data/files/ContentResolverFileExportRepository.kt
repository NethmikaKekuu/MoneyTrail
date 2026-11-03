package lk.moneytrail.app.data.files

import android.content.Context
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lk.moneytrail.app.domain.model.AppException
import lk.moneytrail.app.domain.repository.FileExportRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes to a file the user chose with the system "Save to" picker, e.g. in Downloads or
 * Drive. Android hands back a content:// address, so no file-sharing setup is needed.
 */
@Singleton
class ContentResolverFileExportRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : FileExportRepository {

    override suspend fun writeText(uri: String, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val stream = context.contentResolver.openOutputStream(Uri.parse(uri), "wt")
                ?: return@withContext Result.failure(AppException(EXPORT_FAILED_MESSAGE))
            stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Writing the export failed", e)
            Result.failure(AppException(EXPORT_FAILED_MESSAGE, e))
        }
    }

    private companion object {
        const val TAG = "FileExport"
        const val EXPORT_FAILED_MESSAGE = "Couldn't save the file. Please try again."
    }
}
