package io.paritytech.polkadotapp.feature_wallet_impl.presentation.pocket.compose.components.product

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterExitState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.paritytech.polkadotapp.design.components.spacer.VerticalSpacer
import io.paritytech.polkadotapp.design.components.text.NovaText
import io.paritytech.polkadotapp.design.components.topbar.PolkadotTopBar
import io.paritytech.polkadotapp.design.components.topbar.TopBarTitleAlignment
import io.paritytech.polkadotapp.design.components.topbar.rememberTopBarAction
import io.paritytech.polkadotapp.design.theme.PolkadotTheme
import io.paritytech.polkadotapp.feature_dotns_api.domain.DotNsLoadProgress
import io.paritytech.polkadotapp.feature_products_api.presentation.spaHost.ProductWebViewHost
import io.paritytech.polkadotapp.feature_products_api.presentation.spaHost.SpaHostSession
import io.paritytech.polkadotapp.feature_wallet_impl.presentation.pocket.ProductFaceBindings
import io.paritytech.polkadotapp.feature_wallet_impl.presentation.pocket.compose.LocalNavAnimatedVisibilityScope
import io.paritytech.polkadotapp.feature_wallet_impl.presentation.pocket.compose.pocketCardSharedElement
import io.paritytech.polkadotapp.feature_wallet_impl.presentation.pocket.models.PocketCardUiModel
import io.paritytech.polkadotapp.common.R as RCommon

/**
 * The expanded card: the same card element the list drew, moved to the top, with the product filling
 * the screen under it. The face keeps streaming up here, so the card stays live while it is open.
 */
@Composable
fun ProductPocketCardDetails(
    modifier: Modifier = Modifier,
    card: PocketCardUiModel.ProductCard,
    bindings: ProductFaceBindings,
    session: SpaHostSession?,
    cardIndex: Int,
    onSettled: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    // The product is asked for only once the card has arrived, so its WebView is not built while the
    // card is still travelling.
    val arrival = LocalNavAnimatedVisibilityScope.current?.transition
    val arrived = arrival == null || arrival.currentState == EnterExitState.Visible
    LaunchedEffect(arrived) {
        if (arrived) onSettled()
    }

    Column(modifier = modifier.fillMaxSize()) {
        PolkadotTopBar(
            title = card.title,
            navigationAction = rememberTopBarAction(onBack),
            titleAlignment = TopBarTitleAlignment.Center
        )

        ProductPocketCard(
            modifier = Modifier
                .padding(horizontal = PolkadotTheme.spacings.mediumIncreased)
                .pocketCardSharedElement(cardIndex),
            card = card,
            bindings = bindings,
            onOpen = null,
            onRemoveRequested = null
        )

        VerticalSpacer { mediumIncreased }

        ExpandedProductContent(
            modifier = Modifier.fillMaxSize(),
            session = session
        )
    }
}

@Composable
private fun ExpandedProductContent(
    modifier: Modifier = Modifier,
    session: SpaHostSession?,
) {
    // The app's own ground under the card until the product has something to show, so a page that
    // paints white does not flash through the arrival.
    Box(
        modifier = modifier.background(PolkadotTheme.colors.bg.surface.main),
        contentAlignment = Alignment.Center,
    ) {
        if (session != null) {
            val loadProgress by session.loadProgress.collectAsStateWithLifecycle()
            val webView by session.webView.collectAsStateWithLifecycle()

            // Once the product has painted once it owns the space; its own navigations must not blank it.
            var productShowing by remember(session) { mutableStateOf(false) }
            LaunchedEffect(loadProgress) {
                if (loadProgress == DotNsLoadProgress.Completed) productShowing = true
            }

            when {
                loadProgress is DotNsLoadProgress.Failed -> NovaText(
                    text = stringResource(RCommon.string.product_resolution_error_unknown),
                    style = PolkadotTheme.typography.body.medium,
                    color = PolkadotTheme.colors.fg.secondary
                )

                productShowing -> ProductWebViewHost(modifier = Modifier.fillMaxSize(), webView = webView)
            }
        }
    }
}
