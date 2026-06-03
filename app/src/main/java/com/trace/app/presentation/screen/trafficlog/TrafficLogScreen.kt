package com.trace.app.presentation.screen.trafficlog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trace.app.domain.model.CapturedTraffic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrafficLogScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: TrafficLogViewModel = hiltViewModel()
) {
    val trafficList by viewModel.trafficList.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isCapturing by viewModel.isCapturing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Traffic Log") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isCapturing) {
                        Icon(
                            Icons.Default.Circle,
                            contentDescription = "Capturing",
                            tint = Color.Red,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search by URL, method, or status") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )

            // Traffic List
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(trafficList) { traffic ->
                    TrafficListItem(
                        traffic = traffic,
                        onClick = { onNavigateToDetail(traffic.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun TrafficListItem(
    traffic: CapturedTraffic,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Method Badge
                Surface(
                    color = getMethodColor(traffic.requestMethod),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = traffic.requestMethod,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // Status Code
                Text(
                    text = traffic.responseStatusCode.toString(),
                    fontWeight = FontWeight.Bold,
                    color = getStatusColor(traffic.responseStatusCode)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // URL
            Text(
                text = traffic.url,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )

            // Duration and Package
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${traffic.durationMs}ms",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = traffic.appPackage,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

private fun getMethodColor(method: String): Color {
    return when (method.uppercase()) {
        "GET" -> Color(0xFF4CAF50)
        "POST" -> Color(0xFF2196F3)
        "PUT" -> Color(0xFFFF9800)
        "DELETE" -> Color(0xFFF44336)
        "PATCH" -> Color(0xFF9C27B0)
        else -> Color.Gray
    }
}

private fun getStatusColor(statusCode: Int): Color {
    return when {
        statusCode in 200..299 -> Color(0xFF4CAF50)
        statusCode in 300..399 -> Color(0xFFFF9800)
        statusCode in 400..499 -> Color(0xFFF44336)
        statusCode in 500..599 -> Color(0xFFD32F2F)
        else -> Color.Gray
    }
}
