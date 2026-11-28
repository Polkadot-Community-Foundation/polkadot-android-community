package io.paritytech.polkadotapp.app.internal

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import io.paritytech.polkadotapp.app.R
import io.paritytech.polkadotapp.design.components.button.common.PolkadotButtonStyle
import io.paritytech.polkadotapp.design.components.button.default.PolkadotTextButton
import io.paritytech.polkadotapp.design.components.compound.PolkadotCheckBox
import io.paritytech.polkadotapp.design.components.spacer.HorizontalSpacer
import io.paritytech.polkadotapp.design.components.spacer.VerticalSpacer
import io.paritytech.polkadotapp.design.components.text.NovaText
import io.paritytech.polkadotapp.design.theme.PolkadotTheme
import io.paritytech.polkadotapp.designsystem.colors.LocalPolkadotColors
import io.paritytech.polkadotapp.designsystem.themes.PolkadotAppTheme

@Composable
fun InternalBuildNoticeScreen(
    isDebuggable: Boolean,
    hasDebugMenu: Boolean,
    sentryStatus: SentryStatus,
    onContinue: () -> Unit,
    onCloseApp: () -> Unit,
) {
    PolkadotTheme {
        // Dark whatever the app theme, so the notice never looks like an ordinary app screen.
        val nightColors = remember { PolkadotAppTheme.BerlinNight.colors() }
        CompositionLocalProvider(LocalPolkadotColors provides nightColors) {
            NoticeContent(
                paragraphs = noticeParagraphs(isDebuggable, hasDebugMenu, sentryStatus),
                sentryStatus = sentryStatus,
                onContinue = onContinue,
                onCloseApp = onCloseApp,
            )
        }
    }
}

@Composable
private fun NoticeContent(
    paragraphs: List<NoticeParagraph>,
    sentryStatus: SentryStatus,
    onContinue: () -> Unit,
    onCloseApp: () -> Unit,
) {
    var keysConfirmed by rememberSaveable { mutableStateOf(false) }
    var usernamesConfirmed by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PolkadotTheme.colors.bg.surface.main)
            .systemBarsPadding()
            .padding(horizontal = PolkadotTheme.spacings.mediumIncreased, vertical = PolkadotTheme.spacings.large),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            VerticalSpacer { large }

            NovaText(
                text = stringResource(R.string.internal_build_notice_title),
                style = PolkadotTheme.typography.headline.large,
                color = PolkadotTheme.colors.fg.primary,
            )

            VerticalSpacer { large }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PolkadotTheme.colors.bg.surface.container, PolkadotTheme.shapes.large)
                    .padding(PolkadotTheme.spacings.mediumIncreased),
                verticalArrangement = Arrangement.spacedBy(PolkadotTheme.spacings.small),
            ) {
                paragraphs.forEach { paragraph ->
                    NovaText(
                        text = stringResource(paragraph.textRes),
                        style = PolkadotTheme.typography.body.large,
                        color = PolkadotTheme.colors.fg.primary,
                    )
                }
            }

            VerticalSpacer { mediumIncreased }

            NovaText(
                text = when (sentryStatus) {
                    is SentryStatus.Active -> stringResource(R.string.internal_build_notice_sentry_active, sentryStatus.release)
                    SentryStatus.NotConfigured -> stringResource(R.string.internal_build_notice_sentry_not_configured)
                },
                style = PolkadotTheme.typography.body.medium,
                color = PolkadotTheme.colors.fg.tertiary,
            )

            VerticalSpacer { extraLarge }

            ConfirmationRow(
                text = stringResource(R.string.internal_build_notice_confirm_keys),
                checked = keysConfirmed,
                onCheckedChange = { keysConfirmed = it },
            )

            VerticalSpacer { large }

            ConfirmationRow(
                text = stringResource(R.string.internal_build_notice_confirm_usernames),
                checked = usernamesConfirmed,
                onCheckedChange = { usernamesConfirmed = it },
            )
        }

        VerticalSpacer { large }

        PolkadotTextButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.internal_build_notice_continue),
            enabled = keysConfirmed && usernamesConfirmed,
            style = PolkadotButtonStyle.primary(),
            onClick = onContinue,
        )

        VerticalSpacer { small }

        PolkadotTextButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.internal_build_notice_close_app),
            style = PolkadotButtonStyle.secondary(),
            onClick = onCloseApp,
        )
    }
}

@Composable
private fun ConfirmationRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PolkadotCheckBox(checked = checked, onCheckedChange = null)

        HorizontalSpacer { mediumIncreased }

        NovaText(
            text = text,
            style = PolkadotTheme.typography.body.large,
            color = PolkadotTheme.colors.fg.primary,
        )
    }
}

@get:StringRes
private val NoticeParagraph.textRes: Int
    get() = when (this) {
        NoticeParagraph.SCOPE -> R.string.internal_build_notice_scope
        NoticeParagraph.SENTRY -> R.string.internal_build_notice_sentry
        NoticeParagraph.DEBUGGABLE -> R.string.internal_build_notice_debuggable
        NoticeParagraph.DEBUG_MENU -> R.string.internal_build_notice_debug_menu
        NoticeParagraph.KEYS -> R.string.internal_build_notice_keys
    }

@Preview
@Composable
private fun InternalBuildNoticeScreenPreview() {
    InternalBuildNoticeScreen(
        isDebuggable = true,
        hasDebugMenu = true,
        sentryStatus = SentryStatus.Active("io.pcf.polkadotapp.internal@1.0.0-internal+28"),
        onContinue = {},
        onCloseApp = {},
    )
}
