# Gradle MSIX Plugin

![Kotlin](https://img.shields.io/badge/kotlin-2.3.0-blue.svg?logo=kotlin)
[![License: AGPL v3](https://img.shields.io/badge/license-AGPL--3.0-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)
[![GitHub Sponsors](https://img.shields.io/badge/Sponsor-gray?&logo=GitHub-Sponsors&logoColor=EA4AAA)](https://github.com/sponsors/StefanOltmann)

Create MSIX packages from a Compose Desktop release build with a single task.
The plugin renders MSIX PNG resources from a single SVG, writes an `AppxManifest.xml`,
and packs the release output into an MSIX using `makeappx.exe`.

## Requirements

- Windows host with the Windows SDK installed (for `makeappx.exe`).
- Compose Desktop `createReleaseDistributable` task available in the target project.

Note that the Windows GitHub runner comes with the Windows SDK installed.

### Install makeappx.exe

To install the Windows SDK components needed by `makeappx.exe`:

1. Download and install the Windows SDK from https://learn.microsoft.com/de-de/windows/apps/windows-sdk/downloads
2. Run the Windows SDK setup executable: `winsdksetup.exe /features OptionId.DesktopCPPx64 /quiet /norestart`
3. Confirm the UAC dialog and wait a minute while the installation takes place.

## Usage

Apply the plugin and configure the MSIX manifest values:

```kotlin
plugins {
    id("de.stefan-oltmann.gradle-msix-plugin") version "0.2.0"
}

msix {

    /*
     * Optional: defaults to packaging/msix/resources/AppIcon.svg
     */
    svgIcon.set(layout.projectDirectory.file("packaging/msix/resources/AppIcon.svg"))

    /*
     * Optional: enable signing by pointing at a PFX file.
     * 
     * Also see the notes below regarding configuration from environment variables.
     */
    signingPfx.set(layout.projectDirectory.file("packaging/msix/sign.pfx"))
    signingPassword.set("test")

    manifest {
        appId.set("MyApp")
        displayName.set("My App")
        description.set("Short description for Windows.")
        identityName.set("YourName.MyApp")
        publisher.set("CN=YOUR-PUBLISHER-ID")
        publisherDisplayName.set("My Company")
        version.set("1.0.0.0")
        processorArchitecture.set("x64")
        appExecutable.set("MyApp.exe")
    }
}
```

Run the packaging task:

```
./gradlew :app:createMsix
```

## Tasks

- `createMsix` - Runs `createReleaseDistributable`, creates resources + manifest, packs the MSIX, and signs it when a PFX is configured.
- `createMsixIcons` - Creates the MSIX PNG resources from the SVG.
- `createAppxManifest` - Writes AppxManifest.xml into the app directory.

## Use from environment variables

Signing can also be driven by environment variables, which is handy for CI/CD systems such as GitHub Actions.

- If `msix.signingPfx` is not set (or the file is missing), the plugin will look for `MSIX_SIGN_PFX_BASE64`.
- The password can be supplied either via `msix.signingPassword` or `MSIX_SIGN_PFX_PASSWORD` (useful for keeping secrets out of VCS).

On Windows using PowerShell you can turn your PFX file into base64 like this:

```
[Convert]::ToBase64String([IO.File]::ReadAllBytes("sign.pfx"))
```

Example for GitHub Actions:

```yaml
env:
    MSIX_SIGN_PFX_BASE64: ${{ secrets.MSIX_SIGN_PFX_BASE64 }}
    MSIX_SIGN_PFX_PASSWORD: ${{ secrets.MSIX_SIGN_PFX_PASSWORD }}
```
