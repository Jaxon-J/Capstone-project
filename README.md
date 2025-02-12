(Plugin Name)


_________________________________________________________________
PURPOSE AND CAPABILITIES

(General Description)


_________________________________________________________________
STATUS

(In Progress?  Expected release?  Released?  To Who?  When?)

_________________________________________________________________
POINT OF CONTACTS

(Who is developing this)

_________________________________________________________________
PORTS REQUIRED

(This is important for ATO, networking, and other security concerns)

_________________________________________________________________
EQUIPMENT REQUIRED

_________________________________________________________________
EQUIPMENT SUPPORTED

_________________________________________________________________
COMPILATION

_________________________________________________________________
DEVELOPER NOTES

Setting up dev environment
- Download Android Studio: https://developer.android.com/studio
- Download JDK 11 zip: https://adoptium.net/temurin/releases/
  - Make sure it's the JDK 11 *zip* that matches your computer architecture (x64, typically)
- Download ATAK SDK: https://tak.gov/products/atak-civ
  - requires login, under "Downloadable Resources > Developer Resources"

- Unzip ATAK SDK, within the SDK folder, create a `plugins` folder, then clone this repository into that folder.
```
<SDK-ROOT-FOLDER>
 |--docs
 |--espresso
 |--gradle
 |--license
 |--plugins
    |--<CLONED REPO HERE>
 |--samples
 |--<more files>
```
- Open Android Studio, and open the cloned repo folder as the project.
- Go to `SDK Manager...` (cog icon on top right), make sure Andoid SDK's of API level `34` and `21` are installed.
- Go to `File > Settings > Build, Execution, Deployment > Build Tools > Gradle`
  - Under the `Gradle JDK` dropdown, click `Add JDK from Disk...`, navigate to an unzipped root of the JDK folder (the parent directory of `bin`, `config`, etc) and add it.
- It should be able to build now, you can test this by clicking the `Sync Project with Gradle Files` icon in the top right, or simply `Ctrl+Shift+O`
- Open the terminal by clicking on the icon on the bottom left, and generate `release.keystore` and `debug.keystore` keystores by running the following commands:
    - `keytool -genkeypair -dname "CN=Android Debug,O=Android,C=US" -validity 9999 -keystore debug.keystore -alias androiddebugkey -keypass android -storepass android`
    - `keytool -genkeypair -dname "CN=Android Release,O=Android,C=US" -validity 9999 -keystore release.keystore -alias androidreleasekey -keypass android -storepass android`
- Open up `local.properties`, modify it such that it looks like the following:
```
sdk.dir=<SHOULD BE ALREADY GENERATED>
takDebugKeyFile=<ABSOLUTE_PLUGIN_PATH>\\debug.keystore # example: C\:\\absolute\\path\\to\\debug.keystore
takDebugKeyFilePassword=android
takDebugKeyAlias=androiddebugkey
takDebugKeyPassword=android

takReleaseKeyFile=<ABSOLUTE_PLUGIN_PATH>\\release.keystore # example: C\:\\absolute\\path\\to\\release.keystore
takReleaseKeyFilePassword=android
takReleaseKeyAlias=androidreleasekey
takReleaseKeyPassword=android
```
- Under the `Build Variant` menu (in the triple dot menu on left sidebar), make sure `:app` is set to `civDebug`.

Setting up Android device
- If you have an Android phone you wish to develop with, make sure USB Debugging is enabled (the steps for this proces is very Android version-dependent. Google around to see how to do this.)
- If you have installed ATAK-Civ from Google Play Store, uninstall it from your device.
- Plug in phone to computer under `File Transfer Mode`, copy the `atak.apk` file that came with the ATAK SDK to the `Downloads` folder under your device.
- On your device, navigate to the Downloads folder and click on the `atak.apk`, it should install. This is a development build of ATAK.
- To get Android Studio to detect your device, it needs to be connected to the computer under `Photo Transfer Mode`.
- Select your phone from the dropdown on the top, click the Play button aka `Run`.
- Open ATAK, in the side menu go under `Plugins`, click the refresh button, load and enable the plugin.
