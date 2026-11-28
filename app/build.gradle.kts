import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("polkadotapp.android.application")
    id("polkadotapp.android.compose")
    id("polkadotapp.android.hilt")
    alias(libs.plugins.google.services)
    alias(libs.plugins.sentry.android.gradle)
}

val localProperties = gradleLocalProperties(rootDir, providers)

// The release variant is left unsigned when no release keystore is supplied.
val releaseKeystoreFile: String? = localProperties.readSecretOrNull("RELEASE_KEYSTORE_FILE")

android {
    namespace = "io.paritytech.polkadotapp.app"

    defaultConfig {
        applicationId = localProperties.readSecretOrThrow("APPLICATION_ID")

        versionCode = computeVersionCode()
        versionName = computeVersionName()

        testInstrumentationRunner = "io.paritytech.polkadotapp.app.HiltTestRunner"

        manifestPlaceholders["appName"] = localProperties.readSecretOrThrow("APPLICATION_NAME")
        manifestPlaceholders["sentryDsn"] = localProperties.readSecretOrNull("SENTRY_DSN") ?: ""

        buildConfigString(
            "CONTACT_EMAIL",
            localProperties.readSecretOrThrow("CONTACT_EMAIL")
        )
        buildConfigString(
            "LOG_COLLECTION_EMAIL",
            localProperties.readSecretOrThrow("LOG_COLLECTION_EMAIL")
        )
        buildConfigString(
            "PRIVACY_POLICY_URL",
            localProperties.readSecretOrThrow("PRIVACY_POLICY_URL")
        )
        buildConfigString(
            "TERMS_OF_USE_URL",
            localProperties.readSecretOrThrow("TERMS_OF_USE_URL")
        )
    }

    signingConfigs {
        create("dev") {
            storeFile = file(localProperties.readSecretOrNull("DEV_KEYSTORE_FILE") ?: "../develop_key.jks")
            keyPassword = localProperties.readSecretOrDefault("CI_KEYSTORE_KEY_PASS", "")
            keyAlias = localProperties.readSecretOrDefault("CI_KEYSTORE_KEY_ALIAS", "")
            storePassword = localProperties.readSecretOrDefault("CI_KEYSTORE_PASS", "")
        }

        create("release") {
            releaseKeystoreFile?.let { storeFile = file(it) }
            keyPassword = localProperties.readSecretOrDefault("RELEASE_KEYSTORE_KEY_PASS", "")
            keyAlias = localProperties.readSecretOrDefault("RELEASE_KEYSTORE_KEY_ALIAS", "")
            storePassword = localProperties.readSecretOrDefault("RELEASE_KEYSTORE_PASS", "")
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = releaseKeystoreFile?.let { signingConfigs.getByName("release") }

            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("dev")
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appName"] = localProperties.readSecretOrNull("DEBUG_APPLICATION_NAME")
                ?: "[Debug] ${localProperties.readSecretOrThrow("APPLICATION_NAME")}"

            buildConfigField("String", "BuildType", "\"debug\"")
        }
        getByName("nightly") {
            matchingFallbacks.add("debug")

            signingConfig = signingConfigs.getByName("dev")
            applicationIdSuffix = ".nightly"
            manifestPlaceholders["appName"] = localProperties.readSecretOrNull("NIGHTLY_APPLICATION_NAME")
                ?: localProperties.readSecretOrThrow("APPLICATION_NAME")
        }
        getByName("safetynet") {
            matchingFallbacks.addAll(listOf("nightly", "debug"))

            signingConfig = signingConfigs.getByName("dev")
            applicationIdSuffix = ".safetynet"
            manifestPlaceholders["appName"] = localProperties.readSecretOrNull("SAFETYNET_APPLICATION_NAME")
                ?: "[Safetynet] ${localProperties.readSecretOrThrow("APPLICATION_NAME")}"
        }
        // The production app for internal testers, installed beside the store app. Built unsigned:
        // the APK is signed outside the build with a hardware-held key.
        create("internal") {
            initWith(getByName("release"))
            matchingFallbacks.add("release")

            // Also sets this module's BuildConfig.DEBUG, which turns on its debug-only code (logcat and
            // file log trees, the shake-to-open debug menu). Library modules keep their release
            // BuildConfig. The first-run notice reads the installed flag, so its warning follows this.
            isDebuggable = true

            signingConfig = null
            versionNameSuffix = "-internal"
            manifestPlaceholders["appName"] = localProperties.readSecretOrNull("INTERNAL_APPLICATION_NAME")
                ?: "[Internal] ${localProperties.readSecretOrThrow("APPLICATION_NAME")}"
        }
    }

    sourceSets.getByName("safetynet").manifest.srcFile("src/nightly/AndroidManifest.xml")

    flavorDimensions += "distribution"

    productFlavors {
        create("gp") { dimension = "distribution" }
        create("vanilla") { dimension = "distribution" }
    }
}

dependencies {
    implementation(libs.hilt.androidx.work)
    ksp(libs.hilt.androidx.compiler)

    implementation(libs.androidx.appcompat)

    implementation(libs.bundles.androidx.navigation)

    implementation(libs.chrisbanes.insetter)

    implementation(libs.kirich.viewbinding)

    implementation(libs.coil.kt)

    implementation(project(":common"))
    implementation(project(":design"))
    implementation(project(":chains"))
    implementation(project(":database"))

    // Region features
    implementation(project(":feature:backup:impl"))
    implementation(project(":feature:xcm:impl"))
    implementation(project(":feature:fund:impl"))
    implementation(project(":feature:swap:impl"))
    implementation(project(":feature:chats:impl"))
    implementation(project(":feature:device-sync:impl"))
    implementation(project(":feature:people:impl"))
    implementation(project(":feature:members:impl"))
    implementation(project(":feature:wallet:impl"))
    implementation(project(":feature:tokens:impl"))
    implementation(project(":feature:splash:impl"))
    implementation(project(":feature:prices:impl"))
    implementation(project(":feature:account:impl"))
    implementation(project(":feature:settings:impl"))
    implementation(project(":feature:balances:impl"))
    implementation(project(":feature:transfers:impl"))
    implementation(project(":feature:usernames:impl"))
    implementation(project(":feature:videogame:impl"))
    implementation(project(":feature:transactions:impl"))
    implementation(project(":feature:statement-store:impl"))
    implementation(project(":feature:transaction-storage:impl"))
    implementation(project(":feature:pgas:impl"))
    implementation(project(":feature:chain-resources:impl"))
    implementation(project(":feature:cross-chain-transfers:impl"))
    implementation(project(":feature:sso:impl"))
    implementation(project(":feature:scan:impl"))
    implementation(project(":feature:products:impl"))
    implementation(project(":feature:calls:api"))
    implementation(project(":feature:calls:impl"))
    implementation(project(":feature:coinage:impl"))
    implementation(project(":feature:dotns:impl"))
    implementation(project(":feature:dotns-gateway:impl"))
    implementation(project(":feature:connection-status:api"))
    implementation(project(":feature:connection-status:impl"))
    implementation(project(":feature:revive:impl"))
    implementation(project(":feature:w3s-pay:impl"))
    // Endregion features

    // Region tools
    implementation(project(":tools:auth:impl"))
    implementation(project(":tools:backup:impl"))
    implementation(project(":tools:integrity:impl"))
    implementation(project(":tools:jwt-auth:impl"))
    implementation(project(":tools:biometrics:impl"))
    implementation(project(":tools:remoteconfig:impl"))
    implementation(project(":tools:ipfs:impl"))
    implementation(project(":tools:assethub-sdk:impl"))
    implementation(project(":tools:hydration-sdk:impl"))
    implementation(project(":tools:push-notifications:impl"))
    implementation(project(":tools:media-connection:impl"))
    implementation(project(":tools:media-connection:api"))
    // Endregion tools

    "gpImplementation"(libs.google.play.services.mlkit)
    "vanillaImplementation"(libs.google.mlkit.barcode.scanning)

    "testInternalImplementation"(libs.junit)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.android.compiler)
}

sentry {
    org.set(localProperties.readSecretOrThrow("SENTRY_ORG"))
    projectName.set(localProperties.readSecretOrThrow("SENTRY_PROJECT"))

    // this will upload your source code to Sentry to show it as part of the stack traces
    // disable if you don't want to expose your sources
    includeSourceContext.set(true)

    // Sentry only runs on debug/nightly; skip release so the plugin
    // doesn't try to process a variant that has no DSN/auth token
    ignoredBuildTypes.set(setOf("release"))

    telemetry.set(false)
}

// The internal build reports to Sentry at runtime but uploads nothing at build time: its lane holds
// no Sentry auth token, and it is not minified, so there is no mapping to upload. Ignoring the build
// type instead would also drop the plugin's bytecode instrumentation and integrations for it.
val sentryBuildTimeUploadTasks = listOf("sentryCollectSources", "sentryBundleSources", "sentryUpload", "uploadSentry")
tasks.configureEach {
    if (name.endsWith("Internal") && sentryBuildTimeUploadTasks.any { name.startsWith(it) }) {
        enabled = false
    }
}

// The release build installs as the production application id, whatever APPLICATION_ID holds.
androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        variant.applicationId.set(
            localProperties.readSecretOrDefault("PRODUCTION_APPLICATION_ID", "io.pcf.polkadotapp")
        )
    }
    // Set here rather than through applicationIdSuffix, so it follows PRODUCTION_APPLICATION_ID too.
    onVariants(selector().withBuildType("internal")) { variant ->
        variant.applicationId.set(
            localProperties.readSecretOrDefault("PRODUCTION_APPLICATION_ID", "io.pcf.polkadotapp") + ".internal"
        )
    }
}
