package dev.matto.mcell.ui.manage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.matto.mcell.R
import dev.matto.mcell.ui.AddUrlResult
import dev.matto.mcell.ui.theme.Bg
import dev.matto.mcell.ui.theme.BgElevated
import dev.matto.mcell.ui.theme.TextMuted
import dev.matto.mcell.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun ManageUrlsDialog(
    urls: List<String>,
    onAdd: suspend (String) -> AddUrlResult,
    onRemove: suspend (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val errorDuplicate = stringResource(R.string.manage_duplicate)
    val errorFull = stringResource(R.string.manage_full)
    val errorInvalid = stringResource(R.string.manage_invalid)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Bg)
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.manage_title), color = TextMuted, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                    }
                }
                Spacer(Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(urls) { url ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(BgElevated)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(url, color = TextMuted, modifier = Modifier.weight(1f))
                            IconButton(onClick = { scope.launch { onRemove(url) } }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it; error = null },
                    placeholder = { Text(stringResource(R.string.manage_hint)) },
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    Text(error!!, color = TextSecondary)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = urls.size < 5,
                        onClick = {
                            scope.launch {
                                when (onAdd(input)) {
                                    AddUrlResult.Added -> { input = ""; error = null }
                                    AddUrlResult.Duplicate -> error = errorDuplicate
                                    AddUrlResult.Full -> error = errorFull
                                    AddUrlResult.Invalid -> error = errorInvalid
                                }
                            }
                        },
                    ) {
                        Text(stringResource(R.string.confirm_add))
                    }
                }
            }
        }
    }
}
