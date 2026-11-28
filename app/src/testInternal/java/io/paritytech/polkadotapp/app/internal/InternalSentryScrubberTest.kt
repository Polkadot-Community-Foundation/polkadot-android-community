package io.paritytech.polkadotapp.app.internal

import io.sentry.Breadcrumb
import io.sentry.SentryEvent
import io.sentry.protocol.Request
import io.sentry.protocol.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InternalSentryScrubberTest {

    @Test
    fun `request and response keep no headers, cookies, bodies or query`() {
        val event = SentryEvent().apply {
            request = Request().apply {
                url = "https://backend.example/api/v1/auth/token"
                method = "POST"
                headers = mapOf("Authorization" to "Bearer jwt", "Auth-ClientProof" to "proof", "X-Other" to "v")
                cookies = "session=1"
                data = "{\"payload\":1}"
                queryString = "token=secret"
                fragment = "frag"
            }
            contexts.setResponse(
                Response().apply {
                    statusCode = 403
                    headers = mapOf("Set-Cookie" to "a=b")
                    cookies = "a=b"
                    data = "{\"error\":1}"
                }
            )
        }

        InternalSentryScrubber.scrub(event)

        val request = event.request!!
        assertEquals("https://backend.example/api/v1/auth/token", request.url)
        assertEquals("POST", request.method)
        assertNull(request.headers)
        assertNull(request.cookies)
        assertNull(request.data)
        assertNull(request.queryString)
        assertNull(request.fragment)

        val response = event.contexts.response!!
        assertEquals(403, response.statusCode)
        assertNull(response.headers)
        assertNull(response.cookies)
        assertNull(response.data)
    }

    @Test
    fun `http breadcrumbs keep only url, method, status, sizes and timing`() {
        val breadcrumb = Breadcrumb.http("https://backend.example/api?token=secret#frag", "GET", 200).apply {
            setData("request_headers", "Authorization: Bearer jwt")
            setData("http.response_content_length", 10L)
        }

        InternalSentryScrubber.scrub(breadcrumb)

        assertEquals(setOf("url", "method", "status_code", "http.response_content_length"), breadcrumb.data.keys)
        assertEquals("https://backend.example/api", breadcrumb.getData("url"))
    }

    @Test
    fun `app log breadcrumbs are dropped`() {
        listOf("Timber", "Logcat").forEach { category ->
            val breadcrumb = Breadcrumb("Refresh token failed").apply { this.category = category }

            assertNull("$category breadcrumb should be dropped", InternalSentryScrubber.scrub(breadcrumb))
        }
    }

    @Test
    fun `other breadcrumbs are left as they are`() {
        val breadcrumb = Breadcrumb("screen opened").apply {
            type = "navigation"
            category = "ui.lifecycle"
            setData("key", "value")
        }

        assertEquals("value", InternalSentryScrubber.scrub(breadcrumb)?.getData("key"))
    }
}
