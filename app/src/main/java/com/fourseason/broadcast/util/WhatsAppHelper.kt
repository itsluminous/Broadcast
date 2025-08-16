package com.fourseason.broadcast.util

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

        // WhatsApp does not support sending to multiple numbers directly via an Intent.
        // This will open WhatsApp to the contact picker to choose who to send to.
        // A toast is shown to inform the user.
        Toast.makeText(context, "Please select the contacts in WhatsApp", Toast.LENGTH_LONG).show()


        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp not installed.", Toast.LENGTH_SHORT).show()
        }
    }
}
