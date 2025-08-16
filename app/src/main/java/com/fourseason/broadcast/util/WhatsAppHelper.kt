package com.fourseason.broadcast.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import com.fourseason.broadcast.service.WhatsAppAccessibilityService

object WhatsAppHelper {

    private fun normalizePhoneNumber(phoneNumber: String): String {
        // Remove spaces and hyphens
        var normalizedNumber = phoneNumber.replace(Regex("[\\s-]"), "")
        // Add +91 if no country code is present and number is of typical Indian length
        // More robust validation might be needed for international numbers
        if (!normalizedNumber.startsWith("+")) {
            if (normalizedNumber.length == 10 && !normalizedNumber.startsWith("0")) {
                 normalizedNumber = "+91$normalizedNumber"
            } else if (normalizedNumber.startsWith("0")) {
                normalizedNumber = "+91${normalizedNumber.substring(1)}"
            }
            // If it's not a 10-digit number and doesn't start with 0, it might already have a country code
            // or be an invalid number. For simplicity, we assume +91 if not prefixed.
            // A more sophisticated library should be used for production phone number validation.
        }
        return normalizedNumber
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val expectedComponentName = ComponentName(context, WhatsAppAccessibilityService::class.java)

        for (service in enabledServices) {
            val serviceInfo = service.resolveInfo.serviceInfo
            val actualComponentName = ComponentName(serviceInfo.packageName, serviceInfo.name)
            if (actualComponentName == expectedComponentName) {
                Log.d("WhatsAppHelper", "Accessibility Service is ENABLED.")
                return true
            }
        }
        Log.d("WhatsAppHelper", "Accessibility Service is DISABLED.")
        return false
    }

    fun requestAccessibilityPermission(context: Context) {
        Toast.makeText(context, "Please enable the Broadcast Automation service.", Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Needed if called from non-Activity context
        context.startActivity(intent)
    }


    fun sendMessage(context: Context, message: String, mediaUri: Uri?, phoneNumbers: List<String>) {
        if (!isAccessibilityServiceEnabled(context)) {
            requestAccessibilityPermission(context)
            return
        }

        if (phoneNumbers.isEmpty()) {
            Toast.makeText(context, "No contacts to send message to.", Toast.LENGTH_SHORT).show()
            return
        }

        val normalizedAndDeduplicatedNumbers = phoneNumbers
            .map { normalizePhoneNumber(it) }
            .filter { it.startsWith("+") } // Ensure only valid, normalized numbers are used
            .toSet()
            .toList()

        if (normalizedAndDeduplicatedNumbers.isEmpty()) {
            Toast.makeText(context, "No valid contacts after normalization/deduplication.", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("WhatsAppHelper", "Starting WhatsAppAccessibilityService for ${normalizedAndDeduplicatedNumbers.size} numbers.")
        WhatsAppAccessibilityService.startSendQueue(
            context,
            normalizedAndDeduplicatedNumbers,
            message,
            mediaUri
        )
        
        // Indicate that the process has started.
        // The Accessibility Service will provide more specific feedback (Toast messages).
        Toast.makeText(context, "Starting bulk message process...", Toast.LENGTH_SHORT).show()
    }
}