Documentation:
https://jaxon-j.github.io/Capstone-project/html/

DEVELOPER NOTES

Setting up dev environment
- Download Android Studio: https://developer.android.com/studio
- Download JDK 11 zip: https://adoptium.net/temurin/releases/
  - Make sure it's the JDK 11 *zip* that matches your computer architecture (x64, typically)
- Download ATAK SDK: https://tak.gov/products/atak-civ
  - requires login, under "Downloadable Resources > Developer Resources"
  - this plugin was developed using the SDK versions: ATAK CIV 5.3.0.11-5.4.0.9

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
- Plug in phone to computer under `Photo/image transfer mode`. With Android Studio, under the `Running devices` tab from the right-side tool menu, open your device (it should be mirrored to the screen).
- Click and drag the `atak.apk` file that came in the SDK zip over the screen mirror, and it should install.
- Edit the build configuration such that the "On Launch" option is set to "None" (default is "Default Activity").
- Build the project with the play button on top, and the plugin should be sent to your device which you can now access from ATAK.
- If you do not see it, click the "Plugins" tile on the right pane and make sure the checkbox next to `Loaded` or `Not Loaded` is checked.

Linking SDK JavaDoc
- In the left-side file viewer, select the "Project" view.
- Under `External Libraries`, locate the library that contains `main.jar` when expanded. This is likely called `Gradle: C`.
- Right click the library, go to `Library Properties...`.
- Click the + on the farthest left. Upon hovering, it should say `Add`.
- Navigate to the SDK folder, locate and select `atak-javadoc.jar` and click Ok.
- If prompted, select `JavaDocs` for the item category.
- Click Ok, now the JavaDoc will be listed. Click Ok again, and the JavaDoc is now accessible.
- To access, hover over a token, say `AtakClass` and click `'AtakClass' on Localhost`.
- If there's no pop-up upon hovering over the token, make sure `Settings > Editor > Code Editing > Show quick documentation on hover` is checked.

Setting up an emulator
- Create new virtual device
  - Device Manager on right navbar or Tools > Device Manager
    - Click the "+" then "Create New Device"
  - Choose a device definition
    - I go with the generic "Medium Phone" but this is mostly cosmetic.
  - Select a system image
    - Pick any SDK under the "x86 Images" list with the following attributes:
      - ABI: x86_64
      - Type: Default Android System Image
  - Verify Configuration
    - Click "Show Advanced Settings"
    - VM heap - minimum: 512, recommended: 640
    - Internal storage - minimum: 2048, recommended: 2560
    - The rest of the defaults are fine, most can be changed later.
- Device Manager
  - Click Play
    - 3 dots on the navbar above the emulator screen
    - Settings
      - OpenGL ES renderer: SwiftShader
      - OpenGL ES API level: Renderer maximum (up to OpenGL ES 3.1)
  - Restart the emulator (back to device manager, stop then play)
  - Drag and drop the `atak.apk` file from the SDK folder onto the emulator screen
  
  - In case it's not on the home screen, drag up from the bottom of the screen to get all applications. Click, hold, drag to home screen for a shortcut.
- Once you've opened ATAK, you can run the plugin build, and it will prompt you to add.
- Debugging is going to be done exclusively through logcat (where the `Log.d`, `Log.e` messages go).
  - You can filter to our plugin's logs by filtering by `tag:[tag-prefix]`. The prefix is defined in `Constants.java`

____________________________________________________________________________________________________________________________________________________________
RELEASE NOTES
Milestone 1
Hello world ATAK plugin created and set up for all devs. 

- Bluetooth branch
  In progress: of setting up an initial bluetooth listening program
  Goal: Make a process that continuously listens for bluetooth signatures
  
- UI branch
  In progress: updated wireframe to fit client description
               implementing wireframe to display (nonfunctional)
  Goal: Fully visible UI in accordance to client description

____________________________________________________________________________________________________________________________________________________________
Milestone 2

- Bluetooth branch
  - Implemented scanning
  - Switched scanning to be bluetooth low energy
 
- UI branch
  - Implemented wireframe
  - Started work on basic whitelist
 
- General
  - Refactored to legacy to better utilize public projects

____________________________________________________________________________________________________________________________________________________________
Milestone 3

- UI branch
  - Implemented malluable whitelist
  - Added debug buttons

- Bluetooth branch
  - Implemented whitelist into scanning
  - Refined the scanning process
  - Removed classic bluetooth

- General
  - Implemented a way to define, store, and view device objects across the plugin
  - Refactored code to simplify processes and remove dead code

____________________________________________________________________________________________________________________________________________________________
Milestone 4

UI improvements, input validation, white list visibility toggle.
Implemented displaying of devices on ATAK map.

____________________________________________________________________________________________________________________________________________________________
Milestone 5

Inter-device connectivity configured.
Final touches and tweaks.



Features for the future:
- User Interface and Experience Enhancements 
  - Future iterations of the plugin could focus on refining the user interface and overall user experience based on comprehensive user testing and feedback. This may involve optimizing button layouts and improving visual clarity to make the plugin more intuitive and accessible to a broader range of users. 
- Support for Signal Reader Integration 
  - Incorporating the ability to read from connected hardware that can scan for non-standard or less common radio frequencies (e.g., non-commercial Wi-Fi or Bluetooth spectrums) would expand the plugin’s capabilities. This could bring it closer in functionality to network analysis tools like Wireshark, particularly through features such as Wi-Fi monitor mode and packet sniffing, offering deeper insight into wireless device activity. 
- Context-Aware Iconography Based on MAC Addresses 
  - Enhancing device visualization by associating specific icons with the manufacturer ID from a device’s MAC address can provide users with immediate visual context. This would make it easier to distinguish between types of devices (e.g., consumer electronics vs. industrial hardware) immediately. 
- Advanced Triangulation Techniques 
  - Leveraging more robust triangulation methods using signal strength (RSSI) data from multiple receivers could significantly improve location accuracy. This would enable more precise tracking of devices in real-time, even in complex environments or over extended ranges. 
- Deeper Integration with ATAK Ecosystem 
  - Closer alignment with existing ATAK tools and workflows could improve user efficiency and consistency. This might include syncing with ATAK data layers, interoperating with TAK Server, or adopting standardized symbology and messaging protocols to ensure seamless interoperability. 
- Device Path History Logging 
  - Adding the ability to record and visualize the historical movement paths of detected devices over time could be invaluable for after-action analysis, behavioral pattern recognition, or situational awareness in dynamic operational scenarios. 
- Cross-Platform Compatibility 
  - Expanding support to other TAK environments such as WinTAK (Windows), iTAK (iOS), and any other TAK-compatible platforms would increase the plugin’s versatility and usability across different teams and hardware configurations. This would allow users in different operational contexts to benefit from the plugin’s capabilities without being limited to a single platform. 
