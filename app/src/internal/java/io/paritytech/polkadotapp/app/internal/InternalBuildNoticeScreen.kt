package io.paritytech.polkadotapp.app.internal

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.paritytech.polkadotapp.app.R
import io.paritytech.polkadotapp.design.components.button.common.PolkadotButtonStyle
import io.paritytech.polkadotapp.design.components.button.default.PolkadotButtonSize
import io.paritytech.polkadotapp.design.components.button.default.PolkadotTextButton
import io.paritytech.polkadotapp.design.components.compound.PolkadotCheckBox
import io.paritytech.polkadotapp.design.components.spacer.HorizontalSpacer
import io.paritytech.polkadotapp.design.components.spacer.VerticalSpacer
import io.paritytech.polkadotapp.design.components.text.NovaText
import io.paritytech.polkadotapp.design.theme.PolkadotTheme
import io.paritytech.polkadotapp.designsystem.colors.LocalPolkadotColors
import io.paritytech.polkadotapp.designsystem.themes.PolkadotAppTheme

private const val CONFIRMATIONS_MAX_HEIGHT_FRACTION = 0.4f

@Composable
fun InternalBuildNoticeScreen(
    isDebuggable: Boolean,
    hasDebugMenu: Boolean,
    hasGoogleBackup: Boolean,
    sentryStatus: SentryStatus,
    onContinue: () -> Unit,
    onCloseApp: () -> Unit,
) {
    PolkadotTheme {
        // Dark whatever the app theme, so the notice never looks like an ordinary app screen.
        val nightColors = remember { PolkadotAppTheme.BerlinNight.colors() }
        CompositionLocalProvider(LocalPolkadotColors provides nightColors) {
            NoticeContent(
                paragraphs = noticeParagraphs(isDebuggable, hasDebugMenu, hasGoogleBackup, sentryStatus),
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
    val noticeScrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(PolkadotTheme.colors.bg.surface.main),
    ) {
        // At large font scales the confirmations alone could fill the screen; capped, they scroll
        // on their own and the buttons stay visible.
        val confirmationsMaxHeight = maxHeight * CONFIRMATIONS_MAX_HEIGHT_FRACTION

        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                    .verticalScroll(noticeScrollState)
                    .padding(horizontal = PolkadotTheme.spacings.mediumIncreased, vertical = PolkadotTheme.spacings.medium),
            ) {
                NovaText(
                    text = stringResource(R.string.internal_build_notice_title),
                    style = PolkadotTheme.typography.headline.medium,
                    color = PolkadotTheme.colors.fg.primary,
                )

                VerticalSpacer { medium }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PolkadotTheme.colors.bg.surface.container, PolkadotTheme.shapes.large)
                        .padding(PolkadotTheme.spacings.medium),
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

                VerticalSpacer { small }

                NovaText(
                    text = when (sentryStatus) {
                        is SentryStatus.Active -> stringResource(R.string.internal_build_notice_sentry_active, sentryStatus.release)
                        SentryStatus.NotConfigured -> stringResource(R.string.internal_build_notice_sentry_not_configured)
                    },
                    style = PolkadotTheme.typography.body.medium,
                    color = PolkadotTheme.colors.fg.tertiary,
                )
            }

            // Marks that the text above continues below the fixed confirmations. The line always takes
            // its 1 dp, so the layout does not shift when it appears.
            val dividerColor = PolkadotTheme.colors.stroke.secondary
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .drawBehind { if (noticeScrollState.canScrollForward) drawRect(dividerColor) },
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = PolkadotTheme.spacings.mediumIncreased, vertical = PolkadotTheme.spacings.medium),
            ) {
                Column(
                    modifier = Modifier
                        .heightIn(max = confirmationsMaxHeight)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(PolkadotTheme.spacings.small),
                ) {
                    ConfirmationRow(
                        text = stringResource(R.string.internal_build_notice_confirm_keys),
                        checked = keysConfirmed,
                        onCheckedChange = { keysConfirmed = it },
                    )

                    ConfirmationRow(
                        text = stringResource(R.string.internal_build_notice_confirm_usernames),
                        checked = usernamesConfirmed,
                        onCheckedChange = { usernamesConfirmed = it },
                    )
                }

                VerticalSpacer { medium }

                PolkadotTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.internal_build_notice_continue),
                    enabled = keysConfirmed && usernamesConfirmed,
                    style = PolkadotButtonStyle.primary(),
                    size = PolkadotButtonSize.large(),
                    onClick = onContinue,
                )

                VerticalSpacer { small }

                PolkadotTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.internal_build_notice_close_app),
                    style = PolkadotButtonStyle.secondary(),
                    size = PolkadotButtonSize.large(),
                    onClick = onCloseApp,
                )
            }
        }
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

        HorizontalSpacer { medium }

        NovaText(
            text = text,
            style = PolkadotTheme.typography.body.medium,
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
        NoticeParagraph.GOOGLE_BACKUP -> R.string.internal_build_notice_google_backup
        NoticeParagraph.KEYS -> R.string.internal_build_notice_keys
    }

@Preview
@Preview(name = "Font scale 1.3", widthDp = 393, heightDp = 851, fontScale = 1.3f)
@Preview(name = "Small phone, font scale 1.3", widthDp = 360, heightDp = 640, fontScale = 1.3f)
@Composable
private fun InternalBuildNoticeScreenPreview() {
    InternalBuildNoticeScreen(
        isDebuggable = true,
        hasDebugMenu = true,
        hasGoogleBackup = true,
        sentryStatus = SentryStatus.Active("io.pcf.polkadotapp.internal@1.0.0-internal+28"),
        onContinue = {},
        onCloseApp = {},
    )
}
