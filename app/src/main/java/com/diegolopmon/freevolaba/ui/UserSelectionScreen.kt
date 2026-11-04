package com.diegolopmon.freevolaba.ui

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.diegolopmon.freevolaba.R
import com.diegolopmon.freevolaba.db.UserEntity
import com.diegolopmon.freevolaba.ui.theme.FreeVolabaTheme

@Composable
fun UserSelectionScreen(
    users: List<UserEntity>,
    onSelectUser: (UserEntity) -> Unit,
    onCreateUser: (name: String, avatarId: String, avatarPath: String?, originalAvatarPath: String?, birthDate: Long?, heightCm: Int?, weightKg: Double?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showUserDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showUserDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_create_user)) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title anchored at the top
            Text(
                text = stringResource(R.string.title_select_user),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
            )

            // Centered content area for user cards or empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (users.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.msg_no_users),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    ) {
                        items(users, key = { it.id }) { user ->
                            UserCard(
                                user = user,
                                onSelect = { onSelectUser(user) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showUserDialog) {
        UserDialog(
            userToEdit = null,
            onDismiss = { showUserDialog = false },
            onSave = { name, avatarId, avatarPath, originalAvatarPath, birthDate, heightCm, weightKg ->
                onCreateUser(name, avatarId, avatarPath, originalAvatarPath, birthDate, heightCm, weightKg)
                showUserDialog = false
            }
        )
    }
}

@Composable
fun UserCard(
    user: UserEntity,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .width(160.dp)
            .clickable(onClick = onSelect),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            UserAvatar(
                avatarId = user.avatarId,
                userName = user.name,
                avatarPath = user.avatarPath,
                size = 100.dp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = user.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(
    name = "Sin Usuarios (Estado Vacío)",
    device = "spec:width=1280dp,height=800dp,dpi=240,orientation=landscape",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun UserSelectionScreenEmptyPreview() {
    FreeVolabaTheme {
        UserSelectionScreen(
            users = emptyList(),
            onSelectUser = {},
            onCreateUser = { _, _, _, _, _, _, _ -> }
        )
    }
}

@Preview(
    name = "Un Usuario",
    device = "spec:width=1280dp,height=800dp,dpi=240,orientation=landscape",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun UserSelectionScreenSingleUserPreview() {
    val sampleUsers = listOf(
        UserEntity(id = 1, name = "Alex", avatarId = "face_teal")
    )
    FreeVolabaTheme {
        UserSelectionScreen(
            users = sampleUsers,
            onSelectUser = {},
            onCreateUser = { _, _, _, _, _, _, _ -> }
        )
    }
}

@Preview(
    name = "Varios Usuarios",
    device = "spec:width=1280dp,height=800dp,dpi=240,orientation=landscape",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun UserSelectionScreenMultipleUsersPreview() {
    val sampleUsers = listOf(
        UserEntity(id = 1, name = "Alex", avatarId = "face_teal"),
        UserEntity(id = 2, name = "María", avatarId = "person_blue"),
        UserEntity(id = 3, name = "Carlos", avatarId = "happy_yellow")
    )
    FreeVolabaTheme {
        UserSelectionScreen(
            users = sampleUsers,
            onSelectUser = {},
            onCreateUser = { _, _, _, _, _, _, _ -> }
        )
    }
}
