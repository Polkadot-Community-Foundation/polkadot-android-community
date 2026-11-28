package io.paritytech.polkadotapp.app.internal

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import io.paritytech.polkadotapp.app.BuildConfig
import io.paritytech.polkadotapp.app.root.presentation.root.RootActivity
import io.paritytech.polkadotapp.common.data.memory.ComputationalScope
import io.paritytech.polkadotapp.common.presentation.AppInitializer
import io.sentry.Sentry
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val APP_PACKAGE_PREFIX = "io.paritytech.polkadotapp."
private const val NOTICE_ACKNOWLEDGEMENT_FILE = "internal_build_notice_acknowledged"
private const val GP_FLAVOR = "gp"

/**
 * Adds the internal build's notice, marker and username pre-fill to the app's own activities through
 * lifecycle callbacks, so no feature code knows about this build type. Runs from Application.onCreate,
 * before any activity exists.
 */
@Singleton
class InternalBuildProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppInitializer {
    context(_: ComputationalScope)
    override fun initialize(): Result<Unit> = runCatching {
        val buildInfo = readBuildInfo(context)
        // noBackupFilesDir: a restored backup must not carry the acknowledgement to another install.
        val notice = InternalBuildNotice(File(context.noBackupFilesDir, NOTICE_ACKNOWLEDGEMENT_FILE))
        val callbacks = InternalBuildActivityCallbacks(buildInfo, notice, InternalUsernamePrefill())
        (context as Application).registerActivityLifecycleCallbacks(callbacks)
    }
}

internal class InternalBuildInfo(
    val isDebuggable: Boolean,
    val hasDebugMenu: Boolean,
    val hasGoogleBackup: Boolean,
    val sentryStatus: SentryStatus,
)

private fun readBuildInfo(context: Context): InternalBuildInfo {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val sentryStatus = if (Sentry.isEnabled()) {
        SentryStatus.Active(
            sentryReleaseName(context.packageName, packageInfo.versionName.orEmpty(), packageInfo.longVersionCode)
        )
    } else {
        SentryStatus.NotConfigured
    }

    return InternalBuildInfo(
        isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0,
        // The flag App uses to enable the shake-to-open debug menu and the dev reset prompt.
        hasDebugMenu = BuildConfig.DEBUG,
        // Only the gp flavor signs in with Google and backs up to Drive; vanilla has no-op auth.
        hasGoogleBackup = BuildConfig.FLAVOR == GP_FLAVOR,
        sentryStatus = sentryStatus,
    )
}

private class InternalBuildActivityCallbacks(
    private val buildInfo: InternalBuildInfo,
    private val notice: InternalBuildNotice,
    private val usernamePrefill: InternalUsernamePrefill,
) : Application.ActivityLifecycleCallbacks {
    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity is FragmentActivity && activity.isAppActivity()) {
            usernamePrefill.attach(activity)
        }
    }

    // Post-created: the activity's own content views exist, so the overlays added here draw above them.
    // The notice gates the launcher activity only; an incoming call screen must stay usable.
    override fun onActivityPostCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity is ComponentActivity && activity.isAppActivity()) {
            InternalBuildMarker.attach(activity, buildInfo.sentryStatus)
        }
        if (activity is RootActivity) {
            notice.showIfNeeded(activity, buildInfo)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit

    private fun Activity.isAppActivity(): Boolean = javaClass.name.startsWith(APP_PACKAGE_PREFIX)
}
