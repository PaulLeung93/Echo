package dev.echoapp.echo.data.repository

import dev.echoapp.echo.data.withWriteTimeout
import dev.echoapp.echo.di.IoDispatcher
import dev.echoapp.echo.domain.model.Report
import dev.echoapp.echo.domain.repository.ReportRepository
import dev.echoapp.echo.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ReportRepository {

    private val reportsCollection = firestore.collection(Constants.COLLECTION_REPORTS)

    override suspend fun submitReport(report: Report): Result<Unit> = withContext(ioDispatcher) {
        val user = auth.currentUser
            ?: return@withContext Result.failure(IllegalStateException("You must be signed in to report."))
        if (user.isAnonymous) {
            return@withContext Result.failure(IllegalStateException("Sign in to report content."))
        }
        try {
            val data = mapOf(
                "reporterId" to user.uid,
                "type" to report.type.name.lowercase(),
                "targetId" to report.targetId,
                "targetAuthorId" to report.targetAuthorId,
                "contextId" to (report.contextId ?: ""),
                "reason" to report.reason.name.lowercase(),
                "timestamp" to FieldValue.serverTimestamp()
            )
            withWriteTimeout { reportsCollection.add(data).await() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
