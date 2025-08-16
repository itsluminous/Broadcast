package com.fourseason.broadcast.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object WhatsAppHelper {

    private fun normalizePhoneNumber(phoneNumber: String): String {
        // Remove spaces and hyphens
        var normalizedNumber = phoneNumber.replace(Regex("[\\s-]"), "")
        // Add +91 if no country code is present
        if (!normalizedNumber.startsWith("+")) {
            if (normalizedNumber.startsWith("0")) {
                normalizedNumber = normalizedNumber.substring(1)
            }
            normalizedNumber = "+91$normalizedNumber"
        }
        return normalizedNumber
    }

    fun sendMessage(context: Context, message: String, mediaUri: Uri?, phoneNumbers: List<String>) {
        if (phoneNumbers.isEmpty()) {
            Toast.makeText(context, "No contacts to send message to.", Toast.LENGTH_SHORT).show()
            return
        }

        val normalizedAndDeduplicatedNumbers = phoneNumbers
            .map { normalizePhoneNumber(it) }
            .toSet() // Deduplicates
            .toList() // Convert back to List to iterate

        if (normalizedAndDeduplicatedNumbers.isEmpty()) {
            Toast.makeText(context, "No valid contacts after normalization/deduplication.", Toast.LENGTH_SHORT).show()
            return
        }

        for (phoneNumber in normalizedAndDeduplicatedNumbers) {
            val intent = Intent(Intent.ACTION_SEND)
            intent.setPackage("com.whatsapp")

            intent.putExtra(Intent.EXTRA_TEXT, message)
            
            if (mediaUri != null) {
                intent.putExtra(Intent.EXTRA_STREAM, mediaUri)
                intent.type = context.contentResolver.getType(mediaUri)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                intent.type = "text/plain"
            }
            
            // For sending to a specific number with ACTION_SEND, we use the "jid" extra.
            // This is not an official API, so it might break in future WhatsApp versions.
            // The format is phonenumber@s.whatsapp.net.
            // The 'phoneNumber' is already normalized, e.g., "+919876543210"
            if (phoneNumber.startsWith("+") && phoneNumber.length > 1) {
                val jidFormattedNumber = phoneNumber.substring(1) // Removes the leading '+'
                 intent.putExtra("jid", "$jidFormattedNumber@s.whatsapp.net")
            }
            // else: if phoneNumber is not in the expected format after normalization (e.g. not starting with +)
            // we might skip adding JID or log an issue. Current normalization should ensure it starts with +.


            try {
                context.startActivity(intent)
                Toast.makeText(context, "Preparing message for $phoneNumber. Press send in WhatsApp.", Toast.LENGTH_LONG).show()

            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "WhatsApp not installed.", Toast.LENGTH_SHORT).show()
                return // If WhatsApp is not installed, no point in continuing the loop.
            } catch (e: Exception) {
                Toast.makeText(context, "Could not send message to $phoneNumber: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
