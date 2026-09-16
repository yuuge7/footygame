package com.example.footygame.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.footygame.R
import com.example.footygame.data.ModeRecord
import com.example.footygame.models.DraftMode
import com.example.footygame.theme.Chalk
import com.example.footygame.theme.ChalkLine
import com.example.footygame.theme.ChalkMuted
import com.example.footygame.theme.Dugout
import com.example.footygame.theme.Floodlight
import com.example.footygame.theme.FoilGold
import com.example.footygame.theme.Ink
import com.example.footygame.ui.components.Eyebrow
import com.example.footygame.ui.components.foilFrame
import com.example.footygame.ui.components.mownStripes

@Composable
fun MainMenuScreen(
    records: Map<DraftMode, ModeRecord>,
    onModeSelected: (DraftMode) -> Unit,
) {
    var showHowToPlay by rememberSaveable { mutableStateOf(false) }
    // Play the entrance once per visit to the app, not every time the user comes back to the menu.
    var hasEntered by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { hasEntered = true }

    LazyColumn(
        // Stripes fill the screen; cards read best at phone width, so on landscape and tablets the column stays centred.
        modifier = Modifier
            .fillMaxSize()
            .mownStripes()
            .wrapContentWidth()
            .widthIn(max = 640.dp),
        contentPadding = WindowInsets.safeDrawing
            .add(WindowInsets(left = 20.dp, top = 12.dp, right = 20.dp, bottom = 24.dp))
            .asPaddingValues(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Box(Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = { showHowToPlay = true },
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_help),
                        contentDescription = stringResource(R.string.menu_how_to_play),
                        tint = ChalkMuted,
                    )
                }
                Column(Modifier.padding(top = 36.dp)) {
                    WordmarkSticker()
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.menu_tagline),
                        style = MaterialTheme.typography.bodyLarge,
                        color = ChalkMuted,
                        modifier = Modifier.widthIn(max = 320.dp),
                    )
                    Spacer(Modifier.height(20.dp))
                    Eyebrow(stringResource(R.string.menu_choose))
                }
            }
        }
        itemsIndexed(DraftMode.entries, key = { _, mode -> mode.name }) { index, mode ->
            val visibility = remember { MutableTransitionState(hasEntered) }.apply { targetState = true }
            AnimatedVisibility(
                visibleState = visibility,
                enter = fadeIn(tween(350, delayMillis = 80 * index)) +
                    slideInVertically(tween(350, delayMillis = 80 * index)) { it / 3 },
            ) {
                ModeCard(mode = mode, record = records[mode] ?: ModeRecord(), onClick = { onModeSelected(mode) })
            }
        }
    }

    if (showHowToPlay) HowToPlayDialog(onDismiss = { showHowToPlay = false })
}

/** The title as the first sticker in the album. */
@Composable
private fun WordmarkSticker() {
    val style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp, lineHeight = 64.sp)
    Column(
        modifier = Modifier
            .rotate(-3f)
            .shadow(10.dp, RoundedCornerShape(18.dp))
            .foilFrame(FoilGold, corner = 18.dp, thickness = 5.dp)
            .padding(horizontal = 22.dp, vertical = 14.dp),
    ) {
        Text(stringResource(R.string.menu_title_top).uppercase(), style = style, color = Ink)
        Text(stringResource(R.string.menu_title_bottom).uppercase(), style = style, color = Ink)
    }
}

@Composable
private fun ModeCard(mode: DraftMode, record: ModeRecord, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = Dugout.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, ChalkLine),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mode_${mode.name}"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = mode.challenge,
                style = MaterialTheme.typography.displayMedium,
                color = Floodlight,
                // Wide enough for "38-0" so every card's title starts on the same line.
                modifier = Modifier.widthIn(min = 116.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(mode.titleRes).uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Chalk,
                )
                Text(
                    text = stringResource(mode.descriptionRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChalkMuted,
                )
                Spacer(Modifier.height(4.dp))
                Eyebrow(recordLine(record), color = if (record.runs > 0) Chalk else ChalkMuted)
            }
        }
    }
}

@Composable
private fun recordLine(record: ModeRecord): String {
    if (record.runs == 0) return stringResource(R.string.record_none)
    val parts = buildList {
        add(stringResource(R.string.record_best, record.bestWins, record.bestDraws, record.bestLosses))
        if (record.perfectRuns > 0) add(pluralStringResource(R.plurals.record_perfect, record.perfectRuns, record.perfectRuns))
        if (record.trophies > 0) add(pluralStringResource(R.plurals.record_trophies, record.trophies, record.trophies))
    }
    return parts.joinToString(" · ")
}

@Composable
private fun HowToPlayDialog(onDismiss: () -> Unit) {
    val steps = listOf(
        R.string.how_to_play_step_1,
        R.string.how_to_play_step_2,
        R.string.how_to_play_step_3,
        R.string.how_to_play_step_4,
        R.string.how_to_play_step_5,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.how_to_play_title).uppercase(), style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                steps.forEachIndexed { index, step ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = (index + 1).toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = Floodlight,
                            modifier = Modifier.widthIn(min = 16.dp),
                        )
                        Text(stringResource(step), style = MaterialTheme.typography.bodyMedium, color = Chalk)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_got_it)) }
        },
    )
}
