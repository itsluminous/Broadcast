# Broadcast App Implementation Plan

This document outlines the step-by-step plan to build the Broadcast app as per the requirements.

## 1. Core Features

- **Broadcast Lists:** Create, view, edit, and delete named groups of contacts with an associated emoji icon.
- **Contact Management:** Select contacts from the user's device to add to a list.
- **Message Composition:** A dedicated screen to write a text message and optionally attach one image or video.
- **WhatsApp Integration:** Send the composed message and media to the selected contacts via WhatsApp.
- **Dual Send Flows:**
    1.  Send to a single, specific broadcast list.
    2.  Compose a message first, then select one or more broadcast lists to send to.

## 2. Technical Implementation Plan

The application will be built using modern Android development practices.

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Architecture:** MVVM (Model-View-ViewModel)
- **Data Persistence:** Room Database
- **Navigation:** Jetpack Navigation for Compose
- **Permissions & Media:** Modern Activity Result APIs

---

### Step 1: Project Setup & Dependencies

1.  **Add Dependencies:** Update the `app/build.gradle.kts` file to include the necessary libraries:
    -   Jetpack Compose (UI, Material3, Tooling)
    -   Navigation for Compose
    -   ViewModel for Compose
    -   Room (Compiler, KTX, Runtime)
    -   Coroutines for asynchronous operations.

### Step 2: Data Model and Persistence (Room)

1.  **Create Entities:** Define the database tables as Kotlin data classes.
    -   `BroadcastList`: Will store `id`, `name`, and `iconEmoji`.
    -   `Contact`: Will store the contact's `id` and `phoneNumber`.
    -   `BroadcastListContactCrossRef`: A linking table to manage the many-to-many relationship between lists and contacts.
2.  **Create DAO (Data Access Object):** Define the interface (`BroadcastDao`) with methods to insert, update, delete, and query the database. This will include queries to get lists with their contacts.
3.  **Create Database Class:** Create the abstract `AppDatabase` class that extends `RoomDatabase`.

### Step 3: Main Screen - Displaying Broadcast Lists

1.  **Create `BroadcastListViewModel`:** This ViewModel will fetch all broadcast lists from the Room database and expose them as a StateFlow for the UI to observe.
2.  **Create `MainScreen` Composable:**
    -   This screen will observe the lists from the ViewModel.
    -   It will display a list of `BroadcastList` items. Each item will show the list name and emoji.
    -   Implement a `Scaffold` with a `MultiFloatingActionButton` containing two FABs:
        -   A `+` icon to navigate to the "Create List" screen.
        -   A `Send` icon to navigate to the "Compose Message" screen.
3.  **Click Handling:** When a list item is clicked, show a dialog with "Edit" and "Send Message" options.

### Step 4: Create/Edit Broadcast List Flow

1.  **Permissions:** Implement `READ_CONTACTS` permission handling using the modern Activity Result APIs. This will be triggered before showing the contact picker.
2.  **Contact Picker Screen:**
    -   Create a `ContactPickerViewModel` to fetch all contacts from the device's `ContentResolver`.
    -   Create a `ContactPickerScreen` composable that displays a list of device contacts with checkboxes. It will allow the user to select/deselect multiple contacts.
3.  **Create/Edit List Screen:**
    -   Create a `CreateEditListViewModel` to handle the logic of saving or updating a broadcast list.
    -   Create a `CreateEditListScreen` composable with:
        -   A `TextField` for the broadcast list name.
        -   A simple emoji picker.
        -   A button to save the list, which will persist the list name, emoji, and the selected contacts to the Room database.
    -   In "Edit" mode, this screen will be pre-populated with the existing list data.

### Step 5: Compose Message and Send Flow

1.  **Create `ComposeMessageViewModel`:** This will hold the state for the message text and the URI of any attached media.
2.  **Create `ComposeMessageScreen`:**
    -   A large `TextField` for the message.
    -   A button to "Add Image/Video" which will use the `PickVisualMedia` Activity Result Contract.
    -   A "Send" button.
3.  **Implement Send Logic:**
    -   **Flow 1 (From a single list):** Clicking "Send" will navigate directly to the WhatsApp integration step with the contacts from that specific list.
    -   **Flow 2 (From main FAB):** Clicking "Send" will first open a `BroadcastListPicker` dialog/screen, allowing the user to select multiple lists. After confirmation, it will proceed to the WhatsApp integration step with contacts from all selected lists.

### Step 6: WhatsApp Integration

1.  **Create `WhatsAppHelper`:** A utility object or class to construct and fire the `Intent` to open WhatsApp.
2.  **Build the Intent:**
    -   The intent will be `Intent.ACTION_SEND`.
    -   Set the package to `"com.whatsapp"`.
    -   Add the message text using `Intent.EXTRA_TEXT`.
    -   If media is attached, add the URI using `Intent.EXTRA_STREAM` and set the appropriate MIME type.
3.  **Limitation Note:** The official WhatsApp API does not allow programmatically selecting multiple recipients. The integration will open the WhatsApp contact selection screen with the message and media pre-filled. The user will then have to manually select the contacts or groups to send the message to. This is a technical limitation to prevent spam. We will provide the list of numbers to the user on our side for easy reference if possible.

### Step 7: Navigation

1.  **Setup `NavHost`:** In the main activity, set up the `NavHost` with all the defined screens (routes).
    -   `main`
    -   `contact_picker/{listId}` (listId is optional)
    -   `create_edit_list/{listId}` (listId is optional)
    -   `compose_message/{listId}` (listId is optional, for single list flow)
    -   `compose_message` (for multi-list flow)
2.  **Pass Arguments:** Configure navigation actions to pass arguments like `listId` between screens.
