package com.tplatt.shadowshaper

import java.util.ArrayList
import java.util.Collections
import java.util.Locale
import java.util.Random
import kotlin.math.max
import kotlin.math.min

/**
 * Kotlin-first deterministic game engine. The effect handlers are in
 * ShadowCardEffects.kt; the UI only submits actions through the coordinator.
 * The public API preserves the signatures of the old Java ShadowEngine.
 */
class ShadowEngine(@JvmField val catalog: Map<String, ShadowData.Card>, @JvmField val seed: Long) {
    companion object {
        const val COLUMNS = ShadowRules.COLUMNS
        const val ROUNDS = ShadowRules.ROUNDS
    }

    class Unit(
        @JvmField val owner: Player,
        @JvmField val card: ShadowData.Card,
        @JvmField var row: Int,
        @JvmField var column: Int
    ) {
        @JvmField var power: Int = max(0, card.numericPower())
        @JvmField var bonus: Int = 0
        @JvmField var scorched: Int = 0
        @JvmField var scorchTurns: Int = 0
        @JvmField val timedBonuses: ArrayList<TimedBonus> = ArrayList()
        @JvmField var inspired = false
        @JvmField var suppressed = false
        @JvmField var bonusDoublerConsumed = false
        @JvmField var damageDoublerConsumed = false
        @JvmField var fuseOrigin: Player? = null
        @JvmField var fuseRound: Int = 0
    }

    class TimedBonus(@JvmField val value: Int, @JvmField var turns: Int)

    class Player(@JvmField val human: Boolean) {
        @JvmField val drawPile = ArrayList<ShadowData.Card>()
        @JvmField val hand = ArrayList<ShadowData.Card>()
        @JvmField val board: Array<Array<Unit?>> = Array(2) { arrayOfNulls(COLUMNS) }
        @JvmField var points = 0
    }

    class Target(@JvmField val owner: Player, @JvmField val row: Int, @JvmField val column: Int) {
        fun unit(): Unit? = if (ShadowRules.isOnBoard(row, column)) owner.board[row][column] else null
    }

    @JvmField val human = Player(true)
    @JvmField val cpu = Player(false)
    @JvmField val journal = ArrayList<String>()
    @JvmField val rng = Random(seed)
    @JvmField var lastActionError = ""
    @JvmField var humanFirst = false
    @JvmField var humanTurn = false
    @JvmField var finished = false
    @JvmField var round = 1
    @JvmField var completedTurns = 0
    @JvmField var unresolvedEffects = 0
    @JvmField var lastPlacedSide = -1
    @JvmField var lastPlacedRow = -1
    @JvmField var lastPlacedColumn = -1
    @JvmField var placementRevision = 0
    @JvmField var pendingMovedUnit: Unit? = null
    @JvmField var pendingMoveCaster: Player? = null

    internal val effects = ShadowCardEffects(this)
    private var resolutionDepth = 0

    internal fun log(message: String) {
        journal.add(message)
        if (journal.size > 90) journal.removeAt(0)
    }

    internal fun incomplete(card: ShadowData.Card, reason: String) {
        unresolvedEffects++
        log("UNIMPLEMENTED: " + card.name + " — " + reason)
    }

    internal fun noEffect(card: ShadowData.Card): Boolean =
        card.effect.isBlank() || card.effect.contains("no special effects", ignoreCase = true)

    internal fun card(name: String): ShadowData.Card? = catalog[name]

    fun start(playerDeck: Map<String, Int>, cpuDeck: Map<String, Int>) {
        var error = ShadowData.validate(playerDeck, catalog)
        require(error.isEmpty()) { error }
        error = ShadowData.validate(cpuDeck, catalog)
        require(error.isEmpty()) { "CPU: " + error }
        clear(human)
        clear(cpu)
        journal.clear()
        unresolvedEffects = 0
        lastActionError = ""
        pendingMovedUnit = null
        pendingMoveCaster = null
        human.drawPile.addAll(ShadowData.expanded(playerDeck, catalog))
        cpu.drawPile.addAll(ShadowData.expanded(cpuDeck, catalog))
        Collections.shuffle(human.drawPile, rng)
        Collections.shuffle(cpu.drawPile, rng)
        humanFirst = rng.nextBoolean()
        humanTurn = humanFirst
        finished = false
        round = 1
        completedTurns = 0
        lastPlacedSide = -1
        lastPlacedRow = -1
        lastPlacedColumn = -1
        placementRevision = 0
        resolutionDepth = 0
        draw(human, ShadowRules.openingHand(humanFirst))
        draw(cpu, ShadowRules.openingHand(!humanFirst))
        human.points = ShadowRules.lightForRound(1)
        cpu.points = ShadowRules.lightForRound(1)
        log("MATCH SEED: " + seed)
        log(if (humanFirst) "Offensive: you act first." else "Defensive: CPU acts first.")
        log("Round 1 — 1 play point.")
    }

    private fun clear(side: Player) {
        side.drawPile.clear()
        side.hand.clear()
        side.points = 0
        for (row in 0..1) for (col in 0 until COLUMNS) side.board[row][col] = null
    }

    fun draw(side: Player, count: Int) {
        repeat(count.coerceAtLeast(0)) {
            if (side.drawPile.isNotEmpty()) side.hand.add(side.drawPile.removeAt(side.drawPile.lastIndex))
        }
    }

    fun score(side: Player): Int = all(side).sumOf { max(0, power(it)) }

    fun empty(side: Player, row: Int, column: Int): Boolean =
        ShadowRules.isOnBoard(row, column) && side.board[row][column] == null

    fun affordable(side: Player, index: Int): Boolean =
        index in side.hand.indices && side.hand[index].numericCost() > 0 &&
            side.hand[index].numericCost() <= side.points

    internal fun all(side: Player): ArrayList<Unit> = ArrayList<Unit>().apply {
        for (row in side.board) for (unit in row) if (unit != null) add(unit)
    }

    internal fun faction(side: Player, name: String): ArrayList<Unit> =
        ArrayList(all(side).filter { it.card.faction.equals(name, true) })

    internal fun inspired(side: Player, faction: String): ArrayList<Unit> =
        ArrayList(this.faction(side, faction).filter { it.inspired && !it.suppressed })

    internal fun hasFaction(side: Player, faction: String): Boolean =
        all(side).any { it.card.faction.equals(faction, true) }

    internal fun adjacent(first: Unit, second: Unit): Boolean =
        first.owner === second.owner &&
            ShadowPositionRules.isAdjacent(first.row, first.column, second.row, second.column)

    internal fun sameColumn(first: Unit, second: Unit): Boolean =
        first.owner === second.owner &&
            ShadowPositionRules.isSameColumn(first.row, first.column, second.row, second.column)

    internal fun line(unit: Unit, count: Int): Boolean = lineLength(unit) >= count

    internal fun lineLength(unit: Unit): Int =
        ShadowPositionRules.contiguousFactionLine(
            unit.owner, unit.card.faction, unit.row, unit.column, false
        )

    internal fun partnerInColumn(unit: Unit): Boolean =
        unit.owner.board[1 - unit.row][unit.column] != null

    fun power(unit: Unit): Int = max(0, unit.power + effects.aura(unit))
    fun original(unit: Unit): Int = max(0, unit.card.numericPower())
    fun powerBonus(unit: Unit): Int = max(0, power(unit) - original(unit))

    fun projectedFactionLine(side: Player, card: ShadowData.Card, row: Int, column: Int): Int =
        ShadowPositionRules.contiguousFactionLine(side, card.faction, row, column, true)

    fun isTargeted(card: ShadowData.Card): Boolean = effects.isTargeted(card.name)

    internal fun activeInRow(card: ShadowData.Card, row: Int): Boolean {
        var requirement = card.trigger.lowercase(Locale.ROOT)
        if (requirement.isEmpty()) {
            val text = card.effect.lowercase(Locale.ROOT)
            if (text.startsWith("when played (")) requirement = text.take(45)
        }
        return !((requirement.contains("back row") && row != ShadowRules.BACK_ROW) ||
            (requirement.contains("front row") && row != ShadowRules.FRONT_ROW))
    }

    fun targetOptions(me: Player, card: ShadowData.Card, row: Int, column: Int): List<Target> {
        if (!effects.isTargeted(card.name) || !activeInRow(card, row) || !empty(me, row, column))
            return emptyList()
        val needed = effects.requiredFactionLine(card.name)
        if (needed > 0 && projectedFactionLine(me, card, row, column) < needed)
            return emptyList()
        val side = if (effects.targetsEnemy(card.name)) other(me) else me
        val options = ArrayList<Target>()
        for (r in 0..1) for (col in 0 until COLUMNS) {
            val unit = side.board[r][col] ?: continue
            if (card.name in setOf("Vandal", "Wretch", "Incendior") && r != 1) continue
            if (card.name in setOf("Shinka Incendior", "Shinka Vigile") && r != 0) continue
            if (card.name == "Knight" && (!unit.card.faction.equals("Hive", true) || !unit.inspired))
                continue
            if (card.name == "Hyssop" && unit.bonus <= 0) continue
            if (card.name == "Wolf" && power(unit) <= 8) continue
            if (card.name == "Vigile" && !line(unit, 2)) continue
            if (card.name in setOf("Gladiator", "Shinka Gladiator") && r != 1) continue
            if (card.name == "Sabikis" &&
                (col == 0 || side.board[r][col - 1] == null)) continue
            options.add(Target(side, r, col))
        }
        return options
    }

    fun other(side: Player): Player = if (side === human) cpu else human
    internal fun targetUnit(target: Target?): Unit? = target?.unit()

    private fun chooseAI(me: Player, card: ShadowData.Card, row: Int, col: Int): Target? =
        targetOptions(me, card, row, col).maxByOrNull { power(it.unit()!!) }

    fun validatePlay(me: Player, index: Int, row: Int, column: Int, target: Target?): String {
        if (me !== human && me !== cpu) return "Unknown player."
        if (finished) return "The match has finished."
        if (pendingMovedUnit != null)
            return "Choose a destination for " + pendingMovedUnit!!.card.name + " before playing another card."
        if (me !== (if (humanTurn) human else cpu)) return "It is not this player's turn."
        if (index !in me.hand.indices) return "Selected card is not in hand."
        val card = me.hand[index]
        if (card.numericCost() < 1) return "Card is not playable (missing or invalid cost)."
        if (card.numericPower() < 0) return "Card Power is not verified."
        if (card.numericCost() > me.points) return "Insufficient Light."
        if (!ShadowRules.isOnBoard(row, column)) return "Invalid battlefield position."
        if (!empty(me, row, column)) return "Position is already occupied."
        if (target != null) {
            if (target.owner !== human && target.owner !== cpu)
                return "Target does not belong to this match."
            if (!ShadowRules.isOnBoard(target.row, target.column))
                return "Target is outside the battlefield."
            if (target.unit() == null) return "Target is no longer on the battlefield."
        }
        val choices = targetOptions(me, card, row, column)
        if (target != null) {
            if (choices.none {
                    it.owner === target.owner && it.row == target.row && it.column == target.column
                }) return "That unit is not a legal target for " + card.name + "."
        } else if (choices.isNotEmpty()) return "Choose a target for " + card.name + "."
        return ""
    }

    fun isPlayablePlacement(side: Player, index: Int, row: Int, column: Int): Boolean =
        (side === human || side === cpu) && !finished && pendingMovedUnit == null &&
            side === (if (humanTurn) human else cpu) && index in side.hand.indices &&
            affordable(side, index) && side.hand[index].numericPower() >= 0 &&
            empty(side, row, column)

    fun turnLabel(): String = if (finished) "FINAL" else
        (if (humanTurn) "PLAYER" else "CPU") + " • ROUND " + round

    fun play(me: Player, index: Int, row: Int, col: Int, target: Target?): Boolean {
        lastActionError = validatePlay(me, index, row, col, target)
        if (lastActionError.isNotEmpty()) return false
        val card = me.hand.removeAt(index)
        me.points -= card.numericCost()
        val unit = Unit(me, card, row, col)
        me.board[row][col] = unit
        lastPlacedSide = if (me.human) 0 else 1
        lastPlacedRow = row
        lastPlacedColumn = col
        placementRevision++
        log((if (me.human) "YOU" else "CPU") + " ▶ " + card.name + " • " +
            (if (row == 0) "BACK" else "FRONT") + " " + (col + 1))
        effects.onPlayed(unit, target)
        if (me.board[unit.row][unit.column] === unit) effects.fireEnemyPlacement(unit)
        sweep()
        if (me === cpu) resolveCpuMove()
        return true
    }

    internal fun at(side: Player, row: Int, column: Int): Unit? =
        if (ShadowRules.isOnBoard(row, column)) side.board[row][column] else null

    internal fun countRow(side: Player, row: Int): Int = side.board[row].count { it != null }
    internal fun rowEmpty(side: Player, row: Int): Boolean = countRow(side, row) == 0
    internal fun hasEmptySlot(side: Player): Boolean =
        (0..1).any { row -> (0 until COLUMNS).any { empty(side, row, it) } }

    fun resolveMove(actor: Player, row: Int, col: Int): Boolean {
        val moving = pendingMovedUnit
        if (moving == null || pendingMoveCaster !== actor) {
            lastActionError = "No movement is pending for this player."
            return false
        }
        if (moving.owner.board[moving.row][moving.column] !== moving) {
            pendingMovedUnit = null
            pendingMoveCaster = null
            lastActionError = "Target was removed before relocation."
            return false
        }
        if (!empty(moving.owner, row, col)) {
            lastActionError = "Select an empty destination on the target's battlefield."
            return false
        }
        moving.owner.board[moving.row][moving.column] = null
        moving.row = row
        moving.column = col
        moving.owner.board[row][col] = moving
        lastPlacedSide = if (moving.owner.human) 0 else 1
        lastPlacedRow = row
        lastPlacedColumn = col
        placementRevision++
        pendingMovedUnit = null
        pendingMoveCaster = null
        lastActionError = ""
        log("RELOCATED ▶ " + moving.card.name + " to " +
            (if (row == 0) "BACK" else "FRONT") + " " + (col + 1))
        sweep()
        return true
    }

    fun resolveCpuMove() {
        val moving = pendingMovedUnit ?: return
        if (pendingMoveCaster !== cpu) return
        val choices = ArrayList<Target>()
        for (r in 0..1) for (c in 0 until COLUMNS)
            if (empty(moving.owner, r, c)) choices.add(Target(moving.owner, r, c))
        if (choices.isNotEmpty()) {
            val destination = choices[rng.nextInt(choices.size)]
            resolveMove(cpu, destination.row, destination.column)
        }
    }

    internal fun spawnRandom(side: Player, row: Int, token: String, source: ShadowData.Card) {
        val choices = (0 until COLUMNS).filter { empty(side, row, it) }
        if (choices.isEmpty()) {
            incomplete(source, "No empty slot for " + token + "; full-row behavior not verified.")
            return
        }
        spawnAt(side, row, choices[rng.nextInt(choices.size)], token)
    }

    internal fun spawnAt(side: Player, row: Int, col: Int, name: String) {
        if (!empty(side, row, col)) return
        val card = card(name)
        if (card == null || card.numericPower() < 0) {
            if (card != null) incomplete(card, "Token/base Power missing.")
            return
        }
        val summoned = Unit(side, card, row, col)
        side.board[row][col] = summoned
        log("SUMMON ▶ " + name + " at " + (if (row == 0) "back " else "front ") + (col + 1))
        effects.fireEnemyPlacement(summoned)
    }

    internal fun inspire(target: Unit?) {
        if (target == null) return
        target.inspired = true
        log("INSPIRED ▶ " + target.card.name)
    }

    internal fun inspireRandom(side: Player, faction: String, count: Int) {
        val candidates = faction(side, faction)
        Collections.shuffle(candidates, rng)
        candidates.take(count).forEach(::inspire)
    }

    internal fun buffRandom(choices: List<Unit>, count: Int, amount: Int) {
        val shuffled = ArrayList(choices)
        Collections.shuffle(shuffled, rng)
        shuffled.take(count).forEach { buff(it, amount) }
    }

    internal fun buff(target: Unit?, amount: Int) {
        if (target == null || amount <= 0) return
        var applied = amount
        val doubler = all(target.owner).firstOrNull {
            !it.suppressed && it.card.name == "Tan-2" && !it.bonusDoublerConsumed
        }
        if (doubler != null) {
            applied *= 2
            doubler.bonusDoublerConsumed = true
            log("Tan-2 doubles next Power bonus (+ " + applied + "); remains on battlefield.")
        }
        target.power += applied
        target.bonus += applied
        log("BUFF +" + applied + " ▶ " + target.card.name)
        onBuff(target)
    }

    private fun onBuff(target: Unit) {
        if (resolutionDepth++ > 32) {
            resolutionDepth--
            unresolvedEffects++
            log("SAFETY: recursive Power triggers capped after 32 resolutions.")
            return
        }
        try {
            effects.onBuff(target)
            val foe = other(target.owner)
            if (target.row == 1) {
                for (sniper in all(foe)) if (sniper.card.name == "Elite Sniper Vandal" && !sniper.suppressed)
                    damage(sniper, target, 1)
            }
            sweep()
        } finally {
            resolutionDepth--
        }
    }

    internal fun randomUnit(side: Player): Unit? = all(side).let {
        if (it.isEmpty()) null else it[rng.nextInt(it.size)]
    }

    internal fun suppress(target: Unit?) {
        if (target == null) return
        target.power -= target.bonus
        target.bonus = 0
        target.suppressed = true
        target.inspired = false
        target.scorched = 0
        target.scorchTurns = 0
        target.timedBonuses.clear()
        log("SUPPRESSED ▶ " + target.card.name)
    }

    internal fun damage(source: Unit, target: Unit?, amount: Int) {
        if (target == null || amount <= 0) return
        var actual = ShadowDamageRules.afterAuras(
            amount, effects.extraDamage(source), effects.reduction(target)
        )
        val doubler = all(source.owner).firstOrNull {
            !it.suppressed && it.card.name == "Jolder" && !it.damageDoublerConsumed
        }
        if (doubler != null) {
            actual *= 2
            doubler.damageDoublerConsumed = true
            log("Jolder doubles the next damage event without becoming Suppressed.")
        }
        if (all(source.owner).any {
                it !== source && !it.suppressed && it.card.name == "Umeko" && it.row == 0
            }) actual += 1 + rng.nextInt(3)
        dealDirect(source, target, actual)
    }

    internal fun dealDirect(source: Unit, target: Unit?, damage: Int) {
        if (target == null || damage <= 0) return
        target.power -= damage
        log("DAMAGE " + damage + " ▶ " + target.card.name)
        if (target.card.faction.equals("Humans", true)) {
            for (ally in all(target.owner))
                if (!ally.suppressed && ally.card.name == "Saladin") buff(target, 3)
        }
    }

    internal fun sweep() {
        repeat(12) {
            val killed = (all(human) + all(cpu)).filter { power(it) <= 0 }
            if (killed.isEmpty()) return
            for (dead in killed) {
                if (dead.owner.board[dead.row][dead.column] !== dead) continue
                dead.owner.board[dead.row][dead.column] = null
                log("DESTROYED ▶ " + dead.card.name)
                val splash = when (dead.card.name) {
                    "Clone" -> 3
                    "Exploder Shank" -> 1
                    else -> 0
                }
                if (splash > 0) for (offset in listOf(-1, 1))
                    dealDirect(dead, at(dead.owner, dead.row, dead.column + offset), splash)
            }
        }
    }

    internal fun transferCursedThrall(unit: Unit) {
        val origin = unit.owner
        val destination = other(origin)
        val positions = (0 until COLUMNS).filter { empty(destination, 1, it) }
        if (positions.isEmpty()) {
            incomplete(unit.card, "Opponent Front Row full; transfer failure behavior unverified.")
            return
        }
        val column = positions[rng.nextInt(positions.size)]
        origin.board[unit.row][unit.column] = null
        val bomb = Unit(destination, unit.card, 1, column)
        bomb.power = unit.power
        bomb.bonus = unit.bonus
        bomb.fuseOrigin = origin
        bomb.fuseRound = round + 1
        destination.board[1][column] = bomb
        lastPlacedSide = if (destination.human) 0 else 1
        lastPlacedRow = 1
        lastPlacedColumn = column
        log("CURSED THRALL ▶ opponent Front Row " + (column + 1) + "; detonates on next original turn.")
        effects.fireEnemyPlacement(bomb)
    }

    private fun startSide(side: Player) {
        for (controller in listOf(human, cpu)) for (unit in all(controller)) {
            if (unit.fuseOrigin !== side || unit.fuseRound > round) continue
            if (controller.board[unit.row][unit.column] !== unit) continue
            controller.board[unit.row][unit.column] = null
            log("CURSED THRALL DETONATES ▶ " + (unit.column + 1))
            for (offset in listOf(-1, 1))
                dealDirect(unit, at(controller, unit.row, unit.column + offset), 2)
        }
        sweep()
    }

    fun endTurn() {
        if (finished) return
        if (pendingMovedUnit != null) {
            lastActionError = "Relocate " + pendingMovedUnit!!.card.name + " before ending your turn."
            return
        }
        lastActionError = ""
        val current = if (humanTurn) human else cpu
        effects.endSide(current)
        completedTurns++
        if (completedTurns == 2) {
            if (round == ROUNDS) {
                finished = true
                log("FINAL • YOU " + score(human) + " — " + score(cpu) + " CPU")
                return
            }
            round++
            completedTurns = 0
            human.points = ShadowRules.lightForRound(round)
            cpu.points = ShadowRules.lightForRound(round)
            draw(human, ShadowRules.NEW_ROUND_DRAW)
            draw(cpu, ShadowRules.NEW_ROUND_DRAW)
            humanTurn = humanFirst
            log("ROUND " + round + " • " + round + " play points")
        } else {
            humanTurn = !humanTurn
        }
        startSide(if (humanTurn) human else cpu)
    }

    fun cpuPlayOne(): Boolean {
        if (finished || humanTurn) return false
        if (pendingMovedUnit != null) {
            resolveCpuMove()
            return false
        }
        var bestIndex = -1
        var bestRow = -1
        var bestColumn = -1
        var bestValue = Double.NEGATIVE_INFINITY
        for (index in cpu.hand.indices) {
            val card = cpu.hand[index]
            if (card.numericCost() !in 1..cpu.points) continue
            for (row in 0 until ShadowRules.ROWS) for (col in 0 until COLUMNS) {
                if (!isPlayablePlacement(cpu, index, row, col)) continue
                val value = evaluateCpuPlacement(card, row, col)
                if (value > bestValue) {
                    bestValue = value
                    bestIndex = index
                    bestRow = row
                    bestColumn = col
                }
            }
        }
        if (bestIndex < 0) return false
        val card = cpu.hand[bestIndex]
        val target = chooseAI(cpu, card, bestRow, bestColumn)
        if (!play(cpu, bestIndex, bestRow, bestColumn, target)) {
            log("CPU rejected move: " + lastActionError)
            return false
        }
        return true
    }

    fun cpuTurn() {
        if (finished || humanTurn) return
        var safety = 40
        while (safety-- > 0 && cpuPlayOne()) { }
        if (!finished && !humanTurn) endTurn()
    }

    private fun evaluateCpuPlacement(card: ShadowData.Card, row: Int, col: Int): Double {
        var value = (max(0, card.numericPower()) + 0.5) / card.numericCost().toDouble()
        val trigger = (card.trigger.ifBlank { card.effect }).lowercase(Locale.ROOT)
        if (trigger.contains("front row")) value += if (row == 1) 2.2 else -2.8
        if (trigger.contains("back row")) value += if (row == 0) 2.2 else -2.8
        val contiguous = projectedFactionLine(cpu, card, row, col)
        value += max(0, contiguous - 1) * 1.1
        val required = effects.requiredFactionLine(card.name)
        if (required > 0) value += if (contiguous >= required) 4.0 else -1.5
        if (cpu.board[1 - row][col] != null) value += 0.9
        if (col > 0 && cpu.board[row][col - 1] != null) value += 0.3
        if (col < COLUMNS - 1 && cpu.board[row][col + 1] != null) value += 0.3
        if (isTargeted(card))
            value += if (targetOptions(cpu, card, row, col).isNotEmpty()) 1.3 else -1.0
        return value + rng.nextDouble() * 0.12
    }

    fun diagnosticReport(): String = buildString {
        append("SHADOWSHAPER DUELS • LOCAL SIMULATOR\n")
        append("Seed: ").append(seed).append("\n")
        append("Round: ").append(round).append("/").append(ROUNDS).append("\n")
        append("Turn: ").append(turnLabel()).append("\n")
        append("Offensive: ").append(if (humanFirst) "Player" else "CPU").append("\n")
        append("Score: ").append(score(human)).append(" / ").append(score(cpu)).append("\n")
        append("Light: player ").append(human.points).append(", CPU ").append(cpu.points).append("\n")
        append("Pending card effects: ").append(unresolvedEffects).append("\n")
        append("Match complete: ").append(finished).append("\n\n")
        appendSideReport(this, "PLAYER", human)
        appendSideReport(this, "CPU", cpu)
        append("\nEVENTS\n")
        for (event in journal) append(event).append('\n')
    }

    private fun appendSideReport(report: StringBuilder, label: String, side: Player) {
        report.append(label).append(": Deck ").append(side.drawPile.size)
            .append(" / Hand ").append(side.hand.size).append('\n')
        for (row in 0..1) {
            report.append(if (row == 0) "BACK: " else "FRONT: ")
            for (col in 0 until COLUMNS) {
                val unit = side.board[row][col]
                report.append('[').append(col + 1).append(' ')
                if (unit == null) report.append("EMPTY") else
                    report.append(unit.card.name).append(" Power=").append(power(unit))
                        .append(" Inspired=").append(unit.inspired)
                        .append(" Suppressed=").append(unit.suppressed)
                report.append("] ")
            }
            report.append('\n')
        }
    }

    fun result(): String = if (!finished) "" else ShadowRules.result(score(human), score(cpu))
}
