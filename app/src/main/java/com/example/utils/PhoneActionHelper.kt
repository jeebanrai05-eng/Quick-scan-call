package com.example.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object PhoneActionHelper {

    fun openDialer(context: Context, phoneNumber: String) {
        try {
            val uri = Uri.parse("tel:${Uri.encode(phoneNumber.trim())}")
            val intent = Intent(Intent.ACTION_DIAL, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open dialer: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWhatsApp(context: Context, phoneNumber: String, message: String? = null) {
        try {
            val cleanNumber = phoneNumber.replace(Regex("[^0-9]"), "")
            val textParam = if (!message.isNullOrBlank()) "&text=${Uri.encode(message)}" else ""
            val url = "https://api.whatsapp.com/send?phone=$cleanNumber$textParam"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp is not installed or number invalid", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyToClipboard(context: Context, text: String, label: String = "Copied text") {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to copy: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openSaveContact(
        context: Context,
        phoneNumber: String,
        name: String? = null,
        company: String? = null,
        email: String? = null,
        postalAddress: String? = null
    ) {
        try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                type = ContactsContract.Contacts.CONTENT_TYPE
                putExtra(ContactsContract.Intents.Insert.PHONE, phoneNumber)
                if (!name.isNullOrBlank()) {
                    putExtra(ContactsContract.Intents.Insert.NAME, name)
                }
                if (!company.isNullOrBlank()) {
                    putExtra(ContactsContract.Intents.Insert.COMPANY, company)
                }
                if (!email.isNullOrBlank()) {
                    putExtra(ContactsContract.Intents.Insert.EMAIL, email)
                }
                if (!postalAddress.isNullOrBlank()) {
                    putExtra(ContactsContract.Intents.Insert.POSTAL, postalAddress)
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open contacts: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareNumber(context: Context, phoneNumber: String, name: String? = null) {
        try {
            val text = if (!name.isNullOrBlank()) "$name: $phoneNumber" else phoneNumber
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(intent, "Share Contact Number"))
        } catch (e: Exception) {
            Toast.makeText(context, "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendSms(context: Context, phoneNumber: String, body: String? = null) {
        try {
            val uri = Uri.parse("smsto:${Uri.encode(phoneNumber.trim())}")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                if (!body.isNullOrBlank()) {
                    putExtra("sms_body", body)
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open SMS app: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openEmail(context: Context, email: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWebsite(context: Context, website: String) {
        try {
            val formattedUrl = if (!website.startsWith("http://") && !website.startsWith("https://")) {
                "https://$website"
            } else website
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
        }
    }

    fun openAddressInMap(context: Context, address: String) {
        try {
            val uri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No maps app available", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareExportContent(context: Context, content: String, title: String, filename: String, mimeType: String = "text/plain") {
        try {
            val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(cacheDir, filename)
            FileOutputStream(file).use { it.write(content.toByteArray()) }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            // Fallback to plain text share
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content)
                putExtra(Intent.EXTRA_SUBJECT, title)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(intent, title))
        }
    }
}
