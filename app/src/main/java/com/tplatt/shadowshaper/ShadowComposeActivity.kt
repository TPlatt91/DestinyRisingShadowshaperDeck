package com.tplatt.shadowshaper

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Kotlin-first, Compose-based primary app. BattleActivity remains the existing
 * landscape view until the highly custom battlefield is migrated and validated.
 *
 * Prior Java MainActivity/HomeActivity/RulesActivity stay in source as fallback
 * during migration; the launcher now enters this Activity.
 */
class ShadowComposeActivity : ComponentActivity() {
    // Changes in a normal Activity lifecycle callback automatically refresh
    // the launcher Resume entry, with no extra lifecycle-compose dependency.
    internal var resumePulse by mutableIntStateOf(0)
    override fun onResume() {
        super.onResume()
        resumePulse++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.rgb(23, 18, 16)
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.rgb(23, 18, 16)
        setContent {
            ShadowTheme {
                val vm: ShadowDeckViewModel = viewModel()
                CompanionNavigation(vm, this)
            }
        }
    }
}

private val Ink = Color(0xFF171210)
private val Panel = Color(0xFF352720)
private val Raised = Color(0xFF493426)
private val Gold = Color(0xFFEBC481)
private val Cream = Color(0xFFF7E6C9)
private val Secondary = Color(0xFFC4AF96)
private val CoolBlue = Color(0xFF9DD1EF)
private val Warn = Color(0xFFFFBDB0)

@Composable
private fun ShadowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Gold, onPrimary = Ink,
            secondary = CoolBlue, onSecondary = Ink,
            background = Ink, onBackground = Cream,
            surface = Panel, onSurface = Cream,
            surfaceVariant = Raised, onSurfaceVariant = Secondary
        ),
        content = content
    )
}

@Composable
private fun CompanionNavigation(vm: ShadowDeckViewModel, activity: Activity) {
    var page by rememberSaveable { mutableStateOf(activity.intent.getStringExtra("start_page") ?: "home") }
    var detail by remember { mutableStateOf<ShadowData.Card?>(null) }
    val matchRepository = remember(activity) { ShadowMatchRepository(activity) }
    val resumePulse = (activity as? ShadowComposeActivity)?.resumePulse ?: 0
    val hasUnfinishedMatch = remember(resumePulse) {
        matchRepository.load() != null
    }
    val state by vm.state.collectAsState()
    BackHandler(page != "home") { page = "home" }
    Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            when (page) {
                "home" -> ShadowLandingPage(
                    onSkirmish = {
                        activity.startActivity(Intent(activity, ShadowBattleActivity::class.java))
                    },
                    onContinue = {
                        activity.startActivity(
                            Intent(activity, ShadowBattleActivity::class.java)
                                .putExtra("resume_last_match", true)
                        )
                    },
                    hasSavedMatch = hasUnfinishedMatch,
                    onDeckConstruction = { page = "deck" },
                    onCardCollection = { page = "collection" },
                    onRules = { page = "rules" },
                    catalogCount = vm.catalog.size
                )
                "deck" -> DeckScreen(
                    vm = vm, state = state,
                    onBack = { page = "home" },
                    onCard = { detail = it },
                    onBattle = {
                        val intent = Intent(activity, ShadowBattleActivity::class.java)
                            .putExtra("selected_deck", state.selected)
                        activity.startActivity(intent)
                    }
                )
                "collection" -> CollectionScreen(
                    vm = vm, state = state, onBack = { page = "home" },
                    onCard = { detail = it }
                )
                "rules" -> RulesScreen(onBack = { page = "home" })
            }
        }
    }
    if (detail != null) {
        val card = detail!!
        CardDetailsDialog(card, state.cards[card.name] ?: 0, onDismiss = { detail = null },
            onAdd = { vm.add(card) }, onRemove = { vm.remove(card.name) })
    }
    if (state.message != null) {
        AlertDialog(
            onDismissRequest = vm::clearMessage,
            title = { Text("Deck update") },
            text = { Text(state.message ?: "") },
            confirmButton = { TextButton(onClick = vm::clearMessage) { Text("OK") } }
        )
    }
}

@Composable
private fun PageHeading(title: String, description: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Panel).padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ HOME", color = Gold) }
            Spacer(Modifier.width(8.dp))
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Cream)
        }
        Text(description, fontSize = 12.sp, color = Secondary, modifier = Modifier.padding(start = 8.dp, bottom = 3.dp))
    }
}

@Composable
private fun CardArtPreview(card: ShadowData.Card, modifier: Modifier = Modifier) {
    // Compose can wrap Activity in a themed Context; never skip loading because
    // LocalContext is not literally an Activity. CardArt.bind accepts Context.
    key(card.name) {
        AndroidView(
            factory = { ctx ->
                ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    adjustViewBounds = false
                    CardArt.bind(ctx, card, this)
                }
            },
            update = { view ->
                if (view.contentDescription?.toString() != card.name + " card art") {
                    CardArt.bind(view.context, card, view)
                }
            },
            modifier = modifier
        )
    }
}

@Composable
private fun CardTile(card: ShadowData.Card, copies: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        border = BorderStroke(1.dp, Color(0xFF55402F))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(9.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardArtPreview(card, Modifier.width(67.dp).height(95.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(card.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Cream,
                        modifier = Modifier.weight(1f))
                    if (copies > 0) Text("×$copies", color = Gold, fontWeight = FontWeight.Bold)
                }
                Text("${card.rarity}  •  ${card.faction}", fontSize = 12.sp, color = Gold)
                Text("POWER ${card.power.ifBlank { "?" }}   LIGHT ${card.cost.ifBlank { "?" }}",
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CoolBlue)
                Spacer(Modifier.height(5.dp))
                Text(card.effect.ifBlank { "No effect documented" },
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = Secondary, fontSize = 11.sp, lineHeight = 14.sp)
            }
        }
    }
}

// Shared stone-metal library browser used by Deck Construction and Collection.
@Composable
private fun LibraryCardFace(
    card: ShadowData.Card,
    copies: Int,
    onInspect: () -> Unit,
    onAdd: (() -> Unit)? = null
) {
    Box(
        Modifier.fillMaxWidth().aspectRatio(0.67f)
            .background(Color(0xFF211D1D), RoundedCornerShape(3.dp))
            .border(1.dp, if (copies > 0) Gold else Color(0xFF6F665E),
                RoundedCornerShape(3.dp))
            .clickable(onClick = onInspect)
    ) {
        CardArtPreview(card, Modifier.fillMaxSize().padding(2.dp))
        if (copies > 0) {
            Text(
                "×$copies", color = Cream, fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd)
                    .background(Color(0xD7271C17), RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
        if (onAdd != null && card.isPlayable) {
            TextButton(
                onClick = onAdd,
                modifier = Modifier.align(Alignment.BottomEnd).height(30.dp)
                    .background(Color(0xDD3B2D1C), RoundedCornerShape(3.dp)),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text("+", color = Gold, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LibraryFilterSelector(
    label: String,
    selection: String,
    choices: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.background(Color(0xFF302925),
                RoundedCornerShape(2.dp)).border(1.dp, Color(0xFF645549))
                .height(36.dp),
            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 2.dp)
        ) {
            Text("$label: $selection ▾", color = Cream, fontSize = 11.sp, maxLines = 1)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            choices.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = {
                    onSelect(option); expanded = false
                })
            }
        }
    }
}

@Composable
private fun LibrarySearchBar(
    query: String,
    onQuery: (String) -> Unit,
    faction: String,
    onFaction: (String) -> Unit,
    rarity: String,
    onRarity: (String) -> Unit
) {
    // Portrait-friendly: full-width search and a separate compact filter row.
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().height(36.dp)
                .background(Color(0xFF161718), RoundedCornerShape(2.dp))
                .border(1.dp, Color(0xFF66594D), RoundedCornerShape(2.dp))
                .padding(horizontal = 9.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = query, onValueChange = onQuery,
                singleLine = true,
                textStyle = TextStyle(color = Cream, fontSize = 12.sp),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box {
                        if (query.isBlank())
                            Text("⌕  Search cards", color = Secondary, fontSize = 12.sp)
                        inner()
                    }
                }
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LibraryFilterSelector(
                "FACTION", faction,
                listOf("All", "Humans", "Fallen", "Hive", "Cabal"), onFaction
            )
            LibraryFilterSelector(
                "RARITY", rarity,
                listOf("All", "Rare", "Legendary", "Mythic", "Exotic"), onRarity
            )
        }
    }
}

@Composable
private fun CollectionScreen(
    vm: ShadowDeckViewModel, state: ShadowDeckUiState,
    onBack: () -> Unit, onCard: (ShadowData.Card) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var faction by rememberSaveable { mutableStateOf("All") }
    var rarity by rememberSaveable { mutableStateOf("All") }
    val filtered = remember(query, faction, rarity, vm.alphabetical) {
        vm.alphabetical.filter { card ->
            (faction == "All" || card.faction.equals(faction, true)) &&
                (rarity == "All" || card.rarity.equals(rarity, true)) &&
                (query.isBlank() || listOf(card.name, card.effect, card.faction, card.rarity)
                    .any { it.contains(query.trim(), ignoreCase = true) })
        }
    }
    Column(
        Modifier.fillMaxSize().background(Color(0xFF141415))
    ) {
        Row(
            Modifier.fillMaxWidth().height(43.dp)
                .background(Color(0xFF221E1B))
                .border(1.dp, Color(0xFF554636))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("‹ BACK", color = Gold, fontSize = 12.sp) }
            Text("CARD COLLECTION", color = Cream, fontSize = 17.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("${filtered.size} / ${vm.catalog.size} CARDS",
                color = Gold, fontSize = 12.sp)
        }
        LibrarySearchBar(query, { query = it }, faction, { faction = it },
            rarity, { rarity = it })
        Box(
            Modifier.fillMaxSize().padding(8.dp)
                .background(Color(0xFF272423), RoundedCornerShape(3.dp))
                .border(1.dp, Color(0xFF806648), RoundedCornerShape(3.dp))
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                gridItems(filtered, key = { it.name }) { card ->
                    LibraryCardFace(card, state.cards[card.name] ?: 0, { onCard(card) })
                }
            }
        }
    }
}

@Composable
private fun DeckScreen(
    vm: ShadowDeckViewModel, state: ShadowDeckUiState, onBack: () -> Unit,
    onCard: (ShadowData.Card) -> Unit, onBattle: () -> Unit
) {
    val context = LocalContext.current
    var deckMenu by remember { mutableStateOf(false) }
    var deckDialog by remember { mutableStateOf("") }
    var editName by remember { mutableStateOf("") }
    var importText by remember { mutableStateOf("") }
    var pasteDialog by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var coverDialog by remember { mutableStateOf(false) }
    var addSearch by rememberSaveable { mutableStateOf("") }
    var addFaction by rememberSaveable { mutableStateOf("All") }
    var addRarity by rememberSaveable { mutableStateOf("All") }
    var showDeckPane by rememberSaveable { mutableStateOf(false) }
    var savePressed by remember(state.selected, state.cards) { mutableStateOf(false) }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                vm.importText(reader.readText())
            }
        }
    }
    val exportFile = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                it.write(vm.exportText())
            }
        }
    }
    val count = state.count
    val power = state.cards.entries.sumOf { (name, qty) ->
        (vm.catalog[name]?.numericPower()?.coerceAtLeast(0) ?: 0) * qty
    }
    val curve = (1..6).map { cost ->
        state.cards.entries.sumOf { (name, qty) ->
            val cardCost = vm.catalog[name]?.numericCost() ?: -1
            if (if (cost == 6) cardCost >= 6 else cardCost == cost) qty else 0
        }
    }
    val legality = vm.legality()
    val deckCards = state.cards.keys.mapNotNull { vm.catalog[it] }.sortedBy { it.name }
    val suggestions = vm.alphabetical.filter { card ->
        card.isPlayable &&
            (addFaction == "All" || card.faction.equals(addFaction, true)) &&
            (addRarity == "All" || card.rarity.equals(addRarity, true)) &&
            (addSearch.isBlank() || listOf(card.name, card.effect)
                .any { it.contains(addSearch.trim(), ignoreCase = true) })
    }

    // The original minigame deck builder has two equally important panes:
    // a browseable card gallery at left, and the selected 15-card deck at right.
    Column(Modifier.fillMaxSize().background(Color(0xFF141415))) {
        Row(
            Modifier.fillMaxWidth().height(43.dp)
                .background(Color(0xFF211C1A))
                .border(1.dp, Color(0xFF5C4838))
                .padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            TextButton(onClick = onBack) { Text("‹ BACK", color = Gold, fontSize = 12.sp) }
            Text("DECK", color = Cream, fontWeight = FontWeight.Bold,
                fontSize = 14.sp)
            Box(Modifier.weight(1f)) {
                TextButton(onClick = { deckMenu = true }) {
                    Text("${state.selected} ▾", color = Gold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
                DropdownMenu(expanded = deckMenu, onDismissRequest = { deckMenu = false }) {
                    state.names.forEach { name ->
                        DropdownMenuItem(text = { Text(name) }, onClick = {
                            vm.select(name); deckMenu = false
                        })
                    }
                }
            }
            Text("$count / 15", color = Cream, fontWeight = FontWeight.Bold,
                fontSize = 12.sp)
        }
        Row(
            Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = { showDeckPane = false },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text("CARD GALLERY", color = if (!showDeckPane) Gold else Secondary,
                    fontSize = 11.sp)
            }
            OutlinedButton(
                onClick = { showDeckPane = true },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text("MY DECK  $count / 15", color = if (showDeckPane) Gold else Secondary,
                    fontSize = 11.sp)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(5.dp)) {
            if (!showDeckPane) {
            // Gallery uses the full phone width instead of a cramped left split.
            Column(
                Modifier.fillMaxSize()
                    .background(Color(0xFF262322), RoundedCornerShape(3.dp))
                    .border(1.dp, Color(0xFF786046), RoundedCornerShape(3.dp))
            ) {
                LibrarySearchBar(
                    addSearch, { addSearch = it },
                    addFaction, { addFaction = it },
                    addRarity, { addRarity = it }
                )
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CARD GALLERY", color = Gold, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("${suggestions.size} CARDS  •  TAP ART TO INSPECT",
                        color = Secondary, fontSize = 9.sp)
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(7.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    gridItems(suggestions, key = { it.name }) { card ->
                        LibraryCardFace(
                            card,
                            state.cards[card.name] ?: 0,
                            onInspect = { onCard(card) },
                            onAdd = { vm.add(card) }
                        )
                    }
                }
            }
            } else {
            // The current deck replaces the gallery in portrait.
            Column(
                Modifier.fillMaxSize()
                    .background(Color(0xFF292423), RoundedCornerShape(3.dp))
                    .border(1.dp, Color(0xFF846846), RoundedCornerShape(3.dp))
                    .padding(horizontal = 7.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { editName = ""; deckDialog = "New" },
                        contentPadding = PaddingValues(horizontal = 3.dp)) {
                        Text("+ NEW", color = Gold, fontSize = 10.sp)
                    }
                    TextButton(onClick = {
                        editName = state.selected; deckDialog = "Rename"
                    }, contentPadding = PaddingValues(horizontal = 3.dp)) {
                        Text("RENAME", color = Secondary, fontSize = 10.sp)
                    }
                    Text(
                        state.selected.uppercase(), color = Cream,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text("$count / 15", color = Gold, fontWeight = FontWeight.Bold,
                        fontSize = 12.sp)
                }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("POWER $power", color = CoolBlue,
                        fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(Modifier.width(2.dp))
                    curve.forEachIndexed { index, amount ->
                        Column(
                            Modifier.weight(1f)
                                .background(Color(0xFF1B1D1E), RoundedCornerShape(2.dp))
                                .border(1.dp, Color(0xFF63564B))
                                .padding(vertical = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(if (index == 5) "6+" else "${index + 1}",
                                color = Gold, fontSize = 9.sp)
                            Text("$amount", color = Cream, fontSize = 10.sp,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (legality.isNotBlank()) {
                    Text(legality, color = Warn, fontSize = 10.sp, maxLines = 2)
                }
                HorizontalDivider(color = Color(0xFF5E5349))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    gridItems(deckCards, key = { it.name }) { card ->
                        LibraryCardFace(
                            card, state.cards[card.name] ?: 0,
                            onInspect = { onCard(card) }
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { coverDialog = true },
                        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text("COVER", fontSize = 9.sp)
                    }
                    TextButton(
                        onClick = { pasteDialog = true },
                        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 2.dp)
                    ) { Text("IMPORT", fontSize = 9.sp) }
                    TextButton(
                        onClick = { confirmDelete = true },
                        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 2.dp)
                    ) { Text("DELETE", fontSize = 9.sp, color = Warn) }
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = { exportFile.launch("${state.selected}.txt") },
                        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 2.dp)
                    ) { Text("EXPORT", fontSize = 9.sp) }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Button(
                        onClick = {
                            // Every edit is already persisted by ShadowDeckRepository.
                            // Re-read the selected deck so SAVE verifies persisted state.
                            vm.refresh()
                            savePressed = true
                        },
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(3.dp),
                        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            if (savePressed) "SAVED ✓" else "SAVE",
                            color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp
                        )
                    }
                    OutlinedButton(
                        onClick = onBattle,
                        enabled = legality.isEmpty(),
                        modifier = Modifier.weight(0.55f).height(36.dp),
                        shape = RoundedCornerShape(3.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("BATTLE ›", color = Gold, fontWeight = FontWeight.Bold,
                            fontSize = 10.sp, maxLines = 1)
                    }
                }
            }
            }
        }
    }
    if (coverDialog) {
        AlertDialog(
            onDismissRequest = { coverDialog = false },
            title = { Text("SHADOW SHAPER CARD BACK") },
            text = {
                Column(
                    Modifier.heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Choose one of the nine original covers. Your selection is saved with this deck.",
                        color = Secondary, fontSize = 12.sp)
                    DuelCoverCatalog.choices.forEach { option ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable {
                                    vm.selectCover(option.id)
                                    coverDialog = false
                                }
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            DuelCardCover(option.id, Modifier.width(42.dp).height(61.dp))
                            Text(option.title, color = if (state.cover == option.id) Gold else Cream,
                                fontWeight = if (state.cover == option.id)
                                    FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f))
                            if (state.cover == option.id) Text("✓", color = Gold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { coverDialog = false }) { Text("CLOSE") }
            }
        )
    }
    if (deckDialog.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { deckDialog = "" },
            title = { Text(if (deckDialog == "New") "New deck" else "Rename deck") },
            text = {
                OutlinedTextField(value = editName, onValueChange = { editName = it },
                    label = { Text("Deck name") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    if (deckDialog == "New") vm.create(editName) else vm.rename(editName)
                    deckDialog = ""
                }) { Text("SAVE") }
            },
            dismissButton = { TextButton(onClick = { deckDialog = "" }) { Text("CANCEL") } }
        )
    }
    if (pasteDialog) {
        AlertDialog(
            onDismissRequest = { pasteDialog = false },
            title = { Text("Import deck") },
            text = {
                Column {
                    Text("Accepts lines such as 2x Ayuela, one card per line.",
                        color = Secondary, fontSize = 12.sp)
                    OutlinedTextField(
                        value = importText, onValueChange = { importText = it },
                        minLines = 6, maxLines = 12,
                        label = { Text("Paste deck list") }
                    )
                    TextButton(onClick = {
                        pasteDialog = false
                        importFile.launch(arrayOf("text/plain", "text/*"))
                    }) { Text("Choose text file instead") }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.importText(importText)
                    pasteDialog = false
                }) { Text("IMPORT") }
            },
            dismissButton = { TextButton(onClick = { pasteDialog = false }) { Text("CANCEL") } }
        )
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete ${state.selected}?") },
        text = { Text("This saved deck will be deleted. The card collection remains unchanged.") },
        confirmButton = {
            TextButton(onClick = { vm.delete(); confirmDelete = false }) { Text("DELETE", color = Warn) }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("CANCEL") } }
    )
}

@Composable
private fun CardMetadataLine(label: String, value: String) {
    if (value.isBlank()) return
    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label.uppercase(), fontSize = 10.sp, color = Gold, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 13.sp, color = Cream, lineHeight = 18.sp)
    }
}

@Composable
private fun CardDetailsDialog(
    card: ShadowData.Card, copies: Int,
    onDismiss: () -> Unit, onAdd: () -> Unit, onRemove: () -> Unit
) {
    var enlarged by remember(card.name) { mutableStateOf(false) }
    val context = LocalContext.current
    val keywords = remember(card.name) {
        ShadowKeywordGlossary.onCard(card, ShadowKeywordGlossary.all(context))
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.94f).widthIn(max = 540.dp),
            color = Panel,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Gold)
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        card.name, color = Cream, fontWeight = FontWeight.Bold,
                        fontSize = 19.sp, modifier = Modifier.weight(1f),
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    TextButton(onClick = onDismiss) { Text("CLOSE ×", color = Gold) }
                }
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                        .padding(horizontal = 15.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Give AndroidView an explicit, stable viewport. A
                    // percentage-width/ratio view in a scrolling Column could
                    // remeasure to a thin horizontal strip when expanded.
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val artWidth = minOf(maxWidth * 0.79f, 320.dp)
                        CardArtPreview(
                            card,
                            Modifier.width(artWidth)
                                .height(artWidth * (754f / 500f))
                                .clickable { enlarged = true }
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    TextButton(onClick = { enlarged = true }) {
                        Text("VIEW LARGE CARD ART", color = Gold, fontSize = 12.sp)
                    }
                    Column(Modifier.fillMaxWidth()) {
                            HorizontalDivider(color = Raised)
                            Spacer(Modifier.height(9.dp))
                            Text(
                                "${card.rarity.ifBlank { "Unknown rarity" }}  •  ${card.faction.ifBlank { "Unknown faction" }}",
                                color = Gold, fontSize = 15.sp, fontWeight = FontWeight.Bold
                            )
                            Text(
                                "POWER ${card.power.ifBlank { "?" }}   •   LIGHT ${card.cost.ifBlank { "?" }}",
                                color = CoolBlue, fontSize = 16.sp, fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(10.dp))
                            CardMetadataLine("Printed card effect", card.effect.ifBlank { "No effect recorded." })
                            CardMetadataLine("Card number", card.cardNumber)
                            if (!card.isPlayable) {
                                Text(
                                    "This card cannot be added to a constructed deck.",
                                    color = Warn, fontSize = 12.sp
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "KEYWORDS ON THIS CARD", color = Gold,
                                fontSize = 13.sp, fontWeight = FontWeight.Bold
                            )
                            if (keywords.isEmpty()) {
                                Text(
                                    "No defined keywords appear in this card's recorded ability.",
                                    color = Secondary, fontSize = 12.sp
                                )
                            } else {
                                ShadowKeywordDefinitions(
                                    keywords, accent = Gold, foreground = Cream,
                                    secondary = Secondary
                                )
                            }
                        }
                }
                HorizontalDivider(color = Raised)
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "IN DECK: $copies", color = Gold, fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onRemove, enabled = copies > 0) {
                        Text("− REMOVE")
                    }
                    TextButton(onClick = onAdd, enabled = card.isPlayable) {
                        Text("+ ADD")
                    }
                }
            }
        }
    }
    if (enlarged) {
        Dialog(
            onDismissRequest = { enlarged = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.98f).fillMaxHeight(0.96f),
                color = Ink,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Gold)
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 14.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            card.name, color = Cream, fontSize = 17.sp,
                            fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                            maxLines = 2
                        )
                        TextButton(onClick = { enlarged = false }) {
                            Text("CLOSE ×", color = Gold)
                        }
                    }
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val artWidth = minOf(maxWidth, maxHeight * (500f / 754f))
                        CardArtPreview(
                            card,
                            Modifier.width(artWidth).height(artWidth * (754f / 500f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RulesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val keywords = remember(context) { ShadowKeywordGlossary.all(context) }
    val sections = listOf(
        "OBJECTIVE" to "Finish round 6 with more total surviving Power than your opponent. Ties are draws.",
        "DECK LEGALITY" to "Construct exactly 15 cards. No more than 2 of a name, 3 Exotic, or 5 Mythic. Generated tokens cannot be placed directly into a constructed deck.",
        "ROUNDS & LIGHT" to "Both players act once during each of six rounds. Round N grants N Light points. Costs are paid as cards are played; Light does not carry between rounds.",
        "OFFENSIVE / DEFENSIVE" to "The randomly selected Offensive player acts first. The Defensive player takes the last action in round 6. Opening hands are provisionally modeled as five/four cards.",
        "BOARD POSITIONS" to "Units are placed in vacant Front Row or Back Row slots. Left and right mean immediate neighbors in the same row; a Column is a vertical pair.",
        "LINE & COLUMN" to "For a numbered Line ability, count a continuous horizontal group of units with the printed Line affix. Ordinary neighboring Cabal cards do not count and interrupt the Line. A Column requires a unit directly above or below in the same column.",
        "TARGETING" to "Tap or drag a card into a vacant slot. If its ability chooses a target, tap a highlighted eligible unit. The game rejects illegal targets without spending Light.",
        "STATUS EFFECTS" to "Inspired gives the unit +1 Power at the end of its owner's turn. Sustained effects stay active while their source is on the board and unsuppressed. Suppression removes existing Power bonuses, Inspired and Scorched, and disables sustained effects. Scorched deals delayed damage while active.",
        "WIN CONDITION" to "Total surviving Power determines victory. This is a Power-scoring game, not an attack-and-block game.",
        "SIMULATOR ACCURACY" to "The engine uses printed Line affixes and a shared keyword resolver. Original-game exactness for draw timing, simultaneous triggered effects, some summon pools, and select card edge cases is still undergoing verification. A scripted effect is not automatically a tested effect."
    )
    Column(Modifier.fillMaxSize()) {
        PageHeading("RULES", "Shadow Shaper Duels • six-round gameplay", onBack)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 17.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sections) { (heading, body) ->
                Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(heading, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Gold)
                        Spacer(Modifier.height(5.dp))
                        Text(body, fontSize = 13.sp, lineHeight = 19.sp, color = Cream)
                    }
                }
            }
            item {
                Text(
                    "KEYWORD GLOSSARY", color = Gold, fontWeight = FontWeight.Bold,
                    fontSize = 18.sp, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
            }
            items(keywords, key = { it.name }) { keyword ->
                Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            keyword.name.uppercase(), color = Gold,
                            fontWeight = FontWeight.Bold, fontSize = 15.sp
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(keyword.definition, color = Cream, fontSize = 13.sp, lineHeight = 19.sp)
                        if (!keyword.recordedTooltip) {
                            Text(
                                "Simulator mechanic explanation",
                                color = Secondary, fontSize = 11.sp,
                                modifier = Modifier.padding(top = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
