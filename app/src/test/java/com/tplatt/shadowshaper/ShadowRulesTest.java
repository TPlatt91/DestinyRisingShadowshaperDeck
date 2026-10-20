package com.tplatt.shadowshaper;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.LinkedHashMap;

/** Core rules regression tests. Run locally, not in GitHub Actions. */
public class ShadowRulesTest {
    @Test public void deckAndRoundNumbersAreCentralized(){
        assertEquals(15,ShadowRules.DECK_SIZE);
        assertEquals(2,ShadowRules.COPY_LIMIT);
        assertEquals(3,ShadowRules.EXOTIC_LIMIT);
        assertEquals(5,ShadowRules.MYTHIC_LIMIT);
        assertEquals(6,ShadowRules.ROUNDS);
        assertEquals(5,ShadowRules.openingHand(true));
        assertEquals(4,ShadowRules.openingHand(false));
        assertEquals(1,ShadowRules.lightForRound(1));
        assertEquals(6,ShadowRules.lightForRound(6));
        assertTrue(ShadowRules.isOnBoard(0,0));
        assertTrue(ShadowRules.isOnBoard(1,5));
        assertFalse(ShadowRules.isOnBoard(-1,0));
        assertFalse(ShadowRules.isOnBoard(0,6));
        assertEquals("DRAW",ShadowRules.result(8,8));
        assertEquals("VICTORY",ShadowRules.result(9,8));
        assertEquals("DEFEAT",ShadowRules.result(7,8));
    }
    private LinkedHashMap<String,ShadowData.Card> catalog(){
        LinkedHashMap<String,ShadowData.Card> set=new LinkedHashMap<>();
        for(int i=0;i<15;i++){
            ShadowData.Card card=new ShadowData.Card();
            card.name="Test Unit "+i;card.rarity="Rare";card.faction="Humans";
            card.cost="1";card.power="2";card.effect="No special effects.";
            set.put(card.name,card);
        }
        ShadowData.Card morgran=new ShadowData.Card();
        morgran.name="Morgran";morgran.rarity="Legendary";morgran.faction="Humans";
        morgran.cost="3";morgran.power="6";morgran.effect="Grants +1 Power to a unit.";
        set.put(morgran.name,morgran);
        return set;
    }
    private LinkedHashMap<String,Integer> deck(){
        LinkedHashMap<String,Integer> chosen=new LinkedHashMap<>();
        for(int i=0;i<15;i++)chosen.put("Test Unit "+i,1);
        return chosen;
    }
    private ShadowEngine humanFirst(){
        LinkedHashMap<String,ShadowData.Card> cards=catalog();
        for(long seed=0;seed<600;seed++){
            ShadowEngine game=new ShadowEngine(cards,seed);
            game.start(deck(),deck());
            if(game.humanFirst)return game;
        }
        throw new AssertionError("Unable to find offensive test seed");
    }
    @Test public void rejectedOutOfBoundsPlacementIsAtomic(){
        ShadowEngine game=humanFirst();
        int oldLight=game.human.points,oldHand=game.human.hand.size();
        assertFalse(game.play(game.human,0,1,6,null));
        assertEquals("Invalid battlefield position.",game.lastActionError);
        assertEquals(oldLight,game.human.points);
        assertEquals(oldHand,game.human.hand.size());
        assertEquals(0,game.score(game.human));
    }
    @Test public void cannotPlayOnOtherSideTurn(){
        ShadowEngine game=humanFirst();
        assertFalse(game.play(game.cpu,0,1,0,null));
        assertEquals("It is not this player's turn.",game.lastActionError);
        assertEquals(1,game.cpu.points);
        assertNull(game.cpu.board[1][0]);
    }
    @Test public void paymentAndOccupiedSlotsAreValidated(){
        ShadowEngine game=humanFirst();
        assertTrue(game.play(game.human,0,1,0,null));
        assertEquals(0,game.human.points);
        int hand=game.human.hand.size();
        assertFalse(game.play(game.human,0,1,1,null));
        assertEquals("Insufficient Light.",game.lastActionError);
        assertEquals(hand,game.human.hand.size());
        game.human.points=1;
        assertFalse(game.play(game.human,0,1,0,null));
        assertEquals("Position is already occupied.",game.lastActionError);
        assertEquals(hand,game.human.hand.size());
        assertEquals(1,game.human.points);
    }
    @Test public void targetedPlaysRequireLegalSelectionsAndDoNotChargeForErrors(){
        ShadowEngine game=humanFirst();
        game.human.points=3;
        game.human.hand.clear();
        game.human.hand.add(game.catalog.get("Morgran"));
        // A friendly unit is a legal buff target.
        game.human.board[0][0]=new ShadowEngine.Unit(game.human,game.catalog.get("Test Unit 0"),0,0);
        // And the enemy unit is alive but illegal for Morgran.
        game.cpu.board[0][0]=new ShadowEngine.Unit(game.cpu,game.catalog.get("Test Unit 1"),0,0);
        int light=game.human.points;
        assertFalse(game.play(game.human,0,1,1,null));
        assertTrue(game.lastActionError.contains("Choose a target"));
        assertFalse(game.play(game.human,0,1,1,new ShadowEngine.Target(game.cpu,0,0)));
        assertTrue(game.lastActionError.contains("not a legal target"));
        assertEquals(light,game.human.points);
        assertEquals(1,game.human.hand.size());
        assertNull(game.human.board[1][1]);
        assertTrue(game.play(game.human,0,1,1,new ShadowEngine.Target(game.human,0,0)));
        assertEquals(0,game.human.points);
        assertEquals(3,game.power(game.human.board[0][0]));
    }
    @Test public void firstPlayerMovesFirstEachRoundAndUnusedLightDoesNotCarry(){
        ShadowEngine game=humanFirst();
        game.human.points=0;
        game.endTurn();
        assertFalse(game.humanTurn);
        game.endTurn();
        assertEquals(2,game.round);
        assertTrue(game.humanTurn);
        assertEquals(2,game.human.points);
        assertEquals(2,game.cpu.points);
        game.human.points=1;
        game.endTurn();game.endTurn();
        assertEquals(3,game.round);
        assertEquals(3,game.human.points);
    }

    @Test public void projectedFactionLinesRequireAdjacentSameFactionUnits(){
        ShadowEngine game=humanFirst();
        ShadowData.Card cabal=new ShadowData.Card();
        cabal.name="Shinka Psion";cabal.cost="2";cabal.power="3";
        cabal.faction="Cabal";cabal.rarity="Legendary";
        cabal.effect="When Played (2-Unit Line): Deals 3 damage to a unit.";
        game.catalog.put(cabal.name,cabal);
        ShadowData.Card ally=new ShadowData.Card();
        ally.name="Cabal Ally";ally.faction="Cabal";ally.cost="1";ally.power="2";
        ally.rarity="Rare";ally.effect="No special effects.";
        game.catalog.put(ally.name,ally);
        game.human.board[0][0]=new ShadowEngine.Unit(game.human,ally,0,0);
        assertEquals(2,game.projectedFactionLine(game.human,cabal,0,1));
        assertEquals(1,game.projectedFactionLine(game.human,cabal,0,3));
        // Another faction interrupts what would otherwise look like a line.
        game.human.board[0][2]=new ShadowEngine.Unit(game.human,game.catalog.get("Test Unit 0"),0,2);
        assertEquals(1,game.projectedFactionLine(game.human,cabal,0,3));
        assertEquals(0,game.projectedFactionLine(game.human,cabal,0,2));
    }
    @Test public void noIllegalTargetPromptWithoutRequiredFactionLine(){
        ShadowEngine game=humanFirst();
        ShadowData.Card cabal=new ShadowData.Card();
        cabal.name="Shinka Psion";cabal.cost="2";cabal.power="3";
        cabal.faction="Cabal";cabal.rarity="Legendary";
        cabal.effect="When Played (2-Unit Line): Deals 3 damage to a unit.";
        game.catalog.put(cabal.name,cabal);
        game.cpu.board[0][0]=new ShadowEngine.Unit(game.cpu,game.catalog.get("Test Unit 0"),0,0);
        assertTrue(game.targetOptions(game.human,cabal,1,3).isEmpty());
        ShadowData.Card partner=new ShadowData.Card();
        partner.name="Cabal";partner.cost="1";partner.power="1";partner.faction="Cabal";
        game.human.board[1][2]=new ShadowEngine.Unit(game.human,partner,1,2);
        assertEquals(1,game.targetOptions(game.human,cabal,1,3).size());
    }

    @Test public void constructedDeckEnforcesSizeAndCopyLimit(){
        LinkedHashMap<String,ShadowData.Card> cards=catalog();
        LinkedHashMap<String,Integer> chosen=deck();
        assertEquals("",ShadowData.validate(chosen,cards));
        chosen.remove("Test Unit 14");
        assertTrue(ShadowData.validate(chosen,cards).contains("exactly 15"));
        chosen.put("Test Unit 0",3);
        assertTrue(ShadowData.validate(chosen,cards).contains("Maximum two copies"));
        chosen.put("Test Unit 0",2);
        assertEquals("",ShadowData.validate(chosen,cards));
    }
    @Test public void constructedDeckEnforcesExoticAndMythicCaps(){
        LinkedHashMap<String,ShadowData.Card> cards=catalog();
        LinkedHashMap<String,Integer> chosen=deck();
        for(int i=0;i<4;i++)cards.get("Test Unit "+i).rarity="Exotic";
        assertTrue(ShadowData.validate(chosen,cards).contains("No more than 3 Exotic"));
        cards.get("Test Unit 3").rarity="Rare";
        assertEquals("",ShadowData.validate(chosen,cards));
        for(int i=4;i<10;i++)cards.get("Test Unit "+i).rarity="Mythic";
        assertTrue(ShadowData.validate(chosen,cards).contains("No more than 5 Mythic"));
    }
    @Test public void summonedOnlyCardsCannotBeAddedToLegalDeck(){
        LinkedHashMap<String,ShadowData.Card> cards=catalog();
        LinkedHashMap<String,Integer> chosen=deck();
        ShadowData.Card summoned=cards.get("Test Unit 0");
        summoned.classification="Summoned unit";
        assertFalse(ShadowData.validate(chosen,cards).isEmpty());
        summoned.classification="";
        summoned.cost="";
        assertFalse(ShadowData.validate(chosen,cards).isEmpty());
    }

    @Test public void scorpiusDealsOneToTwoDifferentOpponentsFromBackRow(){
        ShadowEngine game=humanFirst();
        ShadowData.Card scorpius=new ShadowData.Card();
        scorpius.name="Scorpius";scorpius.faction="Cabal";scorpius.rarity="Legendary";
        scorpius.cost="3";scorpius.power="4";
        scorpius.effect="Sustained (Back Row): At the end of your turn, deals 1 damage each to 2 random opposing units.";
        ShadowData.Card target=game.catalog.get("Test Unit 0");
        game.human.board[0][0]=new ShadowEngine.Unit(game.human,scorpius,0,0);
        game.cpu.board[0][0]=new ShadowEngine.Unit(game.cpu,target,0,0);
        game.cpu.board[1][0]=new ShadowEngine.Unit(game.cpu,target,1,0);
        assertEquals(4,game.score(game.cpu));
        game.endTurn();
        assertEquals(2,game.score(game.cpu));
        assertEquals(1,game.cpu.board[0][0].power);
        assertEquals(1,game.cpu.board[1][0].power);
    }
    @Test public void scorpiusDoesNotTriggerFromFrontRow(){
        ShadowEngine game=humanFirst();
        ShadowData.Card scorpius=new ShadowData.Card();
        scorpius.name="Scorpius";scorpius.faction="Cabal";scorpius.rarity="Legendary";
        scorpius.cost="3";scorpius.power="4";
        scorpius.effect="Sustained (Back Row): At end of turn, damages two opposing units.";
        ShadowData.Card target=game.catalog.get("Test Unit 0");
        game.human.board[1][0]=new ShadowEngine.Unit(game.human,scorpius,1,0);
        game.cpu.board[0][0]=new ShadowEngine.Unit(game.cpu,target,0,0);
        game.cpu.board[1][0]=new ShadowEngine.Unit(game.cpu,target,1,0);
        game.endTurn();
        assertEquals(4,game.score(game.cpu));
    }

    @Test public void cpuCanPlayOneCardAtATimeAndWaitBetweenVisibleActions(){
        LinkedHashMap<String,ShadowData.Card> cards=catalog();
        ShadowEngine game=null;
        for(long seed=0;seed<600;seed++){
            ShadowEngine candidate=new ShadowEngine(cards,seed);
            candidate.start(deck(),deck());
            if(!candidate.humanFirst){game=candidate;break;}
        }
        assertNotNull(game);
        assertFalse(game.humanTurn);
        int originalHand=game.cpu.hand.size();
        assertTrue(game.cpuPlayOne());
        assertFalse(game.humanTurn); // The UI gets to render before next AI action.
        assertEquals(originalHand-1,game.cpu.hand.size());
        assertEquals(0,game.cpu.points);
        assertEquals(2,game.score(game.cpu));
        assertFalse(game.cpuPlayOne());
        assertFalse(game.humanTurn);
        game.endTurn();
        assertTrue(game.humanTurn);
    }
    @Test public void diagnosticReportIncludesSeedRoundAndBattlefield(){
        ShadowEngine game=humanFirst();
        String report=game.diagnosticReport();
        assertTrue(report.contains("Seed: "));
        assertTrue(report.contains("Round: 1/6"));
        assertTrue(report.contains("PLAYER: Deck "));
        assertTrue(report.contains("CPU: Deck "));
        assertTrue(report.contains("EVENTS"));
    }

}
