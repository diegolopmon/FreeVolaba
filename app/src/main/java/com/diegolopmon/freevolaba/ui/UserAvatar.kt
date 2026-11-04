package com.diegolopmon.freevolaba.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.TagFaces
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.io.File

data class AvatarPreset(
    val id: String,
    val icon: ImageVector,
    val backgroundColor: Color
)

object AvatarPresets {
    val presets = listOf(
        AvatarPreset("face_teal", Icons.Default.Face, Color(0xFF00897B)),
        AvatarPreset("person_blue", Icons.Default.Person, Color(0xFF1E88E5)),
        AvatarPreset("account_purple", Icons.Default.AccountCircle, Color(0xFF8E24AA)),
        AvatarPreset("tagface_orange", Icons.Default.TagFaces, Color(0xFFFB8C00)),
        AvatarPreset("happy_yellow", Icons.Default.SentimentVerySatisfied, Color(0xFFFDD835)),
        AvatarPreset("outline_red", Icons.Default.PersonOutline, Color(0xFFE53935)),
        AvatarPreset("mood_green", Icons.Default.Mood, Color(0xFF43A047)),
        AvatarPreset("emotions_cyan", Icons.Default.EmojiEmotions, Color(0xFF00ACC1))
    )

    fun getPreset(avatarId: String): AvatarPreset {
        return presets.find { it.id == avatarId } ?: presets.first()
    }
}

@Composable
fun UserAvatar(
    avatarId: String,
    userName: String,
    modifier: Modifier = Modifier,
    avatarPath: String? = null,
    size: Dp = 40.dp,
    borderWidth: Dp = 0.dp,
    borderColor: Color = Color.Transparent,
    onClick: (() -> Unit)? = null
) {
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    val bitmap = remember(avatarPath) {
        if (!avatarPath.isNullOrBlank()) {
            val file = File(avatarPath)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else null
        } else null
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (bitmap == null) {
                    val preset = AvatarPresets.getPreset(avatarId)
                    Modifier.background(preset.backgroundColor)
                } else Modifier
            )
            .then(
                if (borderWidth > 0.dp) {
                    Modifier.border(borderWidth, borderColor, CircleShape)
                } else Modifier
            )
            .then(clickModifier),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = userName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val preset = AvatarPresets.getPreset(avatarId)
            Icon(
                imageVector = preset.icon,
                contentDescription = userName,
                tint = Color.White,
                modifier = Modifier.size(size * 0.6f)
            )
        }
    }
}
