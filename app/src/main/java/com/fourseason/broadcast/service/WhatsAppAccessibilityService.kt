package com.fourseason.broadcast.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class WhatsAppAccessibilityService : AccessibilityService() {

    private val TAG = "WhatsAppAccessibility"
    private val WHATSAPP_PACKAGE_NAME = "com.whatsapp"

    // --- UI Element Identifiers ---
    private val CONTACT_PICKER_SEND_FAB_ID = "com.whatsapp:id/fab"
    private val MEDIA_PREVIEW_SEND_BUTTON_ID = "com.whatsapp:id/send"
    private val CHAT_MESSAGE_ENTRY_ID = "com.whatsapp:id/entry"
    private val CHAT_SEND_BUTTON_ID = "com.whatsapp:id/send"
    private val WHATSAPP_TOOLBAR_ID = "com.whatsapp:id/toolbar"

    // --- State Machine ---
    private enum class State { IDLE, AWAITING_UI, ON_MEDIA_PREVIEW, ON_CHAT_SCREEN, AWAITING_HOME }
    private var currentState = State.IDLE

    // --- Queue Management ---
    private var phoneNumbersQueue: MutableList<String> = mutableListOf()
    private var messageToSend: String? = null
    private var mediaUriString: String? = null
    private var isProcessingQueue: Boolean = false
    private var currentPhoneNumber: String? = null

    // --- Timeout for Contact Not Found ---
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private var contactTimeoutRunnable: Runnable? = null
    private val CONTACT_OPEN_TIMEOUT_MS = 7000L

    companion object {
        fun startSendQueue(context: Context, numbers: List<String>, message: String, mediaUri: Uri?) {
            Toast.makeText(context, "Processing numbers: ${numbers.joinToString(", ")}", Toast.LENGTH_LONG).show()
            val intent = Intent(context, WhatsAppAccessibilityService::class.java).apply {
                action = "ACTION_SEND_BULK"
                putStringArrayListExtra("EXTRA_PHONE_NUMBERS", ArrayList(numbers))
                putExtra("EXTRA_MESSAGE", message)
                mediaUri?.let { putExtra("EXTRA_MEDIA_URI", it.toString()) }
            }
            context.startService(intent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_SEND_BULK") {
            val numbers = intent.getStringArrayListExtra("EXTRA_PHONE_NUMBERS")
            val message = intent.getStringExtra("EXTRA_MESSAGE")
            val mediaUri = intent.getStringExtra("EXTRA_MEDIA_URI")

            if (numbers != null && message != null) {
                phoneNumbersQueue.clear()
                phoneNumbersQueue.addAll(numbers)
                messageToSend = message
                mediaUriString = mediaUri
                if (!isProcessingQueue) {
                    isProcessingQueue = true
                    processNextMessage()
                }
            }
        }
        return START_STICKY
    }

    private fun processNextMessage() {
        contactTimeoutRunnable?.let { timeoutHandler.removeCallbacks(it) }

        if (phoneNumbersQueue.isEmpty()) {
            showToast("All messages processed!")
            isProcessingQueue = false
            currentState = State.IDLE
            stopSelfIfIdle()
            return
        }

        currentPhoneNumber = phoneNumbersQueue.removeAt(0)
        val phoneNumber = currentPhoneNumber ?: return
        
        showToast("Processing: $phoneNumber")
        currentState = State.AWAITING_UI

        contactTimeoutRunnable = Runnable {
            if (currentState == State.AWAITING_UI) {
                showToast("Failed: Contact not found for $phoneNumber")
                performGlobalAction(GLOBAL_ACTION_BACK)
                Handler(Looper.getMainLooper()).postDelayed({ processNextMessage() }, 1000)
            }
        }
        timeoutHandler.postDelayed(contactTimeoutRunnable!!, CONTACT_OPEN_TIMEOUT_MS)

        val intentToLaunch = createIntentForContact(phoneNumber)
        try {
            startActivity(intentToLaunch)
        } catch (e: Exception) {
            contactTimeoutRunnable?.let { timeoutHandler.removeCallbacks(it) }
            showToast("Error opening WhatsApp for $phoneNumber")
            Handler(Looper.getMainLooper()).postDelayed({ processNextMessage() }, 1000)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isProcessingQueue) return
        val rootNode = rootInActiveWindow ?: return

        when (currentState) {
            State.AWAITING_UI -> {
                contactTimeoutRunnable?.let { timeoutHandler.removeCallbacks(it) }
                if (isMediaPreviewScreen(rootNode)) {
                    showToast("Sending media...")
                    findAndClickNodeById(rootNode, MEDIA_PREVIEW_SEND_BUTTON_ID)
                    currentState = State.ON_MEDIA_PREVIEW
                } else if (isChatScreen(rootNode)) {
                    showToast("Sending message...")
                    findAndClickNodeById(rootNode, CHAT_SEND_BUTTON_ID)
                    currentState = State.ON_CHAT_SCREEN
                } else if (isContactPickerScreen(rootNode)) {
                    showToast("Verifying contact...")
                    findAndClickNodeById(rootNode, CONTACT_PICKER_SEND_FAB_ID)
                }
            }
            State.ON_MEDIA_PREVIEW, State.ON_CHAT_SCREEN -> {
                // After sending, we land on the chat screen. Wait for the send button to disappear.
                if (isChatScreen(rootNode) && !isSendButtonPresent(rootNode)) {
                    showToast("Success! Navigating back...")
                    pressBackButton(rootNode)
                    currentState = State.AWAITING_HOME
                }
            }
            State.AWAITING_HOME -> {
                if (!isChatScreen(rootNode)) {
                    currentState = State.IDLE // Prevent re-entry
                    Handler(Looper.getMainLooper()).postDelayed({ processNextMessage() }, 500)
                }
            }
            State.IDLE -> { /* Wait for processNextMessage to start the cycle */ }
        }
        rootNode.recycle()
    }

    // --- Screen Identification & Actions ---

    private fun isContactPickerScreen(node: AccessibilityNodeInfo) =
        node.findAccessibilityNodeInfosByViewId(CONTACT_PICKER_SEND_FAB_ID).isNotEmpty() &&
        !isChatScreen(node) && !isMediaPreviewScreen(node)

    private fun isMediaPreviewScreen(node: AccessibilityNodeInfo) =
        node.findAccessibilityNodeInfosByViewId(MEDIA_PREVIEW_SEND_BUTTON_ID).isNotEmpty() &&
        !node.findAccessibilityNodeInfosByViewId(CHAT_MESSAGE_ENTRY_ID).isNotEmpty()

    private fun isChatScreen(node: AccessibilityNodeInfo) =
        node.findAccessibilityNodeInfosByViewId(CHAT_MESSAGE_ENTRY_ID).isNotEmpty()

    private fun isSendButtonPresent(node: AccessibilityNodeInfo): Boolean {
        return findAndClickNodeById(node, CHAT_SEND_BUTTON_ID, performClick = false)
    }

    private fun pressBackButton(node: AccessibilityNodeInfo) {
        val toolbars = node.findAccessibilityNodeInfosByViewId(WHATSAPP_TOOLBAR_ID)
        val backButton = toolbars.asSequence()
            .flatMap { toolbar -> (0 until toolbar.childCount).map { toolbar.getChild(it) } }
            .find { it?.className == "android.widget.ImageButton" && it.isClickable }
        
        toolbars.forEach { it.recycle() }
        if (backButton != null) {
            backButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            backButton.recycle()
        } else {
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    private fun findAndClickNodeById(node: AccessibilityNodeInfo, viewId: String, performClick: Boolean = true): Boolean {
        val nodes = node.findAccessibilityNodeInfosByViewId(viewId)
        val found = nodes.any { n -> n != null && n.isClickable && n.isEnabled }
        if (performClick && found) {
            nodes.first { n -> n != null && n.isClickable && n.isEnabled }
                 .performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        nodes.forEach { it?.recycle() }
        return found
    }

    private fun createIntentForContact(phoneNumber: String): Intent {
        val fullMessage = messageToSend ?: ""
        return if (mediaUriString != null) {
            Intent(Intent.ACTION_SEND).apply {
                setPackage(WHATSAPP_PACKAGE_NAME)
                putExtra(Intent.EXTRA_TEXT, fullMessage)
                putExtra("jid", "${phoneNumber.replace("+", "")}@s.whatsapp.net")
                val mediaU = Uri.parse(mediaUriString)
                putExtra(Intent.EXTRA_STREAM, mediaU)
                type = contentResolver.getType(mediaU) ?: "*/*"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            val whatsappUri = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(fullMessage)}")
            Intent(Intent.ACTION_VIEW, whatsappUri).apply {
                setPackage(WHATSAPP_PACKAGE_NAME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
    
    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    // --- Service Lifecycle ---

    override fun onInterrupt() { isProcessingQueue = false }

    override fun onServiceConnected() {
        super.onServiceConnected()
        setServiceInfo(AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            packageNames = arrayOf(WHATSAPP_PACKAGE_NAME)
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        })
        showToast("WhatsApp Accessibility Service Connected")
    }
    
    private fun stopSelfIfIdle() {
        if (!isProcessingQueue && phoneNumbersQueue.isEmpty()) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isProcessingQueue = false
        phoneNumbersQueue.clear()
        contactTimeoutRunnable?.let { timeoutHandler.removeCallbacks(it) }
    }
}
