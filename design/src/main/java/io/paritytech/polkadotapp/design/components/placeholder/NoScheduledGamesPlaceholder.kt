package io.paritytech.polkadotapp.design.components.placeholder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import io.paritytech.polkadotapp.design.components.text.NovaText
import io.paritytech.polkadotapp.design.theme.PolkadotTheme

/**
 * Shown in the Polkadot Prizes footer when the game schedule is known to be empty.
 *
 * Without it the footer simply renders nothing, which reads as a broken screen rather than as
 * "there is nothing on yet" — the state a testnet sits in whenever no games are scheduled.
 */
@Composable
fun NoScheduledGamesPlaceholder(
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(PolkadotTheme.spacings.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NovaText(
            text = text,
            style = PolkadotTheme.typography.body.medium,
            color = PolkadotTheme.colors.fg.secondary,
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
private fun NoScheduledGamesPlaceholderPreview() {
    PolkadotTheme {
        NoScheduledGamesPlaceholder(
            text = "No games are scheduled right now. The next one will appear here as soon as it is announced."
        )
    }
}
