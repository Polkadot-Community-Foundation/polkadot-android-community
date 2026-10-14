#!/usr/bin/env bash
#
# Upload an APK to Firebase App Distribution and hand it to tester groups.
#
# Talks to the App Distribution REST API directly, with an OAuth token minted by
# gcloud. That is deliberate: the `firebase-tools` CLI wants a service-account key
# *file*, whereas CI authenticates keylessly through Workload Identity Federation —
# `gcloud auth print-access-token` bridges the two without a long-lived secret.
#
# Note we do NOT send an `x-goog-user-project` header. It is required when calling
# this API with end-user ADC, but with a service account it demands
# `serviceusage.services.use`, which the CI service account intentionally lacks.
#
# Usage:
#   firebase-app-distribution.sh <apk> <firebase-app-id> <tester-groups-csv> [notes-file]
#
# Requires: gcloud (authenticated), curl, jq.

set -euo pipefail

APK=${1:?apk path required}
APP_ID=${2:?firebase app id required}
# NOT named GROUPS: that is a bash special array variable holding the caller's group
# ids, and assignments to it are silently ignored — it would expand to a gid.
TESTER_GROUPS=${3:-}
NOTES_FILE=${4:-}

[ -f "$APK" ] || { echo "::error::APK not found: $APK" >&2; exit 1; }

# App ids look like 1:<project-number>:android:<hash>
PROJECT_NUMBER=${APP_ID#1:}
PROJECT_NUMBER=${PROJECT_NUMBER%%:*}
case "$PROJECT_NUMBER" in
  ''|*[!0-9]*) echo "::error::malformed Firebase app id: $APP_ID" >&2; exit 1 ;;
esac

API=https://firebaseappdistribution.googleapis.com
TOKEN=$(gcloud auth print-access-token)
AUTH=(-H "Authorization: Bearer $TOKEN")

echo "Uploading $(basename "$APK") ($(du -h "$APK" | cut -f1)) to app $APP_ID ..."
upload=$(curl -sS --fail-with-body -X POST "${AUTH[@]}" \
  -H "X-Goog-Upload-File-Name: $(basename "$APK")" \
  -H "X-Goog-Upload-Protocol: raw" \
  -H "Content-Type: application/octet-stream" \
  --data-binary "@$APK" \
  "$API/upload/v1/projects/$PROJECT_NUMBER/apps/$APP_ID/releases:upload")

operation=$(jq -r '.name // empty' <<<"$upload")
[ -n "$operation" ] || { echo "::error::upload did not return an operation: $upload" >&2; exit 1; }

# Processing a ~200 MB APK takes well under a minute, but give it room.
echo "Waiting for release processing ..."
for _ in $(seq 1 120); do
  op=$(curl -sS --fail-with-body "${AUTH[@]}" "$API/v1/$operation")
  [ "$(jq -r '.done // false' <<<"$op")" = "true" ] && break
  sleep 5
done

if [ "$(jq -r '.done // false' <<<"$op")" != "true" ]; then
  echo "::error::release processing did not finish in time (operation $operation)" >&2
  exit 1
fi
if [ "$(jq -r 'has("error")' <<<"$op")" = "true" ]; then
  echo "::error::release processing failed: $(jq -c '.error' <<<"$op")" >&2
  exit 1
fi

release=$(jq -r '.response.release.name' <<<"$op")
result=$(jq -r '.response.result' <<<"$op")
build_version=$(jq -r '.response.release.buildVersion' <<<"$op")
display_version=$(jq -r '.response.release.displayVersion' <<<"$op")
testing_uri=$(jq -r '.response.release.testingUri' <<<"$op")
console_uri=$(jq -r '.response.release.firebaseConsoleUri' <<<"$op")
expire_time=$(jq -r '.response.release.expireTime' <<<"$op")

# RELEASE_UNMODIFIED means an identical binary was already uploaded — not an error;
# we still refresh the notes and re-distribute so a re-run is idempotent.
echo "$result: $display_version ($build_version)"

if [ -n "$NOTES_FILE" ] && [ -f "$NOTES_FILE" ]; then
  echo "Setting release notes ..."
  jq -n --rawfile text "$NOTES_FILE" '{releaseNotes: {text: $text}}' \
    | curl -sS --fail-with-body -o /dev/null -X PATCH "${AUTH[@]}" \
        -H "Content-Type: application/json" --data-binary @- \
        "$API/v1/$release?updateMask=release_notes.text"
fi

if [ -n "$TESTER_GROUPS" ]; then
  groups_json=$(jq -cn --arg g "$TESTER_GROUPS" \
    '$g | split(",") | map(gsub("^\\s+|\\s+$";"")) | map(select(length > 0))')
  echo "Distributing to groups: $groups_json"
  jq -cn --argjson groups "$groups_json" '{groupAliases: $groups}' \
    | curl -sS --fail-with-body -o /dev/null -X POST "${AUTH[@]}" \
        -H "Content-Type: application/json" --data-binary @- \
        "$API/v1/$release:distribute"
else
  echo "No tester groups given — release uploaded but not distributed."
fi

if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
  {
    echo "### Firebase App Distribution"
    echo "- $result — **$display_version** (build \`$build_version\`)"
    echo "- Groups: \`${TESTER_GROUPS:-none}\`"
    echo "- Tester link: $testing_uri"
    echo "- Console: $console_uri"
    echo "- Binary expires: $expire_time (App Distribution retains releases ~150 days)"
  } >> "$GITHUB_STEP_SUMMARY"
fi
