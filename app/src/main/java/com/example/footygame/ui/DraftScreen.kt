package com.example.footygame.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.footygame.R
import com.example.footygame.data.ClubSeasons
import com.example.footygame.game.TeamRatings
import com.example.footygame.models.ClubSeason
import com.example.footygame.models.DraftSession
import com.example.footygame.models.Manager
import com.example.footygame.models.Player
import com.example.footygame.models.Position
import com.example.footygame.models.Slot
import com.example.footygame.theme.Chalk
import com.example.footygame.theme.ChalkLine
import com.example.footygame.theme.ChalkMuted
import com.example.footygame.theme.Dugout
import com.example.footygame.theme.DugoutRaised
import com.example.footygame.theme.Floodlight
import com.example.footygame.theme.Hot
import com.example.footygame.ui.components.ChallengeMark
import com.example.footygame.ui.components.ClubBadge
import com.example.footygame.ui.components.EmptySlot
import com.example.footygame.ui.components.Eyebrow
import com.example.footygame.ui.components.PitchCard
import com.example.footygame.ui.components.Pill
import com.example.footygame.ui.components.PlayerToken
import com.example.footygame.ui.components.PrimaryButton
import com.example.footygame.ui.components.RatingBadge
import com.example.footygame.ui.components.SquareIconButton
import com.example.footygame.ui.components.StatTile
import com.example.footygame.ui.components.TOKEN_ASPECT
import com.example.footygame.ui.components.nightBackdrop
import com.example.footygame.ui.components.panel
import com.example.footygame.ui.components.rememberReducedMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun DraftScreen(
    session: DraftSession,
    onChooseSlot: (slotId: String) -> Unit,
    onPick: (playerId: String, slotId: String?) -> Boolean,
    onRespin: () -> Unit,
    onAppointManager: (managerId: String) -> Unit,
    onPlay: () -> Unit,
    onLeave: () -> Unit,
    /** Replaces the challenge mark in the top bar, for career drafts and signings. */
    title: String? = null,
    /** Replaces the mode's play action once the XI is complete. */
    playLabel: String? = null,
    /** False when leaving loses nothing worth a warning, like backing out of a signing. */
    confirmLeave: Boolean = true,
) {
    var showLeaveDialog by rememberSaveable { mutableStateOf(false) }
    // The squad the sheet was opened for. It outlives the spin, so the sheet can slide away after a pick.
    var sheetSquadId by rememberSaveable { mutableStateOf<String?>(null) }
    var sheetSlotId by rememberSaveable { mutableStateOf<String?>(null) }
    var lastPickedId by remember { mutableStateOf<String?>(null) }

    val reducedMotion = rememberReducedMotion()
    val pool = remember(session.mode, session.settings) { ClubSeasons.poolFor(session.mode, session.settings) }
    val ratings = remember(session.picks) { TeamRatings.of(session.picks.values) }
    var reel by remember { mutableStateOf(session.spin) }
    var spinning by remember { mutableStateOf(false) }
    var reeledSpinNumber by rememberSaveable { mutableIntStateOf(0) }

    // Slot-machine reel through other squads before landing on the real spin.
    LaunchedEffect(session.spinNumber) {
        val target = session.spin
        if (target != null && !reducedMotion && session.spinNumber != reeledSpinNumber) {
            spinning = true
            repeat(REEL_TICKS) { tick ->
                reel = pool.random()
                delay(40L + tick * 14L)
            }
        }
        reel = target
        spinning = false
        reeledSpinNumber = session.spinNumber
    }

    fun openSheet(slotId: String?) {
        sheetSquadId = session.spin?.id
        sheetSlotId = session.targetSlotId ?: slotId
    }

    val onSlotClick = { slot: Slot -> if (session.awaitingSlotChoice) onChooseSlot(slot.id) else openSheet(slot.id) }
    val warnOnLeave = confirmLeave && session.picks.isNotEmpty()
    val requestLeave = { if (warnOnLeave) showLeaveDialog = true else onLeave() }
    BackHandler(enabled = warnOnLeave) { showLeaveDialog = true }

    Column(
        Modifier
            .fillMaxSize()
            .nightBackdrop()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .navigationBarsPadding(),
    ) {
        DraftTopBar(session, title, requestLeave)
        RatingsRow(ratings, visible = session.ratingsVisible)
        SpinPanel(
            session = session,
            reel = reel,
            spinning = spinning,
            onRespin = onRespin,
            onPickPlayer = { openSheet(null) },
            onAppointManager = onAppointManager,
            onPlay = onPlay,
            playLabel = playLabel,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
        PitchArea(
            session, spinning, lastPickedId, onSlotClick,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp),
        )
    }

    // Summer signings can land outside the mode's pool (a European squad), so fall back to every squad.
    val sheetSquad = sheetSquadId?.let { id ->
        session.spin?.takeIf { it.id == id } ?: pool.firstOrNull { it.id == id } ?: ClubSeasons.squad(id)
    }
    if (sheetSquad != null) {
        SquadSheet(
            session = session,
            squad = sheetSquad,
            slotId = sheetSlotId,
            onShowWholeSquad = { sheetSlotId = null },
            onPick = { player, slotId -> onPick(player.id, slotId).also { if (it) lastPickedId = player.id } },
            onRespin = onRespin,
            onDismiss = { sheetSquadId = null },
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            containerColor = DugoutRaised,
            title = { Text(stringResource(R.string.draft_leave_title)) },
            text = { Text(stringResource(R.string.draft_leave_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveDialog = false
                    onLeave()
                }) { Text(stringResource(R.string.draft_leave_confirm), color = Hot) }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text(stringResource(R.string.draft_leave_cancel), color = Chalk)
                }
            },
        )
    }
}

/** Back, the challenge in its colours (or a career's [title]), then how far the draft has got and its shape. */
@Composable
private fun DraftTopBar(session: DraftSession, title: String?, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SquareIconButton(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.cd_back), onBack)
        if (title != null) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = Chalk, maxLines = 1)
        } else {
            ChallengeMark(session.mode.challenge, MaterialTheme.typography.headlineLarge)
        }
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (session.isComplete) {
                    stringResource(R.string.draft_complete_progress)
                } else {
                    stringResource(R.string.draft_pick_progress, session.picks.size + 1, session.formation.slots.size)
                },
                style = MaterialTheme.typography.titleSmall,
                color = Chalk,
            )
            Eyebrow(session.formation.label)
        }
    }
}

@Composable
private fun RatingsRow(ratings: TeamRatings, visible: Boolean) {
    val empty = ratings == TeamRatings.EMPTY
    val hidden = stringResource(R.string.rating_hidden)
    fun shown(value: Int) = when {
        empty -> "–"
        !visible -> hidden
        else -> value.toString()
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatTile(shown(ratings.overall), stringResource(R.string.stat_overall), Modifier.weight(1f), valueColor = Floodlight)
        StatTile(shown(ratings.attack.roundToInt()), stringResource(R.string.stat_attack), Modifier.weight(1f))
        StatTile(shown(ratings.defence.roundToInt()), stringResource(R.string.stat_defence), Modifier.weight(1f))
        StatTile("${ratings.chemistry}/${TeamRatings.MAX_CHEMISTRY}", stringResource(R.string.stat_chemistry), Modifier.weight(1f))
    }
}

@Composable
private fun PitchArea(
    session: DraftSession,
    spinning: Boolean,
    lastPickedId: String?,
    onSlotClick: (Slot) -> Unit,
    modifier: Modifier = Modifier,
) {
    PitchCard(modifier) {
        Box(Modifier.padding(horizontal = 4.dp, vertical = 6.dp)) {
            FormationLayout(session, spinning, lastPickedId, onSlotClick)
        }
    }
}

@Composable
private fun FormationLayout(
    session: DraftSession,
    spinning: Boolean,
    lastPickedId: String?,
    onSlotClick: (Slot) -> Unit,
) {
    // One pulse shared by every glowing slot, read only when drawing.
    val glow = rememberInfiniteTransition(label = "slotGlow").animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "slotGlowAlpha",
    )

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val formation = session.formation
        val perRow = formation.slots.groupingBy { it.row }.eachCount().values.max()
        val gap = 6.dp
        val rowHeight = maxHeight / formation.rows
        // Floor keeps shapes valid when the pitch gets squeezed (large fonts, short screens); tokens may overlap then.
        val slotWidth = minOf(
            (maxWidth - gap * (perRow + 1)) / perRow,
            (rowHeight - gap) / TOKEN_ASPECT,
            88.dp,
        ).coerceAtLeast(MIN_SLOT_WIDTH)
        val slotHeight = slotWidth * TOKEN_ASPECT

        formation.slots.forEach { slot ->
            val left = (maxWidth * slot.x - slotWidth / 2).coerceIn(0.dp, maxWidth - slotWidth)
            val top = rowHeight * slot.row + (rowHeight - slotHeight) / 2
            val pick = session.picks[slot.id]
            val slotModifier = Modifier.offset(left, top)
            if (pick != null) {
                val description = if (session.ratingsVisible) {
                    stringResource(R.string.cd_slot_filled, slot.label, pick.player.name, pick.player.rating)
                } else {
                    stringResource(R.string.cd_slot_filled_hidden, slot.label, pick.player.name)
                }
                PlayerToken(
                    pick = pick,
                    width = slotWidth,
                    showRating = session.ratingsVisible,
                    animateEntry = pick.player.id == lastPickedId,
                    modifier = slotModifier.semantics { contentDescription = description },
                )
            } else {
                val choosing = session.awaitingSlotChoice
                val locked = session.targetSlotId != null
                val isTarget = slot.id == session.targetSlotId
                val fits = !spinning && session.spin?.players?.any { session.canPlace(it, slot) } == true
                val highlighted = choosing || (fits && (!locked || isTarget))
                val clickable = !spinning && (choosing || (session.spin != null && (!locked || isTarget)))
                val description = stringResource(
                    when {
                        choosing -> R.string.cd_slot_spin_for
                        highlighted -> R.string.cd_slot_empty_fits
                        else -> R.string.cd_slot_empty
                    },
                    slot.label,
                )
                EmptySlot(
                    label = slot.label,
                    width = slotWidth,
                    highlighted = highlighted,
                    glow = glow::value,
                    modifier = slotModifier
                        .clip(RoundedCornerShape(slotWidth * 0.3f))
                        .clickable(enabled = clickable) { onSlotClick(slot) }
                        .semantics { contentDescription = description }
                        .testTag("slot_${slot.id}"),
                )
            }
        }
    }
}

@Composable
private fun SpinPanel(
    session: DraftSession,
    reel: ClubSeason?,
    spinning: Boolean,
    onRespin: () -> Unit,
    onPickPlayer: () -> Unit,
    onAppointManager: (String) -> Unit,
    onPlay: () -> Unit,
    playLabel: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .panel(RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        val target = session.targetSlot
        when {
            session.needsManager -> ManagerChoice(session.managerOptions, onAppointManager)

            session.isComplete -> {
                Eyebrow(stringResource(R.string.draft_xi_ready), color = Floodlight)
                session.manager?.let {
                    Text(
                        stringResource(R.string.draft_manager_appointed, it.name),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Chalk,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                PrimaryButton(playLabel ?: stringResource(session.mode.playActionRes), onPlay, Modifier.testTag("play"))
            }

            session.awaitingSlotChoice -> {
                Text(
                    stringResource(R.string.draft_choose_slot),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Chalk,
                )
                Text(
                    stringResource(R.string.draft_choose_slot_detail),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChalkMuted,
                )
            }

            reel == null -> Text(stringResource(R.string.draft_no_squads), color = ChalkMuted)

            else -> {
                if (target != null) {
                    Eyebrow(
                        stringResource(R.string.draft_spin_for_slot, target.label),
                        color = Floodlight,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    )
                }
                SpinReadout(reel)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    PrimaryButton(
                        text = if (target != null) {
                            stringResource(R.string.draft_pick_for_slot, target.label)
                        } else {
                            stringResource(R.string.draft_pick_player)
                        },
                        onClick = onPickPlayer,
                        enabled = !spinning,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_player"),
                    )
                    if (session.settings.difficulty.respins > 0) {
                        RespinButton(session.respinsLeft, enabled = session.respinsLeft > 0 && !spinning, onRespin)
                    }
                }
                Text(
                    stringResource(R.string.draft_spin_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = ChalkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        }
    }
}

/** The spin as two halves, club and season, each flicking through the reel. */
@Composable
private fun SpinReadout(reel: ClubSeason) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .panel(RoundedCornerShape(14.dp), color = DugoutRaised)
            .padding(vertical = 10.dp),
    ) {
        ReadoutHalf(stringResource(R.string.draft_spin_club)) {
            ReelText(reel) { squad ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ClubBadge(squad, 30.dp, showCode = false)
                    Text(squad.code, style = MaterialTheme.typography.headlineLarge, color = Chalk, maxLines = 1)
                }
                Text(
                    squad.club,
                    style = MaterialTheme.typography.bodySmall,
                    color = ChalkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box(
            Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(ChalkLine),
        )
        ReadoutHalf(stringResource(R.string.draft_spin_season)) {
            ReelText(reel) { squad ->
                Text(squad.season, style = MaterialTheme.typography.headlineLarge, color = Chalk, maxLines = 1)
                Text(" ", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun RowScope.ReadoutHalf(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .weight(1f)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = ChalkMuted)
        content()
    }
}

@Composable
private fun ReelText(reel: ClubSeason, content: @Composable ColumnScope.(ClubSeason) -> Unit) {
    AnimatedContent(
        targetState = reel,
        transitionSpec = {
            (slideInVertically(tween(90)) { -it } + fadeIn(tween(90)))
                .togetherWith(slideOutVertically(tween(90)) { it } + fadeOut(tween(90)))
        },
        label = "reel",
    ) { squad ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) { content(squad) }
    }
}

@Composable
private fun RespinButton(left: Int, enabled: Boolean, onRespin: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val description = "${stringResource(R.string.draft_respin)}, " +
        pluralStringResource(R.plurals.draft_respins_left, left, left)
    Column(
        Modifier
            .size(52.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(DugoutRaised)
            .border(1.dp, ChalkLine, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onRespin)
            .clearAndSetSemantics { contentDescription = description }
            .testTag("respin"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(painterResource(R.drawable.ic_refresh), contentDescription = null, tint = Chalk, modifier = Modifier.size(18.dp))
        Text(left.toString(), style = MaterialTheme.typography.labelMedium, color = Floodlight)
    }
}

@Composable
private fun ManagerChoice(options: List<Manager>, onAppoint: (String) -> Unit) {
    Eyebrow(stringResource(R.string.draft_appoint_manager), color = Floodlight)
    Spacer(Modifier.height(10.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { manager ->
            val shape = MaterialTheme.shapes.medium
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(DugoutRaised)
                    .border(1.dp, ChalkLine, shape)
                    .clickable { onAppoint(manager.id) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("manager_option"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(manager.name, style = MaterialTheme.typography.titleMedium, color = Chalk)
                    Text(stringResource(manager.trait.detailRes), style = MaterialTheme.typography.bodySmall, color = ChalkMuted)
                }
                Spacer(Modifier.width(10.dp))
                Pill(stringResource(manager.trait.titleRes))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SquadSheet(
    session: DraftSession,
    squad: ClubSeason,
    slotId: String?,
    onShowWholeSquad: () -> Unit,
    onPick: (Player, slotId: String?) -> Boolean,
    onRespin: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // Once the user has acted the sheet only slides away: no second pick, no hint flashing up behind it.
    var closing by remember { mutableStateOf(false) }
    val slot = slotId?.let { id -> session.formation.slots.firstOrNull { it.id == id } }
    val lockedToSlot = session.targetSlotId != null
    val players = if (slot == null) squad.players else squad.players.filter { it.position in slot.accepts }
    val nobodyFits = !closing && slot != null && players.none { session.canPlace(it, slot) }

    fun close() {
        closing = true
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Dugout) {
        LazyColumn(Modifier.fillMaxWidth()) {
            item {
                Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    ClubBadge(squad, 48.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(squad.club, style = MaterialTheme.typography.headlineMedium, color = Chalk)
                        Text(
                            text = if (slot == null) {
                                "${squad.season} · ${stringResource(R.string.draft_sheet_hint)}"
                            } else {
                                "${squad.season} · ${stringResource(R.string.draft_sheet_slot_hint, slot.label)}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = ChalkMuted,
                        )
                    }
                }
            }
            if (slot != null && !lockedToSlot) {
                item {
                    TextButton(onClick = onShowWholeSquad, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text(stringResource(R.string.draft_show_whole_squad), color = Floodlight)
                    }
                }
            }
            if (nobodyFits) {
                item {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Text(stringResource(R.string.draft_nobody_fits, slot.label), color = Chalk)
                        if (session.respinsLeft > 0) {
                            TextButton(onClick = {
                                onRespin()
                                close()
                            }) {
                                Text(
                                    "${stringResource(R.string.draft_respin)} · " +
                                        pluralStringResource(R.plurals.draft_respins_left, session.respinsLeft, session.respinsLeft),
                                    color = Floodlight,
                                )
                            }
                        }
                    }
                }
            }
            Position.entries.forEach { position ->
                val group = players.filter { it.position == position }
                if (group.isNotEmpty()) {
                    item(key = "header_${position.name}") {
                        Eyebrow(
                            stringResource(position.groupRes),
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
                        )
                    }
                    items(group, key = { it.id }) { player ->
                        SquadRow(
                            session = session,
                            player = player,
                            slot = slot,
                            enabled = !closing,
                            onPick = { chosenSlotId -> if (onPick(player, chosenSlotId)) close() },
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/** A squad player. Tapping the row places them automatically; each chip places them in that position. */
@Composable
private fun SquadRow(
    session: DraftSession,
    player: Player,
    slot: Slot?,
    enabled: Boolean,
    onPick: (slotId: String?) -> Unit,
) {
    val targets = if (slot != null) listOfNotNull(slot.takeIf { session.canPlace(player, it) }) else session.slotsFor(player)
    val available = targets.isNotEmpty()
    val status = when {
        session.isDrafted(player) -> stringResource(R.string.draft_in_xi)
        !available -> stringResource(R.string.draft_no_slot)
        else -> null
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && available) { onPick(slot?.id) }
            .alpha(if (available) 1f else 0.4f)
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag(if (available) "eligible_player" else "unavailable_player"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RatingBadge(player.rating, 40.dp, hidden = !session.ratingsVisible)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(player.name, style = MaterialTheme.typography.titleMedium, color = Chalk)
            if (status != null) {
                Text(status, style = MaterialTheme.typography.bodySmall, color = ChalkMuted)
            }
        }
        if (available) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                targets.distinctBy { it.label }.forEach { target ->
                    val description = stringResource(R.string.cd_place_in, target.label)
                    Surface(
                        onClick = { onPick(target.id) },
                        enabled = enabled,
                        color = Hot.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Hot.copy(alpha = 0.7f)),
                        modifier = Modifier.semantics { contentDescription = description },
                    ) {
                        Text(
                            target.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = Chalk,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

private const val REEL_TICKS = 9
private val MIN_SLOT_WIDTH = 36.dp
