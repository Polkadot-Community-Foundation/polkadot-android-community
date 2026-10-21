# PCF changes on top of upstream

The `production` branch is an upstream release tag of
[paritytech/polkadot-android-community](https://github.com/paritytech/polkadot-android-community)
plus the changes listed here. Upstream base: `v1.0.0-1063`.

On every upstream sync, keep each change below unless its "Retire when" condition holds, and
re-run its check against the new tag to see whether upstream still needs it. `TAG` is the
upstream tag being merged.

## Deployment

These stay for as long as PCF builds this app.

| Change | Files |
|---|---|
| Production build lane: PCF application id, signing, private artifact bucket | `.github/workflows/build-production.yml`, `app/build.gradle.kts` |
| Upstream publishing workflows reduced to manual dispatch | `.github/workflows/firebase_debug_distribution.yml`, `firebase_release_distribution.yml`, `nightly_release.yaml`, `genesis_hash_monitor.yml` |
| PR checks fall back to PCF variables and secrets | `.github/workflows/pr.yml`, `.github/workflows/detekt.yaml` |

## App fixes

### Cross-chain deposits emitted as `Deposited`

- **Origin:** `57dd1714`
- **File:** `feature/balances/impl/.../assets/AssetsTokenEventDetector.kt`
- **Problem:** current Asset Hub and People runtimes settle incoming XCM teleports and reserve
  transfers with `Deposited`, not `Issued`. Without the fix, Get CASH funding reports
  "No deposits detected".
- **Upstream still affected if:**
  `git show TAG:feature/balances/impl/src/main/java/io/paritytech/polkadotapp/feature_balances_impl/data/type/assets/AssetsTokenEventDetector.kt | grep -c '"Deposited"'`
  prints `0`.
- **Retire when:** upstream matches `Deposited`.

### Swap always reserves the input asset's existential deposit

- **Origin:** `b890fa14`
- **File:** `feature/swap/impl/.../AssetInAdditionalSwapDeductionUseCase.kt`
- **Problem:** AssetConversion withdraws the input with `Preservation::Preserve`. When a
  sufficient output asset exists, upstream sizes the swap to the full balance. The extrinsic
  then fails with `Token::NotExpendable`, and Get CASH auto-convert retries forever.
- **Upstream still affected if:** `git show TAG:<file> | grep -c assetOutCanProvideSufficiency`
  is non-zero.
- **Retire when:** upstream always keeps the existential deposit of the input asset.

### Gateway URL with a trailing slash

- **Origin:** `c517e467`
- **File:** `tools/ipfs/impl/.../RealIpfsContentLookup.kt`
- **Problem:** the Remote Config `ipfs_gateway_url` ends in `/`, which iOS needs. Android
  concatenates the URL and the CID, producing `/ipfs//<cid>`.
- **Upstream still affected if:** `git show TAG:<file> | grep -c "trimEnd('/')"` prints `0`.
- **Retire when:** upstream normalises the URL, or the gateway URL is set per platform without
  the slash.

### Stored P-256 chat keys do not crash the app

- **Origin:** `570d6cbf`
- **Files:**
  - `database/.../cleanup/LegacyEcdhKeyCleanup.kt` (a `RoomDatabase.Callback`, not a migration)
  - `database/.../AppDatabase.kt`
  - the chat and SSO mappers and repositories under `feature/chats/impl` and `feature/sso/impl`
- **Problem:** installs upgraded in place across the P-256 → X25519 change crash on launch on
  65-byte keys. No production build ever stored P-256 keys, so on production this is a safety
  net: reads that would throw are mapped to `null` and logged instead.
- **Upstream still affected if:**
  `git show TAG:feature/chats/impl/src/main/java/io/paritytech/polkadotapp/feature_chats_impl/data/repository/ContactDevicesRepository.kt | grep -c requireX25519PublicKey`
  is non-zero (that mapper throws on a malformed stored key).
- **Retire when:** upstream read boundaries no longer throw on malformed stored keys.

### New accounts are not put into the coinage restore gate

- **Origin:** `a1e09e90`
- **File:** `feature/usernames/impl/.../ClaimUsernameViewModel.kt`
- **Problem:** when `recoverUsername()` fails for a freshly created account, `isNewAccount`
  stays `false`. The coinage restore gate then greys out Get CASH and the drip.
- **Upstream still affected if:** `git show TAG:<file> | grep -A3 "BackupOutcome.NoNeedToBackup"`
  does not call `saveIsNewAccount()`.
- **Retire when:** upstream marks new accounts at the create outcome.

### `sr25519-vrf` dependency graph pinned

- **Origin:** `b2e2cdbb`
- **File:** `bindings/sr25519-vrf/rust/Cargo.lock`, plus its removal from `.gitignore`
- **Problem:** without a lockfile the Rust binding resolves dependencies freshly on every build.
- **Upstream still affected if:** `git cat-file -e TAG:bindings/sr25519-vrf/rust/Cargo.lock`
  fails.
- **Retire when:** upstream commits the lockfile.

## Verifying a sync

The fork changes before the sync are `git diff OLD_TAG origin/production`; after it,
`git diff TAG HEAD`. Every file in the first list must still appear in the second, with its
changes intact, unless this file says it was retired. When an entry's check shows upstream no
longer needs it, drop the change and remove the entry in the same PR.
