package com.amarildo.listify

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amarildo.listify.viewmodel.MainViewModel
import com.composables.icons.lucide.Delete
import com.composables.icons.lucide.Filter
import com.composables.icons.lucide.FolderInput
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Lucide
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
@OptIn(ExperimentalMaterial3Api::class)
fun App(vm: MainViewModel = viewModel()) {
    MaterialTheme {
        val scope = rememberCoroutineScope()

        var basePath by remember { mutableStateOf("") }
        val snackbarHostState = remember { SnackbarHostState() }

        val uiState by vm.uiState.collectAsState()

        var prefixChecked by remember { mutableStateOf(false) }
        var prefixText by remember { mutableStateOf("") }
        var isPathLoading by remember { mutableStateOf(false) }

        LaunchedEffect(uiState.successMessage, uiState.error) {
            uiState.successMessage?.let { msg ->
                snackbarHostState.showSnackbar(
                    message = msg,
                    duration = SnackbarDuration.Short,
                )
                vm.clearMessage()
            }
            uiState.error?.let { error ->
                snackbarHostState.showSnackbar(
                    message = error,
                    duration = SnackbarDuration.Long,
                )
                vm.clearMessage()
            }
        }

        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = if (data.visuals.message.contains("Error", ignoreCase = true)) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        contentColor = if (data.visuals.message.contains("Error", ignoreCase = true)) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                    )
                }
            },
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item {
                    // Header section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                        ) {
                            Row(
                                verticalAlignment = CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Lucide.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Configuration",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Configure your file processing settings below",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                        }
                    }
                }

                item {
                    // Base path section
                    EnhancedInputRow(
                        label = "Base Directory Path",
                        value = basePath,
                        onValueChange = { basePath = it },
                        placeholder = "Select or enter directory path...",
                        isLoading = isPathLoading,
                        onFocusAction = {
                            scope.launch {
                                isPathLoading = true
                                try {
                                    basePath = vm.selectDirectory()
                                } finally {
                                    isPathLoading = false
                                }
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Lucide.FolderInput,
                                contentDescription = "Directory",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                    )
                }

                item {
                    // Filter section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Lucide.Filter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Filter Options",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                )
                                Switch(
                                    checked = prefixChecked,
                                    onCheckedChange = { prefixChecked = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    ),
                                )
                            }

                            AnimatedVisibility(
                                visible = prefixChecked,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut(),
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "Remove lines that begin with any of the comma-separated words.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = prefixText,
                                        onValueChange = { prefixText = it },
                                        label = { Text("Prefixes (comma separated)") },
                                        placeholder = { Text("e.g., DEBUG, INFO, ERROR", color = Color.Gray) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    // Action button
                    val isProcessing = uiState.isLoading
                    val isFormValid = basePath.isNotBlank()

                    Button(
                        onClick = { vm.parseAndCreateFile(basePath, prefixText) },
                        enabled = isFormValid && !isProcessing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp,
                        ),
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Processing...")
                        } else {
                            Icon(
                                imageVector = Lucide.Heart,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Generate List",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }

                    if (!isFormValid) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Please provide a base directory path",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onFocusAction: () -> Unit,
    placeholder: String = "",
    isLoading: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = leadingIcon,
            trailingIcon = {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else if (value.isNotEmpty()) {
                    IconButton(
                        onClick = { onValueChange("") },
                    ) {
                        Icon(
                            imageVector = Lucide.Delete,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused && !isLoading) {
                        onFocusAction()
                    }
                },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
            ),
            shape = RoundedCornerShape(12.dp),
        )
    }
}
