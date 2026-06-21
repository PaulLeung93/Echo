package dev.echoapp.echo.domain.repository

import dev.echoapp.echo.domain.model.Report

/** Repository for user-submitted content reports (`reports/{id}`). */
interface ReportRepository {
    /** Submit a report for the current (non-anonymous) user. */
    suspend fun submitReport(report: Report): Result<Unit>
}
