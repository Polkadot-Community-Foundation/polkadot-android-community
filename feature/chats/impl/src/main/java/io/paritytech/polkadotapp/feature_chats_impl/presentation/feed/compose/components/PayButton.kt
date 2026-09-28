package io.paritytech.polkadotapp.feature_chats_impl.presentation.feed.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import io.paritytech.polkadotapp.common.utils.CurrencyConfig
import io.paritytech.polkadotapp.design.components.button.common.PolkadotButtonStyle
import io.paritytech.polkadotapp.design.components.button.icon.PolkadotIconButton
import io.paritytech.polkadotapp.design.components.button.icon.PolkadotIconButtonSize
import io.paritytech.polkadotapp.design.components.icon.NovaIcons
import io.paritytech.polkadotapp.design.components.icon.vectors.CashOutlined
import io.paritytech.polkadotapp.design.components.icon.vectors.DollarIcon
import io.paritytech.polkadotapp.design.theme.PolkadotTheme

@Composable
internal fun PayButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    PolkadotIconButton(
        modifier = modifier,
        icon = fiatIcon,
        onClick = onClick,
        style = PolkadotButtonStyle.secondary(),
        size = PolkadotIconButtonSize.mediumIncreased(),
        shape = PolkadotTheme.shapes.full,
    )
}

private val fiatIcon: ImageVector
    get() = when (CurrencyConfig.fiatSymbol) {
        "$" -> NovaIcons.DollarIcon
        else -> NovaIcons.CashOutlined
    }

@Preview
@Composable
private fun PayButtonPreview() {
    PolkadotTheme {
        PayButton(onClick = {})
    }
}
