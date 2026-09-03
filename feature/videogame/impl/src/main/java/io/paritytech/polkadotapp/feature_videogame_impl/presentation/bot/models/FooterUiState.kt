package io.paritytech.polkadotapp.feature_videogame_impl.presentation.bot.models

import io.paritytech.polkadotapp.feature_upgrade_username_api.presentation.bot.UpgradeUsernameWidgetUiState

class FooterUiState(
    val upcomingGameUiState: UpcomingGameUiState?,
    val upgradeUsernameUiState: UpgradeUsernameWidgetUiState?,
    /**
     * Whether the game schedule has been read at least once.
     *
     * [upcomingGameUiState] is null both before the schedule is known and when it is known to be
     * empty, and the two must not look the same: telling someone "no games are scheduled" while
     * still loading would be wrong for the moment it takes to find out. Only the combine that
     * observes the schedule sets this, so the initial state cannot claim to know.
     */
    val isScheduleKnown: Boolean
)
