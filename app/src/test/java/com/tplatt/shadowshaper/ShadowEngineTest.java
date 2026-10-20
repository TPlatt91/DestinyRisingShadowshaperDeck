package com.tplatt.shadowshaper;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.LinkedHashMap;

public class ShadowEngineTest {
    private final LinkedHashMap<String,ShadowData.Card> catalog=new LinkedHashMap<>();
    private final LinkedHashMap<String,Integer> deck=new LinkedHashMap<>();

    private ShadowData.Card card(String name,int cost,int power,String effect){
        ShadowData.Card c=new ShadowData.Card();c.name=name;c.cost=String.valueOf(cost);
        c.power=String.valueOf(power);c.effect=effect;c.rarity="Rare";c.faction="Humans";
        catalog.put(name,c);return c;
    }
    private void createLegalTestDeck(){
        for(int i=0;i<15;i++){
            String name="Test Unit "+i;
            card(name,1,2,"No special effects.");
            deck.put(name,1);
        }
    }
    private ShadowEngine firstHumanEngine(){
        for(long seed=0;seed<300;seed++){
            ShadowEngine engine=new ShadowEngine(catalog,seed);
            engine.start(deck,deck);
            if(engine.humanFirst)return engine;
        }
        throw new AssertionError("Seed search failed");
    }
    @Test public void sixRoundPowerScoringEndsInDrawWithoutPlays(){
        createLegalTestDeck();
        ShadowEngine engine=firstHumanEngine();
        assertEquals(1,engine.round);
        assertEquals(1,engine.human.points);
        assertEquals(5,engine.human.hand.size());
        assertEquals(4,engine.cpu.hand.size());
        for(int i=0;i<11;i++){
            assertFalse(engine.finished);
            engine.endTurn();
        }
        assertEquals(6,engine.round);
        assertFalse(engine.finished);
        engine.endTurn();
        assertTrue(engine.finished);
        assertEquals("DRAW",engine.result());
    }
    @Test public void placingCardConsumesPlayPointAndAddsPower(){
        createLegalTestDeck();
        ShadowEngine engine=firstHumanEngine();
        int handBefore=engine.human.hand.size();
        assertTrue(engine.play(engine.human,0,1,0,null));
        assertEquals(handBefore-1,engine.human.hand.size());
        assertEquals(0,engine.human.points);
        assertEquals(2,engine.score(engine.human));
        assertFalse(engine.play(engine.human,0,1,1,null));
    }
    @Test public void inspiredGainsPowerAtTurnEnd(){
        createLegalTestDeck();
        ShadowData.Card hex=card("Hexguard",1,1,"When Played: Gains Inspired.");
        hex.faction="Hive";hex.rarity="Legendary";
        ShadowEngine engine=firstHumanEngine();
        engine.human.hand.clear();engine.human.hand.add(hex);
        assertTrue(engine.play(engine.human,0,1,1,null));
        assertEquals(1,engine.score(engine.human));
        engine.endTurn();
        assertEquals(2,engine.score(engine.human));
    }
    @Test public void targetedBuffUsesChosenFriendlyUnit(){
        createLegalTestDeck();
        ShadowData.Card buffer=card("Morgran",3,6,"Grants +1 Power to a unit.");
        ShadowEngine engine=firstHumanEngine();
        engine.human.points=3;
        ShadowData.Card base=catalog.get("Test Unit 1");
        engine.human.board[0][0]=new ShadowEngine.Unit(engine.human,base,0,0);
        engine.human.hand.clear();engine.human.hand.add(buffer);
        ShadowEngine.Target ally=new ShadowEngine.Target(engine.human,0,0);
        assertTrue(engine.play(engine.human,0,0,1,ally));
        assertEquals(3,engine.power(engine.human.board[0][0]));
        assertEquals(9,engine.score(engine.human)); // 3-power ally + 6-power Morgran
    }
}
