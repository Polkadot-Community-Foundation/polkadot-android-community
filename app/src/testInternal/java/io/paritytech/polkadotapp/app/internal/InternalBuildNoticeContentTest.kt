package io.paritytech.polkadotapp.app.internal

import org.junit.Assert.assertEquals
import org.junit.Test

class InternalBuildNoticeContentTest {
    private val active = SentryStatus.Active("io.pcf.polkadotapp.internal@1.0.0-internal+28")

    @Test
    fun `debuggable build with the debug menu names both`() {
        assertEquals(
            listOf(
                NoticeParagraph.SCOPE,
                NoticeParagraph.SENTRY,
                NoticeParagraph.DEBUGGABLE,
                NoticeParagraph.DEBUG_MENU,
                NoticeParagraph.KEYS,
            ),
            noticeParagraphs(isDebuggable = true, hasDebugMenu = true, sentryStatus = active)
        )
    }

    @Test
    fun `non-debuggable build does not claim to be debuggable`() {
        assertEquals(
            listOf(NoticeParagraph.SCOPE, NoticeParagraph.SENTRY, NoticeParagraph.DEBUG_MENU, NoticeParagraph.KEYS),
            noticeParagraphs(isDebuggable = false, hasDebugMenu = true, sentryStatus = active)
        )
    }

    @Test
    fun `without the debug menu its paragraph is left out`() {
        assertEquals(
            listOf(NoticeParagraph.SCOPE, NoticeParagraph.SENTRY, NoticeParagraph.DEBUGGABLE, NoticeParagraph.KEYS),
            noticeParagraphs(isDebuggable = true, hasDebugMenu = false, sentryStatus = active)
        )
    }

    @Test
    fun `without Sentry the reporting paragraph is left out`() {
        assertEquals(
            listOf(NoticeParagraph.SCOPE, NoticeParagraph.DEBUGGABLE, NoticeParagraph.DEBUG_MENU, NoticeParagraph.KEYS),
            noticeParagraphs(isDebuggable = true, hasDebugMenu = true, sentryStatus = SentryStatus.NotConfigured)
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
