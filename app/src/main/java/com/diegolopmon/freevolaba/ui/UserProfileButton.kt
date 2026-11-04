package com.diegolopmon.freevolaba.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.diegolopmon.freevolaba.R
import com.diegolopmon.freevolaba.db.UserEntity

@Composable
fun UserProfileButton(
    activeUser: UserEntity,
    onSwitchUser: () -> Unit,
    onEditProfile: () -> Unit,
    onCreateNewUser: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp,
            modifier = Modifier
                .clip(CircleShape)
                .clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    avatarId = activeUser.avatarId,
                    userName = activeUser.name,
                    avatarPath = activeUser.avatarPath,
                    size = 32.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = activeUser.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_switch_user)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.SwitchAccount,
                        contentDescription = null
                    )
                },
                onClick = {
                    expanded = false
                    onSwitchUser()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.title_edit_user)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.ManageAccounts,
                        contentDescription = null
                    )
                },
                onClick = {
                    expanded = false
                    onEditProfile()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_new_user)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null
                    )
                },
                onClick = {
                    expanded = false
                    onCreateNewUser()
                }
            )
        }
    }
}
