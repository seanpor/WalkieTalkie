# Project Improvement Plan

This document outlines the plan for improving the Walkie Talkie application, broken down into phased deliverables.

## Phase 1: Code Cleanup & Refactoring (Completed)

This phase addresses immediate issues to improve stability and maintainability.

-   **[x] Permissions:** Remove unnecessary permissions from `AndroidManifest.xml` (`ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`) to follow the principle of least privilege.
-   **[x] Resource Management:** Add `try-finally` blocks in `Audio.kt` to ensure `audioRecord` and `audioTrack` are always released, even if an error occurs.
-   **[x] Specific Error Handling:** Replace generic `catch (e: Exception)` blocks with specific exceptions (`IOException`, `SocketException`, etc.) in `Server.kt`, `Client.kt`, and `WalkieTalkieService.kt` to make debugging easier.

## Phase 2: UI/UX Enhancements

This phase focuses on making the app more user-friendly.

-   **[ ] Connection Status:** Provide clear, real-time feedback on the UI (e.g., "Connecting...", "Connected", "1 Client Connected").
-   **[ ] Disconnect Button:** Add a "Disconnect" or "End Session" button that allows users to gracefully leave a session and return to the main screen.
-   **[ ] IP Address Handling:**
    -   For the **Host**, automatically detect and display the correct IP address for the Wi-Fi hotspot.
    -   For the **Client**, validate the entered IP address format to prevent connection errors.
-   **[ ] Visual Feedback:** Add a UI element (like a glowing microphone icon) that visually indicates when the app is actively recording and transmitting audio.

## Phase 3: Core Functionality & Robustness

This phase improves the core features, focusing on the specific use case with Bluetooth headsets.

-   **[ ] Bluetooth Headset Support:** Enhance the audio routing logic in `Audio.kt` to reliably detect and prioritize Bluetooth SCO (Synchronous Connection-Oriented) audio streams when a headset is connected. This is crucial for a hands-free experience.
-   **[ ] Network Service Discovery (NSD):** Implement NSD to allow clients to automatically discover the host on the network. This would be a major improvement, as users would no longer need to manually type in an IP address.
-   **[ ] Audio Quality Improvements:** Research and potentially implement a voice-optimized audio codec (like Opus or Speex) instead of raw PCM. This would reduce network bandwidth and could improve audio quality, especially with multiple clients connected.

## Phase 4: Testing

This phase introduces a proper testing suite to ensure quality and prevent regressions.

-   **[ ] Unit Tests:**
    -   Write unit tests for the `MainViewModel` to verify screen state transitions.
    -   Test utility functions, such as the IP address validator.
-   **[ ] Instrumentation Tests:**
    -   Create basic UI tests to verify navigation between screens.
    -   Mock the `Server` and `Client` classes to test the service logic without requiring a live network connection.
