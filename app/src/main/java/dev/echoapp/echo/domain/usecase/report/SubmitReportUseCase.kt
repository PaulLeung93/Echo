package dev.echoapp.echo.domain.usecase.report

import dev.echoapp.echo.domain.model.Report
import dev.echoapp.echo.domain.repository.ReportRepository
import javax.inject.Inject

/** Submit a user report for objectionable content (a post or comment). */
class SubmitReportUseCase @Inject constructor(
    private val reportRepository: ReportRepository
) {
    suspend operator fun invoke(report: Report): Result<Unit> =
        reportRepository.submitReport(report)
}
