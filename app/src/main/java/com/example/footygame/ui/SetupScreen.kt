package com.example.footygame.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.footygame.R
import com.example.footygame.data.ClubSeasons
import com.example.footygame.models.Difficulty
import com.example.footygame.models.DraftMode
import com.example.footygame.models.DraftSettings
import com.example.footygame.models.DraftStyle
import com.example.footygame.models.EraPreset
import com.example.footygame.models.EraRange
import com.example.footygame.models.Formation
import com.example.footygame.models.RatingMode
import com.example.footygame.models.seasonLabel
import com.example.footygame.theme.Chalk
import com.example.footygame.theme.ChalkLine
import com.example.footygame.theme.ChalkMuted
import com.example.footygame.theme.Dugout
import com.example.footygame.theme.Floodlight
import com.example.footygame.theme.Hot
import com.example.footygame.theme.PitchSlot
import com.example.footygame.theme.ResultLoss
import com.example.footygame.ui.components.ChallengeMark
import com.example.footygame.ui.components.Eyebrow
import com.example.footygame.ui.components.PitchCard
import com.example.footygame.ui.components.PrimaryButton
import com.example.footygame.ui.components.ScreenHeader
import com.example.footygame.ui.components.nightBackdrop
import kotlin.math.roundToInt

@Composable
fun SetupScreen(
    mode: DraftMode,
    settings: DraftSettings,
    onSettingsChange: (DraftSettings) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    val squadCount = remember(mode, settings.era) { ClubSeasons.squadCount(mode, settings.era) }
    val eraValid = squadCount >= ClubSeasons.MIN_SQUADS

    Column(
        Modifier
            .fillMaxSize()
            .nightBackdrop()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        ScreenHeader(mode = mode, title = stringResource(R.string.setup_title), onBack = onBack)

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ChallengeMark(mode.challenge, MaterialTheme.typography.displayMedium)
                    Text(stringResource(mode.descriptionRes), style = MaterialTheme.typography.bodyMedium, color = ChalkMuted)
                }

                FormationSection(settings.formation) { onSettingsChange(settings.copy(formation = it)) }

                Section(R.string.setup_section_difficulty) {
                    OptionRow {
                        Difficulty.entries.forEach { difficulty ->
                            OptionCard(
                                title = stringResource(difficulty.titleRes),
                                detail = stringResource(difficulty.detailRes),
                                selected = settings.difficulty == difficulty,
                                onClick = { onSettingsChange(settings.copy(difficulty = difficulty)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("difficulty_${difficulty.name}"),
                            )
                        }
                    }
                }

                Section(R.string.setup_section_ratings) {
                    val forcedHidden = settings.difficulty.hidesRatings
                    OptionRow {
                        OptionCard(
                            title = stringResource(R.string.ratings_on),
                            detail = stringResource(R.string.ratings_on_detail),
                            selected = settings.ratingsShown,
                            enabled = !forcedHidden,
                            onClick = { onSettingsChange(settings.copy(showRatings = true)) },
                            modifier = Modifier.weight(1f),
                        )
                        OptionCard(
                            title = stringResource(R.string.ratings_off),
                            detail = stringResource(R.string.ratings_off_detail),
                            selected = !settings.ratingsShown,
                            enabled = !forcedHidden,
                            onClick = { onSettingsChange(settings.copy(showRatings = false)) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ratings_off"),
                        )
                    }
                    if (forcedHidden) Hint(stringResource(R.string.ratings_hard_note))
                }

                Section(R.string.setup_section_style) {
                    OptionRow {
                        DraftStyle.entries.forEach { style ->
                            OptionCard(
                                title = stringResource(style.titleRes),
                                detail = stringResource(style.detailRes),
                                selected = settings.style == style,
                                onClick = { onSettingsChange(settings.copy(style = style)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("style_${style.name}"),
                            )
                        }
                    }
                }

                Section(R.string.setup_section_rating_mode) {
                    OptionRow {
                        RatingMode.entries.forEach { ratingMode ->
                            OptionCard(
                                title = stringResource(ratingMode.titleRes),
                                detail = stringResource(ratingMode.detailRes),
                                selected = settings.ratingMode == ratingMode,
                                onClick = { onSettingsChange(settings.copy(ratingMode = ratingMode)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("rating_mode_${ratingMode.name}"),
                            )
                        }
                    }
                }

                Section(R.string.setup_section_era) {
                    EraPicker(mode, settings.era, squadCount) { onSettingsChange(settings.copy(era = it)) }
                }

                AdvancedSection(mode, settings, onSettingsChange)
            }
        }

        Surface(color = Dugout, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                PrimaryButton(
                    text = stringResource(R.string.setup_start),
                    onClick = onStart,
                    enabled = eraValid,
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .testTag("start_draft"),
                )
            }
        }
    }
}

@Composable
private fun Section(@StringRes title: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Eyebrow(stringResource(title))
        content()
    }
}

@Composable
private fun Hint(text: String, color: Color = ChalkMuted) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = color)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormationSection(selected: Formation, onSelect: (Formation) -> Unit) {
    Section(R.string.setup_section_formation) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.selectableGroup(),
        ) {
            Formation.entries.forEach { formation ->
                SettingChip(
                    label = formation.label,
                    selected = formation == selected,
                    onClick = { onSelect(formation) },
                    modifier = Modifier.testTag("formation_${formation.label}"),
                )
            }
        }
        Hint(stringResource(selected.descriptionRes))
        FormationPreview(
            selected,
            Modifier
                .fillMaxWidth()
                .height(190.dp),
        )
    }
}

/** A miniature pitch with the formation's positions as labelled dots. */
@Composable
private fun FormationPreview(formation: Formation, modifier: Modifier = Modifier) {
    PitchCard(modifier.clearAndSetSemantics { contentDescription = formation.label }, corner = 12.dp) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val dot = 30.dp
            val inset = 8.dp
            val rowHeight = (maxHeight - inset * 2) / formation.rows
            formation.slots.forEach { slot ->
                val left = (maxWidth * slot.x - dot / 2).coerceIn(0.dp, maxWidth - dot)
                val top = inset + rowHeight * slot.row + (rowHeight - dot) / 2
                Box(
                    Modifier
                        .offset(left, top)
                        .size(dot)
                        .background(PitchSlot, CircleShape)
                        .border(1.dp, Chalk.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(slot.label, color = Chalk, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.sp))
                }
            }
        }
    }
}

@Composable
private fun SettingChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        colors = FilterChipDefaults.filterChipColors(
            labelColor = Chalk,
            selectedContainerColor = Hot,
            selectedLabelColor = Chalk,
        ),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = ChalkLine),
        modifier = modifier,
    )
}

@Composable
private fun OptionRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
private fun OptionCard(
    title: String,
    detail: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = MaterialTheme.shapes.medium
    Column(
        modifier
            .fillMaxHeight()
            .alpha(if (enabled) 1f else 0.5f)
            .clip(shape)
            .background(if (selected) Hot.copy(alpha = 0.12f) else Dugout.copy(alpha = 0.9f))
            .border(if (selected) 1.5.dp else 1.dp, if (selected) Hot else ChalkLine, shape)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Chalk, textAlign = TextAlign.Center)
        Text(detail, style = MaterialTheme.typography.bodySmall, color = ChalkMuted, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun EraPicker(mode: DraftMode, era: EraRange?, squadCount: Int, onEraChange: (EraRange?) -> Unit) {
    val years = remember(mode) { ClubSeasons.years(mode) }
    val range = era ?: EraRange(years.first(), years.last())
    val selectedPreset = EraPreset.entries.firstOrNull { ClubSeasons.eraFor(mode, it) == era }
    val fromLabel = seasonLabel(range.from, mode.isTournament)
    val toLabel = seasonLabel(range.to, mode.isTournament)

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
        EraPreset.entries.forEach { preset ->
            SettingChip(
                label = stringResource(preset.labelRes),
                selected = preset == selectedPreset,
                onClick = { onEraChange(ClubSeasons.eraFor(mode, preset)) },
                modifier = Modifier.testTag("era_${preset.name}"),
            )
        }
    }

    val rangeDescription = stringResource(R.string.cd_era_range, fromLabel, toLabel)
    RangeSlider(
        value = years.indexOf(range.from).coerceAtLeast(0).toFloat()..years.indexOf(range.to).let {
            if (it < 0) years.lastIndex else it
        }.toFloat(),
        onValueChange = { value ->
            val from = years[value.start.roundToInt().coerceIn(0, years.lastIndex)]
            val to = years[value.endInclusive.roundToInt().coerceIn(0, years.lastIndex)]
            if (from != range.from || to != range.to) onEraChange(ClubSeasons.normalise(mode, EraRange(from, to)))
        },
        valueRange = 0f..years.lastIndex.toFloat(),
        steps = (years.size - 2).coerceAtLeast(0),
        colors = SliderDefaults.colors(
            thumbColor = Hot,
            activeTrackColor = Hot,
            inactiveTrackColor = ChalkLine,
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent,
        ),
        modifier = Modifier.semantics { contentDescription = rangeDescription },
    )

    val inRange = years.count { it in range }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(fromLabel, style = MaterialTheme.typography.labelLarge, color = Floodlight)
        Text(
            stringResource(if (mode.isTournament) R.string.era_tournaments else R.string.era_seasons, inRange, years.size),
            style = MaterialTheme.typography.bodySmall,
            color = ChalkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Text(toLabel, style = MaterialTheme.typography.labelLarge, color = Floodlight)
    }
    if (squadCount >= ClubSeasons.MIN_SQUADS) {
        Hint(stringResource(R.string.era_hint))
    } else {
        Hint(stringResource(R.string.era_too_narrow, ClubSeasons.MIN_SQUADS, squadCount), color = ResultLoss)
    }
}

@Composable
private fun AdvancedSection(mode: DraftMode, settings: DraftSettings, onSettingsChange: (DraftSettings) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    val chevron by animateFloatAsState(if (open) 180f else 0f, label = "chevron")

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable { open = !open }
                .padding(vertical = 8.dp)
                .testTag("advanced"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Eyebrow(stringResource(R.string.setup_section_advanced), modifier = Modifier.weight(1f))
            Icon(
                painterResource(R.drawable.ic_expand_more),
                contentDescription = stringResource(if (open) R.string.cd_hide_advanced else R.string.cd_show_advanced),
                tint = ChalkMuted,
                modifier = Modifier.rotate(chevron),
            )
        }
        AnimatedVisibility(visible = open) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ToggleCard(
                    title = stringResource(R.string.advanced_managers),
                    detail = stringResource(R.string.advanced_managers_detail),
                    checked = settings.managers,
                    onCheckedChange = { onSettingsChange(settings.copy(managers = it)) },
                    modifier = Modifier.testTag("toggle_managers"),
                )
                if (mode.hasEuropeanNights) {
                    ToggleCard(
                        title = stringResource(R.string.advanced_europe),
                        detail = stringResource(R.string.advanced_europe_detail),
                        checked = settings.europeanNights,
                        onCheckedChange = { onSettingsChange(settings.copy(europeanNights = it)) },
                        modifier = Modifier.testTag("toggle_europe"),
                    )
                }
                if (mode.hasJanuaryWindow) {
                    ToggleCard(
                        title = stringResource(R.string.advanced_january),
                        detail = stringResource(R.string.advanced_january_detail),
                        checked = settings.januaryWindow,
                        onCheckedChange = { onSettingsChange(settings.copy(januaryWindow = it)) },
                        modifier = Modifier.testTag("toggle_january"),
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleCard(
    title: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Dugout.copy(alpha = 0.9f))
            .border(1.dp, if (checked) Hot.copy(alpha = 0.6f) else ChalkLine, shape)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Chalk,
                checkedTrackColor = Hot,
                uncheckedThumbColor = ChalkMuted,
                uncheckedTrackColor = Dugout,
                uncheckedBorderColor = ChalkMuted,
            ),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Chalk)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = ChalkMuted)
        }
    }
}
