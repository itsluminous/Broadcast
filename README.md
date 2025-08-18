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