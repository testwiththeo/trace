package com.trace.app.presentation.screen.trafficdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrafficDetailScreen(
    trafficId: Long,
    onNavigateBack: () -> Unit,
    viewModel: TrafficDetailViewModel = hiltViewModel()
) {
    val traffic by viewModel.traffic.collectAsState()

    LaunchedEffect(trafficId) {
        viewModel.loadTraffic(trafficId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Traffic Detail") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (traffic == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Method and URL
                Text(
                    text = "${traffic!!.requestMethod} ${traffic!!.url}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Status and Duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Status: ${traffic!!.responseStatusCode}")
                    Text("Duration: ${traffic!!.durationMs}ms")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Request Section
                Text(
                    text = "Request",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Headers:", fontWeight = FontWeight.Bold)
                        traffic!!.requestHeaders.forEach { (key, value) ->
                            Text("$key: $value", style = MaterialTheme.typography.bodySmall)
                        }

                        if (traffic!!.requestBody != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Body:", fontWeight = FontWeight.Bold)
                            Text(
                                text = traffic!!.requestBody!!,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Response Section
                Text(
                    text = "Response",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Headers:", fontWeight = FontWeight.Bold)
                        traffic!!.responseHeaders.forEach { (key, value) ->
                            Text("$key: $value", style = MaterialTheme.typography.bodySmall)
                        }

                        if (traffic!!.responseBody != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Body:", fontWeight = FontWeight.Bold)
                            Text(
                                text = traffic!!.responseBody!!,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
