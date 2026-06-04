package com.trace.app.presentation.screen.mockrules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trace.app.domain.model.MockRule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockRulesScreen(
    onNavigateBack: () -> Unit,
    viewModel: MockRulesViewModel = hiltViewModel()
) {
    val rules by viewModel.rules.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mock Rules") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Rule")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(rules) { rule ->
                MockRuleItem(
                    rule = rule,
                    onToggle = { viewModel.toggleRule(rule.id, !rule.isEnabled) },
                    onDelete = { viewModel.deleteRule(rule) }
                )
            }
        }
    }

    if (showDialog) {
        AddMockRuleDialog(
            onDismiss = { showDialog = false },
            onAdd = { urlPattern, statusCode, body ->
                viewModel.addRule(urlPattern, statusCode, body)
                showDialog = false
            }
        )
    }
}

@Composable
fun MockRuleItem(
    rule: MockRule,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = rule.isEnabled,
                onCheckedChange = { onToggle() }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.urlPattern,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "→ ${rule.responseStatusCode}",
                    style = MaterialTheme.typography.bodySmall
                )
                rule.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
fun AddMockRuleDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Int, String) -> Unit
) {
    var urlPattern by remember { mutableStateOf("") }
    var statusCode by remember { mutableStateOf("200") }
    var body by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Mock Rule") },
        text = {
            Column {
                OutlinedTextField(
                    value = urlPattern,
                    onValueChange = { urlPattern = it },
                    label = { Text("URL Pattern") },
                    placeholder = { Text("*/api/users/*") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = statusCode,
                    onValueChange = { statusCode = it },
                    label = { Text("Status Code") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Response Body") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onAdd(urlPattern, statusCode.toIntOrNull() ?: 200, body)
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
