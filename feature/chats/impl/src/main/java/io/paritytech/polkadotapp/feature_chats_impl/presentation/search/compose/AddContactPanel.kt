package io.paritytech.polkadotapp.feature_chats_impl.presentation.search.compose

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.paritytech.polkadotapp.common.domain.model.intoAccountId
import io.paritytech.polkadotapp.common.presentation.notification.rememberAppNotifier
import io.paritytech.polkadotapp.common.presentation.screens.ObserveViewModelEvents
import io.paritytech.polkadotapp.common.presentation.search.SearchState
import io.paritytech.polkadotapp.common.utils.SizedList
import io.paritytech.polkadotapp.common.utils.randomBytes
import io.paritytech.polkadotapp.common.utils.toSizedList
import io.paritytech.polkadotapp.design.components.avatar.AvatarUiModel
import io.paritytech.polkadotapp.design.components.avatar.Mock
import io.paritytech.polkadotapp.design.components.button.common.PolkadotButtonShape
import io.paritytech.polkadotapp.design.components.button.common.PolkadotButtonStyle
import io.paritytech.polkadotapp.design.components.button.icon.PolkadotIconButton
import io.paritytech.polkadotapp.design.components.button.icon.PolkadotIconButtonSize
import io.paritytech.polkadotapp.design.components.icon.NovaIcons
import io.paritytech.polkadotapp.design.components.icon.vectors.Scanner
import io.paritytech.polkadotapp.design.components.spacer.VerticalSpacer
import io.paritytech.polkadotapp.design.components.surface.PolkadotSurface
import io.paritytech.polkadotapp.design.components.topbar.PolkadotSearchField
import io.paritytech.polkadotapp.design.theme.PolkadotTheme
import io.paritytech.polkadotapp.feature_chats_api.domain.model.ChatId
import io.paritytech.polkadotapp.feature_chats_impl.presentation.chatSearch.models.NoRowStatus
import io.paritytech.polkadotapp.feature_chats_impl.presentation.chatSearch.models.RecentChatUiModel
import io.paritytech.polkadotapp.feature_chats_impl.presentation.search.AddContactUiState
import io.paritytech.polkadotapp.feature_chats_impl.presentation.search.AddContactViewModel
import io.paritytech.polkadotapp.feature_chats_impl.presentation.search.compose.components.AddContactSearchContent
import io.paritytech.polkadotapp.feature_chats_impl.presentation.search.models.UserSearchResultUiModel
import io.paritytech.polkadotapp.feature_chats_impl.presentation.util.rememberCompositionViewModelStoreOwner
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.random.Random
import io.paritytech.polkadotapp.common.R as RCommon

@Composable
fun AddContactPanel(
    modifier: Modifier = Modifier,
    scannerShape: Shape,
    scanner: @Composable (modifier: Modifier, recognitionArmed: Boolean) -> Unit,
) {
    val viewModel = hiltViewModel<AddContactViewModel>(viewModelStoreOwner = rememberCompositionViewModelStoreOwner())
    val state by viewModel.state.collectAsStateWithLifecycle()
    var searchActive by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    ObserveViewModelEvents(viewModel, rememberAppNotifier())

    val actions = remember(viewModel, focusManager) {
        AddContactPanelActions(
            onSearchFocused = { searchActive = true },
            onSearchChange = viewModel::onSearchChange,
            onCloseSearch = {
                focusManager.clearFocus()
                viewModel.onSearchChange("")
                searchActive = false
            },
            onSearchResultClick = viewModel::onSearchResultClick,
            onRecentClick = viewModel::onRecentClick,
        )
    }

    BackHandler(enabled = searchActive, onBack = actions.onCloseSearch)

    AddContactPanelInternal(
        modifier = modifier,
        state = state,
        searchActive = searchActive,
        actions = actions,
        scannerShape = scannerShape,
        scanner = scanner,
    )
}

@Composable
private fun AddContactPanelInternal(
    modifier: Modifier,
    state: AddContactUiState,
    searchActive: Boolean,
    actions: AddContactPanelActions,
    scannerShape: Shape,
    scanner: @Composable (modifier: Modifier, recognitionArmed: Boolean) -> Unit,
) {
    Column(modifier = modifier) {
        ScannerSearchArea(
            modifier = Modifier
                .weight(1f, fill = false)
                .squareUpToMaxHeight(),
            state = state,
            searchActive = searchActive,
            actions = actions,
            scannerShape = scannerShape,
            scanner = scanner,
        )

        VerticalSpacer { small }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(visible = searchActive) {
                ScanButton(
                    modifier = Modifier.padding(end = PolkadotTheme.spacings.small),
                    onClick = actions.onCloseSearch,
                )
            }

            PolkadotSearchField(
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { if (it.hasFocus) actions.onSearchFocused() },
                value = state.searchQuery,
                onValueChange = actions.onSearchChange,
                onClear = {
                    if (state.searchQuery.isEmpty()) actions.onCloseSearch() else actions.onSearchChange("")
                },
                placeholder = stringResource(RCommon.string.add_contact_search_placeholder),
                showClear = searchActive,
                contentPadding = PaddingValues(PolkadotTheme.spacings.zero),
            )
        }
    }
}

@Composable
private fun ScannerSearchArea(
    modifier: Modifier,
    state: AddContactUiState,
    searchActive: Boolean,
    actions: AddContactPanelActions,
    scannerShape: Shape,
    scanner: @Composable (modifier: Modifier, recognitionArmed: Boolean) -> Unit,
) {
    val collapseProgress = animateFloatAsState(
        targetValue = if (searchActive) 1f else 0f,
        label = "ScannerCollapseProgress"
    )

    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = searchActive,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            AddContactSearchContent(
                state = state,
                onSearchResultClick = actions.onSearchResultClick,
                onRecentClick = actions.onRecentClick,
            )
        }

        CollapsingScanner(
            collapseProgress = collapseProgress,
            recognitionArmed = !searchActive,
            shape = scannerShape,
            scanner = scanner,
        )
    }
}

// Scaled rather than resized, so the camera surface is not re-laid out on every animation frame. Kept in composition
// while collapsed: leaving it would unbind the camera, so closing search would restart it and repeat the permission ask.
// Recognition is off meanwhile, so a code the user cannot see is not acted on.
@Composable
private fun BoxScope.CollapsingScanner(
    collapseProgress: State<Float>,
    recognitionArmed: Boolean,
    shape: Shape,
    scanner: @Composable (modifier: Modifier, recognitionArmed: Boolean) -> Unit,
) {
    PolkadotSurface(
        // Sized by the height: above the keyboard the area can be shorter than wide, and a width-sized square
        // would overflow it and get centered.
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxHeight()
            .aspectRatio(1f, matchHeightConstraintsFirst = true)
            .graphicsLayer {
                val scale = 1f - collapseProgress.value

                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0.5f, 1f)
                this.shape = shape
                clip = true
            },
        color = PolkadotTheme.colors.bg.surface.nested,
    ) {
        scanner(Modifier.fillMaxSize(), recognitionArmed)
    }
}

@Composable
private fun ScanButton(
    modifier: Modifier,
    onClick: () -> Unit,
) {
    PolkadotIconButton(
        modifier = modifier,
        icon = NovaIcons.Scanner,
        onClick = onClick,
        style = PolkadotButtonStyle.secondary(),
        size = PolkadotIconButtonSize.mediumIncreased(),
        shape = PolkadotButtonShape.pill,
    )
}

// A width-sized square that gives up height when the column has less room, e.g. above the keyboard on short screens.
private fun Modifier.squareUpToMaxHeight(): Modifier = layout { measurable, constraints ->
    val width = constraints.maxWidth
    val height = width.coerceAtMost(constraints.maxHeight)
    val placeable = measurable.measure(Constraints.fixed(width, height))

    layout(width, height) {
        placeable.place(0, 0)
    }
}

@Immutable
private data class AddContactPanelActions(
    val onSearchFocused: () -> Unit,
    val onSearchChange: (String) -> Unit,
    val onCloseSearch: () -> Unit,
    val onSearchResultClick: (UserSearchResultUiModel) -> Unit,
    val onRecentClick: (ChatId) -> Unit,
)

@Composable
private fun AddContactPanelPreview(state: AddContactUiState, searchActive: Boolean) {
    PolkadotTheme {
        PolkadotSurface(color = PolkadotTheme.colors.bg.surface.container) {
            AddContactPanelInternal(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PolkadotTheme.spacings.small),
                state = state,
                searchActive = searchActive,
                actions = AddContactPanelActions(
                    onSearchFocused = {},
                    onSearchChange = {},
                    onCloseSearch = {},
                    onSearchResultClick = {},
                    onRecentClick = {},
                ),
                scannerShape = PolkadotTheme.shapes.large,
                scanner = { modifier, _ -> Box(modifier = modifier) },
            )
        }
    }
}

@Preview(widthDp = 380)
@Composable
private fun AddContactPanelScannerPreview() {
    AddContactPanelPreview(
        state = previewState(query = "", searchResult = SearchState.Initial),
        searchActive = false,
    )
}

@Preview(widthDp = 380)
@Composable
private fun AddContactPanelNoRecentsPreview() {
    AddContactPanelPreview(
        state = previewState(query = "", searchResult = SearchState.Initial),
        searchActive = true,
    )
}

@Preview(widthDp = 380)
@Composable
private fun AddContactPanelRecentsPreview() {
    val recents = listOf("mosticRiver.88", "delaware.01", "franz", "dmitry.01", "euclid.01").map { name ->
        val accountId = Random.randomBytes(32).intoAccountId()
        RecentChatUiModel(
            chatId = ChatId.fromContact(accountId),
            key = name,
            title = name,
            avatarModel = AvatarUiModel.Mock.fromName(name),
            status = NoRowStatus,
            isMenuOpen = false,
        )
    }

    AddContactPanelPreview(
        state = previewState(query = "", searchResult = SearchState.Initial).copy(recents = recents.toImmutableList()),
        searchActive = true,
    )
}

@Preview(widthDp = 380)
@Composable
private fun AddContactPanelResultsPreview() {
    val users = listOf("mosticRiver.88", "monster.01", "molecule", "mostwanted", "morales").map { name ->
        UserSearchResultUiModel(
            contactAccountId = Random.randomBytes(32).intoAccountId(),
            username = name,
            avatarModel = AvatarUiModel.Mock.fromName(name),
        )
    }

    AddContactPanelPreview(
        state = previewState(query = "Mo", searchResult = SearchState.Loaded(users.toSizedList())),
        searchActive = true,
    )
}

private fun previewState(
    query: String,
    searchResult: SearchState<SizedList<UserSearchResultUiModel>>,
) = AddContactUiState(
    searchQuery = query,
    searchResult = searchResult,
    loadingContactId = null,
    recents = persistentListOf(),
)
