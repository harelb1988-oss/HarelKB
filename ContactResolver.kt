package com.harelkb.keyboard

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract

class ContactResolver(private val context: Context) {
    fun findPhone(name: String): String? {
        if (name.isBlank()) return null
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val args = arrayOf("%$name%")
        var cursor: Cursor? = null
        return try {
            cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, selection, args,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC"
            )
            if (cursor != null && cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ))
            } else null
        } catch (_: SecurityException) {
            null
        } finally {
            cursor?.close()
        }
    }
}
