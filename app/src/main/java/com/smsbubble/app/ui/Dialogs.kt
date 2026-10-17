package com.smsbubble.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.smsbubble.app.data.ContactUtils
import com.smsbubble.app.data.Conversation
import com.smsbubble.app.data.Message
import com.smsbubble.app.data.PickPhoneContact
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

// ======================================================================
// Chọn hoặc nhập ngày giờ của tin nhắn
// ======================================================================

/**
 * @param initial thời điểm ban đầu; null = "bây giờ".
 * @param allowNow hiện nút "Dùng giờ hiện tại" (trả về null qua onConfirm).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageTimeDialog(
    initial: Long?,
    allowNow: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit,
) {
    var dt by remember {
        mutableStateOf((initial ?: System.currentTimeMillis()).toLdt().withSecond(0).withNano(0))
    }
    var text by remember { mutableStateOf(dt.format(INPUT_FMT)) }
    var error by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }

    fun setDt(v: LocalDateTime) {
        dt = v
        text = v.format(INPUT_FMT)
        error = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thời gian tin nhắn") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { v ->
                        text = v
                        val parsed = parseUserDateTime(v, dt.toLocalTime())
                        if (parsed != null) {
                            dt = parsed
                            error = false
                        } else {
                            error = true
                        }
                    },
                    label = { Text("Nhập ngày giờ (dd/MM/yyyy HH:mm)") },
                    isError = error,
                    supportingText = {
                        Text(
                            if (error) "Sai định dạng — ví dụ 17/10/2026 16:24"
                            else fullLabel(dt.toMillis())
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.CalendarMonth, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Chọn ngày")
                    }
                    OutlinedButton(onClick = { pickTime = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Schedule, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Chọn giờ")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !error, onClick = { onConfirm(dt.toMillis()) }) { Text("Xong") }
        },
        dismissButton = {
            Row {
                if (allowNow) {
                    TextButton(onClick = { onConfirm(null) }) { Text("Giờ hiện tại") }
                }
                TextButton(onClick = onDismiss) { Text("Huỷ") }
            }
        },
    )

    if (pickDate) {
        // DatePicker làm việc với nửa đêm UTC.
        val state = rememberDatePickerState(
            initialSelectedDateMillis = dt.toLocalDate()
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        val date = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        setDt(date.atTime(dt.toLocalTime()))
                    }
                    pickDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Huỷ") } },
        ) {
            DatePicker(state = state)
        }
    }

    if (pickTime) {
        val state = rememberTimePickerState(
            initialHour = dt.hour,
            initialMinute = dt.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { pickTime = false },
            title = { Text("Chọn giờ") },
            text = {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = state)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    setDt(dt.withHour(state.hour).withMinute(state.minute))
                    pickTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text("Huỷ") } },
        )
    }
}

// ======================================================================
// Tạo / sửa liên hệ của cuộc trò chuyện
// ======================================================================

@Composable
fun ContactDialog(
    initial: Conversation?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String) -> Unit,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var phone by remember { mutableStateOf(initial?.phone.orEmpty()) }
    var info by remember { mutableStateOf<String?>(null) }

    fun lookup() {
        val found = ContactUtils.lookupName(context, phone)
        info = if (found != null) {
            name = found
            "Đã tìm thấy trong danh bạ: $found"
        } else {
            "Không tìm thấy số này trong danh bạ"
        }
    }

    val pickContact = rememberLauncherForActivityResult(PickPhoneContact()) { uri ->
        val picked = uri?.let { ContactUtils.readPicked(context, it) }
        if (picked != null) {
            name = picked.name
            phone = picked.phone
            info = null
        }
    }
    val askPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) lookup() else info = "Bạn chưa cho phép đọc danh bạ"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Cuộc trò chuyện mới" else "Sửa liên hệ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { pickContact.launch(Unit) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Contacts, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Chọn từ danh bạ")
                }
                Text(
                    "Hoặc nhập tay:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên hiển thị") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    enabled = phone.isNotBlank(),
                    onClick = {
                        if (ContactUtils.hasContactsPermission(context)) lookup()
                        else askPermission.launch(Manifest.permission.READ_CONTACTS)
                    },
                ) { Text("Lấy tên trong danh bạ theo số này") }
                info?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() || phone.isNotBlank(),
                onClick = { onConfirm(name.trim(), phone.trim()) },
            ) { Text(if (initial == null) "Tạo" else "Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Huỷ") } },
    )
}

// ======================================================================
// Sửa / xoá một tin nhắn (nhấn giữ bong bóng)
// ======================================================================

@Composable
fun EditMessageDialog(
    message: Message,
    otherName: String,
    onDismiss: () -> Unit,
    onSave: (Message) -> Unit,
    onDelete: () -> Unit,
) {
    var text by remember { mutableStateOf(message.text) }
    var outgoing by remember { mutableStateOf(message.outgoing) }
    var ts by remember { mutableLongStateOf(message.timestamp) }
    var pickTime by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sửa tin nhắn") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Nội dung") },
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = outgoing,
                        onClick = { outgoing = true },
                        label = { Text("Tôi gửi") },
                    )
                    FilterChip(
                        selected = !outgoing,
                        onClick = { outgoing = false },
                        label = { Text("$otherName gửi") },
                    )
                }
                OutlinedButton(onClick = { pickTime = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Schedule, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(fullLabel(ts))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = {
                    onSave(message.copy(text = text.trim(), outgoing = outgoing, timestamp = ts))
                },
            ) { Text("Lưu") }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                ) { Text("Xoá") }
                TextButton(onClick = onDismiss) { Text("Huỷ") }
            }
        },
    )

    if (pickTime) {
        MessageTimeDialog(
            initial = ts,
            allowNow = false,
            onDismiss = { pickTime = false },
            onConfirm = { v ->
                if (v != null) ts = v
                pickTime = false
            },
        )
    }
}
