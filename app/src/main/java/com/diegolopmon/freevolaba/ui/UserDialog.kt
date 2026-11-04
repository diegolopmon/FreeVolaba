package com.diegolopmon.freevolaba.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.diegolopmon.freevolaba.R
import com.diegolopmon.freevolaba.db.UserEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UserDialog(
    userToEdit: UserEntity? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, avatarId: String, avatarPath: String?, originalAvatarPath: String?, birthDate: Long?, heightCm: Int?, weightKg: Double?) -> Unit,
    onDelete: ((user: UserEntity) -> Unit)? = null
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(userToEdit?.name ?: "") }
    var selectedAvatarId by remember { mutableStateOf(userToEdit?.avatarId ?: AvatarPresets.presets.first().id) }
    var avatarPath by remember { mutableStateOf(userToEdit?.avatarPath) }
    var originalAvatarPath by remember { mutableStateOf(userToEdit?.originalAvatarPath) }
    var heightCmText by remember { mutableStateOf(userToEdit?.heightCm?.toString() ?: "") }
    var weightKgText by remember { mutableStateOf(userToEdit?.weightKg?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "") }
    var birthDateText by remember { mutableStateOf(formatBirthDate(userToEdit?.birthDate)) }

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var uncroppedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { rawBitmap ->
        if (rawBitmap != null) {
            val avatarsDir = File(context.filesDir, "avatars").apply { if (!exists()) mkdirs() }
            val originalFile = File(avatarsDir, "original_${System.currentTimeMillis()}.jpg")
            try {
                FileOutputStream(originalFile).use { out ->
                    rawBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                originalAvatarPath = originalFile.absolutePath
            } catch (_: Exception) {
            }
            uncroppedBitmap = rawBitmap
        }
    }

    if (uncroppedBitmap != null) {
        ImageCropDialog(
            sourceBitmap = uncroppedBitmap!!,
            onDismiss = { uncroppedBitmap = null },
            onCropSuccess = { cropped ->
                val avatarsDir = File(context.filesDir, "avatars").apply { if (!exists()) mkdirs() }
                val photoFile = File(avatarsDir, "avatar_${System.currentTimeMillis()}.jpg")
                try {
                    FileOutputStream(photoFile).use { out ->
                        cropped.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    avatarPath = photoFile.absolutePath
                } catch (_: Exception) {
                }
                uncroppedBitmap = null
            }
        )
    }

    if (showDeleteConfirmation && userToEdit != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.delete_user_title)) },
            text = { Text(stringResource(R.string.delete_user_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete(userToEdit)
                    }
                ) {
                    Text(
                        stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (userToEdit == null) {
                        stringResource(R.string.title_create_user)
                    } else {
                        stringResource(R.string.title_edit_user)
                    }
                )
                if (userToEdit != null && onDelete != null) {
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.action_delete_user),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.label_user_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. Selfie Camera Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    UserAvatar(
                        avatarId = selectedAvatarId,
                        userName = name.ifBlank { "User" },
                        avatarPath = avatarPath,
                        size = 64.dp
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedButton(
                            onClick = { cameraLauncher.launch(null) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.action_take_photo))
                        }

                        if (avatarPath != null || originalAvatarPath != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = {
                                        val targetPath = originalAvatarPath ?: avatarPath
                                        targetPath?.let { path ->
                                            val file = File(path)
                                            if (file.exists()) {
                                                uncroppedBitmap = BitmapFactory.decodeFile(file.absolutePath)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Crop,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.action_edit_crop))
                                }

                                TextButton(
                                    onClick = {
                                        avatarPath = null
                                        originalAvatarPath = null
                                    }
                                ) {
                                    Text(
                                        stringResource(R.string.action_remove_photo),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Avatar Presets Section (Only displayed when NO selfie photo is active)
                if (avatarPath == null) {
                    Text(
                        text = stringResource(R.string.label_choose_avatar),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AvatarPresets.presets.forEach { preset ->
                            val isSelected = preset.id == selectedAvatarId
                            UserAvatar(
                                avatarId = preset.id,
                                userName = preset.id,
                                size = 48.dp,
                                borderWidth = if (isSelected) 3.dp else 0.dp,
                                borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                onClick = {
                                    selectedAvatarId = preset.id
                                    avatarPath = null
                                    originalAvatarPath = null
                                }
                            )
                        }
                    }
                }

                // 4. Health / Optional Information Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = heightCmText,
                        onValueChange = { heightCmText = it.filter { char -> char.isDigit() } },
                        label = { Text(stringResource(R.string.label_height_cm)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = weightKgText,
                        onValueChange = { weightKgText = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text(stringResource(R.string.label_weight_kg)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = birthDateText,
                    onValueChange = { birthDateText = it },
                    label = { Text(stringResource(R.string.label_birth_date)) },
                    placeholder = { Text("DD/MM/YYYY") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val parsedHeight = heightCmText.toIntOrNull()
                        val parsedWeight = weightKgText.toDoubleOrNull()
                        val parsedDate = parseBirthDate(birthDateText)
                        onSave(name.trim(), selectedAvatarId, avatarPath, originalAvatarPath, parsedDate, parsedHeight, parsedWeight)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

private fun parseBirthDate(input: String): Long? {
    if (input.isBlank()) return null
    return try {
        val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        format.parse(input.trim())?.time
    } catch (_: Exception) {
        try {
            val formatAlt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            formatAlt.parse(input.trim())?.time
        } catch (_: Exception) {
            null
        }
    }
}

private fun formatBirthDate(timeMillis: Long?): String {
    if (timeMillis == null) return ""
    val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return format.format(Date(timeMillis))
}
