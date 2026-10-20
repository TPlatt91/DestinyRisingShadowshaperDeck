package com.tplatt.shadowshaper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Local, deterministic turn/position/scoring engine for Shadowshaper Duels.
 * Card interactions live here, not in Android views. Unknown effects are
 * explicitly logged as unimplemented; they must not be counted as resolved.
 */
public final class ShadowEngine {
    public static final int COLUMNS=6, ROUNDS=6;
    public final Map<String,ShadowData.Card> catalog;
    public final Player human=new Player(true), cpu=new Player(false);
    public final ArrayList<String> journal=new ArrayList<>();
    public final Random rng;
    public boolean humanFirst, humanTurn, finished;
    public int round=1, completedTurns=0, unresolvedEffects=0;
    public int lastPlacedSide=-1,lastPlacedRow=-1,lastPlacedColumn=-1;

    public static final class Unit {
        public final ShadowData.Card card;
        public final Player owner;
        public int row,column,power,bonus,scorched,scorchTurns;
        public final ArrayList<TimedBonus> timedBonuses=new ArrayList<>();
        public boolean inspired,suppressed;
        public Unit(Player owner,ShadowData.Card card,int row,int col) {
            this.owner=owner;this.card=card;this.row=row;this.column=col;
            this.power=Math.max(0,card.numericPower());
        }
    }
    private static final class TimedBonus {
        int turns,value;
        TimedBonus(int value,int turns){this.value=value;this.turns=turns;}
    }
    public static final class Player {
        public final boolean human;
        public final ArrayList<ShadowData.Card> drawPile=new ArrayList<>(),hand=new ArrayList<>();
        public final Unit[][] board=new Unit[2][COLUMNS];
        public int points;
        Player(boolean human){this.human=human;}
    }
    public static final class Target {
        public final Player owner;
        public final int row,column;
        public Target(Player owner,int row,int column){this.owner=owner;this.row=row;this.column=column;}
        public Unit unit(){return owner.board[row][column];}
    }
    public ShadowEngine(Map<String,ShadowData.Card> catalog,long seed){
        this.catalog=catalog;this.rng=new Random(seed);
    }
    private void log(String message){
        journal.add(message);
        if(journal.size()>90)journal.remove(0);
    }
    private void incomplete(ShadowData.Card card,String reason){
        unresolvedEffects++;
        log("UNIMPLEMENTED: "+card.name+" — "+reason);
    }
    private static boolean noEffect(ShadowData.Card card){
        String value=card.effect.toLowerCase(Locale.ROOT);
        return value.trim().isEmpty()||value.contains("no special effects");
    }
    private ShadowData.Card card(String name){return catalog.get(name);}
    public void start(Map<String,Integer> playerDeck,Map<String,Integer> cpuDeck){
        String error=ShadowData.validate(playerDeck,catalog);
        if(!error.isEmpty())throw new IllegalArgumentException(error);
        error=ShadowData.validate(cpuDeck,catalog);
        if(!error.isEmpty())throw new IllegalArgumentException("CPU: "+error);
        clear(human);clear(cpu);journal.clear();unresolvedEffects=0;
        human.drawPile.addAll(ShadowData.expanded(playerDeck,catalog));
        cpu.drawPile.addAll(ShadowData.expanded(cpuDeck,catalog));
        Collections.shuffle(human.drawPile,rng);Collections.shuffle(cpu.drawPile,rng);
        humanFirst=rng.nextBoolean();humanTurn=humanFirst;finished=false;
        round=1;completedTurns=0;
        draw(human,humanFirst?5:4);
        draw(cpu,humanFirst?4:5);
        human.points=1;cpu.points=1;
        log(humanFirst?"Offensive: you act first.":"Defensive: CPU acts first.");
        log("Round 1 — 1 play point.");
    }
    private void clear(Player player){
        player.drawPile.clear();player.hand.clear();player.points=0;
        for(int row=0;row<2;row++)for(int col=0;col<COLUMNS;col++)player.board[row][col]=null;
    }
    public void draw(Player player,int count){
        for(int i=0;i<count&&!player.drawPile.isEmpty();i++)
            player.hand.add(player.drawPile.remove(player.drawPile.size()-1));
    }
    public int score(Player player){
        int score=0;for(Unit[] row:player.board)for(Unit unit:row)
            if(unit!=null)score+=Math.max(0,power(unit));
        return score;
    }
    public boolean empty(Player player,int row,int column){
        return row>=0&&row<2&&column>=0&&column<COLUMNS&&player.board[row][column]==null;
    }
    public boolean affordable(Player side,int index){
        return index>=0&&index<side.hand.size()&&side.hand.get(index).numericCost()>0
            &&side.hand.get(index).numericCost()<=side.points;
    }
    private boolean hasFaction(Player player,String faction){
        for(Unit[] row:player.board)for(Unit unit:row)if(unit!=null&&unit.card.faction.equalsIgnoreCase(faction))
            return true;return false;
    }
    private ArrayList<Unit> all(Player player){
        ArrayList<Unit> units=new ArrayList<>();
        for(Unit[] row:player.board)for(Unit unit:row)if(unit!=null)units.add(unit);
        return units;
    }
    private ArrayList<Unit> faction(Player player,String faction){
        ArrayList<Unit> units=all(player);
        units.removeIf(u->!u.card.faction.equalsIgnoreCase(faction));
        return units;
    }
    private ArrayList<Unit> inspired(Player player,String faction){
        ArrayList<Unit> units=faction(player,faction);
        units.removeIf(u->!u.inspired||u.suppressed);
        return units;
    }
    private boolean adjacent(Unit a,Unit b){
        return a.owner==b.owner&&a.row==b.row&&Math.abs(a.column-b.column)==1;
    }
    private boolean sameColumn(Unit a,Unit b){
        return a.owner==b.owner&&a.column==b.column&&a.row!=b.row;
    }
    private boolean line(Unit unit,int threshold){
        int left=unit.column,right=unit.column;
        while(left>0&&validLine(unit,unit.owner.board[unit.row][left-1]))left--;
        while(right<COLUMNS-1&&validLine(unit,unit.owner.board[unit.row][right+1]))right++;
        return right-left+1>=threshold;
    }
    private boolean validLine(Unit origin,Unit next){
        return next!=null&&next.card.faction.equalsIgnoreCase(origin.card.faction);
    }
    private int aura(Unit target){
        if(target.suppressed)return 0;
        int bonus=0;
        for(Unit other:all(target.owner)){
            if(other==target||other.suppressed)continue;
            String name=other.card.name;
            switch(name){
                case "Legionary":
                    if(other==target)break;
                    break;
                case "Shinka Legionary":
                    break;
                case "Decurion":
                    if(sameColumn(other,target))bonus+=2;break;
                case "Colossus":
                    if(sameColumn(other,target))bonus+=1;break;
                case "Shinka Centurion":
                    if(adjacent(other,target))bonus+=3;break;
                case "Centurion":
                    if(line(other,4)&&adjacent(other,target))bonus+=2;break;
                default:break;
            }
        }
        if("Legionary".equals(target.card.name)&&partnerInColumn(target))bonus++;
        if("Shinka Legionary".equals(target.card.name)&&partnerInColumn(target))bonus+=2;
        if("Paradoxical Neophyte's Shade".equals(target.card.name)){
            bonus+=4*inspired(target.owner,"Hive").size();
        }
        return bonus;
    }
    private boolean partnerInColumn(Unit unit){
        return unit.owner.board[1-unit.row][unit.column]!=null;
    }
    public int power(Unit unit){return Math.max(0,unit.power+aura(unit));}
    public int original(Unit unit){return Math.max(0,unit.card.numericPower());}
    public int powerBonus(Unit unit){return Math.max(0,power(unit)-original(unit));}
    private int reduction(Unit target){
        int reduce=0;
        for(Unit other:all(target.owner)){
            if(other.suppressed)continue;
            if(other.card.name.equals("Shinka Phalanx")&&sameColumn(other,target))reduce++;
            if(other.card.name.equals("Phalanx")&&line(other,3)
                &&target.card.faction.equalsIgnoreCase("Cabal"))reduce++;
        }
        return reduce;
    }
    private int extraDamage(Player attacking){
        int bonus=0;
        for(Unit other:all(attacking)){
            if(other.suppressed)continue;
            if(other.card.name.equals("Servitor"))bonus++;
            if(other.card.name.equals("Juggernaut")&&partnerInColumn(other))bonus++;
            if(other.card.name.equals("Shinka Decurion")&&line(other,3))bonus+=2;
        }
        return bonus;
    }
    private boolean isTargeted(String name){
        switch(name){
            case "Morgran":case "Hyssop":case "Knight":case "Wizard":case "Guarding Servitor":
            case "Finnala":case "Chinnawat":case "Attal":case "Estela":case "Vigile":
            case "Zhong Wenbin":case "Sabikis":case "Gladiator":case "Shinka Gladiator":
            case "Shinka Psion":case "Inquisitor":case "Psion":case "Tracer Shank":
            case "Vandal":case "Wretch":case "Incendior":case "Resilient Marauder":
            case "Augmented Shank":case "High Servitor":case "Thinktank":case "Amu-Atu":
            case "Shinka Incendior":case "Shinka Vigile":case "Thagrius":
            case "Wolf":case "Wu Ming":case "Gwynn":case "Xuan Wei":
                return true;
            default:return false;
        }
    }
    public boolean isTargeted(ShadowData.Card card){return isTargeted(card.name);}
    private boolean targetsEnemy(String card){
        switch(card){
            case "Inquisitor":case "Psion":case "Tracer Shank":case "Vandal":
            case "Wretch":case "Incendior":case "Resilient Marauder":
            case "Augmented Shank":case "High Servitor":case "Thinktank":
            case "Amu-Atu":case "Shinka Incendior":case "Shinka Vigile":
            case "Thagrius":case "Wolf":case "Wu Ming":case "Gwynn":case "Xuan Wei":
            case "Shinka Psion":return true;
            default:return false;
        }
    }
    private boolean activeInRow(ShadowData.Card card,int row){
        String requirement=card.trigger.toLowerCase(Locale.ROOT);
        if(requirement.isEmpty()){
            String effect=card.effect.toLowerCase(Locale.ROOT);
            if(effect.startsWith("when played ("))requirement=effect.substring(0,Math.min(effect.length(),45));
        }
        if(requirement.contains("back row")&&row!=0)return false;
        if(requirement.contains("front row")&&row!=1)return false;
        return true;
    }
    public List<Target> targetOptions(Player me,ShadowData.Card card,int row,int column){
        ArrayList<Target> options=new ArrayList<>();
        if(!isTargeted(card.name)||!activeInRow(card,row))return options;
        Player side=targetsEnemy(card.name)?other(me):me;
        for(int r=0;r<2;r++)for(int c=0;c<COLUMNS;c++){
            Unit unit=side.board[r][c];
            if(unit==null)continue;
            if("Vandal".equals(card.name)||"Wretch".equals(card.name)||"Incendior".equals(card.name)
              ||"Shinka Psion".equals(card.name)&&false){
                if(r!=1)continue;
            }
            if("Shinka Incendior".equals(card.name)||"Shinka Vigile".equals(card.name)){
                if(r!=0)continue;
            }
            if("Knight".equals(card.name)&&(!unit.card.faction.equalsIgnoreCase("Hive")||!unit.inspired))continue;
            if("Hyssop".equals(card.name)&&unit.bonus<=0)continue;
            if("Wolf".equals(card.name)&&power(unit)<=8)continue;
            if("Vigile".equals(card.name)&&!line(unit,2))continue;
            options.add(new Target(side,r,c));
        }
        return options;
    }
    public Player other(Player me){return me==human?cpu:human;}
    private Unit targetUnit(Target target){
        return target==null?null:target.owner.board[target.row][target.column];
    }
    private Target chooseAI(Player me,ShadowData.Card card,int row,int column){
        List<Target> choices=targetOptions(me,card,row,column);
        if(choices.isEmpty())return null;
        choices=new ArrayList<>(choices);
        choices.sort((a,b)->Integer.compare(power(b.unit()),power(a.unit())));
        return choices.get(0);
    }
    public boolean play(Player me,int handIndex,int row,int column,Target target){
        if(finished||!me.equals(humanTurn?human:cpu)||!affordable(me,handIndex)||!empty(me,row,column))return false;
        ShadowData.Card card=me.hand.get(handIndex);
        if(target!=null&&targetUnit(target)==null)return false;
        me.points-=card.numericCost();me.hand.remove(handIndex);
        Unit placed=new Unit(me,card,row,column);me.board[row][column]=placed;
        lastPlacedSide=me.human?0:1;lastPlacedRow=row;lastPlacedColumn=column;
        log((me.human?"YOU":"CPU")+" ▶ "+card.name+" • "+(row==0?"BACK":"FRONT")+" "+(column+1));
        onPlayed(placed,target);
        fireEnemyPlacement(placed);
        sweep();
        return true;
    }
    private void onPlayed(Unit actor,Target target){
        Player me=actor.owner,foe=other(me);
        String name=actor.card.name;
        Unit picked=targetUnit(target);
        if(!activeInRow(actor.card,actor.row)){
            if(!noEffect(actor.card))log(name+": position requirement not met.");
            return;
        }
        switch(name){
            case "Sparrow":case "Resilient Captain":
                buff(actor,3);break;
            case "Elite Vandal":
                if(!rowEmpty(foe,1))buff(actor,3);break;
            case "Hexguard":
                actor.inspired=true;log("Inspired: "+name);break;
            case "Tenacious Shielded Knight":
                for(Unit u:all(me))if(adjacent(actor,u)&&"Hive".equalsIgnoreCase(u.card.faction))inspire(u);
                break;
            case "Wizard":case "Guarding Servitor":
                inspire(picked);break;
            case "Tenacious Wizard":
                inspireRandom(me,"Hive",2);break;
            case "Knight":
                if(picked!=null)buff(picked,3);break;
            case "Tenacious Knight":
                if(actor.row==1)buffRandom(inspired(me,"Hive"),2,3);
                break;
            case "Morgran":if(picked!=null)buff(picked,1);break;
            case "Hyssop":if(picked!=null)buff(picked,3);break;
            case "Mishra":
                for(Unit u:all(me))if(adjacent(actor,u))buff(u,2);break;
            case "Zhong Wenbin":
                if(actor.row==1){Unit right=at(me,actor.row,actor.column+1);buff(right,2);}
                break;
            case "Finnala":
                if(actor.row==1 && picked!=null){buff(picked,8);dealDirect(actor,actor,3);}
                break;
            case "Attal":
                if(picked!=null){int boost=power(picked);buff(picked,boost);}
                break;
            case "Vanok":
                for(Unit u:faction(me,"Fallen"))buff(u,2);break;
            case "Kabr":
                for(Unit u:all(me))if(u.card.name.equals("Guerrilla"))buff(u,3);break;
            case "Radegast":
                if(hasFaction(foe,"Fallen"))
                    for(Unit u:all(me))if(u.row==actor.row&&u.card.name.equals("Guerrilla"))buff(u,4);
                break;
            case "Chinnawat":
                if(actor.row==1&&picked!=null)for(Unit u:all(me))
                    if(u==picked||adjacent(u,picked))buff(u,1);
                break;
            case "Lady Skorri":
                if(!rowEmpty(foe,0))buff(at(me,actor.row,actor.column-1),5);
                break;
            case "Estela":
                if(picked!=null)buff(picked,2*countRow(foe,1));break;
            case "Sabikis":
                if(actor.row==0&&picked!=null){
                    Unit left=at(me,picked.row,picked.column-1);
                    if(left!=null)buff(picked,power(left));
                }break;
            case "Buhr":
                if(partnerInColumn(actor))
                    for(Unit u:all(me))if(u.column==actor.column)buff(u,3);
                break;
            case "Vigile":
                if(actor.row==1&&picked!=null)buff(picked,2);break;
            case "Gladiator":case "Shinka Gladiator":
                if(picked!=null){
                    int minimum=name.equals("Shinka Gladiator")?5:0;
                    if(minimum==0||line(actor,minimum)){
                        int amount=name.equals("Shinka Gladiator")?2:1;
                        for(Unit u:all(me))if(u==picked||adjacent(u,picked))buff(u,amount);
                    }
                }break;
            case "Inquisitor":if(actor.row==0)damage(actor,picked,4);break;
            case "Tracer Shank":if(actor.row==0)damage(actor,picked,3);break;
            case "Psion":if(actor.row==0)damage(actor,picked,2);break;
            case "Vandal":damage(actor,picked,2);break;
            case "Wretch":if(actor.row==1)damage(actor,picked,1);break;
            case "Incendior":damage(actor,picked,3);break;
            case "Resilient Marauder":if(actor.row==1)damage(actor,picked,5);break;
            case "High Servitor":damage(actor,picked,faction(me,"Fallen").size());break;
            case "Thinktank":{
                int best=0;for(Unit u:all(me))best=Math.max(best,powerBonus(u));
                damage(actor,picked,best);break;
            }
            case "Amu-Atu":damage(actor,picked,countRow(foe,0));break;
            case "Shinka Psion":if(line(actor,2))damage(actor,picked,3);break;
            case "Shinka Incendior":if(line(actor,3))damage(actor,picked,3);break;
            case "Shinka Vigile":if(line(actor,3))damage(actor,picked,4);break;
            case "Thagrius":if(line(actor,6))damage(actor,picked,2*lineLength(actor));break;
            case "Wolf":if(picked!=null&&power(picked)>8)damage(actor,picked,power(picked));break;
            case "Wu Ming":if(actor.row==0&&picked!=null)suppress(picked);break;
            case "Gwynn":
                if(actor.row==1&&picked!=null){
                    for(Unit u:all(foe))if(u==picked||adjacent(u,picked))damage(actor,u,3);
                    dealDirect(actor,actor,2);
                }break;
            case "Xuan Wei":
                if(actor.row==0&&picked!=null){
                    damage(actor,picked,5);
                    if(targetUnit(target)!=null)incomplete(actor.card,"Choose destination for displaced unit.");
                }break;
            case "Augmented Shank":
                if(actor.row==1&&picked!=null)
                    for(Unit u:new ArrayList<>(all(foe)))if(u==picked||adjacent(u,picked))damage(actor,u,2);
                break;
            case "Blinder Shank":
                for(Unit u:all(me))if(adjacent(actor,u))suppress(u);
                break;
            case "Kevarrh":draw(me,2);log("Kevarrh: draw 2.");break;
            case "Luksis-3":
                int rocks=0;
                for(Unit u:all(foe))if(u.card.name.equals("Rock")||u.card.name.equals("Rock Pile"))rocks++;
                buff(actor,rocks*2);break;
            case "Carrier":case "Ketch":
                spawnAt(me,actor.row,actor.column-1,"Dreg");
                spawnAt(me,actor.row,actor.column+1,"Dreg");break;
            case "Mu Ying":case "Paradoxical Neophyte":case "Denzin":case "Unrelenting Captain":
                incomplete(actor.card,"Summon token has no verified base Power in catalog.");break;
            case "Tomb Ship":case "Drop Pod":
                incomplete(actor.card,"Random faction summoning pool not confirmed.");break;
            case "Ning Fei":
                incomplete(actor.card,"Enemy Clone token Power not verified.");break;
            case "Cursed Thrall":
                incomplete(actor.card,"Opponent-side delayed-death transfer remains under verification.");break;
            case "Shinka Scorpius":case "Scorpius":case "Elite Walker":
            case "Pandora-7":break;
            default:
                if(!noEffect(actor.card)&&!isKnownSustained(actor.card.name))
                    incomplete(actor.card,"No verified scripted resolver.");
        }
    }
    private boolean isKnownSustained(String name){
        switch(name){
            case "Legionary":case "Shinka Legionary":case "Decurion":
            case "Colossus":case "Shinka Centurion":case "Centurion":
            case "Shinka Phalanx":case "Phalanx":case "Juggernaut":
            case "Servitor":case "Shinka Decurion":
            case "Paradoxical Neophyte's Shade":case "The Disfavored":
            case "Ogre":case "Tenacious Ogre":case "Shrieker":
            case "Saladin":case "Tan-2":case "Elite Sniper Vandal":
            case "Ayuela":case "Resilient Grenade Dreg":case "Zhiyao":
                return true;
            default:return false;
        }
    }
    private int lineLength(Unit unit){
        int total=1;
        for(int c=unit.column-1;c>=0&&validLine(unit,at(unit.owner,unit.row,c));c--)total++;
        for(int c=unit.column+1;c<COLUMNS&&validLine(unit,at(unit.owner,unit.row,c));c++)total++;
        return total;
    }
    private boolean rowEmpty(Player player,int row){return countRow(player,row)==0;}
    private int countRow(Player player,int row){
        int n=0;for(Unit unit:player.board[row])if(unit!=null)n++;return n;
    }
    private Unit at(Player p,int row,int col){
        return row>=0&&row<2&&col>=0&&col<COLUMNS?p.board[row][col]:null;
    }
    private void spawnAt(Player me,int row,int col,String name){
        if(!empty(me,row,col))return;
        ShadowData.Card card=card(name);
        if(card==null||card.numericPower()<0){
            if(card!=null)incomplete(card,"Token/base Power missing.");
            return;
        }
        Unit unit=new Unit(me,card,row,col);me.board[row][col]=unit;
        if(name.equals("Dreg")){
            for(Unit watcher:all(me))if(watcher!=unit&&!watcher.suppressed&&watcher.card.name.equals("Unrelenting Captain")){
                unit.power++;unit.bonus++;log("Unrelenting Captain buffs summoned Dreg.");
            }
        }
        log("SUMMON ▶ "+name+" at "+(row==0?"back ":"front ")+(col+1));
        fireEnemyPlacement(unit);
    }
    private void inspire(Unit target){if(target!=null){target.inspired=true;log("INSPIRED ▶ "+target.card.name);}}
    private void inspireRandom(Player me,String faction,int count){
        ArrayList<Unit> choices=faction(me,faction);
        Collections.shuffle(choices,rng);
        for(int i=0;i<Math.min(count,choices.size());i++)inspire(choices.get(i));
    }
    private void buffRandom(List<Unit> choices,int count,int amount){
        ArrayList<Unit> shuffled=new ArrayList<>(choices);Collections.shuffle(shuffled,rng);
        for(int i=0;i<Math.min(count,shuffled.size());i++)buff(shuffled.get(i),amount);
    }
    private void buff(Unit target,int amount){
        if(target==null||amount<=0)return;
        Player owner=target.owner;
        int applied=amount;
        for(Unit u:all(owner))if(u!=target&&!u.suppressed&&u.card.name.equals("Tan-2")){
            applied*=2;suppress(u);log("Tan-2 doubles bonus.");break;
        }
        target.power+=applied;target.bonus+=applied;
        log("BUFF +"+applied+" ▶ "+target.card.name);
        onBuff(target);
    }
    private int resolutionDepth=0;
    private void onBuff(Unit target){
        if(resolutionDepth++>32){
            resolutionDepth--;unresolvedEffects++;
            log("SAFETY: recursive Power triggers capped after 32 resolutions.");return;
        }
        Player own=target.owner,foe=other(own);
        // Copy the snapshot to avoid iteration changes during triggers.
        for(Unit unit:new ArrayList<>(all(own))){
            if(unit.suppressed)continue;
            String name=unit.card.name;
            if(name.equals("Ogre")||name.equals("Tenacious Ogre")||name.equals("Shrieker")){
                if(unit!=target)continue;
                int hits=name.equals("Tenacious Ogre")?4:name.equals("Ogre")?2:1;
                int points=name.equals("Shrieker")?2:1;
                for(int i=0;i<hits;i++){
                    Unit victim=randomUnit(foe);
                    if(victim!=null)damage(unit,victim,points);
                }
            }
            if(unit==target&&name.equals("The Disfavored")&&unit.row==1){
                ArrayList<Unit> hive=faction(own,"Hive");hive.remove(target);
                if(!hive.isEmpty())buff(hive.get(rng.nextInt(hive.size())),2);
            }
            if(unit==target&&name.equals("Guerrilla Captain")){
                Unit victim=randomUnit(foe);if(victim!=null)damage(unit,victim,2);
            }
            if(unit==target&&(name.equals("Denzin")||name.equals("Unrelenting Captain")))
                incomplete(unit.card,"Summoned token stats not verified.");
            if(unit==target&&name.equals("Xur"))
                incomplete(unit.card,"Rock summon destination/token Power missing.");
        }
        // Opposing Elite Sniper Vandals react to buffs on a front-row enemy unit.
        if(target.row==1)for(Unit sniper:all(foe))if(sniper.card.name.equals("Elite Sniper Vandal")&&!sniper.suppressed)
            damage(sniper,target,1);
        sweep();
        resolutionDepth--;
    }
    private Unit randomUnit(Player player){
        ArrayList<Unit> units=all(player);
        return units.isEmpty()?null:units.get(rng.nextInt(units.size()));
    }
    private void suppress(Unit target){
        if(target==null)return;
        target.power-=target.bonus;target.bonus=0;
        target.suppressed=true;target.inspired=false;target.scorched=0;target.scorchTurns=0;
        target.timedBonuses.clear();
        log("SUPPRESSED ▶ "+target.card.name);
    }
    private void damage(Unit source,Unit target,int amount){
        if(target==null||amount<=0)return;
        int actual=Math.max(0,amount+extraDamage(source.owner)-reduction(target));
        for(Unit ally:all(source.owner))if(ally!=source&&!ally.suppressed&&ally.card.name.equals("Jolder")){
            actual*=2;suppress(ally);break;
        }
        for(Unit ally:all(source.owner))if(ally!=source&&!ally.suppressed&&ally.card.name.equals("Umeko")&&ally.row==0){
            actual+=1+rng.nextInt(3);break;
        }
        dealDirect(source,target,actual);
    }
    private void dealDirect(Unit source,Unit target,int damage){
        if(target==null||damage<=0)return;
        target.power-=damage;log("DAMAGE "+damage+" ▶ "+target.card.name);
        if(target.card.faction.equalsIgnoreCase("Humans"))for(Unit ally:all(target.owner)){
            if(!ally.suppressed&&ally.card.name.equals("Saladin"))buff(target,3);
        }
    }
    private void fireEnemyPlacement(Unit newUnit){
        Player enemy=other(newUnit.owner);
        for(Unit watcher:all(enemy)){
            if(watcher.suppressed)continue;
            if(newUnit.row==1&&watcher.card.name.equals("Ayuela")&&watcher.row==0){
                newUnit.scorched=2;newUnit.scorchTurns=2;
                log("SCORCHED ▶ "+newUnit.card.name+" for 2 turns.");
            }
            if(newUnit.row==1&&watcher.card.name.equals("Zhiyao")){
                watcher.power++;watcher.bonus++;
                watcher.timedBonuses.add(new TimedBonus(1,3));
                log("Zhiyao reacts +1 Power for 3 turns.");
            }
            if(newUnit.row==0&&watcher.card.name.equals("Resilient Grenade Dreg")&&watcher.row==0){
                newUnit.scorched=2;newUnit.scorchTurns=2;
                dealDirect(watcher,newUnit,2);
            }
        }
    }
    private void sweep(){
        // Death chains can kill further adjacent units. Remove after a stable snapshot.
        for(int pass=0;pass<12;pass++){
            ArrayList<Unit> killed=new ArrayList<>();
            for(Player side:new Player[]{human,cpu})for(Unit u:all(side))if(power(u)<=0)killed.add(u);
            if(killed.isEmpty())break;
            for(Unit dead:killed){
                if(dead.owner.board[dead.row][dead.column]!=dead)continue;
                dead.owner.board[dead.row][dead.column]=null;
                log("DESTROYED ▶ "+dead.card.name);
                int splash=dead.card.name.equals("Clone")?3:dead.card.name.equals("Exploder Shank")?1:0;
                if(splash>0)for(int offset:new int[]{-1,1}){
                    Unit nearby=at(dead.owner,dead.row,dead.column+offset);
                    if(nearby!=null)dealDirect(dead,nearby,splash);
                }
            }
        }
    }
    private void endSide(Player side){
        for(Unit unit:new ArrayList<>(all(side))){
            if(unit.owner.board[unit.row][unit.column]!=unit)continue;
            if(unit.inspired&&!unit.suppressed)buff(unit,1);
            if(unit.scorchTurns>0){unit.scorchTurns--;dealDirect(unit,unit,unit.scorched);}
            for(int i=unit.timedBonuses.size()-1;i>=0;i--){
                TimedBonus active=unit.timedBonuses.get(i);
                if(--active.turns<=0){
                    unit.power-=active.value;unit.bonus=Math.max(0,unit.bonus-active.value);
                    unit.timedBonuses.remove(i);
                    log(unit.card.name+" temporary bonus expired.");
                }
            }
        }
        for(Unit unit:new ArrayList<>(all(side))){
            if(unit.suppressed)continue;
            switch(unit.card.name){
                case "Scorpius":case "Shinka Scorpius":
                    if(unit.card.name.equals("Shinka Scorpius")&&!line(unit,3))break;
                    int harm=unit.card.name.equals("Scorpius")?1:2;
                    Unit victim=randomUnit(other(side));if(victim!=null)damage(unit,victim,harm);
                    break;
                case "Elite Walker":
                    for(int i=0;i<2;i++){Unit chosen=randomUnit(other(side));if(chosen!=null)damage(unit,chosen,2);}
                    break;
                case "Pandora-7":
                    buff(at(side,unit.row,unit.column+1),2);break;
                default:break;
            }
        }
        sweep();
    }
    public void endTurn(){
        if(finished)return;
        Player current=humanTurn?human:cpu;
        endSide(current);
        completedTurns++;
        if(completedTurns==2){
            if(round==ROUNDS){
                finished=true;log("FINAL • YOU "+score(human)+" — "+score(cpu)+" CPU");
                return;
            }
            round++;completedTurns=0;human.points=round;cpu.points=round;
            draw(human,1);draw(cpu,1);
            humanTurn=humanFirst;
            log("ROUND "+round+" • "+round+" play points");
        }else{
            humanTurn=!humanTurn;
        }
    }
    public void cpuTurn(){
        if(finished||humanTurn)return;
        int safety=40;
        while(safety-->0){
            double highest=Double.NEGATIVE_INFINITY;int chosen=-1;int[] location=null;
            for(int i=0;i<cpu.hand.size();i++){
                ShadowData.Card c=cpu.hand.get(i);
                if(c.numericCost()<1||c.numericCost()>cpu.points)continue;
                int[] slot=chooseSlot(c);
                if(slot==null)continue;
                double value=(Math.max(0,c.numericPower())+0.5)/(double)c.numericCost();
                if(isTargeted(c.name)&&!targetOptions(cpu,c,slot[0],slot[1]).isEmpty())value+=0.6;
                value+=rng.nextDouble()*0.2;
                if(value>highest){highest=value;chosen=i;location=slot;}
            }
            if(chosen==-1||location==null)break;
            ShadowData.Card c=cpu.hand.get(chosen);
            Target target=chooseAI(cpu,c,location[0],location[1]);
            if(!play(cpu,chosen,location[0],location[1],target))break;
        }
        endTurn();
    }
    private int[] chooseSlot(ShadowData.Card card){
        String text=card.trigger.toLowerCase(Locale.ROOT);
        if(text.isEmpty())text=card.effect.toLowerCase(Locale.ROOT);
        int preferred=text.contains("back row")?0:1;
        for(int row:new int[]{preferred,1-preferred})
            for(int col=0;col<COLUMNS;col++)if(empty(cpu,row,col))return new int[]{row,col};
        return null;
    }
    public String result(){
        if(!finished)return "";
        int left=score(human),right=score(cpu);
        return left>right?"VICTORY":left<right?"DEFEAT":"DRAW";
    }
}
