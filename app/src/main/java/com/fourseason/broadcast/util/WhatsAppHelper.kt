package com.fourseason.broadcast.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object WhatsAppHelper {

    fun sendMessage(context: Context, message: String, mediaUri: Uri?, phoneNumbers: List<String>) {
        if (phoneNumbers.isEmpty()) {
            Toast.makeText(context, "No contacts to send message to.", Toast.LENGTH_SHORT).show()
            return
        }

        for (phoneNumber in phoneNumbers) {
            val intent = Intent(Intent.ACTION_SEND)
            intent.setPackage("com.whatsapp")

            intent.putExtra(Intent.EXTRA_TEXT, message)
            // Recipient's phone number with country code, for WhatsApp to identify the contact.
            // WhatsApp uses JID (Jabber ID) which is typically formatted as: [phone_number]@s.whatsapp.net
            // However, simply providing the phone number to ACTION_SEND intent usually works as WhatsApp
            // will try to resolve it or ask the user to pick from contacts if ambiguous.
            // For a more direct approach without relying on WhatsApp's contact resolution for a specific number,
            // one might need to use different intents or approaches, but this is the common way.
            // N.B. WhatsApp does not officially support sending to a number directly via public Intent API
            // without user interaction if the number is not in the contact list or if it's ambiguous.
            // This setup will pre-fill the message and media for the chat with the given number if WhatsApp can resolve it.
            // If WhatsApp cannot directly resolve the number to a chat, it might open the contact picker or an error.
            // A common workaround is to use ACTION_VIEW with a Uri like: "whatsapp://send?phone=$phoneNumber&text=$message"
            // but this is not officially documented for EXTRA_STREAM.

            // Sticking to ACTION_SEND as it supports EXTRA_STREAM more reliably.
            // We are targeting a single user at a time in this loop.
            // Setting the specific phone number in the intent data or an extra (like 'jid') is tricky with ACTION_SEND
            // as it's not universally supported or documented for all versions of WhatsApp.
            // The current approach will open WhatsApp, pre-fill the message and media.
            // If a chat with this number exists and is unique, it might open it.
            // Otherwise, it will likely go to WhatsApp's contact/chat selection screen.
            // To ensure it goes to a specific number, a different intent structure might be needed,
            // potentially using `Intent.ACTION_VIEW` with a `Uri` like `https://wa.me/$phoneNumber/?text=yourMessageEncoded`
            // but `EXTRA_STREAM` is not standard with `ACTION_VIEW`.

            if (mediaUri != null) {
                intent.putExtra(Intent.EXTRA_STREAM, mediaUri)
                intent.type = context.contentResolver.getType(mediaUri)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                intent.type = "text/plain"
            }
            
            // For sending to a specific number with ACTION_SEND, we can try setting the phone number in "jid" extra
            // This is not an official API, so it might break in future WhatsApp versions.
            // The format is usually phonenumber@s.whatsapp.net
            // We'll strip non-digits from the phone number first.
            val strippedPhoneNumber = phoneNumber.replace(Regex("[^0-9]"), "")
            if (strippedPhoneNumber.isNotEmpty()) {
                 intent.putExtra("jid", "$strippedPhoneNumber@s.whatsapp.net")
            }


            try {
                context.startActivity(intent)
                // It's good practice to inform the user for each message, or wait for user action.
                // Depending on the desired UX, you might want to add a small delay or a prompt.
                // For now, it will open WhatsApp for each contact. The user has to press send and then navigate back.
                Toast.makeText(context, "Preparing message for $phoneNumber. Press send in WhatsApp.", Toast.LENGTH_LONG).show()

                // If you want to automate pressing send, it would require Accessibility Services, which is complex and has privacy implications.
                // A short delay might be needed if you want to ensure the user has time to react or if the system needs time to switch apps.
                // For example: Thread.sleep(2000) // Not recommended on UI thread. Use coroutines or a Handler.

            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "WhatsApp not installed.", Toast.LENGTH_SHORT).show()
                return // If WhatsApp is not installed, no point in continuing the loop.
            } catch (e: Exception) {
                Toast.makeText(context, "Could not send message to $phoneNumber: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        // Remove the general "Please select the contacts in WhatsApp" toast as we are iterating
    }
}
