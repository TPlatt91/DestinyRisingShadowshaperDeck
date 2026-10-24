package com.tplatt.shadowshaper

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Compose portrait battle UI. Rules and AI are resolved by the shared engine. */
class ShadowBattleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        // The original Destiny: Rising duel occupies the whole display,
        // without an Android status/navigation band consuming arena space.
        // Immersive-STICKY still allows a swipe to reveal system bars.
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = BattleGold, onPrimary = BattleInk,
                background = BattleInk, onBackground = BattleCream,
                surface = BattleBrown, onSurface = BattleCream
            )) {
                ShadowBattleScreen(
                    requestedDeck = intent.getStringExtra("selected_deck"),
                    onExit = { finish() },
                    onRules = { startActivity(Intent(this, ShadowComposeActivity::class.java)
                        .putExtra("start_page", "rules")) }
                )
            }
        }
    }
}

private val BattleInk = Color(0xFF17110F)
private val BattleBrown = Color(0xFF372820)
private val BattlePanel = Color(0xFF4C3527)
private val BattleGold = Color(0xFFE9C17C)
private val BattleCream = Color(0xFFF8EBD6)
private val BattleBlue = Color(0xFF94C8ED)
private val BattleFaded = Color(0xFFCAB7A2)
private val BattleDanger = Color(0xFFDDA89A)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShadowBattleScreen(requestedDeck: String?, onExit: () -> Unit, onRules: () -> Unit) {
    val context = LocalContext.current
    val sounds = remember(context) { DuelSoundPlayer(context.applicationContext) }
    var audioEnabled by remember(sounds) { mutableStateOf(sounds.enabled) }
    var audioVolume by remember(sounds) { mutableFloatStateOf(sounds.volume) }
    LaunchedEffect(sounds) { sounds.prepare() }
    DisposableEffect(sounds) { onDispose { sounds.close() } }
    val screenConfig = LocalConfiguration.current
    val compactLayout = screenConfig.screenWidthDp < 790
    val catalog = remember { ShadowData.load(context) }
    val saves = remember { ShadowDeckRepository(context) }
    val practice = remember(catalog) { ShadowData.opponentDeck(catalog) }
    val matchRepository = remember(context) { ShadowMatchRepository(context) }
    val savedAtLaunch = remember(matchRepository) { matchRepository.load() }
    var resumeChoice by remember { mutableStateOf(
        savedAtLaunch != null &&
            !((context as? android.app.Activity)?.intent
                ?.getBooleanExtra("resume_last_match", false) ?: false)
    ) }
    var restoringError by remember { mutableStateOf<String?>(null) }
    var chosenDeckName by remember { mutableStateOf("Starter Practice Deck") }
    var selectedCpuFaction by remember { mutableStateOf("Mixed") }
    var freshOpeningAudio by remember { mutableStateOf(true) }
    // Existing restored board units should not re-enter visually. Units
    // placed AFTER this revision should animate normally on resumed games.
    var resumedAtRevision by remember { mutableIntStateOf(-1) }
    var coordinator by remember { mutableStateOf<ShadowBattleCoordinator?>(null) }
    var inspectingFinalBoard by remember { mutableStateOf(false) }
    val current = coordinator
    val snapshotState = current?.state?.collectAsState()
    val snapshot = snapshotState?.value
    val engine = current?.engine
    // Remember delivered journal entries and turn status per match, so
    // recomposition never replays SFX for earlier combat.
    var lastAudioJournal by remember(current) { mutableStateOf<List<String>?>(null) }
    var lastAudioTurn by remember(current) { mutableStateOf<Pair<Int, Boolean>?>(null) }
    var matchOutcomePlayed by remember(current) { mutableStateOf(false) }
    var lastAudioDraws by remember(current) {
        mutableStateOf<DuelDrawAudioSnapshot?>(null)
    }

    var deckDialog by remember { mutableStateOf(savedAtLaunch == null) }
    var factionDialog by remember { mutableStateOf(false) }
    var openingDialog by remember { mutableStateOf(false) }
    var endConfirmation by remember { mutableStateOf(false) }
    var settingsDialog by remember { mutableStateOf(false) }
    var exitConfirmation by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }
    var chosenDeck by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var chosenCover by remember { mutableStateOf("default") }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var pendingPosition by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var inspectedUnit by remember { mutableStateOf<ShadowEngine.Unit?>(null) }
    var inspectedCard by remember { mutableStateOf<ShadowData.Card?>(null) }
    var message by remember { mutableStateOf("Tap a card, then choose an empty position.") }
    var turnAnnouncement by remember { mutableStateOf<String?>(null) }
    var effectAnnouncement by remember { mutableStateOf<String?>(null) }
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    // Visual-only hand-to-slot travel. Coordinates use the same window-space
    // geometry as drag hit testing; they are never game rules or extra slots.
    var pendingFingerRelease by remember { mutableStateOf<Offset?>(null) }
    var latestHumanLaunch by remember { mutableStateOf<DuelHandLaunch?>(null) }
    var rootTopLeftInWindow by remember { mutableStateOf(Offset.Zero) }
    val cardBounds = remember { mutableStateMapOf<Int, Rect>() }
    // A card drag must win horizontal gesture recognition over the hand list.
    // Visible navigation controls scroll the list when it overflows.
    val handListState = rememberLazyListState()
    val handScrollScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val floatingCardHeightPx = with(density) { 130.dp.toPx() }
    val slotBounds = remember { mutableStateMapOf<String, Rect>() }
    fun resumeMatch(saved: ShadowSavedMatch) {
        try {
            val restored = ShadowBattleCoordinator(catalog, saved.engine.seed)
            restored.resume(saved.engine)
            // Persistence reattaches only AFTER restoring, so the restored
            // deck is not accidentally shuffled or overwritten.
            restored.setCheckpointListener { live ->
                matchRepository.save(saved.copy(engine = ShadowMatchCodec.capture(live)))
            }
            coordinator = restored
            chosenDeck = saved.playerDeck
            chosenCover = saved.coverId
            chosenDeckName = saved.deckName
            selectedCpuFaction = saved.cpuFaction
            freshOpeningAudio = false
            resumedAtRevision = restored.engine.placementRevision
            selectedIndex = -1
            pendingPosition = null
            draggedIndex = -1
            inspectedUnit = null
            inspectedCard = null
            openingDialog = false
            deckDialog = false
            factionDialog = false
            resumeChoice = false
            inspectingFinalBoard = false
            message = "Match restored. Continue playing."
        } catch (exception: Exception) {
            matchRepository.clear()
            resumeChoice = false
            deckDialog = true
            restoringError = "Saved match could not be recovered: ${exception.message}"
        }
    }

    // Resume directly only when the player explicitly selected CONTINUE on
    // the launcher. Ordinary Battle always asks Continue or Start New.
    LaunchedEffect(savedAtLaunch) {
        if (savedAtLaunch != null &&
            (context as? android.app.Activity)?.intent
                ?.getBooleanExtra("resume_last_match", false) == true) {
            resumeMatch(savedAtLaunch)
        }
    }

    val validDecks = remember(catalog, deckDialog) {
        buildList {
            if (ShadowDeckRules.validate(practice, catalog).isEmpty())
                add("Starter Practice Deck" to practice)
            saves.deckNames().forEach { name ->
                val deck = saves.read(name)
                if (ShadowDeckRules.validate(deck, catalog).isEmpty()) add(name to deck)
            }
        }
    }

    // A legal deck explicitly selected in Deck Construction should continue
    // directly to opponent choice, without forcing a second deck selection.
    LaunchedEffect(requestedDeck, resumeChoice) {
        if (!requestedDeck.isNullOrBlank() && !resumeChoice && coordinator == null) {
            val saved = validDecks.firstOrNull { it.first == requestedDeck }
            if (saved != null) {
                chosenDeck = saved.second
                chosenDeckName = requestedDeck
                chosenCover = if (requestedDeck == "Starter Practice Deck") "default" else saves.cover(requestedDeck)
                deckDialog = false
                factionDialog = true
            }
        }
    }

    fun startMatch(faction: String) {
        val cpu = ShadowData.opponentDeck(catalog, faction)
        val candidate = ShadowBattleCoordinator(catalog, System.nanoTime())
        val ownError = ShadowDeckRules.validate(chosenDeck, catalog)
        val cpuError = ShadowDeckRules.validate(cpu, catalog)
        if (ownError.isNotEmpty() || cpuError.isNotEmpty()) {
            message = "Cannot start: " + ownError.ifEmpty { cpuError }
            deckDialog = true
            factionDialog = false
            return
        }
        try {
            // Every NEW match gets a fresh RNG seed and new, independently
            // shuffled player and CPU draw piles. Replays never inherit the
            // previous match's depleted hand, board or timed effects.
            selectedCpuFaction = faction
            freshOpeningAudio = true
            resumedAtRevision = -1
            candidate.setCheckpointListener { live ->
                matchRepository.save(
                    ShadowSavedMatch(
                        deckName = chosenDeckName,
                        coverId = chosenCover,
                        playerDeck = LinkedHashMap(chosenDeck),
                        cpuFaction = faction,
                        engine = ShadowMatchCodec.capture(live)
                    )
                )
            }
            candidate.start(chosenDeck, cpu)
            coordinator = candidate
            inspectingFinalBoard = false
            showLog = false
            latestHumanLaunch = null
            pendingFingerRelease = null
            selectedIndex = -1
            pendingPosition = null
            inspectedUnit = null
            inspectedCard = null
            deckDialog = false
            factionDialog = false
            openingDialog = true
            message = "Choose a card and position to play."
        } catch (e: Exception) {
            message = "Could not start: " + e.message
            deckDialog = true
            factionDialog = false
        }
    }

    fun recordSuccessfulLaunch(
        e: ShadowEngine,
        handIndex: Int,
        row: Int,
        column: Int,
        release: Offset? = null
    ) {
        val source = release ?: cardBounds[handIndex]?.center
        val destination = slotBounds["0-$row-$column"]?.center
        latestHumanLaunch = if (source != null && destination != null) {
            DuelHandLaunch(
                placementRevision = e.placementRevision,
                row = row,
                column = column,
                offsetX = source.x - destination.x,
                offsetY = source.y - destination.y
            )
        } else null
    }

    fun playAt(row: Int, column: Int) {
        val e = engine ?: return
        val co = current ?: return
        val release = pendingFingerRelease
        pendingFingerRelease = null
        if (e.pendingMovedUnit != null && e.pendingMoveCaster === e.human) {
            message = if (co.resolveMove(e.human, row, column)) "Relocation complete." else e.lastActionError
            return
        }
        val index = selectedIndex
        if (index !in e.human.hand.indices) {
            message = "Select a card from your hand first."
            return
        }
        if (!e.isPlayablePlacement(e.human, index, row, column)) {
            message = e.validatePlay(e.human, index, row, column, null)
                .ifBlank { "Illegal position." }
            return
        }
        val card = e.human.hand[index]
        if (e.isTargeted(card) && e.targetOptions(e.human, card, row, column).isNotEmpty()) {
            sounds.play(DuelSound.TARGET_LOCK)
            pendingPosition = row to column
            message = if (card.name == "Rossi-11")
                "Choose a highlighted ally or SHIELD SELF."
            else "Choose a gold-highlighted target for " + card.name
        } else {
            if (co.play(e.human, index, row, column, null)) {
                recordSuccessfulLaunch(e, index, row, column, release)
                selectedIndex = -1
                pendingPosition = null
                message = "Card played."
            } else message = e.lastActionError
        }
    }

    fun chooseTarget(owner: ShadowEngine.Player, row: Int, column: Int) {
        val e = engine ?: return
        val co = current ?: return
        val position = pendingPosition ?: return
        if (selectedIndex !in e.human.hand.indices) return
        if (co.play(e.human, selectedIndex, position.first, position.second,
                ShadowEngine.Target(owner, row, column))) {
            recordSuccessfulLaunch(e, selectedIndex, position.first, position.second)
            sounds.play(DuelSound.ABILITY_TRIGGER)
            selectedIndex = -1
            pendingPosition = null
            message = "Effect resolved."
        } else message = e.lastActionError
    }

    fun shieldRossiSelf() {
        val e = engine ?: return
        val co = current ?: return
        val position = pendingPosition ?: return
        val card = e.human.hand.getOrNull(selectedIndex) ?: return
        if (card.name != "Rossi-11") return
        if (co.play(e.human, selectedIndex, position.first, position.second, null)) {
            recordSuccessfulLaunch(e, selectedIndex, position.first, position.second)
            sounds.play(DuelSound.ABILITY_TRIGGER)
            selectedIndex = -1
            pendingPosition = null
            message = "Overshield granted to Rossi-11."
        } else message = e.lastActionError
    }

    BackHandler(settingsDialog || inspectingFinalBoard || engine != null) {
        when {
            settingsDialog -> settingsDialog = false
            inspectedUnit != null || inspectedCard != null -> {
                inspectedUnit = null
                inspectedCard = null
            }
            showLog -> showLog = false
            inspectingFinalBoard -> inspectingFinalBoard = false
            snapshot?.finished == true -> Unit
            else -> exitConfirmation = true
        }
    }

    // CPU acts once per coroutine tick so the user sees individual plays.
    LaunchedEffect(current, snapshot?.playerTurn, snapshot?.revision, snapshot?.round,
        openingDialog, settingsDialog, inspectedUnit, inspectedCard,
        showLog, deckDialog, factionDialog) {
        if (current != null && snapshot != null && !snapshot.finished && !snapshot.playerTurn &&
            !openingDialog && !settingsDialog && inspectedUnit == null && inspectedCard == null &&
            !showLog && !deckDialog && !factionDialog) {
            // Allow the faction's 170ms precharge and the existing 760ms
            // card slam/bounce to complete before the next CPU card enters.
            // This affects presentation pacing, not AI decisions or rules.
            delay(1070)
            if (!current.cpuPlayOne()) current.endTurn()
        }
    }

    // Cinematic but short: use existing engine turn/event data. The game
    // continues to advance exclusively through the battle coordinator.
    LaunchedEffect(current, snapshot?.round, snapshot?.playerTurn) {
        if (snapshot == null || current == null || snapshot.finished) {
            turnAnnouncement = null
        } else {
            turnAnnouncement = "ROUND ${snapshot.round}  •  " +
                if (snapshot.playerTurn) "YOUR TURN" else "OPPONENT TURN"
            delay(1100)
            turnAnnouncement = null
        }
    }
    LaunchedEffect(current, snapshot?.revision) {
        if (snapshot == null || snapshot.revision == 0 || current == null) {
            effectAnnouncement = null
        } else {
            effectAnnouncement = current.engine.journal.lastOrNull()?.take(100)
            delay(1450)
            effectAnnouncement = null
        }
    }

    // Engine.start() really shuffles both decks and deals an opening hand.
    // Run only once for this match, after asynchronously loaded samples have
    // become available. Do not invent a mulligan or discard gameplay event.
    LaunchedEffect(current, freshOpeningAudio) {
        if (current != null && freshOpeningAudio) {
            var attempts = 0
            while (!sounds.isReady(DuelSound.SHUFFLE) && attempts < 32) {
                delay(45)
                attempts++
            }
            sounds.play(DuelSound.SHUFFLE)
            delay(230)
            sounds.play(DuelSound.OPENING_DEAL)
            repeat(current.engine.human.hand.size.coerceAtMost(5)) {
                delay(110)
                sounds.play(DuelSound.DRAW_SINGLE)
            }
        }
    }

    // Listen only to actual state changes; never play unconfirmed clicks as
    // a card landing. Card arrival/impact SFX are synchronized separately in
    // DuelAnimatedUnit with its 170ms transmat + 760ms landing timeline.
    LaunchedEffect(
        current, snapshot?.revision, snapshot?.round, snapshot?.playerTurn,
        snapshot?.playerPower, snapshot?.cpuPower,
        snapshot?.playerLight, snapshot?.cpuLight,
        snapshot?.playerHand, snapshot?.cpuHand, snapshot?.finished
    ) {
        if (current != null && snapshot != null) {
            val journalNow = current.engine.journal.toList()
            val prior = lastAudioJournal
            if (prior != null) {
                DuelSoundEvents.newLines(prior, journalNow)
                    .mapNotNull(DuelSoundEvents::forJournalLine)
                    .takeLast(4)
                    .forEach { cue ->
                        sounds.play(cue)
                        if (cue == DuelSound.DESTROY) {
                            sounds.play(DuelSound.CARD_BREAK)
                        }
                    }
            }
            lastAudioJournal = journalNow

            val drawNow = DuelDrawAudioSnapshot(
                playerDraws = current.engine.humanDrawTotal,
                opponentDraws = current.engine.cpuDrawTotal,
                playerEmptyAttempts = current.engine.humanEmptyDrawTotal,
                opponentEmptyAttempts = current.engine.cpuEmptyDrawTotal
            )
            lastAudioDraws?.let { priorDraws ->
                val change = drawNow.since(priorDraws)
                // Opening hand is separately scored by the shuffle/deal
                // sequence above. Real later draws are counted even when a
                // simultaneous card play hides them from hand-size changes.
                val drawCue = change.sound
                if (drawCue != null) {
                    sounds.play(drawCue)
                    if (change.totalDrawn > 1) {
                        repeat(change.totalDrawn.coerceAtMost(4)) {
                            delay(105)
                            sounds.play(DuelSound.DRAW_SINGLE)
                        }
                    }
                }
                if (change.emptyAttempts > 0 && drawCue != DuelSound.DECK_EMPTY) {
                    sounds.play(DuelSound.DECK_EMPTY)
                }
            }
            lastAudioDraws = drawNow
            val key = snapshot.round to snapshot.playerTurn
            if (lastAudioTurn != null && key != lastAudioTurn && !snapshot.finished) {
                sounds.play(DuelSound.TURN)
            }
            lastAudioTurn = key
            if (snapshot.finished && !matchOutcomePlayed) {
                matchOutcomePlayed = true
                sounds.play(
                    if (snapshot.playerPower > snapshot.cpuPower) DuelSound.WIN
                    else DuelSound.LOSE
                )
            }
        }
    }

    Box(Modifier.fillMaxSize().safeDrawingPadding().onGloballyPositioned {
        val position = it.boundsInWindow().topLeft
        if (rootTopLeftInWindow != position) rootTopLeftInWindow = position
    }) {
    Column(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(
                    Color(0xFF111820), Color(0xFF20242A), Color(0xFF141820)
                )
            )
        )
    ) {
        DuelBattleTopBar(
            round = snapshot?.round ?: 1,
            status = when {
                snapshot?.finished == true -> "DUEL COMPLETE"
                engine?.pendingMovedUnit != null &&
                    engine.pendingMoveCaster === engine.human -> "MOVE UNIT"
                pendingPosition != null -> "TARGET"
                snapshot?.playerTurn == true -> "YOUR TURN"
                snapshot != null -> "CPU TURN"
                else -> "SINGLE PLAYER"
            },
            reviewingBoard = inspectingFinalBoard,
            yourTurn = snapshot?.playerTurn == true,
            onNavigation = {
                sounds.play(DuelSound.UI_TAP)
                if (inspectingFinalBoard) inspectingFinalBoard = false
                else settingsDialog = true
            }
        )
        if (engine != null && snapshot != null) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(3.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Stone fills the entire arena, including the space above
                // and below the tightly grouped card rows. No black gutters.
                Box(
                    Modifier.weight(1f).fillMaxWidth()
                        .border(1.dp, Color(0xFF817D75), RoundedCornerShape(5.dp))
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DuelStoneArena(Modifier.fillMaxSize())
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                    Box(
                        Modifier.fillMaxWidth(
                            DuelLayoutMetrics.PORTRAIT_LANE_WIDTH_FRACTION
                        )
                            .height(DuelLayoutMetrics.portraitBoardHeight(
                                screenConfig.screenWidthDp,
                                screenConfig.screenHeightDp
                            ).dp)
                    ) {
                        DuelArenaGrid(Modifier.fillMaxSize())
                        Column(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                    val targets = if (pendingPosition != null && selectedIndex in engine.human.hand.indices)
                        engine.targetOptions(engine.human, engine.human.hand[selectedIndex],
                            pendingPosition!!.first, pendingPosition!!.second)
                    else emptyList()
                    val moving = engine.pendingMovedUnit != null &&
                        engine.pendingMoveCaster === engine.human
                    listOf(
                        Triple("CPU BACK", engine.cpu, 0),
                        Triple("CPU FRONT", engine.cpu, 1),
                        Triple("YOUR FRONT", engine.human, 1),
                        Triple("YOUR BACK", engine.human, 0)
                    ).forEach { (heading, owner, row) ->
                        if (heading == "YOUR FRONT") {
                            Box(
                                Modifier.fillMaxWidth().height(1.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color.Transparent,
                                                Color(0x99C8C3B4),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                            for (col in 0 until ShadowRules.COLUMNS) {
                                val unit = owner.board[row][col]
                                val targeting = targets.any {
                                    it.owner === owner && it.row == row && it.column == col
                                }
                                val relocating = moving && unit == null &&
                                    owner === engine.pendingMovedUnit?.owner
                                val legal = !snapshot.finished && !moving && unit == null &&
                                    owner === engine.human &&
                                    pendingPosition == null && selectedIndex >= 0 &&
                                    engine.isPlayablePlacement(engine.human, selectedIndex, row, col)
                                val id = (if (owner === engine.human) "0" else "1") + "-$row-$col"
                                BoxWithConstraints(
                                    Modifier.weight(1f).fillMaxHeight()
                                        .onGloballyPositioned { slotBounds[id] = it.boundsInWindow() }
                                        .semantics {
                                            contentDescription = heading + " position " + (col + 1) +
                                                if (unit != null) ", " + unit.card.name +
                                                    ", Power " + engine.power(unit)
                                                else ", empty"
                                            if (targeting) stateDescription = "Select as ability target"
                                            else if (relocating) stateDescription = "Available relocation slot"
                                            else if (legal) stateDescription = "Available card placement"
                                        }
                                        .then(
                                            if (!snapshot.finished &&
                                                (targeting || relocating || (owner === engine.human && unit == null)))
                                                Modifier.clickable {
                                                    if (relocating) playAt(row, col)
                                                    else if (targeting) chooseTarget(owner, row, col)
                                                    else if (pendingPosition == null) playAt(row, col)
                                                }
                                            else if (unit != null) Modifier.clickable { inspectedUnit = unit }
                                            else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    DuelSlotBackground(
                                        selected = legal || relocating || targeting,
                                        friendly = owner === engine.human,
                                        occupied = unit != null,
                                        modifier = Modifier.fillMaxSize(),
                                        label = if (legal || relocating || targeting) "+" else ""
                                    )
                                    if (unit != null) {
                                        // Remember when each specific unit
                                        // first entered this rendered slot.
                                        // Multi-unit abilities may summon
                                        // several cards before Compose draws
                                        // the board; animate all newcomers,
                                        // not merely engine.lastPlacedColumn.
                                        val firstVisibleRevision = remember(current, unit) {
                                            engine.placementRevision
                                        }
                                        val newlyVisible =
                                            firstVisibleRevision > 0 &&
                                                firstVisibleRevision == engine.placementRevision
                                        val fit = DuelLayoutMetrics.fittingCard(maxWidth.value, maxHeight.value)
                                        DuelAnimatedUnit(
                                            sounds = sounds,
                                            unit = unit,
                                            currentPower = engine.power(unit),
                                            placementRevision = engine.placementRevision,
                                            isNewestPlacement = !snapshot.finished &&
                                                (resumedAtRevision < 0 ||
                                                    engine.placementRevision > resumedAtRevision) &&
                                                (newlyVisible || DuelPlacementCue.isNewlyPlaced(
                                                placementRevision = engine.placementRevision,
                                                lastSide = engine.lastPlacedSide,
                                                lastRow = engine.lastPlacedRow,
                                                lastColumn = engine.lastPlacedColumn,
                                                side = if (owner === engine.human) 0 else 1,
                                                row = row,
                                                column = col
                                            )),
                                            entryFromHand = if (owner === engine.human) {
                                                latestHumanLaunch?.takeIf {
                                                    it.matches(engine.placementRevision, row, col)
                                                }?.let { Offset(it.offsetX, it.offsetY) }
                                            } else null,
                                            modifier = Modifier.width(fit.width.dp).height(fit.height.dp)
                                                .graphicsLayer {
                                                    // Visual art gains presence inside the
                                                    // same legal slot hitbox; no new zones.
                                                    scaleX = 1.035f
                                                    scaleY = 1.035f
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    }
                    }
                }
                    // In the original game the deck stack and score/light
                    // ornaments surround the centered card lanes on stone.
                    Row(
                        Modifier.align(Alignment.TopStart)
                            .padding(start = 8.dp, top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DuelUtilityPill(snapshot.cpuHand, engine.cpu.drawPile.size)
                    }
                    Column(
                        Modifier.align(Alignment.TopCenter)
                            .padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "ROUND ${snapshot.round}/${ShadowRules.ROUNDS}",
                            color = DuelPalette.cream,
                            fontWeight = FontWeight.Black, fontSize = 12.sp
                        )
                        DuelRoundMarkers(snapshot.round)
                    }
                    Column(
                        Modifier.align(Alignment.TopEnd)
                            .padding(top = 37.dp, end = 9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DuelLightOrb(snapshot.cpuLight, snapshot.round)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "CPU", color = DuelPalette.red,
                                fontSize = 9.sp, fontWeight = FontWeight.Bold
                            )
                            DuelPowerCrest(snapshot.cpuPower, isPlayer = false)
                        }
                    }
                    // The original game places card/deck utilities on the
                    // left and its score gems on the right side of the arena.
                    // The deck pile here reads the real player draw pile.
                    Row(
                        Modifier.align(Alignment.BottomStart)
                            .padding(start = 8.dp, bottom = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DuelDeckCounter(
                            coverId = chosenCover,
                            label = "YOUR DECK",
                            count = engine.human.drawPile.size
                        )
                        TextButton(
                            onClick = { showLog = true },
                            contentPadding = PaddingValues(horizontal = 5.dp)
                        ) {
                            Text("LOG", color = BattleGold, fontSize = 10.sp)
                        }
                    }
                    // Player Power and its blue Light orb echo the two
                    // original right-side jewels, below the opposing score.
                    Row(
                        Modifier.align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 95.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "YOU", color = DuelPalette.gold,
                                fontSize = 9.sp, fontWeight = FontWeight.Black
                            )
                            DuelLightOrb(snapshot.playerLight, snapshot.round)
                        }
                        DuelPowerCrest(snapshot.playerPower, isPlayer = true)
                    }
                    Row(
                        Modifier.align(Alignment.BottomEnd)
                            .padding(end = 7.dp, bottom = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (pendingPosition != null &&
                            engine.human.hand.getOrNull(selectedIndex)?.name == "Rossi-11") {
                            TextButton(onClick = { sounds.play(DuelSound.UI_TAP); shieldRossiSelf() }) {
                                Text("SHIELD", color = DuelPalette.gold, fontSize = 9.sp)
                            }
                        }
                        DuelGoldTurnDial(
                            enabled = snapshot.playerTurn && !snapshot.finished,
                            label = when {
                                pendingPosition != null -> "CANCEL"
                                engine.pendingMovedUnit != null -> "MOVE"
                                else -> "END\nTURN"
                            },
                            onClick = {
                                sounds.play(DuelSound.UI_TAP)
                                if (pendingPosition != null) pendingPosition = null
                                else if (engine.pendingMovedUnit != null)
                                    message = "Choose a relocation slot first."
                                else if (engine.human.points > 0 &&
                                    engine.human.hand.indices.any {
                                        engine.affordable(engine.human, it)
                                    }) endConfirmation = true
                                else current.endTurn()
                            }
                        )
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth()
                    .height(DuelLayoutMetrics.portraitHandHeight(
                        screenConfig.screenHeightDp
                    ).dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF34373A),
                                Color(0xFF202429),
                                Color(0xFF171B20)
                            )
                        )
                    )
                    .border(1.dp, Color(0xFF655E54), RoundedCornerShape(3.dp))
                    .drawBehind {
                        val line = Color(0x23C2BBA7)
                        val pitch = 24.dp.toPx()
                        val h = size.height
                        val w = size.width
                        // Subtle original-style triangle/diamond etching in
                        // the recessed card tray, not behind the card artwork.
                        var x = -pitch
                        while (x < w + pitch) {
                            drawLine(
                                line,
                                Offset(x, h), Offset(x + pitch * 1.35f, 0f),
                                .65.dp.toPx()
                            )
                            drawLine(
                                line.copy(alpha = .11f),
                                Offset(x, 0f), Offset(x + pitch * 1.35f, h),
                                .65.dp.toPx()
                            )
                            x += pitch * 1.7f
                        }
                        drawLine(
                            Color(0xB99C8157),
                            Offset(0f, 1.dp.toPx()), Offset(w, 1.dp.toPx()),
                            1.1.dp.toPx()
                        )
                        drawLine(
                            Color(0x6A111A24),
                            Offset(0f, 3.dp.toPx()), Offset(w, 3.dp.toPx()),
                            1.dp.toPx()
                        )
                    }
                    .padding(horizontal = 4.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(51.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    DuelLightMeter(snapshot.playerLight, snapshot.round, compact = true)
                    Text("HAND ${snapshot.playerHand}",
                        color = BattleFaded, fontSize = 9.sp,
                        fontWeight = FontWeight.Bold, maxLines = 1)
                }
                LazyRow(
                    state = handListState,
                    // Horizontal scrolling previously stole a card's drag
                    // after touch slop. Navigation buttons below handle
                    // overflowing hands; every visible card owns its drag.
                    userScrollEnabled = false,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        DuelLayoutMetrics.portraitHandSpacing(
                            engine.human.hand.size,
                            screenConfig.screenWidthDp
                        ).dp
                    )
                ) {
                    itemsIndexed(engine.human.hand.toList()) { index, card ->
                        Card(
                            modifier = Modifier.width(if (screenConfig.screenWidthDp < 380) 70.dp else 76.dp)
                                .aspectRatio(DuelLayoutMetrics.CARD_WIDTH_TO_HEIGHT)
                                .zIndex(if (selectedIndex == index || draggedIndex == index) 5f else index.toFloat() / 100f)
                                .graphicsLayer {
                                    val midpoint = (engine.human.hand.size - 1) / 2f
                                    rotationZ = if (selectedIndex == index) 0f
                                        else (index - midpoint) * 1.6f
                                    translationY = if (selectedIndex == index) (-3).dp.toPx() else 0f
                                }
                                .onGloballyPositioned { cardBounds[index] = it.boundsInWindow() }
                                .pointerInput(current, index, card.name) {
                                    detectDragGestures(
                                        onDragStart = { start ->
                                            // pointerInput is keyed by card identity, not
                                            // the snapshot revision. Read the live engine
                                            // instead of a stale snapshot captured when
                                            // the card first entered the LazyRow.
                                            val canPlace = engine.humanTurn &&
                                                !engine.finished &&
                                                pendingPosition == null &&
                                                engine.pendingMovedUnit == null &&
                                                (0 until ShadowRules.ROWS).any { row ->
                                                    (0 until ShadowRules.COLUMNS).any { col ->
                                                        engine.isPlayablePlacement(
                                                            engine.human, index, row, col
                                                        )
                                                    }
                                                }
                                            if (canPlace) {
                                                sounds.play(DuelSound.CARD_LIFT)
                                                draggedIndex = index
                                                selectedIndex = index
                                                dragPosition =
                                                    (cardBounds[index]?.topLeft ?: Offset.Zero) + start
                                            } else {
                                                draggedIndex = -1
                                                message = "Card cannot be played now. Resolve any pending action."
                                            }
                                        },
                                        onDrag = { change, delta ->
                                            if (draggedIndex == index) {
                                                change.consume()
                                                dragPosition += delta
                                            }
                                        },
                                        onDragEnd = {
                                            if (draggedIndex == index) {
                                                // Use the live board here too. The visible
                                                // glowing positions are precisely the cells
                                                // returned by this engine legality check.
                                                val legalSlots = buildSet {
                                                    for (row in 0 until ShadowRules.ROWS) {
                                                        for (col in 0 until ShadowRules.COLUMNS) {
                                                            if (engine.isPlayablePlacement(
                                                                    engine.human, index, row, col
                                                                )) add(DuelDropCell(row, col))
                                                        }
                                                    }
                                                }
                                                val drop = DuelHitTesting.playerSlotForDrop(
                                                    dragPosition,
                                                    slotBounds,
                                                    floatingCardHeightPx,
                                                    legalSlots
                                                )
                                                draggedIndex = -1
                                                if (drop != null) {
                                                    selectedIndex = index
                                                    pendingFingerRelease = dragPosition
                                                    playAt(drop.row, drop.column)
                                                } else {
                                                    message = "Release the card over a glowing empty slot."
                                                }
                                            }
                                        },
                                        onDragCancel = { draggedIndex = -1 }
                                    )
                                }
                                .combinedClickable(
                                    onClick = {
                                        sounds.play(DuelSound.CARD_SELECT)
                                        selectedIndex = if (selectedIndex == index) -1 else index
                                        pendingPosition = null
                                    },
                                    onLongClick = { inspectedCard = card }
                                ),
                            border = BorderStroke(1.dp,
                                if (selectedIndex == index) BattleGold else Color(0xFF73543D)),
                            colors = CardDefaults.cardColors(containerColor =
                                if (selectedIndex == index) Color(0xFF624934) else BattlePanel)
                        ) {
                            DuelCardFrame(
                                card = card, power = card.numericPower(),
                                modifier = Modifier.fillMaxSize().padding(2.dp),
                                selected = selectedIndex == index,
                                playable = snapshot.playerTurn && engine.affordable(engine.human, index),
                                compact = false
                            )
                        }
                    }
                }
                if (engine.human.hand.size > 4) {
                    Column(
                        modifier = Modifier.width(23.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            Modifier.size(width = 23.dp, height = 37.dp)
                                .clickable {
                                    handScrollScope.launch {
                                        handListState.animateScrollToItem(
                                            (handListState.firstVisibleItemIndex - 1)
                                                .coerceAtLeast(0)
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("‹", color = BattleGold, fontSize = 23.sp)
                        }
                        Box(
                            Modifier.size(width = 23.dp, height = 37.dp)
                                .clickable {
                                    handScrollScope.launch {
                                        handListState.animateScrollToItem(
                                            (handListState.firstVisibleItemIndex + 1)
                                                .coerceAtMost(engine.human.hand.lastIndex)
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("›", color = BattleGold, fontSize = 23.sp)
                        }
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Choose a legal 15-card deck to start.", color = BattleFaded)
            }
        }
    }
    AnimatedVisibility(
        visible = turnAnnouncement != null && !openingDialog && !deckDialog && !factionDialog,
        enter = fadeIn(), exit = fadeOut(),
        modifier = Modifier.align(Alignment.Center).zIndex(12f)
    ) {
        DuelCinematicTurnBanner(turnAnnouncement.orEmpty())
    }
    AnimatedVisibility(
        visible = effectAnnouncement != null && turnAnnouncement == null &&
            !openingDialog && !deckDialog && !factionDialog,
        enter = fadeIn(), exit = fadeOut(),
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 41.dp).zIndex(9f)
    ) {
        DuelCinematicEventRibbon(effectAnnouncement.orEmpty())
    }
    if (pendingPosition != null || engine?.pendingMovedUnit != null) {
        Box(
            modifier = Modifier.align(Alignment.BottomCenter)
                .padding(bottom = 134.dp).fillMaxWidth(0.72f)
                .background(Color(0xEC241E1B), RoundedCornerShape(6.dp))
                .border(1.dp, DuelPalette.gold, RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 5.dp).zIndex(8f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (engine?.pendingMovedUnit != null)
                    "MOVE ${engine.pendingMovedUnit?.card?.name ?: "UNIT"} TO A HIGHLIGHTED EMPTY SLOT"
                else "CHOOSE A GOLD-HIGHLIGHTED CARD TO RESOLVE THE EFFECT",
                color = DuelPalette.gold, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, maxLines = 2
            )
        }
    }
    // Floating card stays under the finger while the user drags across the field.
    if (engine != null && draggedIndex in engine.human.hand.indices) {
        val held = engine.human.hand[draggedIndex]
        val density = LocalDensity.current
        val ghostWidth = with(density) { 96.dp.toPx() }
        val ghostHeight = with(density) { 130.dp.toPx() }
        Box(
            Modifier.offset {
                val position = DuelHitTesting.floatingCardTopLeft(
                    dragPosition, rootTopLeftInWindow, ghostWidth, ghostHeight
                )
                IntOffset(position.x.roundToInt(), position.y.roundToInt())
            }.size(width = 96.dp, height = 130.dp).zIndex(40f)
        ) {
            DuelCardFrame(
                card = held, power = held.numericPower(),
                modifier = Modifier.fillMaxSize(),
                selected = true, compact = false
            )
        }
    }
    }

    // All battle modals share the same original, Destiny-inspired popup
    // frame. Only their presentation changes; save/gameplay callbacks stay
    // exactly as before.
    if (resumeChoice && savedAtLaunch != null && coordinator == null) {
        DuelPopupDialog(
            title = "CONTINUE LAST MATCH?",
            subtitle = "ROUND ${savedAtLaunch.engine.round} / ${ShadowRules.ROUNDS}  •  " +
                savedAtLaunch.deckName +
                "\nContinue with your exact saved board, hand, deck order and turn?",
            symbol = DuelPopupSymbol.ORBIT,
            onDismissRequest = {},
            allowDismiss = false
        ) {
            Text(
                "SELECT YOUR NEXT ACTION",
                color = DuelPopupColors.goldSoft,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                DuelPopupAction(
                    label = "START NEW",
                    style = DuelPopupActionStyle.SECONDARY,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        matchRepository.clear()
                        resumeChoice = false
                        deckDialog = true
                    }
                )
                DuelPopupAction(
                    label = "CONTINUE",
                    style = DuelPopupActionStyle.PRIMARY,
                    modifier = Modifier.weight(1f),
                    onClick = { resumeMatch(savedAtLaunch) }
                )
            }
        }
    }

    if (restoringError != null) {
        DuelPopupDialog(
            title = "SAVE RECOVERY",
            subtitle = restoringError.orEmpty(),
            symbol = DuelPopupSymbol.WARNING,
            onDismissRequest = { restoringError = null }
        ) {
            DuelPopupAction(
                label = "ACKNOWLEDGE",
                style = DuelPopupActionStyle.PRIMARY,
                modifier = Modifier.fillMaxWidth(),
                onClick = { restoringError = null }
            )
        }
    }

    if (deckDialog && !resumeChoice) {
        DuelPopupDialog(
            title = "CHOOSE YOUR DECK",
            subtitle = "SELECT A LEGAL 15-CARD LOADOUT",
            symbol = DuelPopupSymbol.CARDS,
            onDismissRequest = { if (engine != null) deckDialog = false },
            allowDismiss = engine != null
        ) {
            if (validDecks.isEmpty()) {
                Text(
                    "Build a legal 15-card deck in Deck Construction first.",
                    fontSize = 12.sp,
                    color = DuelPopupColors.muted
                )
            }
            validDecks.forEach { (name, deck) ->
                DuelPopupMenuRow(
                    label = name.uppercase(),
                    description = "EQUIP AND CHOOSE OPPONENT",
                    onClick = {
                        chosenDeck = deck
                        chosenDeckName = name
                        chosenCover =
                            if (name == "Starter Practice Deck") "default"
                            else saves.cover(name)
                        deckDialog = false
                        factionDialog = true
                    }
                )
            }
            DuelPopupRule()
            DuelPopupAction(
                label = "HOME",
                modifier = Modifier.fillMaxWidth(),
                onClick = onExit
            )
        }
    }

    if (factionDialog) {
        DuelPopupDialog(
            title = "SELECT OPPONENT",
            subtitle = "CHOOSE AN ENEMY FACTION FOR THIS SKIRMISH",
            symbol = DuelPopupSymbol.OPPONENT,
            onDismissRequest = { factionDialog = false; deckDialog = true }
        ) {
            listOf("Mixed", "Fallen", "Hive", "Cabal", "Humans").forEach { faction ->
                DuelPopupMenuRow(
                    label = faction.uppercase(),
                    description = if (faction == "Mixed")
                        "VARIED ENEMY DECK" else "FACTION-SPECIFIC ENEMY DECK",
                    onClick = { startMatch(faction) }
                )
            }
            DuelPopupRule()
            DuelPopupAction(
                label = "BACK TO DECKS",
                modifier = Modifier.fillMaxWidth(),
                onClick = { factionDialog = false; deckDialog = true }
            )
        }
    }

    if (openingDialog && engine != null) {
        DuelPopupDialog(
            title = if (engine.humanFirst) "YOU ATTACK FIRST" else "CPU ATTACKS FIRST",
            subtitle = "SIX ROUNDS  •  15 CARDS PER DECK\n" +
                "Build the greatest total surviving Power.",
            symbol = DuelPopupSymbol.READY,
            onDismissRequest = {},
            allowDismiss = false
        ) {
            Text(
                "OPENING HAND   ${engine.human.hand.size} CARDS",
                color = DuelPopupColors.gold,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontSize = 12.sp
            )
            DuelPopupAction(
                label = "START DUEL",
                modifier = Modifier.fillMaxWidth(),
                style = DuelPopupActionStyle.PRIMARY,
                onClick = { openingDialog = false }
            )
        }
    }

    if (settingsDialog) {
        DuelPopupDialog(
            title = "SHADOW SHAPER DUELS",
            subtitle = "SKIRMISH  /  SETTINGS",
            symbol = DuelPopupSymbol.SETTINGS,
            onDismissRequest = { settingsDialog = false }
        ) {
            DuelPopupSoundSettings(
                enabled = audioEnabled,
                volume = audioVolume,
                onEnabledChange = { value ->
                    audioEnabled = value
                    sounds.enabled = value
                    if (value) sounds.play(DuelSound.UI_TAP)
                },
                onVolumeChange = { value ->
                    audioVolume = value
                    sounds.volume = value
                }
            )
            DuelPopupRule()
            DuelPopupMenuRow(
                label = "TEST SOUND",
                description = "PREVIEW BATTLE AUDIO",
                highlighted = true,
                enabled = audioEnabled,
                onClick = { sounds.play(DuelSound.CARD_SELECT) }
            )
            DuelPopupMenuRow(
                label = "RESUME",
                description = "RETURN TO THE BATTLEFIELD",
                onClick = { settingsDialog = false }
            )
            DuelPopupMenuRow(
                label = "NEW MATCH",
                description = "SELECT A NEW SKIRMISH",
                onClick = {
                    settingsDialog = false
                    matchRepository.clear()
                    coordinator = null
                    deckDialog = true
                    factionDialog = false
                }
            )
            DuelPopupMenuRow(
                label = "RULES",
                description = "VIEW GAMEPLAY GUIDANCE",
                onClick = { settingsDialog = false; onRules() }
            )
            DuelPopupMenuRow(
                label = "HOME",
                description = "LEAVE THE CURRENT SKIRMISH",
                onClick = { settingsDialog = false; exitConfirmation = true }
            )
            DuelPopupRule()
            DuelPopupAction(
                label = "CLOSE",
                modifier = Modifier.fillMaxWidth(),
                onClick = { settingsDialog = false }
            )
        }
    }

    if (exitConfirmation) {
        DuelPopupDialog(
            title = "LEAVE SKIRMISH?",
            subtitle = "Your current match is automatically saved. " +
                "You can continue from the same turn later.",
            symbol = DuelPopupSymbol.WARNING,
            onDismissRequest = { exitConfirmation = false }
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                DuelPopupAction(
                    label = "STAY",
                    modifier = Modifier.weight(1f),
                    onClick = { exitConfirmation = false }
                )
                DuelPopupAction(
                    label = "SAVE & HOME",
                    modifier = Modifier.weight(1f),
                    style = DuelPopupActionStyle.PRIMARY,
                    onClick = onExit
                )
            }
        }
    }

    if (endConfirmation) {
        DuelPopupDialog(
            title = "END THIS TURN?",
            subtitle = "You still have unspent Light. " +
                "Unused Light does not carry over.",
            symbol = DuelPopupSymbol.WARNING,
            onDismissRequest = { endConfirmation = false }
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                DuelPopupAction(
                    label = "KEEP PLAYING",
                    modifier = Modifier.weight(1f),
                    onClick = { endConfirmation = false }
                )
                DuelPopupAction(
                    label = "END TURN",
                    modifier = Modifier.weight(1f),
                    style = DuelPopupActionStyle.PRIMARY,
                    onClick = {
                        endConfirmation = false
                        sounds.play(DuelSound.UI_TAP)
                        current?.endTurn()
                    }
                )
            }
        }
    }

    if (snapshot?.finished == true && !inspectingFinalBoard &&
        !showLog && inspectedUnit == null && inspectedCard == null) {
        DuelMatchResult(
            playerPower = snapshot.playerPower,
            cpuPower = snapshot.cpuPower,
            unresolvedEffects = snapshot.unresolvedEffects,
            onInspectBoard = { inspectingFinalBoard = true },
            onReviewLog = { showLog = true },
            onRematch = {
                // Fresh engine/seed/deck shuffle, but reuse chosen decks.
                startMatch(selectedCpuFaction)
            },
            onSelectDecks = {
                coordinator = null
                selectedIndex = -1
                pendingPosition = null
                inspectedUnit = null
                inspectedCard = null
                inspectingFinalBoard = false
                openingDialog = false
                deckDialog = true
                factionDialog = false
            },
            onHome = onExit
        )
    }
    if (showLog && engine != null) {
        DuelPopupDialog(
            title = "BATTLE LOG",
            subtitle = "COMPLETE SKIRMISH EVENT HISTORY",
            symbol = DuelPopupSymbol.LOG,
            onDismissRequest = { showLog = false }
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .height(240.dp)
                    .background(Color(0xFF09121A))
                    .border(.7.dp, DuelPopupColors.border.copy(alpha = .68f))
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                engine.fullJournal.forEachIndexed { index, event ->
                    Text(
                        "${index + 1}.  $event",
                        fontSize = 11.sp,
                        color = DuelPopupColors.muted,
                        lineHeight = 15.sp
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                DuelPopupAction(
                    label = "CLOSE",
                    modifier = Modifier.weight(1f),
                    onClick = { showLog = false }
                )
                DuelPopupAction(
                    label = "COPY REPORT",
                    style = DuelPopupActionStyle.PRIMARY,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                            as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText(
                                "Shadowshaper match report",
                                engine.diagnosticReport()
                            )
                        )
                        showLog = false
                    }
                )
            }
        }
    }

    if (inspectedUnit != null || inspectedCard != null) {
        val unit = inspectedUnit
        val card = inspectedCard ?: unit?.card
        if (card != null) {
            DuelCardInspection(
                card = card,
                unit = unit,
                power = unit?.let { engine?.power(it) } ?: card.numericPower(),
                onClose = { inspectedUnit = null; inspectedCard = null }
            )
        }
    }

}
