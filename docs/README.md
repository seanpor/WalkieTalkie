# Walkie Talkie

A simple, local-network audio communication application for Android. This app allows users to turn their devices into walkie-talkies, communicating over a shared Wi-Fi network or a portable hotspot without needing an internet connection.

## Features

*   **Host & Client Mode:** One device can act as a host (server), and other devices can connect as clients.
*   **Hotspot Support:** Works seamlessly when one device creates a Wi-Fi hotspot, allowing for communication in areas without existing network infrastructure.
*   **Open-Mic Communication:** The default mode is a continuous, open-mic conversation, not push-to-talk.
*   **Automatic Audio Switching:** Automatically switches between the device's speaker and a connected Bluetooth headset.

## How to Build and Run

1.  **Clone the repository:**
    ```bash
    git clone <repository-url>
    ```
2.  **Open in Android Studio:**
    Open the project in the latest stable version of Android Studio.
3.  **Sync Gradle:**
    Let Android Studio sync the Gradle files and download all necessary dependencies.
4.  **Build the APK:**
    You can build a debug APK by running the following command in the project's root directory:
    ```bash
    ./gradlew assembleDebug
    ```
5.  **Install the APK:**
    The generated APK will be located at `app/build/outputs/apk/debug/app-debug.apk`. You can install it on your Android device using ADB:
    ```bash
    adb install app/build/outputs/apk/debug/app-debug.apk
    ```

## User Guide

1.  **Connect Devices:** Ensure all devices are connected to the same Wi-Fi network or that one device is acting as a hotspot and the others are connected to it.
2.  **Start the App:** Launch the Walkie Talkie app on all devices.
3.  **Select Mode:**
    *   **On one device (the host):** Tap the "Host" button. The app will display the IP address of the host device.
    *   **On the other devices (the clients):** Tap the "Client" button.
4.  **Connect Client to Host:**
    *   On each client device, enter the IP address displayed on the host device and tap "Connect".
5.  **Communicate:**
    Once connected, all devices can communicate in an open-mic session.

## Improvement Plan

This project is under active development. The current improvement plan includes:

*   Implementing a proper connection handshake and heartbeat.
*   Improving audio quality with echo cancellation and noise suppression.
*   Adding more comprehensive unit and UI tests.
*   Refactoring the codebase to use Hilt for dependency injection.
*   Adding a visual indicator for when a user is speaking.