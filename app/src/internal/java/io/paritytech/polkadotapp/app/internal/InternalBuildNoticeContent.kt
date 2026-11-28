package io.paritytech.polkadotapp.app.internal

enum class NoticeParagraph {
    SCOPE,
    SENTRY,
    DEBUGGABLE,
    DEBUG_MENU,
    GOOGLE_BACKUP,
    KEYS
}

sealed interface SentryStatus {
    data class Active(val release: String) : SentryStatus

    data object NotConfigured : SentryStatus
}

/** Matches the Sentry Android SDK's default release name, which this build does not override. */
fun sentryReleaseName(applicationId: String, versionName: String, versionCode: Long): String =
    "$applicationId@$versionName+$versionCode"

fun noticeParagraphs(
    isDebuggable: Boolean,
    hasDebugMenu: Boolean,
    hasGoogleBackup: Boolean,
    sentryStatus: SentryStatus,
): List<NoticeParagraph> = buildList {
    add(NoticeParagraph.SCOPE)
    if (sentryStatus is SentryStatus.Active) add(NoticeParagraph.SENTRY)
    if (isDebuggable) add(NoticeParagraph.DEBUGGABLE)
    if (hasDebugMenu) add(NoticeParagraph.DEBUG_MENU)
    if (hasGoogleBackup) add(NoticeParagraph.GOOGLE_BACKUP)
    add(NoticeParagraph.KEYS)
}
