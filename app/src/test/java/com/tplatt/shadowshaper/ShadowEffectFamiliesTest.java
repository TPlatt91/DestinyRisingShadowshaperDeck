package com.tplatt.shadowshaper;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.LinkedHashMap;

/** Regression tests for actual reusable effect families (run locally only). */
public class ShadowEffectFamiliesTest {
    private LinkedHashMap<String,ShadowData.Card> cards;
    private LinkedHashMap<String,Integer> selected;
    private ShadowData.Card make(String name,String faction,int cost,int power,String text){
        ShadowData.Card c=new ShadowData.Card();
        c.name=name;c.faction=faction;c.cost=String.valueOf(cost);c.power=String.valueOf(power);
        c.effect=text;c.rarity="Legendary";cards.put(name,c);return c;
    }
    private ShadowEngine game(){
        cards=new LinkedHashMap<>();selected=new LinkedHashMap<>();
        for(int i=0;i<15;i++){
            ShadowData.Card c=make("Filler "+i,"Humans",1,3,"No special effects.");
            c.rarity="Rare";selected.put(c.name,1);
        }
        for(long seed=0;seed<800;seed++){
            ShadowEngine g=new ShadowEngine(cards,seed);
            g.start(selected,selected);
            if(g.humanFirst)return g;
        }
        throw new AssertionError("Couldn't resolve offensive test seed");
    }
    @Test public void tan2DoublesExactlyOneBuffWithoutSuppressingItself(){
        ShadowEngine g=game();
        ShadowData.Card tan=make("Tan-2","Humans",2,2,"Doubles your next Power bonus.");
        ShadowData.Card mor=make("Morgran","Humans",3,6,"Grants +1 Power to a unit.");
        ShadowData.Card targetCard=cards.get("Filler 0");
        ShadowEngine.Unit tanUnit=new ShadowEngine.Unit(g.human,tan,0,0);
        ShadowEngine.Unit targetUnit=new ShadowEngine.Unit(g.human,targetCard,0,1);
        g.human.board[0][0]=tanUnit;g.human.board[0][1]=targetUnit;
        g.human.hand.clear();g.human.hand.add(mor);g.human.hand.add(mor);
        g.human.points=6;
        assertTrue(g.play(g.human,0,0,2,new ShadowEngine.Target(g.human,0,1)));
        assertEquals(5,g.power(targetUnit)); // 3 starting + doubled 1
        assertFalse(tanUnit.suppressed);
        assertTrue(tanUnit.bonusDoublerConsumed);
        assertTrue(g.play(g.human,0,1,2,new ShadowEngine.Target(g.human,0,1)));
        assertEquals(6,g.power(targetUnit)); // second buff is only +1
        assertFalse(tanUnit.suppressed);
    }
    @Test public void unrelentingCaptainSummonsDregWhenCaptainItselfGetsBonus(){
        ShadowEngine g=game();
        ShadowData.Card captain=make("Unrelenting Captain","Fallen",1,1,
            "When acquiring a Power bonus, summons a Fallen Dreg on the left.");
        ShadowData.Card dreg=make("Dreg","Fallen",1,1,"No special effects.");
        ShadowData.Card mor=make("Morgran","Humans",3,6,"Grants +1 Power to a unit.");
        ShadowEngine.Unit captainUnit=new ShadowEngine.Unit(g.human,captain,0,2);
        g.human.board[0][2]=captainUnit;
        g.human.hand.clear();g.human.hand.add(mor);g.human.points=3;
        assertTrue(g.play(g.human,0,1,3,new ShadowEngine.Target(g.human,0,2)));
        assertEquals(2,g.power(captainUnit));
        assertNotNull(g.human.board[0][1]);
        assertEquals("Dreg",g.human.board[0][1].card.name);
        assertEquals(1,g.power(g.human.board[0][1]));
    }
    @Test public void cursedThrallTransfersAndDetonatesOnOriginalPlayersNextTurn(){
        ShadowEngine g=game();
        ShadowData.Card thrall=make("Cursed Thrall","Hive",1,1,
            "When Played: Moves to opponent front row and dies next turn, splashing adjacent cards.");
        ShadowData.Card baseline=cards.get("Filler 0");
        for(int col=0;col<ShadowRules.COLUMNS;col++){
            if(col==2)continue;
            ShadowEngine.Unit u=new ShadowEngine.Unit(g.cpu,baseline,1,col);
            u.power=4;g.cpu.board[1][col]=u;
        }
        g.human.hand.clear();g.human.hand.add(thrall);g.human.points=1;
        assertTrue(g.play(g.human,0,0,0,null));
        assertNull(g.human.board[0][0]);
        assertNotNull(g.cpu.board[1][2]);
        assertEquals("Cursed Thrall",g.cpu.board[1][2].card.name);
        g.endTurn(); // end original owner's first turn, bomb stays
        assertNotNull(g.cpu.board[1][2]);
        g.endTurn(); // opponent ends round 1; next owner turn starts in round 2
        assertEquals(2,g.round);
        assertNull(g.cpu.board[1][2]);
        assertEquals(2,g.cpu.board[1][1].power);
        assertEquals(2,g.cpu.board[1][3].power);
    }
    @Test public void servitorDamageBoostAppliesWithoutAffectingBasePower(){
        ShadowEngine g=game();
        ShadowData.Card serv=make("Servitor","Fallen",2,2,"Sustained: Your units deal 1 extra damage.");
        ShadowData.Card vandal=make("Vandal","Fallen",2,2,"Deals 2 damage to a front-row enemy.");
        ShadowData.Card victim=cards.get("Filler 0");
        ShadowEngine.Unit support=new ShadowEngine.Unit(g.human,serv,0,0);
        g.human.board[0][0]=support;
        g.cpu.board[1][0]=new ShadowEngine.Unit(g.cpu,victim,1,0);
        g.cpu.board[1][0].power=5;
        g.human.hand.clear();g.human.hand.add(vandal);g.human.points=2;
        assertTrue(g.play(g.human,0,1,2,new ShadowEngine.Target(g.cpu,1,0)));
        assertEquals(2,g.cpu.board[1][0].power); // 2 baseline damage plus Servitor +1
        assertEquals(2,g.power(support));
    }
    @Test public void jolderDoublesOnlyFirstDamageWithoutSelfSuppression(){
        ShadowEngine g=game();
        ShadowData.Card jolder=make("Jolder","Humans",3,3,"Next damage is doubled.");
        ShadowData.Card vandal=make("Vandal","Fallen",2,2,"Deal 2 damage to enemy front.");
        ShadowData.Card enemy=cards.get("Filler 0");
        ShadowEngine.Unit doubler=new ShadowEngine.Unit(g.human,jolder,0,0);
        g.human.board[0][0]=doubler;
        g.cpu.board[1][0]=new ShadowEngine.Unit(g.cpu,enemy,1,0);
        g.cpu.board[1][0].power=10;
        g.human.hand.clear();g.human.hand.add(vandal);g.human.hand.add(vandal);
        g.human.points=4;
        assertTrue(g.play(g.human,0,1,2,new ShadowEngine.Target(g.cpu,1,0)));
        assertEquals(6,g.cpu.board[1][0].power); // 2 damage doubled to 4
        assertTrue(doubler.damageDoublerConsumed);
        assertFalse(doubler.suppressed);
        assertTrue(g.play(g.human,0,1,3,new ShadowEngine.Target(g.cpu,1,0)));
        assertEquals(4,g.cpu.board[1][0].power); // only 2 this time
    }
    @Test public void eliteWalkerDamagesTwoDifferentTargets(){
        ShadowEngine g=game();
        ShadowData.Card walker=make("Elite Walker","Fallen",4,3,
            "When your turn ends, deals 2 damage to 2 random opposing units.");
        ShadowData.Card basic=cards.get("Filler 0");
        g.human.board[0][0]=new ShadowEngine.Unit(g.human,walker,0,0);
        g.cpu.board[1][0]=new ShadowEngine.Unit(g.cpu,basic,1,0);
        g.cpu.board[1][0].power=6;
        g.cpu.board[1][1]=new ShadowEngine.Unit(g.cpu,basic,1,1);
        g.cpu.board[1][1].power=6;
        g.endTurn();
        assertEquals(4,g.cpu.board[1][0].power);
        assertEquals(4,g.cpu.board[1][1].power);
    }
    @Test public void resilientGrenadeDregScorchesTwiceWithoutTripleDamage(){
        ShadowEngine g=game();
        ShadowData.Card watcher=make("Resilient Grenade Dreg","Fallen",3,7,
            "Sustained (Back Row): When opponent places a unit in back row deals 2 damage for 2 turns.");
        g.cpu.board[0][0]=new ShadowEngine.Unit(g.cpu,watcher,0,0);
        ShadowData.Card newcomer=cards.get("Filler 0");
        g.human.hand.clear();g.human.hand.add(newcomer);g.human.points=1;
        assertTrue(g.play(g.human,0,0,1,null));
        ShadowEngine.Unit affected=g.human.board[0][1];
        assertNotNull(affected);
        assertEquals(3,g.power(affected)); // no extra immediate damage
        g.endTurn(); // human's end; first scorch tick
        assertEquals(1,g.power(affected));
        assertEquals(1,affected.scorchTurns);
    }

}
