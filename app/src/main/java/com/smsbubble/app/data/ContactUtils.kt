package com.smsbubble.app.data

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.ContextCompat

data class PickedContact(val name: String, val phone: String)

/**
 * Mở trình chọn số điện thoại của danh bạ.
 * Android cấp quyền đọc tạm cho đúng dòng được chọn, nên KHÔNG cần quyền READ_CONTACTS.
 */
class PickPhoneContact : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}

object ContactUtils {

    /** Đọc tên + số từ kết quả của [PickPhoneContact]. */
    fun readPicked(context: Context, uri: Uri): PickedContact? {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
        )
        return try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    PickedContact(
                        name = c.getString(0).orEmpty(),
                        phone = c.getString(1).orEmpty(),
                    )
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun hasContactsPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    /** Tra tên hiển thị trong danh bạ theo số điện thoại (cần quyền READ_CONTACTS). */
    fun lookupName(context: Context, phone: String): String? {
        if (phone.isBlank() || !hasContactsPermission(context)) return null
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phone)
        )
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        } catch (e: Exception) {
            null
        }
    }
}
