package io.paritytech.polkadotapp.feature_videogame_impl.presentation.bot.models

import io.paritytech.polkadotapp.feature_upgrade_username_api.presentation.bot.UpgradeUsernameWidgetUiState

class FooterUiState(
    val upcomingGameUiState: UpcomingGameUiState?,
    val upgradeUsernameUiState: UpgradeUsernameWidgetUiState?,
    /**
     * Whether the schedule has been read. [upcomingGameUiState] is null both while loading and
     * when nothing is scheduled, and only the latter shows the empty state.
     */
    val isScheduleKnown: Boolean
)
