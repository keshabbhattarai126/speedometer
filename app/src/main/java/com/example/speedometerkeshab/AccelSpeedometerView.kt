package com.example.speedometerkeshab

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel


@Composable
fun AccelSpeedometerScreen(
    // Ensure the correct ViewModel is imported and used
    viewModel: AccelerometerViewModel = viewModel()
) {
    // Collect the state from the ViewModel
    val state by viewModel.state
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Warning about accuracy
        Text(
            text = "This uses the accelerometer only - Value is Scaled Acceleration (m/s²)",
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // The digital display
        Text(
            // Display the scaled value
            text = "${"%.1f".format(state.speedMps)}",
            fontSize = 80.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            // Updated text to reflect it is not true speed
            text = "SCALED KM/H",
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        // Control Buttons
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isRunning) {
                Button(onClick = viewModel::stopMeasurement) {
                    Text("Stop & Reset")
                }
            } else {
                Button(onClick = viewModel::startMeasurement) {
                    Text("Start Measurement")
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))

        // NAVIGATE USING INTENT
        OutlinedButton(onClick = {
            val intent = Intent(context, AccelRecieverView::class.java)
            context.startActivity(intent)
        }) {
            Text("Go to Remote View")
        }
    }
}