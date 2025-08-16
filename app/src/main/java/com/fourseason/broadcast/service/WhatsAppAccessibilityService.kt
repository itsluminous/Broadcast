package com.fourseason.broadcast.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log
import android.widget.Toast

class WhatsAppAccessibilityService : AccessibilityService() {

    private val TAG = "WhatsAppAccessibility"
    private val WHATSAPP_PACKAGE_NAME = "com.whatsapp"
    // TODO: Verify these with Layout Inspector for current WhatsApp version
    private val SEND_BUTTON_TEXT_OPTIONS = listOf("Send", "SEND")
    private val SEND_BUTTON_CONTENT_DESCRIPTION_OPTIONS = listOf("Send", "send", "Enviar") // Added Spanish for broader compatibility
    private val SEND_BUTTON_ID_OPTIONS = listOf("com.whatsapp:id/send", "com.whatsapp:id/entry_send")


    private var phoneNumbersQueue: MutableList<String> = mutableListOf()
    private var messageToSend: String? = null
    private var currentPhoneNumber: String? = null
    private var isProcessingQueue: Boolean = false
    private var mediaUriString: String? = null


    companion object {
        const val ACTION_SEND_BULK = "com.fourseason.broadcast.service.ACTION_SEND_BULK"
        const val EXTRA_PHONE_NUMBERS = "com.fourseason.broadcast.service.EXTRA_PHONE_NUMBERS"
        const val EXTRA_MESSAGE = "com.fourseason.broadcast.service.EXTRA_MESSAGE"
        const val EXTRA_MEDIA_URI = "com.fourseason.broadcast.service.EXTRA_MEDIA_URI"

        fun startSendQueue(context: Context, numbers: List<String>, message: String, mediaUri: Uri?) {
            val intent = Intent(context, WhatsAppAccessibilityService::class.java).apply {
                action = ACTION_SEND_BULK
                putStringArrayListExtra(EXTRA_PHONE_NUMBERS, ArrayList(numbers))
                putExtra(EXTRA_MESSAGE, message)
                mediaUri?.let { putExtra(EXTRA_MEDIA_URI, it.toString()) }
            }
            context.startService(intent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand received")
        if (intent?.action == ACTION_SEND_BULK) {
            val numbers = intent.getStringArrayListExtra(EXTRA_PHONE_NUMBERS)
            val message = intent.getStringExtra(EXTRA_MESSAGE)
            val mediaUri = intent.getStringExtra(EXTRA_MEDIA_URI)

            if (numbers != null && message != null) {
                phoneNumbersQueue.clear()
                phoneNumbersQueue.addAll(numbers)
                messageToSend = message
                mediaUriString = mediaUri
                currentPhoneNumber = null // Reset current phone number

                Log.d(TAG, "Queue initialized with ${numbers.size} numbers. Message: '$message'. Media: $mediaUriString")

                if (!isProcessingQueue && phoneNumbersQueue.isNotEmpty()) {
                    isProcessingQueue = true
                    Log.d(TAG, "Starting to process queue.")
                    processNextMessage()
                } else if (isProcessingQueue) {
                    Log.d(TAG, "Already processing queue. New request will be handled after current batch.")
                    // Potentially, you could stop the current processing and restart,
                    // or queue this new request. For now, we ignore if already processing.
                } else if (phoneNumbersQueue.isEmpty()) {
                    Log.d(TAG, "Queue is empty, nothing to process.")
                    stopSelfIfIdle()
                }
            } else {
                Log.w(TAG, "Received incomplete data for ACTION_SEND_BULK.")
                stopSelfIfIdle()
            }
        }
        return START_STICKY // Or START_NOT_STICKY if you don't want it to restart automatically
    }

    private fun processNextMessage() {
        if (phoneNumbersQueue.isEmpty()) {
            Log.i(TAG, "Bulk sending complete. All messages processed.")
            Toast.makeText(this, "All messages processed!", Toast.LENGTH_LONG).show()
            isProcessingQueue = false
            currentPhoneNumber = null
            stopSelfIfIdle()
            return
        }

        currentPhoneNumber = phoneNumbersQueue.removeAt(0)
        Log.i(TAG, "Processing next message for: $currentPhoneNumber. ${phoneNumbersQueue.size} remaining.")

        val intent = Intent(Intent.ACTION_SEND)
        intent.setPackage(WHATSAPP_PACKAGE_NAME)

        val fullMessage = messageToSend ?: ""
        intent.putExtra(Intent.EXTRA_TEXT, fullMessage)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        mediaUriString?.let { uriStr ->
            try {
                val mediaUri = Uri.parse(uriStr)
                intent.putExtra(Intent.EXTRA_STREAM, mediaUri)
                intent.type = contentResolver.getType(mediaUri) ?: "*/*"
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                Log.d(TAG, "Added media URI to intent: $mediaUri")
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing media URI: $uriStr", e)
            }
        } ?: run {
             intent.type = "text/plain"
        }
        
        // Construct the WhatsApp specific URI for phone number
        // Note: This format sends directly to the contact if possible, bypassing contact picker.
        // It requires the country code and the number without spaces or special characters.
        val whatsappUri = Uri.parse("whatsapp://send?phone=$currentPhoneNumber&text=${Uri.encode(fullMessage)}")
        val finalIntent = Intent(Intent.ACTION_VIEW, whatsappUri)
        finalIntent.setPackage(WHATSAPP_PACKAGE_NAME)
        finalIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        // If media is present, the ACTION_SEND intent is more reliable.
        // If only text, ACTION_VIEW with whatsapp:// URI can be more direct.
        // We will try ACTION_SEND first if media is present.
        // If WhatsApp doesn't open to the chat with ACTION_SEND, this service won't work for media.
        val intentToLaunch = if (mediaUriString != null) {
            Intent(Intent.ACTION_SEND).apply {
                setPackage(WHATSAPP_PACKAGE_NAME)
                putExtra(Intent.EXTRA_TEXT, fullMessage)
                putExtra("jid", "$currentPhoneNumber@s.whatsapp.net") // For specific user
                val mediaU = Uri.parse(mediaUriString)
                putExtra(Intent.EXTRA_STREAM, mediaU)
                type = contentResolver.getType(mediaU) ?: "*/*"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            finalIntent // ACTION_VIEW for text only
        }


        try {
            Log.d(TAG, "Launching WhatsApp for $currentPhoneNumber with intent: $intentToLaunch")
            startActivity(intentToLaunch)
            // Expect onAccessibilityEvent to handle clicking send
        } catch (e: Exception) {
            Log.e(TAG, "Error starting WhatsApp for $currentPhoneNumber: ${e.message}", e)
            // Handle error: skip this number, retry, or stop processing
            Toast.makeText(this, "Error opening WhatsApp for $currentPhoneNumber", Toast.LENGTH_SHORT).show()
            // Schedule next attempt after a short delay to avoid rapid failures
            Handler(Looper.getMainLooper()).postDelayed({
                processNextMessage()
            }, 2000) // 2 second delay
        }
    }


    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isProcessingQueue || currentPhoneNumber == null) {
            // Log.v(TAG, "Not processing queue or currentPhoneNumber is null, ignoring event.")
            return
        }
        if (event == null) return

        // Log.d(TAG, "Event: ${AccessibilityEvent.eventTypeToString(event.eventType)}, Pkg: ${event.packageName}, Class: ${event.className}")

        if (event.packageName == WHATSAPP_PACKAGE_NAME &&
            (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)) {
            
            val rootNode = rootInActiveWindow ?: return
            Log.d(TAG, "WhatsApp window changed/state changed. Looking for send button for $currentPhoneNumber")

            if (findAndClickSendButton(rootNode)) {
                Log.i(TAG, "Send button clicked for $currentPhoneNumber.")
                Toast.makeText(this, "Message sent to $currentPhoneNumber", Toast.LENGTH_SHORT).show()
                // Successfully sent, wait a bit for WhatsApp to process and then move to next
                Handler(Looper.getMainLooper()).postDelayed({
                    processNextMessage()
                }, 2000) // 2-second delay before processing next, adjust as needed
            } else {
                Log.w(TAG, "Send button not found or not clicked for $currentPhoneNumber on event: ${AccessibilityEvent.eventTypeToString(event.eventType)}")
                // TODO: Implement more robust error handling here.
                // Could retry on the next window update, or after a timeout, or skip.
                // For now, we will wait for another event or a timeout (implicit by not calling processNextMessage)
            }
            rootNode.recycle()
        }
    }

    private fun findAndClickSendButton(nodeInfo: AccessibilityNodeInfo): Boolean {
        // Option 1: Find by View ID (most reliable if the ID is known and stable)
        for (idOption in SEND_BUTTON_ID_OPTIONS) {
            val sendButtonsById = nodeInfo.findAccessibilityNodeInfosByViewId(idOption)
            for (button in sendButtonsById) {
                if (button.isClickable && button.isEnabled) {
                    Log.d(TAG, "Send button found by ID: $idOption. Clicking.")
                    val clicked = button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    button.recycle()
                    if (clicked) return true
                } else {
                    button.recycle()
                }
            }
        }
        
        // Option 2: Find by text
        for (textOption in SEND_BUTTON_TEXT_OPTIONS) {
            val sendButtonsByText = nodeInfo.findAccessibilityNodeInfosByText(textOption)
            for (button in sendButtonsByText) {
                if (button.isClickable && button.isEnabled) {
                    Log.d(TAG, "Send button found by text: $textOption. Clicking.")
                    val clicked = button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    button.recycle()
                    if (clicked) return true
                } else {
                    button.recycle()
                }
            }
        }

        // Option 3: Find by content description
        val allNodes = mutableListOf<AccessibilityNodeInfo>()
        collectNodes(nodeInfo, allNodes) // Search all nodes

        for (descOption in SEND_BUTTON_CONTENT_DESCRIPTION_OPTIONS) {
            for (button in allNodes) {
                if (button.contentDescription?.toString().equals(descOption, ignoreCase = true) &&
                    button.isClickable && button.isEnabled) {
                    Log.d(TAG, "Send button found by content description: $descOption. Clicking.")
                    val clicked = button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    // button.recycle() // Already recycled by collectNodes logic if not returned
                    if (clicked) return true // Found and clicked
                }
                // button.recycle() // Already recycled
            }
        }
        allNodes.forEach { it.recycle() }


        Log.d(TAG, "Send button not found or not clickable/enabled.")
        return false
    }
    
    // Helper to collect all nodes, recycle them after check if not the one.
    private fun collectNodes(node: AccessibilityNodeInfo?, collectedNodes: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        collectedNodes.add(node) // Add the node itself for checking
        for (i in 0 until node.childCount) {
            collectNodes(node.getChild(i), collectedNodes)
        }
    }


    private fun clickByCoordinates(node: AccessibilityNodeInfo) { // Keep for debugging or specific cases
        val rect = Rect()
        node.getBoundsInScreen(rect)
        val x = rect.centerX().toFloat()
        val y = rect.centerY().toFloat()

        val path = Path()
        path.moveTo(x, y)
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 100L))
        dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                super.onCompleted(gestureDescription)
                Log.d(TAG, "Gesture click completed.")
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                super.onCancelled(gestureDescription)
                Log.d(TAG, "Gesture click cancelled.")
            }
        }, null)
    }

    override fun onInterrupt() {
        Log.d(TAG, "onInterrupt: Service interrupted.")
        isProcessingQueue = false // Stop processing if interrupted
        // Potentially save state here if you want to resume later
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or AccessibilityEvent.TYPE_VIEW_CLICKED or AccessibilityEvent.TYPE_VIEW_FOCUSED
            packageNames = arrayOf(WHATSAPP_PACKAGE_NAME)
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100 // ms
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        setServiceInfo(serviceInfo)
        Log.d(TAG, "Accessibility Service connected and configured for WhatsApp.")
        Toast.makeText(this, "WhatsApp Accessibility Service Connected", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopSelfIfIdle() {
        if (!isProcessingQueue && phoneNumbersQueue.isEmpty()) {
            Log.i(TAG, "Service is idle, stopping self.")
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Service destroyed.")
        isProcessingQueue = false
        phoneNumbersQueue.clear()
    }
}
