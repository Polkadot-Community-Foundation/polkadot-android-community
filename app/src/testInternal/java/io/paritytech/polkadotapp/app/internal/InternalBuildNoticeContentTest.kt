package io.paritytech.polkadotapp.app.internal

import org.junit.Assert.assertEquals
import org.junit.Test

class InternalBuildNoticeContentTest {
    private val active = SentryStatus.Active("io.pcf.polkadotapp.internal@1.0.0-internal+28")

    @Test
    fun `gp debuggable build with the debug menu names everything`() {
        assertEquals(
            listOf(
                NoticeParagraph.SCOPE,
                NoticeParagraph.SENTRY,
                NoticeParagraph.DEBUGGABLE,
                NoticeParagraph.DEBUG_MENU,
                NoticeParagraph.GOOGLE_BACKUP,
                NoticeParagraph.KEYS,
            ),
            noticeParagraphs(isDebuggable = true, hasDebugMenu = true, hasGoogleBackup = true, sentryStatus = active)
        )
    }

    @Test
    fun `non-debuggable build does not claim to be debuggable`() {
        assertEquals(
            listOf(
                NoticeParagraph.SCOPE,
                NoticeParagraph.SENTRY,
                NoticeParagraph.DEBUG_MENU,
                NoticeParagraph.GOOGLE_BACKUP,
                NoticeParagraph.KEYS,
            ),
            noticeParagraphs(isDebuggable = false, hasDebugMenu = true, hasGoogleBackup = true, sentryStatus = active)
        )
    }

    @Test
    fun `without the debug menu its paragraph is left out`() {
        assertEquals(
            listOf(
                NoticeParagraph.SCOPE,
                NoticeParagraph.SENTRY,
                NoticeParagraph.DEBUGGABLE,
                NoticeParagraph.GOOGLE_BACKUP,
                NoticeParagraph.KEYS,
            ),
            noticeParagraphs(isDebuggable = true, hasDebugMenu = false, hasGoogleBackup = true, sentryStatus = active)
        )
    }

    @Test
    fun `without Google backup its paragraph is left out`() {
        assertEquals(
            listOf(
                NoticeParagraph.SCOPE,
                NoticeParagraph.SENTRY,
                NoticeParagraph.DEBUGGABLE,
                NoticeParagraph.DEBUG_MENU,
                NoticeParagraph.KEYS,
            ),
            noticeParagraphs(isDebuggable = true, hasDebugMenu = true, hasGoogleBackup = false, sentryStatus = active)
        )
    }

    @Test
    fun `without Sentry the reporting paragraph is left out`() {
        assertEquals(
            listOf(
                NoticeParagraph.SCOPE,
                NoticeParagraph.DEBUGGABLE,
                NoticeParagraph.DEBUG_MENU,
                NoticeParagraph.GOOGLE_BACKUP,
                NoticeParagraph.KEYS,
            ),
            noticeParagraphs(
                isDebuggable = true,
                hasDebugMenu = true,
                hasGoogleBackup = true,
                sentryStatus = SentryStatus.NotConfigured,
            )
        )
    }

    @Test
    fun `release name is applicationId at versionName plus versionCode`() {
        assertEquals(
            "io.pcf.polkadotapp.internal@1.0.0-internal+28",
            sentryReleaseName("io.pcf.polkadotapp.internal", "1.0.0-internal", 28L)
        )
    }
}
