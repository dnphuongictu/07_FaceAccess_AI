package com.faceaccess.app.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.faceaccess.app.R
import com.faceaccess.app.ui.theme.FaceAccessAccentGreen
import com.faceaccess.app.ui.theme.FaceAccessAccentRed

/** Trang thai hien thi cua bang dieu khien noi; khong chua du lieu nhay cam. */
data class OverlayUiState(
    val currentLabel: String = "-",
    val scanIndex: Int = 0,
    val scanSize: Int = 0,
    val faceDetected: Boolean = false,
    val earLeft: Float = 0f,
    val earRight: Float = 0f,
    val yawDeg: Float = 0f,
    val pitchDeg: Float = 0f,
)

@Composable
fun OverlayContent(state: OverlayUiState, onStopClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(20.dp)),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (state.faceDetected) FaceAccessAccentGreen else FaceAccessAccentRed),
                )
                Text(
                    text = if (state.faceDetected) {
                        stringResource(R.string.status_face_ok)
                    } else {
                        stringResource(R.string.status_face_lost)
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Text(
                text = state.currentLabel,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )

            if (state.faceDetected) {
                Text(
                    text = "EAR %.2f/%.2f  Yaw %.1f°  Pitch %.1f°".format(
                        state.earLeft,
                        state.earRight,
                        state.yawDeg,
                        state.pitchDeg,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                )
            }

            if (state.scanSize > 0) {
                Text(
                    text = "${state.scanIndex + 1}/${state.scanSize}",
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Button(
                onClick = onStopClick,
                colors = ButtonDefaults.buttonColors(containerColor = FaceAccessAccentRed),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_emergency_stop))
            }
        }
    }
}
