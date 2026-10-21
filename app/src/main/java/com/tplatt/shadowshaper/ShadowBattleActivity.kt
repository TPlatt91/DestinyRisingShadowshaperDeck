package com.tplatt.shadowshaper

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
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

/** Compose landscape battle UI. Rules and AI are resolved by the shared engine. */
class ShadowBattleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
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
    val screenConfig = LocalConfiguration.current
    val compactLayout = screenConfig.screenWidthDp < 790
    val catalog = remember { ShadowData.load(context) }
    val saves = remember { ShadowDeckRepository(context) }
    val practice = remember(catalog) { ShadowData.opponentDeck(catalog) }
    var coordinator by remember { mutableStateOf<ShadowBattleCoordinator?>(null) }
    val current = coordinator
    val snapshotState = current?.state?.collectAsState()
    val snapshot = snapshotState?.value
    val engine = current?.engine

    var deckDialog by remember { mutableStateOf(true) }
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
    var rootTopLeftInWindow by remember { mutableStateOf(Offset.Zero) }
    val cardBounds = remember { mutableStateMapOf<Int, Rect>() }
    val slotBounds = remember { mutableStateMapOf<String, Rect>() }
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
    LaunchedEffect(requestedDeck) {
        if (!requestedDeck.isNullOrBlank()) {
            val saved = validDecks.firstOrNull { it.first == requestedDeck }
            if (saved != null) {
                chosenDeck = saved.second
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
            candidate.start(chosenDeck, cpu)
            coordinator = candidate
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

    fun playAt(row: Int, column: Int) {
        val e = engine ?: return
        val co = current ?: return
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
            pendingPosition = row to column
            message = "Choose a gold-highlighted target for " + card.name
        } else {
            if (co.play(e.human, index, row, column, null)) {
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
            selectedIndex = -1
            pendingPosition = null
            message = "Effect resolved."
        } else message = e.lastActionError
    }

    BackHandler(settingsDialog || engine != null) {
        if (settingsDialog) settingsDialog = false else exitConfirmation = true
    }

    // CPU acts once per coroutine tick so the user sees individual plays.
    LaunchedEffect(current, snapshot?.playerTurn, snapshot?.revision, snapshot?.round,
        openingDialog, settingsDialog, inspectedUnit, inspectedCard,
        showLog, deckDialog, factionDialog) {
        if (current != null && snapshot != null && !snapshot.finished && !snapshot.playerTurn &&
            !openingDialog && !settingsDialog && inspectedUnit == null && inspectedCard == null &&
            !showLog && !deckDialog && !factionDialog) {
            delay(540)
            if (!current.cpuPlayOne()) current.endTurn()
        }
    }

    // Short on-screen announcements make turn/ability changes visible without
    // making the player read the entire event log on every action.
    LaunchedEffect(current, snapshot?.round, snapshot?.playerTurn) {
        if (snapshot == null || current == null || snapshot.finished) {
            turnAnnouncement = null
        } else {
            turnAnnouncement = "ROUND ${snapshot.round}  •  " +
                if (snapshot.playerTurn) "YOUR TURN" else "OPPONENT TURN"
            delay(850)
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

    Box(Modifier.fillMaxSize().onGloballyPositioned {
        val position = it.boundsInWindow().topLeft
        if (rootTopLeftInWindow != position) rootTopLeftInWindow = position
    }) {
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF17110E),Color(0xFF302018),Color(0xFF19151A))))) {
        Row(Modifier.fillMaxWidth().height(44.dp)
            .background(Brush.horizontalGradient(listOf(Color(0xFF32241C),Color(0xFF221B1B),Color(0xFF32251E))))
            .border(width = 1.dp, color = Color(0xFF8D6748))
            .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { settingsDialog = true }) { Text("☰ MENU", color = BattleGold) }
            Text("SHADOWSHAPER  •  DUELS", color = DuelPalette.cream,
                letterSpacing = 2.sp, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text(
                when {
                    snapshot?.finished == true -> "DUEL COMPLETE"
                    engine?.pendingMovedUnit != null && engine.pendingMoveCaster === engine.human ->
                        "CHOOSE DESTINATION"
                    pendingPosition != null -> "SELECT EFFECT TARGET"
                    snapshot?.playerTurn == true -> "YOUR TURN"
                    snapshot != null -> "CPU TURN"
                    else -> "SINGLE PLAYER"
                }, color = BattleGold, fontSize = 11.sp
            )
        }
        if (engine != null && snapshot != null) {
            Row(Modifier.weight(1f).fillMaxWidth().padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Column(
                    Modifier.width(DuelLayoutMetrics.leftHudWidth(screenConfig.screenWidthDp).dp).fillMaxHeight()
                        .background(DuelPalette.tableLow, RoundedCornerShape(8.dp))
                        .border(1.dp, DuelPalette.rim, RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text("OPPONENT", color = DuelPalette.red,
                        fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    DuelCardBacks(snapshot.cpuHand)
                    Text("HAND " + snapshot.cpuHand + "   •   DECK " + engine.cpu.drawPile.size,
                        color = BattleFaded, fontSize = 9.sp)
                    Text("ROUND " + snapshot.round + " / " + ShadowRules.ROUNDS,
                        color = DuelPalette.gold, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    DuelRoundMarkers(snapshot.round)
                    Text(if (engine.humanFirst) "OFFENSIVE" else "DEFENSIVE",
                        color = DuelPalette.blue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("YOUR FIELD", color = DuelPalette.cream,
                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    Modifier.weight(1f).fillMaxHeight()
                        .background(Brush.verticalGradient(listOf(
                            Color(0xFF37271F), Color(0xFF28221F), Color(0xFF3B2A21)
                        )), RoundedCornerShape(10.dp))
                        .border(2.dp, DuelPalette.rim, RoundedCornerShape(10.dp))
                        .padding(6.dp)
                ) {
                    DuelTableEtching(Modifier.fillMaxSize())
                    Column(Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)) {
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
                            Box(Modifier.fillMaxWidth().height(2.dp)
                                .background(Brush.horizontalGradient(
                                    listOf(Color.Transparent, DuelPalette.gold, Color.Transparent)
                                )))
                        }
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(heading.replace(" ", "\n"), fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (owner === engine.human) DuelPalette.blue else DuelPalette.red,
                                modifier = Modifier.width(45.dp).align(Alignment.CenterVertically),
                                lineHeight = 10.sp, textAlign = TextAlign.Center)
                            for (col in 0 until ShadowRules.COLUMNS) {
                                val unit = owner.board[row][col]
                                val targeting = targets.any {
                                    it.owner === owner && it.row == row && it.column == col
                                }
                                val relocating = moving && unit == null &&
                                    owner === engine.pendingMovedUnit?.owner
                                val legal = !moving && unit == null && owner === engine.human &&
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
                                            if (targeting || relocating || (owner === engine.human && unit == null))
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
                                        label = if (legal || relocating || targeting) "+" else "·"
                                    )
                                    if (unit != null) {
                                        val fit = DuelLayoutMetrics.fittingCard(maxWidth.value, maxHeight.value)
                                        DuelAnimatedUnit(
                                            unit = unit,
                                            currentPower = engine.power(unit),
                                            placementRevision = engine.placementRevision,
                                            isNewestPlacement =
                                                engine.lastPlacedSide == (if (owner === engine.human) 0 else 1) &&
                                                engine.lastPlacedRow == row &&
                                                engine.lastPlacedColumn == col,
                                            modifier = Modifier.width(fit.width.dp).height(fit.height.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                }
                Column(
                    Modifier.width(DuelLayoutMetrics.rightHudWidth(screenConfig.screenWidthDp).dp).fillMaxHeight().background(BattleBrown).padding(if (compactLayout) 2.dp else 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DuelScore(snapshot.playerPower, snapshot.cpuPower, snapshot.playerTurn)
                    Spacer(Modifier.height(6.dp))
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        engine.journal.takeLast(8).forEach {
                            Text(it, color = BattleCream, fontSize = 10.sp, lineHeight = 12.sp)
                        }
                    }
                    TextButton(onClick = { showLog = true }) {
                        Text("MATCH LOG", color = BattleGold, fontSize = 11.sp)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().height(DuelLayoutMetrics.handHeight(screenConfig.screenHeightDp).dp).background(BattleBrown).padding(5.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(if (compactLayout) 86.dp else 108.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    DuelLightMeter(snapshot.playerLight, snapshot.round)
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DuelCardCover(chosenCover, Modifier.width(19.dp).height(28.dp))
                        Text("DECK " + engine.human.drawPile.size +
                            "\nHAND " + snapshot.playerHand,
                            color = BattleFaded, fontSize = 9.sp, lineHeight = 10.sp)
                    }
                }
                LazyRow(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy((-11).dp)
                ) {
                    itemsIndexed(engine.human.hand.toList()) { index, card ->
                        Card(
                            modifier = Modifier.width(82.dp).fillMaxHeight()
                                .zIndex(if (selectedIndex == index || draggedIndex == index) 5f else index.toFloat() / 100f)
                                .graphicsLayer {
                                    val midpoint = (engine.human.hand.size - 1) / 2f
                                    rotationZ = if (selectedIndex == index) 0f
                                        else (index - midpoint) * 1.6f
                                    translationY = if (selectedIndex == index) (-3).dp.toPx() else 0f
                                }
                                .onGloballyPositioned { cardBounds[index] = it.boundsInWindow() }
                                .pointerInput(index, card.name) {
                                    detectDragGestures(
                                        onDragStart = { start ->
                                            if (snapshot.playerTurn && !snapshot.finished &&
                                                pendingPosition == null &&
                                                engine.pendingMovedUnit == null &&
                                                engine.affordable(engine.human, index)) {
                                                draggedIndex = index
                                                selectedIndex = index
                                                dragPosition =
                                                    (cardBounds[index]?.topLeft ?: Offset.Zero) + start
                                            } else {
                                                draggedIndex = -1
                                                message = "Resolve the current action or choose an affordable card."
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
                                                val drop = DuelHitTesting.playerSlotAt(dragPosition, slotBounds)
                                                draggedIndex = -1
                                                if (drop != null) playAt(drop.row, drop.column)
                                                else message = "Drag onto a glowing empty slot in your field."
                                            }
                                        },
                                        onDragCancel = { draggedIndex = -1 }
                                    )
                                }
                                .combinedClickable(
                                    onClick = {
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
                Column(Modifier.width(if (compactLayout) 106.dp else 120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        enabled = snapshot.playerTurn && !snapshot.finished,
                        onClick = {
                            if (pendingPosition != null) pendingPosition = null
                            else if (engine.pendingMovedUnit != null)
                                message = "Choose a relocation slot first."
                            else if (engine.human.points > 0 &&
                                engine.human.hand.indices.any { engine.affordable(engine.human, it) })
                                endConfirmation = true
                            else current.endTurn()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(when {
                            pendingPosition != null -> "CANCEL TARGET"
                            engine.pendingMovedUnit != null -> "MOVE UNIT"
                            else -> "END TURN  ›"
                        }, fontSize = 11.sp,
                            fontWeight = FontWeight.Black)
                    }
                    Text(message, color = BattleFaded, fontSize = 9.sp,
                        lineHeight = 11.sp, maxLines = 3, textAlign = TextAlign.Center)
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
        Box(Modifier.background(Color(0xE929221F), RoundedCornerShape(10.dp))
            .border(2.dp, DuelPalette.gold, RoundedCornerShape(10.dp))
            .padding(horizontal = 32.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center) {
            Text(turnAnnouncement.orEmpty(), color = DuelPalette.gold,
                fontSize = 19.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
        }
    }
    AnimatedVisibility(
        visible = effectAnnouncement != null && turnAnnouncement == null,
        enter = fadeIn(), exit = fadeOut(),
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 53.dp).zIndex(9f)
    ) {
        Text(effectAnnouncement.orEmpty(),
            modifier = Modifier.background(Color(0xEB261D19), RoundedCornerShape(7.dp))
                .border(1.dp, DuelPalette.rim, RoundedCornerShape(7.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            color = DuelPalette.cream, fontSize = 11.sp, maxLines = 2,
            textAlign = TextAlign.Center)
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

    if (deckDialog) AlertDialog(
        onDismissRequest = { if (engine != null) deckDialog = false },
        title = { Text("CHOOSE A DECK") },
        text = {
            Column {
                if (validDecks.isEmpty()) Text("Build a legal 15-card deck in Deck Construction first.")
                validDecks.forEach { (name, deck) ->
                    TextButton(onClick = {
                        chosenDeck = deck
                        chosenCover = if (name == "Starter Practice Deck") "default" else saves.cover(name)
                        deckDialog = false
                        factionDialog = true
                    }) { Text(name) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onExit) { Text("HOME") } }
    )
    if (factionDialog) AlertDialog(
        onDismissRequest = { factionDialog = false; deckDialog = true },
        title = { Text("CPU FACTION") },
        text = {
            Column {
                listOf("Mixed", "Fallen", "Hive", "Cabal", "Humans").forEach { faction ->
                    TextButton(onClick = { startMatch(faction) }) { Text(faction) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { factionDialog = false; deckDialog = true }) { Text("BACK") }
        }
    )
    if (openingDialog && engine != null) AlertDialog(
        onDismissRequest = {},
        title = { Text(if (engine.humanFirst) "OFFENSIVE • YOU GO FIRST" else "DEFENSIVE • CPU FIRST") },
        text = { Text("Six rounds • 15 cards per deck\nBuild the greatest total surviving Power.\nOpening hand: " + engine.human.hand.size) },
        confirmButton = { TextButton(onClick = { openingDialog = false }) { Text("START DUEL") } }
    )
    if (settingsDialog) AlertDialog(
        onDismissRequest = { settingsDialog = false },
        title = { Text("SHADOWSHAPER DUELS") },
        text = {
            Column {
                TextButton(onClick = { settingsDialog = false }) { Text("RESUME") }
                TextButton(onClick = { settingsDialog = false; deckDialog = true }) { Text("NEW MATCH") }
                TextButton(onClick = { settingsDialog = false; onRules() }) { Text("RULES") }
                TextButton(onClick = { settingsDialog = false; exitConfirmation = true }) { Text("HOME") }
            }
        },
        confirmButton = { TextButton(onClick = { settingsDialog = false }) { Text("CLOSE") } }
    )
    if (exitConfirmation) AlertDialog(
        onDismissRequest = { exitConfirmation = false },
        title = { Text("Leave battle?") },
        text = { Text("This match will be lost.") },
        confirmButton = { TextButton(onClick = onExit) { Text("LEAVE") } },
        dismissButton = { TextButton(onClick = { exitConfirmation = false }) { Text("STAY") } }
    )
    if (endConfirmation) AlertDialog(
        onDismissRequest = { endConfirmation = false },
        title = { Text("End turn with Light remaining?") },
        text = { Text("Unspent Light does not carry over.") },
        confirmButton = {
            TextButton(onClick = { endConfirmation = false; current?.endTurn() }) { Text("END TURN") }
        },
        dismissButton = { TextButton(onClick = { endConfirmation = false }) { Text("KEEP PLAYING") } }
    )
    if (snapshot?.finished == true) {
        DuelMatchResult(
            playerPower = snapshot.playerPower,
            cpuPower = snapshot.cpuPower,
            unresolvedEffects = snapshot.unresolvedEffects,
            onRematch = {
                coordinator = null
                deckDialog = true
                factionDialog = false
                openingDialog = false
                pendingPosition = null
                inspectedUnit = null
                inspectedCard = null
                selectedIndex = -1
            },
            onHome = onExit
        )
    }
    if (showLog && engine != null) AlertDialog(
        onDismissRequest = { showLog = false },
        title = { Text("BATTLE LOG") },
        text = {
            Column(Modifier.height(250.dp).verticalScroll(rememberScrollState())) {
                engine.journal.forEach { Text(it, fontSize = 11.sp, color = BattleCream) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText(
                    "Shadowshaper match report", engine.diagnosticReport()))
                showLog = false
            }) { Text("COPY REPORT") }
        },
        dismissButton = { TextButton(onClick = { showLog = false }) { Text("CLOSE") } }
    )
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
