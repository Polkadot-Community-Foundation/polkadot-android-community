package io.paritytech.polkadotapp.feature_chats_impl.presentation.search.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.paritytech.polkadotapp.common.domain.model.AccountId
import io.paritytech.polkadotapp.common.presentation.search.SearchState
import io.paritytech.polkadotapp.design.components.progress.NovaCircularProgressIndicator
import io.paritytech.polkadotapp.design.components.text.NovaText
import io.paritytech.polkadotapp.design.theme.PolkadotTheme
import io.paritytech.polkadotapp.design.utils.withBold
import io.paritytech.polkadotapp.feature_chats_api.domain.model.ChatId
import io.paritytech.polkadotapp.feature_chats_impl.presentation.chatSearch.compose.components.ChatSearchPersonRow
import io.paritytech.polkadotapp.feature_chats_impl.presentation.chatSearch.models.NoRowStatus
import io.paritytech.polkadotapp.feature_chats_impl.presentation.chatSearch.models.RecentChatUiModel
import io.paritytech.polkadotapp.feature_chats_impl.presentation.search.AddContactUiState
import io.paritytech.polkadotapp.feature_chats_impl.presentation.search.models.UserSearchResultUiModel
import kotlinx.collections.immutable.ImmutableList
import io.paritytech.polkadotapp.common.R as RCommon

@Composable
internal fun AddContactSearchContent(
    state: AddContactUiState,
    onSearchResultClick: (UserSearchResultUiModel) -> Unit,
    onRecentClick: (ChatId) -> Unit,
) {
    val searchResult = state.searchResult

    Column(modifier = Modifier.fillMaxSize()) {
        NovaText(
            modifier = Modifier.padding(
                horizontal = PolkadotTheme.spacings.extraMedium,
                vertical = PolkadotTheme.spacings.small
            ),
            text = stringResource(
                if (searchResult is SearchState.Initial) {
                    RCommon.string.add_contact_recents_header
                } else {
                    RCommon.string.add_contact_users_header
                }
            ),
            style = PolkadotTheme.typography.caption.medium,
            color = PolkadotTheme.colors.fg.secondary,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (searchResult) {
                SearchState.Initial -> if (state.recents.isEmpty()) {
                    CenteredMessage(
                        text = AnnotatedString(stringResource(RCommon.string.add_contact_no_recent_searches))
                    )
                } else {
                    RecentsList(
                        recents = state.recents,
                        onRecentClick = onRecentClick
                    )
                }

                SearchState.Loading -> CenteredLoading()

                SearchState.Empty -> CenteredMessage(
                    text = stringResource(RCommon.string.common_no_results_for, state.searchQuery).withBold(state.searchQuery)
                )

                is SearchState.Error -> CenteredMessage(
                    text = AnnotatedString(stringResource(RCommon.string.add_contact_search_error))
                )

                is SearchState.Loaded -> UsersList(
                    users = searchResult.results,
                    loadingContactId = state.loadingContactId,
                    onSearchResultClick = onSearchResultClick
                )
            }
        }
    }
}

@Composable
private fun RecentsList(
    recents: ImmutableList<RecentChatUiModel>,
    onRecentClick: (ChatId) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            items = recents,
            key = { recent -> recent.key }
        ) { recent ->
            ChatSearchPersonRow(
                title = recent.title,
                avatarModel = recent.avatarModel,
                status = recent.status,
                onClick = { onRecentClick(recent.chatId) },
            )
        }
    }
}

@Composable
private fun UsersList(
    users: ImmutableList<UserSearchResultUiModel>,
    loadingContactId: AccountId?,
    onSearchResultClick: (UserSearchResultUiModel) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            items = users,
            key = { user -> user.contactAccountId.value.toHexString() }
        ) { user ->
            ChatSearchPersonRow(
                title = user.username,
                avatarModel = user.avatarModel,
                status = NoRowStatus,
                onClick = { onSearchResultClick(user) },
                loading = user.contactAccountId == loadingContactId,
            )
        }
    }
}

@Composable
private fun CenteredMessage(text: AnnotatedString) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        NovaText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PolkadotTheme.spacings.large),
            text = text,
            style = PolkadotTheme.typography.body.large,
            color = PolkadotTheme.colors.fg.secondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CenteredLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        NovaCircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = PolkadotTheme.colors.fg.primary,
            strokeWidth = 3.dp
        )
    }
}
