package dev.echoapp.echo.domain.model

/** What kind of content a report targets. */
enum class ReportType { POST, COMMENT }

/** Canned reasons a user can pick when reporting content. */
enum class ReportReason(val label: String) {
    SPAM("Spam or misleading"),
    HARASSMENT("Harassment or hate"),
    INAPPROPRIATE("Inappropriate content"),
    OTHER("Something else")
}

/**
 * A user-submitted report of objectionable content. Write-only from the client
 * (see Firestore rules): users create reports, moderation happens out-of-band in
 * the Firebase console.
 */
data class Report(
    val type: ReportType,
    /** The reported document id (post id or comment id). */
    val targetId: String,
    /** Uid of the author of the reported content. */
    val targetAuthorId: String,
    /** Parent context for a comment (post id or POI id); null for a post report. */
    val contextId: String? = null,
    val reason: ReportReason
)
