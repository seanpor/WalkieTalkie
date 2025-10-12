# Walkie Talkie

A local-network, serverless, hands-free voice communication app for Android.

## Use Case

This application is designed for hands-free voice communication between multiple people in close proximity, especially in environments where internet access is unavailable. It's ideal for activities such as:

*   **Yachting:** Communicate with crew members across a vessel.
*   **Motorbiking:** Talk with other riders in your group.
*   **Hiking/Camping:** Stay in touch with your group in remote areas.

The app works by using one phone to create a Wi-Fi hotspot. All other participants connect to this hotspot, allowing the app to transmit voice data over the local network without needing an internet connection.

## How It Works

The app uses a simple Host/Client model:

*   **Host:** One user starts a session by tapping "Host". Their phone acts as the central server for the voice chat. The app will display the Host's IP address on the hotspot network.
*   **Client:** All other users join the session by tapping "Client" and entering the IP address displayed on the Host's phone.

Once connected, all users can speak freely. The app continuously records audio and broadcasts it to all other participants in the session.

## How to Use

1.  **Start a Hotspot:** One person enables the Wi-Fi hotspot on their Android phone.
2.  **Connect to Hotspot:** All other participants connect their phones to that Wi-Fi hotspot.
3.  **Start the Host:** The person who created the hotspot opens the Walkie Talkie app and taps the **Host** button. The app will show their local IP address (e.g., `192.168.43.1`).
4.  **Connect Clients:** Everyone else opens the app, taps the **Client** button, enters the Host's IP address, and taps "Connect".
5.  **Talk:** Once connected, you can start talking. The app is designed for hands-free use, so there is no push-to-talk button.

## Building From Source

### Prerequisites

*   Android Studio (latest version recommended)
*   Java Development Kit (JDK) 11 or higher

### Build Command

Open the project in Android Studio, or run the following command in the project's root directory to build the application:

```bash
./gradlew assembleDebug
```

The built APK will be located in `app/build/outputs/apk/debug/`.
