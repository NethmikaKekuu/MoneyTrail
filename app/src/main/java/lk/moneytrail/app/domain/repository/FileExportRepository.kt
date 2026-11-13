package lk.moneytrail.app.domain.repository

/** Writes files the user picked a place for, such as an exported statement. */
interface FileExportRepository {

    /** Writes [text] to the file at [uri], replacing what it held. */
    suspend fun writeText(uri: String, text: String): Result<Unit>
}
