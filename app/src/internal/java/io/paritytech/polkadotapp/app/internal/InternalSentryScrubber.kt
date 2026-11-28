package io.paritytech.polkadotapp.app.internal

import io.sentry.Breadcrumb
import io.sentry.SentryBaseEvent
import io.sentry.android.core.SentryAndroidOptions
import io.sentry.protocol.Request
import io.sentry.protocol.Response

/**
 * Backend calls carry bearer tokens, attestation payloads and client proofs in headers, and the
 * bodies hold account data, so every header, cookie and body is dropped, not only the names the SDK
 * recognises as sensitive. App logs are not sent at all: the SDK and the Gradle plugin would
 * otherwise forward Timber and logcat output as breadcrumbs, and Timber errors as events.
 * Transaction spans are left as recorded, so their URLs keep their query strings.
 */
object InternalSentryScrubber {
    private const val HTTP_BREADCRUMB_TYPE = "http"

    // Matched by name: the Gradle plugin installs this integration; the module does not declare it.
    private const val TIMBER_INTEGRATION_CLASS = "io.sentry.android.timber.SentryTimberIntegration"

    internal val logBreadcrumbCategories = setOf("Timber", "Logcat")

    internal val httpBreadcrumbDataKept = setOf(
        "url",
        "method",
        "status_code",
        "http.request_content_length",
        "http.response_content_length",
        "http.start_timestamp",
        "http.end_timestamp",
    )

    // SentryAndroid.init calls this after adding its default integrations and before installing them.
    fun applyTo(options: SentryAndroidOptions) {
        options.isSendDefaultPii = false
        options.isAttachScreenshot = false
        options.logs.isEnabled = false
        options.integrations.removeAll { it.javaClass.name == TIMBER_INTEGRATION_CLASS }
        options.setBeforeSend { event, _ -> scrub(event) }
        options.setBeforeSendTransaction { transaction, _ -> scrub(transaction) }
        options.setBeforeBreadcrumb { breadcrumb, _ -> scrub(breadcrumb) }
    }

    fun <T : SentryBaseEvent> scrub(event: T): T {
        event.request?.let { scrubRequest(it) }
        event.contexts.response?.let { scrubResponse(it) }
        return event
    }

    /** Returns null for a breadcrumb that must not be sent. */
    fun scrub(breadcrumb: Breadcrumb): Breadcrumb? {
        if (breadcrumb.category in logBreadcrumbCategories) return null

        if (breadcrumb.type == HTTP_BREADCRUMB_TYPE) {
            breadcrumb.data.keys
                .filterNot { it in httpBreadcrumbDataKept }
                .forEach(breadcrumb::removeData)
        }
        return breadcrumb
    }

    private fun scrubRequest(request: Request) {
        request.headers = null
        request.cookies = null
        request.data = null
        request.queryString = null
        request.fragment = null
    }

    private fun scrubResponse(response: Response) {
        response.headers = null
        response.cookies = null
        response.data = null
    }
}
