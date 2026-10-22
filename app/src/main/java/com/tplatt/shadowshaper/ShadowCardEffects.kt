package com.tplatt.shadowshaper

import java.util.ArrayList
import java.util.Collections
import kotlin.math.max
import kotlin.math.min

/**
 * Shared Kotlin card-effect families. All unit changes are performed against
 * ShadowEngine state; the UI must not implement card rules directly.
 *
 * The original community card text is not always verified. A scripted branch
 * is not proof of exact in-game effect fidelity.
 */
internal class ShadowCardEffects(private val e: ShadowEngine) {
    private val allyTargets = setOf(
        "Morgran", "Hyssop", "Umbliks Prime", "Rossi-11", "Knight", "Wizard", "Guarding Servitor",
        "Finnala", "Chinnawat", "Attal", "Estela", "Vigile",
        "Sabikis", "Gladiator", "Shinka Gladiator"
    )
    private val enemyTargets = setOf(
        "Shinka Psion", "Inquisitor", "Psion", "Tracer Shank",
        "Vandal", "Wretch", "Incendior", "Devil Sentry", "Resilient Marauder",
        "Augmented Shank", "High Servitor", "Thinktank", "Amu-Atu",
        "Shinka Incendior", "Shinka Vigile", "Thagrius",
        "Wolf", "Wu Ming", "Gwynn", "Xuan Wei", "Ikora", "Goliath Tank", "Molkrul"
    )
    private val knownSustained = setOf(
        "Legionary", "Shinka Legionary", "Decurion", "Colossus",
        "Shinka Centurion", "Centurion", "Shinka Phalanx", "Phalanx",
        "Juggernaut", "Servitor", "Grenade Dreg", "Shinka Decurion",
        "Paradoxical Neophyte's Shade", "The Disfavored", "Ogre",
        "Tenacious Ogre", "Shrieker", "Saladin", "Tan-2",
        "Elite Sniper Vandal", "Ayuela", "Resilient Grenade Dreg",
        "Zhiyao", "Denzin", "Unrelenting Captain", "The Speaker",
        "Guerrilla Captain", "Umeko", "Jolder", "Xur",
        "Mind Disruptor", "Resilient Captain", "Helhest"
    )

    fun isTargeted(name: String): Boolean = name in allyTargets || name in enemyTargets
    fun targetsEnemy(name: String): Boolean = name in enemyTargets

    fun requiredFactionLine(name: String): Int = when (name) {
        "Shinka Psion" -> 2
        "Shinka Incendior", "Shinka Vigile" -> 3
        "Shinka Gladiator", "Goliath Tank" -> 5
        "Thagrius" -> 6
        else -> 0
    }

    /** Ongoing Power modifiers are computed rather than baked into base Power. */
    fun aura(target: ShadowEngine.Unit): Int = with(e) {
        if (target.suppressed) return@with 0
        var bonus = 0
        for (other in all(target.owner)) {
            if (other === target || other.suppressed) continue
            when (other.card.name) {
                "Decurion" -> if (sameColumn(other, target)) bonus += 2
                "Colossus" -> if (sameColumn(other, target) &&
                    target.card.faction.equals("Cabal", true)) bonus++
                "Shinka Centurion" -> if (partnerInColumn(other) &&
                    adjacent(other, target)) bonus += 3
                "Centurion" -> if (line(other, 4) && adjacent(other, target)) bonus += 2
            }
        }
        if (target.card.name == "Legionary" && partnerInColumn(target)) bonus++
        if (target.card.name == "Shinka Legionary" && partnerInColumn(target)) bonus += 2
        if (target.card.name == "Paradoxical Neophyte's Shade")
            bonus += 4 * inspired(target.owner, "Hive").size
        bonus
    }

    fun reduction(target: ShadowEngine.Unit): Int = with(e) {
        var decrease = 0
        for (other in all(target.owner)) {
            if (other.suppressed) continue
            if (other.card.name == "Shinka Phalanx" && sameColumn(other, target)) decrease++
            if (other.card.name == "Phalanx" && line(other, 3) &&
                target.card.faction.equals("Cabal", true)) decrease++
        }
        decrease
    }

    fun extraDamage(source: ShadowEngine.Unit): Int = with(e) {
        var bonus = source.damageBonus
        for (other in all(source.owner)) {
            if (other.suppressed) continue
            if (other.card.name == "Servitor") bonus++
            if (other.card.name == "Juggernaut" && partnerInColumn(other) &&
                source.card.faction.equals("Cabal", true) && adjacent(other, source)) bonus++
            if (other.card.name == "Shinka Decurion" && line(other, 3) &&
                source.card.faction.equals("Cabal", true)) bonus += 2
        }
        bonus
    }

    fun onPlayed(actor: ShadowEngine.Unit, target: ShadowEngine.Target?) = with(e) {
        val me = actor.owner
        val foe = other(me)
        val name = actor.card.name
        val picked = targetUnit(target)
        if (!activeInRow(actor.card, actor.row)) {
            if (!noEffect(actor.card)) log(name + ": position requirement not met.")
            return@with
        }

        when (name) {
            "Sparrow" -> buff(actor, 3)
            "Elite Vandal" -> if (!rowEmpty(foe, 1)) buff(actor, 3)
            "Jaren" -> if (actor.row == ShadowRules.FRONT_ROW) {
                // Six individually allocated random damage points; destroyed units
                // leave the random target pool before the next point is assigned.
                repeat(6) {
                    val victim = randomUnit(foe)
                    if (victim != null) {
                        damage(actor, victim, 1)
                        sweep()
                    }
                }
            }
            "Devil Sentry" -> if (rng.nextBoolean()) {
                log("Devil Sentry: heads.")
                damage(actor, picked, 5)
            } else log("Devil Sentry: tails; no damage.")
            "Common Cover" -> if (actor.row == ShadowRules.FRONT_ROW) {
                // Two separate random empty opponent Front Row positions.
                // Newly summoned Rock occupies its position before the second roll.
                repeat(2) { spawnRandom(foe, ShadowRules.FRONT_ROW, "Rock", actor.card) }
            }
            "Rossi-11" -> {
                val protectedUnit = picked ?: actor
                protectedUnit.overshield = true
                log("OVERSHIELD granted ▶ " + protectedUnit.card.name)
            }
            "Harrow" -> if (rng.nextBoolean()) {
                buff(actor, 5)
                log("Harrow: heads, +5 Power.")
            } else log("Harrow: tails, no bonus.")
            "Umbliks Prime" -> {
                val recipient = picked ?: actor
                if (rng.nextBoolean()) {
                    buff(recipient, 6)
                    log("Umbliks Prime: heads, +6 Power to " + recipient.card.name)
                } else log("Umbliks Prime: tails, no Power bonus.")
            }
            "Hexguard" -> {
                actor.inspired = true
                log("Inspired: " + name)
            }
            "Tenacious Shielded Knight", "Tenacious Hexguard" ->
                all(me).filter {
                    adjacent(actor, it) && it.card.faction.equals("Hive", true)
                }.forEach(::inspire)
            "Wizard", "Guarding Servitor" -> inspire(picked)
            "Tenacious Wizard" -> inspireRandom(me, "Hive", 2)
            "Knight" -> buff(picked, 3)
            "Tenacious Knight" -> if (actor.row == 1) buffRandom(inspired(me, "Hive"), 2, 3)
            "Morgran" -> buff(picked, 1)
            "Hyssop" -> buff(picked, 3)
            "Mishra" -> all(me).filter { adjacent(actor, it) }.forEach { buff(it, 3) }
            "Zhong Wenbin" ->
                if (actor.row == 1) buff(at(me, actor.row, actor.column + 1), 2)
            "Finnala" -> {
                if (actor.row == 1 && picked != null) {
                    buff(picked, 8)
                    actor.power -= 3
                    log("Finnala: -3 Power (self penalty; not damage).")
                }
            }
            "Attal" -> if (picked != null) buff(picked, power(picked))
            "Maru" -> for (unit in all(me)) {
                if (unit !== actor && unit.row == actor.row) buff(unit, 2)
            }
            "Vanok" -> faction(me, "Fallen").forEach { buff(it, 2) }
            "Kabr" -> all(me).filter { it.card.name == "Guerrilla" }.forEach { buff(it, 3) }
            "Bazinet" -> if (actor.row == 0) {
                for (unit in all(foe)) if (powerBonus(unit) > 0) damage(actor, unit, 3)
            }
            "Radegast" -> if (hasFaction(foe, "Fallen")) {
                for (unit in all(me)) if (unit.row == actor.row &&
                    unit.card.name == "Guerrilla") buff(unit, 4)
            }
            "Chinnawat" -> if (actor.row == 1 && picked != null) {
                for (unit in all(me))
                    if (unit === picked || adjacent(unit, picked)) buff(unit, 1)
            }
            "Lady Skorri" -> if (!rowEmpty(foe, 0))
                buff(at(me, actor.row, actor.column - 1), 5)
            "Estela" -> buff(picked, 2 * countRow(foe, 1))
            "Sabikis" -> if (actor.row == 0 && picked != null) {
                val left = at(me, picked.row, picked.column - 1)
                if (left != null) buff(picked, power(left))
            }
            "Buhr" -> if (partnerInColumn(actor)) {
                all(me).filter { it.column == actor.column }.forEach { buff(it, 3) }
            }
            "Vigile" -> if (actor.row == 1) buff(picked, 2)
            "Gladiator" -> if (actor.row == ShadowRules.FRONT_ROW && picked != null) {
                for (unit in all(me)) {
                    if (unit === picked || adjacent(unit, picked)) {
                        unit.damageBonus++
                        log("Gladiator: +1 damage to " + unit.card.name)
                    }
                }
            }
            "Shinka Gladiator" -> if (picked != null && line(actor, 5)) {
                // The card increases damage dealt, not base Power.
                for (unit in all(me)) {
                    if (unit === picked || adjacent(unit, picked)) {
                        unit.damageBonus += 2
                        log("Shinka Gladiator: +2 outgoing damage to " + unit.card.name)
                    }
                }
            }

            "Inquisitor" -> if (actor.row == 0) damage(actor, picked, 4)
            "Tracer Shank" -> if (actor.row == 0) damage(actor, picked, 3)
            "Psion" -> if (actor.row == 0) damage(actor, picked, 2)
            "Vandal" -> damage(actor, picked, 2)
            "Wretch" -> if (actor.row == 1) damage(actor, picked, 1)
            "Incendior" -> damage(actor, picked, 3)
            "Resilient Marauder" -> if (actor.row == 1) damage(actor, picked, 5)
            "High Servitor" -> damage(actor, picked, faction(me, "Fallen").size)
            "Thinktank" -> {
                val best = all(me).maxOfOrNull { powerBonus(it) } ?: 0
                damage(actor, picked, best)
            }
            "Amu-Atu" -> damage(actor, picked, countRow(foe, 0))
            "Shinka Psion" -> if (line(actor, 2)) damage(actor, picked, 3)
            "Shinka Incendior" -> if (line(actor, 3)) damage(actor, picked, 3)
            "Shinka Vigile" -> if (line(actor, 3)) damage(actor, picked, 4)
            "Thagrius" -> if (line(actor, 6)) damage(actor, picked, 2 * lineLength(actor))
            "Ikora" -> if (picked != null) {
                for (unit in all(foe)) if (unit.row == picked.row) damage(actor, unit, 2)
            }
            "Goliath Tank" -> if (picked != null && line(actor, 5)) {
                for (unit in all(foe)) if (unit.row == picked.row) damage(actor, unit, 3)
            }
            "Molkrul" -> if (actor.row == ShadowRules.BACK_ROW &&
                picked != null && power(picked) <= 10) {
                if (rng.nextBoolean()) {
                    log("Molkrul: heads; instantly defeats " + picked.card.name)
                    destroyUnit(picked)
                } else {
                    log("Molkrul: tails; target remains.")
                }
            }
            "Wolf" -> if (picked != null &&
                ShadowSpecialCardRules.wolfCanDestroy(all(me).size, power(picked)))
                destroyUnit(picked)
            "Wu Ming" -> if (actor.row == 0) suppress(picked)
            "Gwynn" -> if (actor.row == 1 && picked != null) {
                for (unit in all(foe))
                    if (unit === picked || adjacent(unit, picked)) damage(actor, unit, 3)
                dealDirect(actor, actor, 2)
            }
            "Xuan Wei" -> if (actor.row == ShadowRules.FRONT_ROW && picked != null) {
                damage(actor, picked, 5)
                if (power(picked) > 0 &&
                    picked.owner.board[picked.row][picked.column] === picked) {
                    if (hasEmptySlot(picked.owner)) {
                        pendingMovedUnit = picked
                        pendingMoveCaster = me
                        log("XUAN WEI: Choose a new position for " + picked.card.name + ".")
                    } else incomplete(actor.card,
                        "No open destination; full-board relocation behavior unverified.")
                }
            }
            "Augmented Shank" -> if (actor.row == 1 && picked != null) {
                for (unit in all(foe))
                    if (unit === picked || adjacent(unit, picked)) damage(actor, unit, 2)
            }
            "Blinder Shank" -> all(me).filter { adjacent(actor, it) }.forEach(::suppress)
            "Kevarrh" -> {
                draw(me, 2)
                log("Kevarrh: draw 2.")
            }
            "Luksis-3" -> {
                // The recorded effect names "Rocks", not "Rock Piles".
                val rocks = all(foe).count { it.card.name == "Rock" }
                buff(actor, rocks * 2)
            }
            "Carrier", "Ketch" -> {
                spawnAt(me, actor.row, actor.column - 1, "Dreg")
                spawnAt(me, actor.row, actor.column + 1, "Dreg")
            }
            "Paradoxical Neophyte" ->
                spawnAt(me, actor.row, actor.column - 1, "Paradoxical Neophyte's Shade")
            "Mu Ying" -> {
                spawnAt(me, actor.row, actor.column - 1, "Guerrilla")
                spawnAt(me, actor.row, actor.column + 1, "Guerrilla")
            }
            "Tomb Ship" -> {
                // Recorded: one random Hive unit on EACH side of the Tomb Ship.
                // The exact hidden game summon pool is not exposed by the clip,
                // so use the verified, constructible Hive units in our catalog.
                val hivePool = catalog.values.filter {
                    it.faction.equals("Hive", true) && it.isPlayable
                }.sortedBy { it.name }
                if (hivePool.isEmpty()) {
                    incomplete(actor.card, "No playable Hive units in the card catalog.")
                } else {
                    for (col in listOf(actor.column - 1, actor.column + 1)) {
                        if (empty(me, actor.row, col)) {
                            val randomHive = hivePool[rng.nextInt(hivePool.size)]
                            spawnAt(me, actor.row, col, randomHive.name)
                        }
                    }
                }
            }
            "Drop Pod" -> if (line(actor, 2)) {
                // The effect requires a free LEFT slot. Do not falsely report
                // a summon if the Line was formed on the actor's right.
                val destination = actor.column - 1
                if (!empty(me, actor.row, destination)) {
                    log("Drop Pod: left slot unavailable; no unit summoned.")
                } else {
                    val candidates = catalog.values.filter {
                        it.isPlayable && it.name != "Drop Pod" &&
                            ShadowPositionRules.hasLineAffix(it.effect)
                    }.sortedBy { it.name }
                    if (candidates.isNotEmpty()) {
                        val randomCard = candidates[rng.nextInt(candidates.size)]
                        spawnAt(me, actor.row, destination, randomCard.name)
                        log("Drop Pod summons Line unit: " + randomCard.name)
                    } else incomplete(actor.card, "No Line-affix summon candidates in catalog.")
                }
            }
            "Ning Fei" -> spawnRandom(foe, 1, "Clone", actor.card)
            "Cursed Thrall", "Exploder Shank" -> transferCursedThrall(actor)
            "Shinka Scorpius", "Scorpius", "Elite Walker", "Pandora-7" -> { }
            else -> if (!noEffect(actor.card) && name !in knownSustained)
                incomplete(actor.card, "No verified " +
                    ShadowEffectRegistry.family(actor.card) + " resolver.")
        }
    }

    fun onBuff(target: ShadowEngine.Unit) = with(e) {
        val own = target.owner
        val foe = other(own)
        for (unit in all(own)) {
            if (unit.suppressed) continue
            val name = unit.card.name
            if (unit === target && name in setOf("Ogre", "Tenacious Ogre", "Shrieker")) {
                if (name != "Shrieker" || unit.row == ShadowRules.BACK_ROW) {
                    val targets = all(foe)
                    Collections.shuffle(targets, rng)
                    val count = when (name) {
                        "Tenacious Ogre" -> 4
                        "Ogre" -> 2
                        else -> 1
                    }
                    val amount = if (name == "Shrieker") 2 else 1
                    for (victim in targets.take(count)) damage(unit, victim, amount)
                }
            }
            if (unit === target && name == "The Disfavored" && unit.row == 1) {
                val hive = faction(own, "Hive")
                hive.remove(target)
                if (hive.isNotEmpty()) buff(hive[rng.nextInt(hive.size)], 2)
            }
            if (unit === target && name == "The Speaker" && unit.row == 0)
                buff(at(own, unit.row, unit.column + 1), 2)
            if (unit === target && name == "Guerrilla Captain")
                randomUnit(foe)?.let { damage(unit, it, 2) }
            if (unit === target && name == "Denzin")
                spawnAt(own, unit.row, unit.column - 1, "Guerrilla")
            if (unit === target && name == "Xur")
                spawnRandom(foe, ShadowRules.FRONT_ROW, "Rock", unit.card)
        }
    }

    /**
     * Grenade Dreg: "Every unit deployed in this row grants this unit +1 Power."
     * Applies to direct plays and token deployments on the owner's field.
     * The card does not exclude itself, so its own initial deployment counts.
     * Repeated placements grant separate +1 buffs and suppressed Grenade Dregs
     * cannot apply their sustained ability.
     */
    fun fireAllyPlacement(newUnit: ShadowEngine.Unit) = with(e) {
        val own = newUnit.owner
        if (own.board[newUnit.row][newUnit.column] !== newUnit) return@with
        if (newUnit.card.name == "Dreg") {
            val unrelenting = all(own).count {
                it.card.name == "Unrelenting Captain" && !it.suppressed
            }
            if (unrelenting > 0) buff(newUnit, unrelenting)
        }
        for (watcher in all(own)) {
            if (watcher.card.name != "Grenade Dreg" || watcher.suppressed ||
                watcher.row != newUnit.row || own.board[watcher.row][watcher.column] !== watcher)
                continue
            buff(watcher, 1)
            log("Grenade Dreg +1 Power: " + newUnit.card.name + " deployed in its row.")
        }
    }

    /** Resilient Captain: allied Fallen deal damage, summon at most six Dregs. */
    fun fireFallenDamage(source: ShadowEngine.Unit) = with(e) {
        if (!source.card.faction.equals("Fallen", true)) return@with
        if (source.owner.board[source.row][source.column] !== source) return@with
        for (captain in all(source.owner)) {
            if (captain.card.name != "Resilient Captain" || captain.suppressed ||
                captain.resilientCaptainDregs >= 6) continue
            val col = captain.column + 1
            if (!empty(source.owner, captain.row, col)) continue
            spawnAt(source.owner, captain.row, col, "Dreg")
            if (at(source.owner, captain.row, col)?.card?.name == "Dreg") {
                captain.resilientCaptainDregs++
                log("Resilient Captain summons Dreg " + captain.resilientCaptainDregs + "/6.")
            }
        }
    }

    /** Trigger effects run when the opposing side receives a new unit. */
    fun fireEnemyPlacement(newUnit: ShadowEngine.Unit) = with(e) {
        val enemy = other(newUnit.owner)
        for (watcher in all(enemy)) {
            // An earlier triggered effect may remove the newly arrived unit;
            // later watchers must not react to a nonexistent target.
            if (!isPresent(newUnit)) break
            if (watcher.suppressed || !isPresent(watcher)) continue
            if (newUnit.row == 1 && watcher.card.name == "Ayuela" && watcher.row == 0) {
                newUnit.scorched = 2
                newUnit.scorchTurns = 2
                newUnit.scorchSource = watcher
                log("SCORCHED ▶ " + newUnit.card.name + " for 2 turns.")
            }
            if (newUnit.row == ShadowRules.FRONT_ROW &&
                watcher.card.name == "Zhiyao") {
                // Route every Power bonus through the shared resolver so
                // Tan-2 doubling and own-buff triggers also work for Zhiyao.
                val beforeBonus = watcher.bonus
                buff(watcher, 1)
                val granted = (watcher.bonus - beforeBonus).coerceAtLeast(0)
                if (granted > 0) {
                    watcher.timedBonuses.add(ShadowEngine.TimedBonus(granted, 3))
                    log("Zhiyao reacts +" + granted + " Power for 3 turns.")
                }
            }
            if (newUnit.row == 0 &&
                watcher.card.name == "Resilient Grenade Dreg" && watcher.row == 0) {
                newUnit.scorched = 2
                newUnit.scorchTurns = 2
                newUnit.scorchSource = watcher
                log("SCORCHED ▶ " + newUnit.card.name +
                    " for 2 turns (Resilient Grenade Dreg).")
            }
        }
    }

    fun endSide(side: ShadowEngine.Player) = with(e) {
        for (unit in all(side)) {
            if (side.board[unit.row][unit.column] !== unit) continue
            if (unit.inspired && !unit.suppressed) buff(unit, 1)
            // Inspired can trigger retaliation and immediately destroy the
            // inspired unit. Do not resolve the rest of its stale snapshot.
            if (!isPresent(unit)) continue
            if (unit.scorchTurns > 0) {
                unit.scorchTurns--
                // Scorched is enemy-origin damage. Using the victim as the
                // source wrongly fires that victim's "Fallen deals damage"
                // effects and credits an opponent's ongoing effect to them.
                dealDirect(unit.scorchSource ?: unit, unit, unit.scorched)
                sweep()
                if (!isPresent(unit)) continue
                if (unit.scorchTurns == 0) {
                    unit.scorched = 0
                    unit.scorchSource = null
                }
            }
            if (unit.card.name == "Elite Sniper Vandal" && unit.sniperTurnsRemaining > 0)
                unit.sniperTurnsRemaining--
            for (i in unit.timedBonuses.indices.reversed()) {
                val active = unit.timedBonuses[i]
                active.turns--
                if (active.turns <= 0) {
                    unit.power -= active.value
                    unit.bonus = max(0, unit.bonus - active.value)
                    unit.timedBonuses.removeAt(i)
                    log(unit.card.name + " temporary bonus expired.")
                }
            }
        }
        // Scorched and expiring bonuses can reduce a unit to zero before
        // end-turn Sustained effects resolve. Remove it (and handle death
        // splashes) first so a destroyed source cannot still activate.
        sweep()
        for (unit in all(side)) {
            if (side.board[unit.row][unit.column] !== unit || unit.suppressed) continue
            when (unit.card.name) {
                "Scorpius" -> randomUnit(other(side))?.let { damage(unit, it, 1) }
                "Shinka Scorpius" -> if (line(unit, 3))
                    randomUnit(other(side))?.let { damage(unit, it, 2) }
                "Elite Walker" -> {
                    if (unit.row != ShadowRules.BACK_ROW) continue
                    val targets = all(other(side))
                    Collections.shuffle(targets, rng)
                    for (victim in targets.take(2))
                        if (victim.owner.board[victim.row][victim.column] === victim)
                            damage(unit, victim, 2)
                }
                "Pandora-7" -> buff(at(side, unit.row, unit.column + 1), 2)
                "Helhest" -> if (unit.row == ShadowRules.BACK_ROW) {
                    val chosen = randomUnit(other(side))
                    if (chosen != null) {
                        val targets = listOfNotNull(
                            at(chosen.owner, chosen.row, chosen.column - 1),
                            chosen,
                            at(chosen.owner, chosen.row, chosen.column + 1)
                        )
                        var totalDamage = 0
                        for (victim in targets) {
                            if (!isPresent(victim)) continue
                            // Count the actual hit, not target Power delta:
                            // Saladin and similar counter-buffs change that delta,
                            // and overkill still represents the full damage hit.
                            totalDamage += resolvedDamage(unit, victim, 1)
                        }
                        // Original-game reports support 1 Power per point of
                        // damage dealt, including overkill (but not blocked hits).
                        if (totalDamage > 0) buff(unit, totalDamage)
                        log("Helhest: dealt " + totalDamage +
                            " damage and gained that much Power.")
                    }
                }
            }
            // Complete each source's damage/death effects before the next
            // end-turn source. A dead source must never fire from the snapshot.
            sweep()
        }
        sweep()
    }
}
