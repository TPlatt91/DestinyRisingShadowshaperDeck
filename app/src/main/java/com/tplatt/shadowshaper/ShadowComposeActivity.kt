package com.tplatt.shadowshaper

import android.app.Activity
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Kotlin-first, Compose-based primary app. BattleActivity remains the existing
 * landscape view until the highly custom battlefield is migrated and validated.
 *
 * Prior Java MainActivity/HomeActivity/RulesActivity stay in source as fallback
 * during migration; the launcher now enters this Activity.
 */
class ShadowComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    var page by rememberSaveable { mutableStateOf("home") }
    var detail by remember { mutableStateOf<ShadowData.Card?>(null) }
    val state by vm.state.collectAsState()
    BackHandler(page != "home") { page = "home" }
    Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            when (page) {
                "home" -> HomeScreen(
                    onBattle = { activity.startActivity(Intent(activity, BattleActivity::class.java)) },
                    onOpen = { page = it },
                    catalogCount = vm.catalog.size
                )
                "deck" -> DeckScreen(
                    vm = vm, state = state,
                    onBack = { page = "home" },
                    onCard = { detail = it },
                    onBattle = {
                        val intent = Intent(activity, BattleActivity::class.java)
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
private fun HomeScreen(onBattle: () -> Unit, onOpen: (String) -> Unit, catalogCount: Int) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("DESTINY: RISING", color = Gold, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            Spacer(Modifier.height(9.dp))
            Text("SHADOWSHAPER\nDUELS", fontSize = 37.sp, lineHeight = 40.sp,
                color = Cream, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(7.dp))
            Text("CARD ARCHIVE & SINGLE-PLAYER SIMULATOR", fontSize = 11.sp, color = Secondary)
            Spacer(Modifier.height(22.dp))
        }
        item { HomeDestination("⚔", "SIM BATTLE", "Play a six-round match against AI", onBattle) }
        item { HomeDestination("▤", "DECK CONSTRUCTION", "Build decks and manage 15-card limits") { onOpen("deck") } }
        item { HomeDestination("◈", "CARD COLLECTION", "Browse $catalogCount recorded cards and their effects") { onOpen("collection") } }
        item { HomeDestination("≡", "RULES", "Rounds, positioning, status and scoring") { onOpen("rules") } }
        item {
            Spacer(Modifier.height(18.dp))
            Text("KOTLIN + JETPACK COMPOSE  •  SOLO AGAINST AI",
                fontSize = 10.sp, color = Secondary, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
            Text("Unverified card effects are labeled in the card viewer.",
                fontSize = 11.sp, color = Secondary, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        }
    }
}

@Composable
private fun HomeDestination(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick, shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF785C41)),
        colors = CardDefaults.cardColors(containerColor = Panel)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                Modifier.size(54.dp).background(Raised, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) { Text(icon, color = Gold, fontSize = 24.sp) }
            Column(Modifier.weight(1f)) {
                Text(title, color = Cream, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(subtitle, color = Secondary, fontSize = 12.sp)
            }
            Text("›", color = Gold, fontSize = 25.sp)
        }
    }
}

@Composable
private fun CardArtPreview(card: ShadowData.Card, modifier: Modifier = Modifier) {
    val activity = LocalContext.current as? Activity
    key(card.name) {
        AndroidView(
            factory = { ctx ->
                ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    if (activity != null) CardArt.bind(activity, card, this)
                }
            },
            update = { view ->
                if (activity != null &&
                    view.contentDescription?.toString() != card.name + " card art"
                ) CardArt.bind(activity, card, view)
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
                Text("\${card.rarity}  •  \${card.faction}", fontSize = 12.sp, color = Gold)
                Text("POWER \${card.power.ifBlank { "?" }}   LIGHT \${card.cost.ifBlank { "?" }}",
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CoolBlue)
                Spacer(Modifier.height(5.dp))
                Text(card.effect.ifBlank { "No effect documented" },
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = Secondary, fontSize = 11.sp, lineHeight = 14.sp)
            }
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
    Column(Modifier.fillMaxSize()) {
        PageHeading("CARD COLLECTION", "\${filtered.size} visible of \${vm.catalog.size} cards", onBack)
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            label = { Text("Search name, faction or ability") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            lazyRowItems(listOf("All", "Humans", "Fallen", "Hive", "Cabal")) { option ->
                FilterChip(
                    selected = faction == option, onClick = { faction = option },
                    label = { Text(option) }
                )
            }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            lazyRowItems(listOf("All", "Rare", "Legendary", "Mythic", "Exotic")) { option ->
                FilterChip(selected = rarity == option, onClick = { rarity = option },
                    label = { Text(option) })
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            items(filtered, key = { it.name }) { card ->
                CardTile(card, state.cards[card.name] ?: 0) { onCard(card) }
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
    var addSearch by rememberSaveable { mutableStateOf("") }
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
        card.name !in state.cards &&
        (addSearch.isBlank() || card.name.contains(addSearch.trim(), true))
    }.take(30)

    Column(Modifier.fillMaxSize()) {
        PageHeading("DECK CONSTRUCTION", "Your saved decks are preserved from the Java version", onBack)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f)) {
                OutlinedButton(onClick = { deckMenu = true }) {
                    Text(state.selected, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("  ▾")
                }
                DropdownMenu(expanded = deckMenu, onDismissRequest = { deckMenu = false }) {
                    state.names.forEach { name ->
                        DropdownMenuItem(text = { Text(name) }, onClick = {
                            vm.select(name); deckMenu = false
                        })
                    }
                }
            }
            TextButton(onClick = { editName = ""; deckDialog = "New" }) { Text("+ NEW") }
            TextButton(onClick = { editName = state.selected; deckDialog = "Rename" }) { Text("RENAME") }
        }
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Raised)
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$power POWER", color = Gold, fontSize = 23.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("$count/15", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Cream)
                }
                Text(if (legality.isEmpty()) "LEGAL DECK" else legality,
                    color = if (legality.isEmpty()) Color(0xFFB6E1C1) else Warn,
                    fontSize = 12.sp)
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    curve.forEachIndexed { index, amount ->
                        Column(
                            modifier = Modifier.weight(1f)
                                .background(Panel, RoundedCornerShape(7.dp))
                                .padding(vertical = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(if (index == 5) "6+" else "\${index + 1}",
                                color = Gold, fontSize = 11.sp)
                            Text("$amount", color = Cream, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Button(onClick = onBattle, enabled = legality.isEmpty(),
                modifier = Modifier.weight(1f)) { Text("⚔  BATTLE") }
            OutlinedButton(onClick = { pasteDialog = true }) { Text("IMPORT") }
            OutlinedButton(onClick = { exportFile.launch("\${state.selected}.txt") }) { Text("SAVE FILE") }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TextButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Shadowshaper deck", vm.exportText()))
            }) { Text("Copy deck") }
            TextButton(onClick = {
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, vm.exportText())
                    putExtra(Intent.EXTRA_SUBJECT, state.selected)
                }, "Share deck"))
            }) { Text("Share") }
            TextButton(onClick = { confirmDelete = true }) { Text("Delete", color = Warn) }
        }
        OutlinedTextField(
            value = addSearch, onValueChange = { addSearch = it },
            label = { Text("Find cards to add") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            item { Text("IN YOUR DECK", color = Gold, fontWeight = FontWeight.Bold) }
            if (deckCards.isEmpty()) item { Text("No cards added yet. Choose a card below.", color = Secondary) }
            items(deckCards, key = { "selected_" + it.name }) { card ->
                CardTile(card, state.cards[card.name] ?: 0) { onCard(card) }
            }
            item {
                HorizontalDivider(color = Color(0xFF775943), modifier = Modifier.padding(vertical = 8.dp))
                Text("ADD CARDS", color = Gold, fontWeight = FontWeight.Bold)
            }
            items(suggestions, key = { "suggested_" + it.name }) { card ->
                CardTile(card, 0) { onCard(card) }
            }
        }
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
        title = { Text("Delete \${state.selected}?") },
        text = { Text("This saved deck will be deleted. The card collection remains unchanged.") },
        confirmButton = {
            TextButton(onClick = { vm.delete(); confirmDelete = false }) { Text("DELETE", color = Warn) }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("CANCEL") } }
    )
}

@Composable
private fun CardDetailsDialog(
    card: ShadowData.Card, copies: Int,
    onDismiss: () -> Unit, onAdd: () -> Unit, onRemove: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(card.name, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                CardArtPreview(card, Modifier.fillMaxWidth().height(225.dp))
                Spacer(Modifier.height(8.dp))
                Text("\${card.rarity}  •  \${card.faction}", color = Gold, fontWeight = FontWeight.Bold)
                Text("POWER: \${card.power.ifBlank { "Unknown" }}     LIGHT: \${card.cost.ifBlank { "Unknown" }}",
                    color = CoolBlue, fontWeight = FontWeight.Bold)
                if (card.cardNumber.isNotBlank()) Text("Card number: \${card.cardNumber}", color = Secondary)
                if (card.trigger.isNotBlank()) Text(card.trigger, fontSize = 12.sp, color = Gold)
                Spacer(Modifier.height(5.dp))
                Text(card.effect, color = Cream, maxLines = 7)
                if (card.acquisition.isNotBlank()) Text("Obtain: \${card.acquisition}",
                    fontSize = 11.sp, color = Secondary)
                Text(ShadowEffectCoverage.status(card.name, card.effect, card.cost, card.power),
                    fontSize = 11.sp, color = Secondary, modifier = Modifier.padding(top = 7.dp))
                Text("In current deck: $copies", color = Gold, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onAdd, enabled = card.isPlayable) { Text("+ ADD") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onRemove, enabled = copies > 0) { Text("− REMOVE") }
                TextButton(onClick = onDismiss) { Text("CLOSE") }
            }
        }
    )
}

@Composable
private fun RulesScreen(onBack: () -> Unit) {
    val sections = listOf(
        "OBJECTIVE" to "Finish round 6 with more total surviving Power than your opponent. Ties are draws.",
        "DECK LEGALITY" to "Construct exactly 15 cards. No more than 2 of a name, 3 Exotic, or 5 Mythic. Generated tokens cannot be placed directly into a constructed deck.",
        "ROUNDS & LIGHT" to "Both players act once during each of six rounds. Round N grants N Light points. Costs are paid as cards are played; Light does not carry between rounds.",
        "OFFENSIVE / DEFENSIVE" to "The randomly selected Offensive player acts first. The Defensive player takes the last action in round 6. Opening hands are provisionally modeled as five/four cards.",
        "BOARD POSITIONS" to "Units are placed on your front or back row. Many abilities have requirements involving a row, adjacent cards, columns, or same-faction lines.",
        "TARGETING" to "Tap or drag a card into a vacant slot. If its ability chooses a target, tap a highlighted eligible unit. The game rejects illegal targets without spending Light.",
        "STATUS EFFECTS" to "Inspired grants +1 Power at turn end. Sustained effects persist while active. Suppression disables qualifying effects and removes bonuses. Scorched deals delayed damage.",
        "WIN CONDITION" to "Total surviving Power determines victory. This is a Power-scoring game, not an attack-and-block game.",
        "SIMULATOR ACCURACY" to "The engine is under verification. Board dimensions, exact Line geometry, later-round draw timing and some complex card effects remain provisional. A scripted ability is not automatically a fully tested ability."
    )
    Column(Modifier.fillMaxSize()) {
        PageHeading("RULES", "Shadowshaper Duels • six-round gameplay", onBack)
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
        }
    }
}
