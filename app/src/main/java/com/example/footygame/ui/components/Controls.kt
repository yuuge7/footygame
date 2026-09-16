package com.example.footygame.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.footygame.R
import com.example.footygame.models.DraftMode
import com.example.footygame.theme.Chalk
import com.example.footygame.theme.ChalkMuted
import com.example.footygame.theme.Floodlight
import com.example.footygame.theme.Ink
import com.example.footygame.ui.titleRes

private val ButtonHeight = 54.dp

/** Back arrow, the challenge as an eyebrow, the screen's big title, and an optional action on the right. */
@Composable
fun ScreenHeader(
    mode: DraftMode,
    title: String,
    onBack: () -> Unit,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 8.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.cd_back), tint = Chalk)
        }
        Column(Modifier.weight(1f)) {
            Eyebrow("${mode.challenge} · ${stringResource(mode.titleRes)}")
            Text(title.uppercase(), style = MaterialTheme.typography.headlineSmall, color = Chalk)
        }
        trailing()
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = Floodlight, contentColor = Ink),
        modifier = modifier
            .fillMaxWidth()
            .height(ButtonHeight),
    ) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, ChalkMuted),
        modifier = modifier
            .fillMaxWidth()
            .height(ButtonHeight),
    ) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelLarge, color = Chalk)
    }
}
