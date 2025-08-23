[![Latest Release](https://img.shields.io/github/v/release/itsluminous/Broadcast)](https://github.com/itsluminous/Broadcast/releases/latest)
[![CI Pipeline](https://github.com/itsluminous/Broadcast/actions/workflows/android_ci.yml/badge.svg)](https://github.com/itsluminous/Broadcast/actions/workflows/android_ci.yml)

# Broadcast

Broadcast is an Android application that simplifies sending WhatsApp messages to custom-created lists of contacts. It streamlines the process of reaching out to multiple people at once, directly from your device.

## Features

- **Create and Manage Broadcast Lists:** Easily create, edit, and delete custom lists of contacts. You can select contacts directly from your phone's address book to add them to a list.
- **Compose and Send Messages:** The app provides a simple interface for composing text messages. You can also attach one image or video to your message.
- **WhatsApp Integration:** Broadcast automates the process of sending messages through WhatsApp. It uses Android's accessibility services to pre-select the contacts and send the message, saving you the manual effort of selecting each contact individually.
- **Flexible Sending Options:**
    - **Send to a Single List:** Quickly send a message to a specific broadcast list.
    - **Compose First, Then Send:** Compose your message and then select one or more broadcast lists to send it to.
- **Share from other apps:** You can share text and media from other apps directly to Broadcast to send them as WhatsApp messages.
- **Backup and Import Broadcast Lists:** Easily backup all your broadcast lists and contacts to a JSON file, and import them back into the app. This allows for easy migration or data recovery.

## How It Works

The app uses the following permissions and services to provide its functionality:

- **Read Contacts:** To allow you to select contacts from your address book and create broadcast lists.
- **Accessibility Service:** To automate the process of sending messages to multiple contacts in WhatsApp. This service is only used for sending messages and does not collect any personal information.

## Getting Started

1.  **Install the app:** Download and install the app on your Android device.
2.  **Grant Permissions:** The app will ask for permission to read your contacts. This is necessary to create broadcast lists.
3.  **Enable Accessibility Service:** The app will guide you to enable its accessibility service. This is required to automate sending messages on WhatsApp.
4.  **Create a Broadcast List:** Go to the main screen and tap the "+" button to create a new broadcast list. Select the contacts you want to add to the list and give it a name.
5.  **Send a Message:**
    -   To send a message to a single list, tap on the list and select "Send Message".
    -   To send a message to multiple lists, tap the "Send" button on the main screen, compose your message, and then select the lists you want to send it to.

## App Signing

To build and install release APKs, you need to sign your application with a keystore. This project uses a `broadcast.keystore` file and GitHub Secrets for secure signing in CI/CD.

### Generating a Keystore Locally

If you are building the app locally or forking the repository, you will need to generate your own keystore.

1.  **Generate the Keystore:** Open your terminal and run the following command. You will be prompted to enter passwords and information for your certificate. **Remember these passwords and the alias.**

    ```bash
    keytool -genkey -v -keystore broadcast.keystore -alias broadcast_alias -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Your Name, OU=Your Org Unit, O=Your Organization, L=Your City, ST=Your State, C=Your Country Code"
    ```

    *   Replace `broadcast.keystore` with your desired keystore filename.
    *   Replace `broadcast_alias` with your desired key alias.
    *   Replace the `dname` values (e.g., "Your Name", "Your Organization") with your actual information.
    *   Ensure your keystore password and key password are at least 6 characters long.

2.  **Secure Your Keystore:** **DO NOT commit `broadcast.keystore` to your Git repository.**

### Setting up GitHub Secrets for CI/CD (for Forks)

If you have forked this repository and want to use GitHub Actions for building and signing your release APKs, you need to set up the following GitHub Secrets:

1.  **Base64 Encode Your Keystore:**
    *   On macOS/Linux:
        ```bash
        base64 -i broadcast.keystore
        ```
    *   On Windows (using PowerShell):
        ```powershell
        [System.Convert]::ToBase64String([System.IO.File]::ReadAllBytes("broadcast.keystore"))
        ```
    *   Copy the entire output.

2.  **Create GitHub Secrets:** Go to your forked repository on GitHub, navigate to `Settings` > `Secrets and variables` > `Actions`, and create three new repository secrets:
    *   `KEYSTORE_FILE_BASE64`: Paste the base64 encoded content of your `broadcast.keystore` file here.
    *   `KEYSTORE_STORE_PASSWORD`: Enter the password you set for your keystore.
    *   `KEYSTORE_KEY_PASSWORD`: Enter the password you set for your key alias.

Once these secrets are configured, your GitHub Actions workflow will be able to sign your release APKs.

## Building the Project Locally

To build the project on your local machine:

1.  **Ensure Keystore is Set Up:** Make sure you have generated a `broadcast.keystore` file as described in the "Generating a Keystore Locally" section above, and that your `gradle.properties` file contains the correct `KEYSTORE_STORE_PASSWORD` and `KEYSTORE_KEY_PASSWORD` values.
2.  **Grant Execute Permissions:** If you haven't already, grant execute permissions to the Gradle wrapper:
    ```bash
    chmod +x gradlew
    ```
3.  **Run the Build Command:**
    *   For a **debug APK**:
        ```bash
        ./gradlew assembleDebug
        ```
    *   For a **release APK** (which will be signed using your local keystore):
        ```bash
        ./gradlew assembleRelease
        ```

### APK Location

After a successful build, the generated APK files can be found in the following directory:

*   `app/build/outputs/apk/`
    *   Debug APKs: `app/build/outputs/apk/debug/app-debug.apk`
    *   Release APKs: `app/build/outputs/apk/release/app-release.apk` (or similar, depending on signing and renaming steps)