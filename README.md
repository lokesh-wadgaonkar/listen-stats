# Listen Stats

Personal, offline listening stats for Poweramp (and any other media app you switch on).
No internet permission, no accounts, no ads. All data stays on the phone.

## Build the APK (GitHub Actions)

1. Push this folder to a GitHub repository.
2. Open the repository, Actions tab, "Build signed APK", Run workflow.
3. When it finishes, download the artifact "ListenStats-signed-apk", unzip it, install `ListenStats-signed.apk`.

The workflow signs every build with the same key (kept in the Actions cache under
`listenstats-keystore-v1`), so new builds install over old ones. GitHub deletes caches that
are not used for 7 days. If a later build refuses to update ("signatures do not match"),
uninstall the app once (export a backup first) and install the new build.

## Restyle

Colors, shapes, typography and spacing are all in
`app/src/main/java/com/listenstats/app/ui/Theme.kt`.
