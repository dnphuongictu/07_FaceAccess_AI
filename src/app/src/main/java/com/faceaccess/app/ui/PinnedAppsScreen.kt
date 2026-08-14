package com.faceaccess.app.ui

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.faceaccess.app.R
import com.faceaccess.app.data.PinnedApp
import com.faceaccess.app.data.PinnedAppsStore
import kotlinx.coroutines.launch

/** Quan ly danh sach ung dung nguoi dung ghim de mo bang cu chi quet. */
@Composable
fun PinnedAppsScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val pinnedAppsStore = remember { PinnedAppsStore(context) }
    val pinnedApps by pinnedAppsStore.pinnedAppsFlow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val installedApps = remember {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .map { PinnedApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .distinctBy { it.packageName }
            .sortedBy { it.label }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(stringResource(R.string.pinned_apps_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        if (pinnedApps.isEmpty()) {
            Text(stringResource(R.string.pinned_apps_empty), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
        } else {
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(pinnedApps, key = { it.packageName }) { app ->
                    ListItem(
                        headlineContent = { Text(app.label) },
                        trailingContent = {
                            TextButton(onClick = { scope.launch { pinnedAppsStore.removePinnedApp(app.packageName) } }) {
                                Text("Xóa")
                            }
                        },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Text(stringResource(R.string.pinned_apps_add), style = MaterialTheme.typography.titleLarge)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(installedApps, key = { it.packageName }) { app ->
                val alreadyPinned = pinnedApps.any { it.packageName == app.packageName }
                ListItem(
                    headlineContent = { Text(app.label) },
                    trailingContent = {
                        TextButton(
                            enabled = !alreadyPinned,
                            onClick = { scope.launch { pinnedAppsStore.addPinnedApp(app) } },
                        ) {
                            Text(if (alreadyPinned) "Đã ghim" else "Ghim")
                        }
                    },
                )
            }
        }

        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.pinned_apps_done))
        }
    }
}
