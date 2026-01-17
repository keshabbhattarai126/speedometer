package com.example.speedometerkeshab

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AccelSpeedometerScreen(viewModel: AccelerometerViewModel = viewModel()) {
    val state by viewModel.state
    val context = LocalContext.current
    val permissionState = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)

    // Request permission on launch
    LaunchedEffect(Unit) {
        permissionState.launchPermissionRequest()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (permissionState.status.isGranted) {
            Text("GPS Speedometer Active", color = MaterialTheme.colorScheme.primary)

            Text(
                text = "${"%.1f".format(state.speedMps)}",
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold
            )
            Text(text = "KM/H", fontSize = 20.sp, modifier = Modifier.padding(bottom = 48.dp))

            Button(onClick = { if (state.isRunning) viewModel.stopMeasurement() else viewModel.startMeasurement() }) {
                Text(if (state.isRunning) "Stop GPS" else "Start GPS")
            }
        } else {
            Text("GPS Permission is required for speed accuracy.")
            Button(onClick = { permissionState.launchPermissionRequest() }) {
                Text("Grant Permission")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(onClick = {
            context.startActivity(Intent(context, AccelRecieverView::class.java))
        }) {
            Text("Go to Remote View")
        }
    }
}