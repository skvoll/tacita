# Tacita

Tacita sets the media volume when the phone screen turns off, then checks again one minute later in case another app starts playback during the lock transition. It never raises the current volume. It leaves the volume alone whenever a Bluetooth audio device is connected by default. Monitoring is opt-in and is disabled on a fresh install.

## Build

Open this folder in Android Studio, allow it to install the Android SDK and Gradle dependencies, then run the `app` configuration on an Android 8.0 (API 26) or later device. The project compiles and targets Android 17 (API 37), which needs Android Gradle Plugin 9.1.1 and Gradle 9.3.1.

When you enable monitoring, Tacita requests notification permission on Android 13+. The ongoing notification is required by Android so the app can reliably receive screen-off events in the background.

## Release identity

- Version: `0.1` (version code `1`)

## Privacy policy

The policy source is [docs/privacy-policy/index.html](docs/privacy-policy/index.html). After GitHub Pages is enabled and the first deployment succeeds, the public URL is <https://skvoll.github.io/tacita/privacy-policy>. Use that URL wherever a public privacy policy is required. The site root redirects to the policy; GitHub Pages may add a trailing slash to the policy URL.

### Publishing

1. In the repository's **Settings → Pages → Build and deployment**, select **GitHub Actions** as the source. See [GitHub's setup instructions](https://docs.github.com/en/pages/getting-started-with-github-pages/configuring-a-publishing-source-for-your-github-pages-site#publishing-with-a-custom-github-actions-workflow).
2. Push the policy and `.github/workflows/pages.yml` to `main`. The **Publish privacy policy** workflow deploys only the `docs/` directory. Later pushes affecting `docs/**` or that workflow publish updates automatically.
3. To publish manually, run **Publish privacy policy** from the Actions tab with `main` selected. The workflow's deployment environment links to the published site.

The workflow uses GitHub's built-in token; no custom secret, site generator, or Android build is needed. It only deploys from `main`, including manual runs. If the default branch changes, update both the push branch filter and the job condition in the workflow.

### Editing and previewing

Edit `docs/privacy-policy/index.html` and update its “Last updated” date when the policy changes. Keep its descriptions aligned with the manifest, settings repository, monitoring service, audio controller, dependencies, and backup rules. The app's local data practices are separate from GitHub Pages hosting and voluntary support emails.

To preview locally, run `python3 -m http.server 8000 --bind 127.0.0.1 --directory docs` from the repository root, then open <http://127.0.0.1:8000/privacy-policy>. Both pages share `docs/styles.css` and the app's artwork in `docs/assets/tacita.png`, matching the feature graphic's navy, gold, and cream palette. The layout adapts to mobile screens and printing, and needs no JavaScript or external assets. If the app's header artwork changes, update the website copy too.

## Settings

- **Target volume:** target media-stream volume, default `5%`.
- **Include Bluetooth devices:** when enabled, apply the rule even with a Bluetooth audio output connected; default `false`.
- **Check when screen turns on:** when enabled, perform an additional volume check as the screen turns on; default `false`.

## Architecture

- `data`: persisted settings and their repository.
- `domain`: platform-independent volume decisions, covered by JVM unit tests.
- `platform`: Android audio APIs and connected-device detection.
- `service`: screen-event monitoring and delayed check scheduling.
- `ui`: Compose theme and settings presentation; `MainActivity` coordinates UI actions, permission, and service lifetime.
