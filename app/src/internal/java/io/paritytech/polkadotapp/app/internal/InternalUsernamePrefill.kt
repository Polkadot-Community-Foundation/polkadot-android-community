package io.paritytech.polkadotapp.app.internal

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import io.paritytech.polkadotapp.feature_usernames_impl.presentation.claim.ClaimUsernameContract
import io.paritytech.polkadotapp.feature_usernames_impl.presentation.claim.ClaimUsernameFragment
import io.paritytech.polkadotapp.feature_usernames_impl.presentation.claim.ClaimUsernameViewModel

/** Fills the empty "Pick a username" field with a throwaway suggestion, as if the tester had typed it. */
internal class InternalUsernamePrefill(
    private val suggest: () -> String = { InternalUsernameSuggestion.next() },
) : FragmentManager.FragmentLifecycleCallbacks() {
    fun attach(activity: FragmentActivity) {
        activity.supportFragmentManager.registerFragmentLifecycleCallbacks(this, true)
    }

    // Only on a fresh fragment: a restored one, or one the tester cleared, keeps what it has.
    override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        if (f !is ClaimUsernameFragment || savedInstanceState != null) return

        // Same owner and default key as the fragment's own `by viewModels()`, so this is its instance.
        val contract: ClaimUsernameContract = ViewModelProvider(f)[ClaimUsernameViewModel::class.java]
        if (contract.state.value.username.isEmpty()) {
            contract.onUsernameChanged(suggest())
        }
    }
}
