package io.paritytech.polkadotapp.app.internal

import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.paritytech.polkadotapp.app.R
import io.paritytech.polkadotapp.design.components.text.NovaText
import io.paritytech.polkadotapp.design.theme.PolkadotTheme
import io.paritytech.polkadotapp.feature_connection_status_api.presentation.ChainHealthBarDefaults

/** A pill in the top-left corner, level with the chain health bar, on every screen of this build. */
object InternalBuildMarker {
    fun attach(activity: ComponentActivity, sentryStatus: SentryStatus) {
        val view = ComposeView(activity).apply {
            consumeWindowInsets = false
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { InternalBuildMarkerPill(sentryActive = sentryStatus is SentryStatus.Active) }
        }
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.START,
        )
        activity.addContentView(view, layoutParams)
    }
}

@Composable
private fun InternalBuildMarkerPill(sentryActive: Boolean) {
    PolkadotTheme {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .height(ChainHealthBarDefaults.ContentHeight)
                .padding(horizontal = PolkadotTheme.spacings.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NovaText(
                modifier = Modifier
                    .background(PolkadotTheme.colors.fg.error, PolkadotTheme.shapes.tiny)
                    .padding(horizontal = PolkadotTheme.spacings.small, vertical = PolkadotTheme.spacings.extraTiny),
                text = stringResource(
                    if (sentryActive) R.string.internal_build_marker_sentry else R.string.internal_build_marker
                ),
                style = PolkadotTheme.typography.label.smallEmphasized,
                color = PolkadotTheme.colors.fg.staticWhite,
            )
        }
    }
}

@Preview
@Composable
private fun InternalBuildMarkerPillPreview() {
    InternalBuildMarkerPill(sentryActive = true)
}
