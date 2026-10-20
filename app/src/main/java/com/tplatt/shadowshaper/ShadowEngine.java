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
    public static final int COLUMNS=ShadowRules.COLUMNS, ROUNDS=ShadowRules.ROUNDS;
    public final Map<String,ShadowData.Card> catalog;
    public final Player human=new Player(true), cpu=new Player(false);
    public final ArrayList<String> journal=new ArrayList<>();
    public final Random rng;
    public final long seed;
    public String lastActionError="";
    public boolean humanFirst, humanTurn, finished;
    public int round=1, completedTurns=0, unresolvedEffects=0;
    public int lastPlacedSide=-1,lastPlacedRow=-1,lastPlacedColumn=-1,placementRevision=0;
    public Unit pendingMovedUnit;
    public Player pendingMoveCaster;

    public static final class Unit {
        public final ShadowData.Card card;
        public final Player owner;
        public int row,column,power,bonus,scorched,scorchTurns;
        public final ArrayList<TimedBonus> timedBonuses=new ArrayList<>();
        public boolean inspired,suppressed,bonusDoublerConsumed,damageDoublerConsumed;
        // Cursed Thrall is controlled by the opponent until its original player\u0027s next turn.
        public Player fuseOrigin;
        public int fuseRound;
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
        this.catalog=catalog;this.seed=seed;this.rng=new Random(seed);
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
        clear(human);clear(cpu);journal.clear();unresolvedEffects=0;lastActionError="";
        pendingMovedUnit=null;pendingMoveCaster=null;
        human.drawPile.addAll(ShadowData.expanded(playerDeck,catalog));
        cpu.drawPile.addAll(ShadowData.expanded(cpuDeck,catalog));
        Collections.shuffle(human.drawPile,rng);Collections.shuffle(cpu.drawPile,rng);
        humanFirst=rng.nextBoolean();humanTurn=humanFirst;finished=false;
        round=1;completedTurns=0;lastPlacedSide=-1;lastPlacedRow=-1;lastPlacedColumn=-1;placementRevision=0;resolutionDepth=0;
        draw(human,ShadowRules.openingHand(humanFirst));
        draw(cpu,ShadowRules.openingHand(!humanFirst));
        human.points=ShadowRules.lightForRound(1);cpu.points=ShadowRules.lightForRound(1);
        log("MATCH SEED: "+seed);
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
        return ShadowRules.isOnBoard(row,column)&&player.board[row][column]==null;
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
        return a.owner==b.owner &&
            ShadowPositionRules.isAdjacent(a.row,a.column,b.row,b.column);
    }
    private boolean sameColumn(Unit a,Unit b){
        return a.owner==b.owner &&
            ShadowPositionRules.isSameColumn(a.row,a.column,b.row,b.column);
    }
    private boolean line(Unit unit,int threshold){
        return ShadowPositionRules.contiguousFactionLine(unit.owner,unit.card.faction,
            unit.row,unit.column,false)>=threshold;
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
    private int extraDamage(Unit source){
        int bonus=0;
        for(Unit other:all(source.owner)){
            if(other.suppressed)continue;
            if(other.card.name.equals("Servitor"))bonus++;
            if(other.card.name.equals("Juggernaut")&&
                source.card.faction.equalsIgnoreCase("Cabal")&&adjacent(other,source))bonus++;
            if(other.card.name.equals("Shinka Decurion")&&line(other,3)&&
                source.card.faction.equalsIgnoreCase("Cabal"))bonus+=2;
        }
        return bonus;
    }
    private boolean isTargeted(String name){
        switch(name){
            case "Morgran":case "Hyssop":case "Knight":case "Wizard":case "Guarding Servitor":
            case "Finnala":case "Chinnawat":case "Attal":case "Estela":case "Vigile":
            case "Sabikis":case "Gladiator":case "Shinka Gladiator":
            case "Shinka Psion":case "Inquisitor":case "Psion":case "Tracer Shank":
            case "Vandal":case "Wretch":case "Incendior":case "Resilient Marauder":
            case "Augmented Shank":case "High Servitor":case "Thinktank":case "Amu-Atu":
            case "Shinka Incendior":case "Shinka Vigile":case "Thagrius":
            case "Wolf":case "Wu Ming":case "Gwynn":case "Xuan Wei":case "Ikora":case "Goliath Tank":
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
            case "Ikora":case "Goliath Tank":
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
    /**
     * Line length INCLUDING the card proposed for an empty position.
     * An uninterrupted line only counts cards of the same faction in
     * the same row; visually adjacent units from other factions break it.
     */
    public int projectedFactionLine(Player player,ShadowData.Card card,int row,int column){
        if(player==null||card==null)return 0;
        return ShadowPositionRules.contiguousFactionLine(player,card.faction,row,column,true);
    }
    private int requiredFactionLine(String name){
        switch(name){
            case "Shinka Psion":return 2;
            case "Shinka Incendior":case "Shinka Vigile":return 3;
            case "Shinka Gladiator":case "Goliath Tank":return 5;
            case "Thagrius":return 6;
            default:return 0;
        }
    }
    public List<Target> targetOptions(Player me,ShadowData.Card card,int row,int column){
        ArrayList<Target> options=new ArrayList<>();
        if(me==null||card==null||!isTargeted(card.name)||!activeInRow(card,row))return options;
        if(!ShadowRules.isOnBoard(row,column) || !empty(me,row,column))return options;
        int needed=requiredFactionLine(card.name);
        if(needed>0&&projectedFactionLine(me,card,row,column)<needed)return options;
        Player side=targetsEnemy(card.name)?other(me):me;
        for(int r=0;r<2;r++)for(int c=0;c<COLUMNS;c++){
            Unit unit=side.board[r][c];
            if(unit==null)continue;
            if("Vandal".equals(card.name)||"Wretch".equals(card.name)||"Incendior".equals(card.name)){
                if(r!=1)continue;
            }
            if("Shinka Incendior".equals(card.name)||"Shinka Vigile".equals(card.name)){
                if(r!=0)continue;
            }
            if("Knight".equals(card.name)&&(!unit.card.faction.equalsIgnoreCase("Hive")||!unit.inspired))continue;
            if("Hyssop".equals(card.name)&&unit.bonus<=0)continue;
            if("Wolf".equals(card.name)&&power(unit)<=8)continue;
            if("Vigile".equals(card.name)&&!line(unit,2))continue;
            if(("Gladiator".equals(card.name)||"Shinka Gladiator".equals(card.name))&&r!=1)continue;
            if("Sabikis".equals(card.name)&&c==0)continue; // chosen unit needs a left neighbor
            if("Sabikis".equals(card.name)&&side.board[r][c-1]==null)continue;
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
    /**
     * Validate an entire player action BEFORE payment or any board mutation.
     * Enforces turn ownership, hand index, affordable cost, empty in-bounds
     * placement, and selection of a currently legal target.
     * No target is required when a targeted ability has no legal candidates.
     */
    public String validatePlay(Player side,int index,int row,int column,Target selected){
        if(side!=human&&side!=cpu)return "Unknown player.";
        if(finished)return "The match has finished.";
        if(pendingMovedUnit!=null)return "Choose a destination for "+pendingMovedUnit.card.name+" before playing another card.";
        if(side!=(humanTurn?human:cpu))return "It is not this player's turn.";
        if(index<0||index>=side.hand.size())return "Selected card is not in hand.";
        ShadowData.Card card=side.hand.get(index);
        if(card.numericCost()<1)return "Card is not playable (missing or invalid cost).";
        if(card.numericPower()<0)return "Card Power is not verified.";
        if(card.numericCost()>side.points)return "Insufficient Light.";
        if(!ShadowRules.isOnBoard(row,column))return "Invalid battlefield position.";
        if(!empty(side,row,column))return "Position is already occupied.";
        if(selected!=null) {
            if(selected.owner!=human && selected.owner!=cpu)
                return "Target does not belong to this match.";
            if(!ShadowRules.isOnBoard(selected.row,selected.column))
                return "Target is outside the battlefield.";
            if(selected.unit()==null)return "Target is no longer on the battlefield.";
        }
        List<Target> possible=targetOptions(side,card,row,column);
        if(selected!=null){
            boolean found=false;
            for(Target legal:possible)if(legal.owner==selected.owner&&legal.row==selected.row
                &&legal.column==selected.column){found=true;break;}
            if(!found)return "That unit is not a legal target for "+card.name+".";
        }else if(!possible.isEmpty()) {
            return "Choose a target for "+card.name+".";
        }
        return "";
    }
    public boolean isPlayablePlacement(Player side,int index,int row,int column){
        // Useful for UI drop highlighting; target choice is resolved after drop.
        if((side!=human&&side!=cpu)||finished||pendingMovedUnit!=null||side!=(humanTurn?human:cpu))return false;
        if(index<0||index>=side.hand.size())return false;
        return affordable(side,index) && side.hand.get(index).numericPower()>=0
            && empty(side,row,column);
    }
    public String turnLabel(){
        if(finished)return "FINAL";
        return (humanTurn?"PLAYER":"CPU")+" • ROUND "+round;
    }
    public boolean play(Player me,int handIndex,int row,int column,Target target){
        lastActionError=validatePlay(me,handIndex,row,column,target);
        if(!lastActionError.isEmpty())return false;
        ShadowData.Card card=me.hand.get(handIndex);
        me.points-=card.numericCost();me.hand.remove(handIndex);
        Unit placed=new Unit(me,card,row,column);me.board[row][column]=placed;
        lastPlacedSide=me.human?0:1;lastPlacedRow=row;lastPlacedColumn=column;placementRevision++;
        log((me.human?"YOU":"CPU")+" ▶ "+card.name+" • "+(row==0?"BACK":"FRONT")+" "+(column+1));
        onPlayed(placed,target);
        if(placed.owner.board[placed.row][placed.column]==placed)
            fireEnemyPlacement(placed); // transfer abilities dispatch their own placement event
        sweep();
        if(me==cpu)resolveCpuMove();
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
            case "Bazinet":
                if(actor.row==0)for(Unit u:new ArrayList<>(all(foe)))if(powerBonus(u)>0)damage(actor,u,3);
                break;
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
            case "Ikora":
                if(picked!=null)for(Unit u:new ArrayList<>(all(foe)))
                    if(u.row==picked.row)damage(actor,u,2);
                break;
            case "Goliath Tank":
                if(picked!=null&&line(actor,5))for(Unit u:new ArrayList<>(all(foe)))
                    if(u.row==picked.row)damage(actor,u,3);
                break;
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
                    if(power(picked)>0 && picked.owner.board[picked.row][picked.column]==picked){
                        if(hasEmptySlot(picked.owner)){
                            pendingMovedUnit=picked;
                            pendingMoveCaster=me;
                            log("XUAN WEI: Choose a new position for "+picked.card.name+".");
                        }else{
                            incomplete(actor.card,"No open destination; full-board relocation behavior unverified.");
                        }
                    }
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
            case "Paradoxical Neophyte":
                spawnAt(me,actor.row,actor.column-1,"Paradoxical Neophyte's Shade");
                break;
            case "Mu Ying":
                spawnAt(me,actor.row,actor.column-1,"Guerrilla");
                spawnAt(me,actor.row,actor.column+1,"Guerrilla");
                break;
            case "Tomb Ship":case "Drop Pod":
                incomplete(actor.card,"Random faction summoning pool not confirmed.");break;
            case "Ning Fei":
                spawnRandom(foe,1,"Clone",actor.card);
                break;
            case "Cursed Thrall":
                transferCursedThrall(actor);break;
            case "Shinka Scorpius":case "Scorpius":case "Elite Walker":
            case "Pandora-7":break;
            default:
                if(!noEffect(actor.card)&&!isKnownSustained(actor.card.name))
                    incomplete(actor.card,"No verified "+ShadowEffectRegistry.family(actor.card)+" resolver.");
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
            case "Denzin":case "Unrelenting Captain":case "The Speaker":
            case "Guerrilla Captain":case "Umeko":case "Jolder":
                return true;
            default:return false;
        }
    }
    private int lineLength(Unit unit){
        return ShadowPositionRules.contiguousFactionLine(unit.owner,unit.card.faction,
            unit.row,unit.column,false);
    }
    private boolean rowEmpty(Player player,int row){return countRow(player,row)==0;}
    private int countRow(Player player,int row){
        int n=0;for(Unit unit:player.board[row])if(unit!=null)n++;return n;
    }
    private Unit at(Player p,int row,int col){
        return row>=0&&row<2&&col>=0&&col<COLUMNS?p.board[row][col]:null;
    }
    /** A named, playable token is placed into a uniformly random free slot. */
    private void spawnRandom(Player owner,int row,String token,ShadowData.Card source){
        ArrayList<Integer> open=new ArrayList<>();
        for(int col=0;col<COLUMNS;col++)if(empty(owner,row,col))open.add(col);
        if(open.isEmpty()){
            incomplete(source,"No empty slot for "+token+"; full-row behavior not verified.");
            return;
        }
        spawnAt(owner,row,open.get(rng.nextInt(open.size())),token);
    }
    private boolean hasEmptySlot(Player side){
        for(int r=0;r<ShadowRules.ROWS;r++)
            for(int c=0;c<COLUMNS;c++)if(empty(side,r,c))return true;
        return false;
    }
    /**
     * Xuan Wei: after damage, the surviving target must move to an empty
     * position of its current owner's board. The exact choice pattern in
     * the game client remains to be independently verified.
     */
    public boolean resolveMove(Player actor,int row,int col){
        if(pendingMovedUnit==null || pendingMoveCaster!=actor){
            lastActionError="No movement is pending for this player.";return false;
        }
        Unit moving=pendingMovedUnit;
        if(moving.owner.board[moving.row][moving.column]!=moving){
            pendingMovedUnit=null;pendingMoveCaster=null;
            lastActionError="Target was removed before relocation.";return false;
        }
        if(!empty(moving.owner,row,col)){
            lastActionError="Select an empty destination on the target's battlefield.";
            return false;
        }
        moving.owner.board[moving.row][moving.column]=null;
        moving.row=row;moving.column=col;
        moving.owner.board[row][col]=moving;
        lastPlacedSide=moving.owner.human?0:1;
        lastPlacedRow=row;lastPlacedColumn=col;placementRevision++;
        pendingMovedUnit=null;pendingMoveCaster=null;
        lastActionError="";
        log("RELOCATED ▶ "+moving.card.name+" to "+(row==0?"BACK":"FRONT")+" "+(col+1));
        sweep();
        return true;
    }
    public void resolveCpuMove(){
        if(pendingMovedUnit==null || pendingMoveCaster!=cpu)return;
        ArrayList<Target> options=new ArrayList<>();
        for(int r=0;r<2;r++)for(int c=0;c<COLUMNS;c++)
            if(empty(pendingMovedUnit.owner,r,c))options.add(new Target(pendingMovedUnit.owner,r,c));
        if(!options.isEmpty()){
            Target chosen=options.get(rng.nextInt(options.size()));
            resolveMove(cpu,chosen.row,chosen.column);
        }
    }
    private void spawnAt(Player me,int row,int col,String name){
        if(!empty(me,row,col))return;
        ShadowData.Card card=card(name);
        if(card==null||card.numericPower()<0){
            if(card!=null)incomplete(card,"Token/base Power missing.");
            return;
        }
        Unit unit=new Unit(me,card,row,col);me.board[row][col]=unit;
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
        for(Unit u:all(owner))if(!u.suppressed && u.card.name.equals("Tan-2") &&
                !u.bonusDoublerConsumed){
            applied*=2;
            u.bonusDoublerConsumed=true;
            log("Tan-2 doubles next Power bonus (+ "+applied+"); remains on battlefield.");
            break;
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
            if(unit==target&&name.equals("The Speaker")&&unit.row==0)
                buff(at(own,unit.row,unit.column+1),2);
            if(unit==target&&name.equals("Guerrilla Captain")){
                Unit victim=randomUnit(foe);if(victim!=null)damage(unit,victim,2);
            }
            if(unit==target && name.equals("Denzin"))
                spawnAt(own,unit.row,unit.column-1,"Guerrilla");
            if(unit==target && name.equals("Unrelenting Captain"))
                spawnAt(own,unit.row,unit.column-1,"Dreg");
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
        int actual=ShadowDamageRules.afterAuras(amount,extraDamage(source),reduction(target));
        for(Unit ally:all(source.owner))if(!ally.suppressed &&
                ally.card.name.equals("Jolder") && !ally.damageDoublerConsumed){
            actual*=2;ally.damageDoublerConsumed=true;
            log("Jolder doubles the next damage event without becoming Suppressed.");
            break;
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
                log("SCORCHED ▶ "+newUnit.card.name+" for 2 turns (Resilient Grenade Dreg).");
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
    /**
     * Cursed Thrall occupies a random empty opponent Front Row slot and
     * detonates at the beginning of the original player's next turn.
     * This uses a new controlled unit so owner and board are consistent.
     */
    private void transferCursedThrall(Unit from){
        Player origin=from.owner,destination=other(origin);
        ArrayList<Integer> candidates=new ArrayList<>();
        for(int col=0;col<COLUMNS;col++)if(empty(destination,1,col))candidates.add(col);
        if(candidates.isEmpty()){
            incomplete(from.card,"Opponent Front Row full; transfer failure behavior unverified.");
            return;
        }
        int column=candidates.get(rng.nextInt(candidates.size()));
        origin.board[from.row][from.column]=null;
        Unit bomb=new Unit(destination,from.card,1,column);
        bomb.power=from.power;bomb.bonus=from.bonus;
        bomb.fuseOrigin=origin;
        bomb.fuseRound=round+1;
        destination.board[1][column]=bomb;
        lastPlacedSide=destination.human?0:1;lastPlacedRow=1;lastPlacedColumn=column;
        log("CURSED THRALL ▶ opponent Front Row "+(column+1)+"; detonates on next original turn.");
        fireEnemyPlacement(bomb);
    }
    private void startSide(Player side){
        // Side's own summoned or transferred units may need delayed activation.
        // A cursed unit is stored on the opponent's battlefield until its caster
        // begins the next turn, and cannot fire twice after removal.
        for(Player controller:new Player[]{human,cpu})
            for(Unit unit:new ArrayList<>(all(controller))){
                if(unit.fuseOrigin!=side||unit.fuseRound>round)continue;
                if(controller.board[unit.row][unit.column]!=unit)continue;
                controller.board[unit.row][unit.column]=null;
                log("CURSED THRALL DETONATES ▶ "+(unit.column+1));
                for(int offset:new int[]{-1,1}){
                    Unit adjacentUnit=at(controller,unit.row,unit.column+offset);
                    if(adjacentUnit!=null)dealDirect(unit,adjacentUnit,2);
                }
            }
        sweep();
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
            if(side.board[unit.row][unit.column]!=unit||unit.suppressed)continue;
            switch(unit.card.name){
                case "Scorpius":
                    // Corrected community card text: Back Row, 1 damage to two
                    // distinct random opposing units (fewer if only one exists).
                    if(unit.row!=ShadowRules.BACK_ROW)break;
                    ArrayList<Unit> scorpiusTargets=new ArrayList<>(all(other(side)));
                    Collections.shuffle(scorpiusTargets,rng);
                    for(int i=0;i<Math.min(2,scorpiusTargets.size());i++){
                        Unit victim=scorpiusTargets.get(i);
                        if(victim.owner.board[victim.row][victim.column]==victim)
                            damage(unit,victim,1);
                    }
                    break;
                case "Shinka Scorpius":
                    if(!line(unit,3))break;
                    Unit victim=randomUnit(other(side));
                    if(victim!=null)damage(unit,victim,2);
                    break;
                case "Elite Walker":
                    ArrayList<Unit> walkerTargets=new ArrayList<>(all(other(side)));
                    Collections.shuffle(walkerTargets,rng);
                    for(int i=0;i<Math.min(2,walkerTargets.size());i++){
                        Unit chosen=walkerTargets.get(i);
                        if(chosen.owner.board[chosen.row][chosen.column]==chosen)
                            damage(unit,chosen,2);
                    }
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
        if(pendingMovedUnit!=null){
            lastActionError="Relocate "+pendingMovedUnit.card.name+" before ending your turn.";
            return;
        }
        lastActionError="";
        Player current=humanTurn?human:cpu;
        endSide(current);
        completedTurns++;
        if(completedTurns==2){
            if(round==ROUNDS){
                finished=true;log("FINAL • YOU "+score(human)+" — "+score(cpu)+" CPU");
                return;
            }
            round++;completedTurns=0;
            human.points=ShadowRules.lightForRound(round);cpu.points=ShadowRules.lightForRound(round);
            draw(human,ShadowRules.NEW_ROUND_DRAW);draw(cpu,ShadowRules.NEW_ROUND_DRAW);
            humanTurn=humanFirst;
            log("ROUND "+round+" • "+round+" play points");
            startSide(humanTurn?human:cpu);
        }else{
            humanTurn=!humanTurn;
            startSide(humanTurn?human:cpu);
        }
    }
    /**
     * Executes ONE CPU card play. Called by the Android screen with a
     * short delay between plays so users can watch each placement/effect.
     * Returns false when no more legal actions are available.
     */
    public boolean cpuPlayOne(){
        if(finished||humanTurn)return false;
        if(pendingMovedUnit!=null){resolveCpuMove();return false;}
        int bestIndex=-1,bestRow=-1,bestColumn=-1;
        double bestValue=Double.NEGATIVE_INFINITY;
        for(int index=0;index<cpu.hand.size();index++){
            ShadowData.Card card=cpu.hand.get(index);
            if(card.numericCost()<1||card.numericCost()>cpu.points)continue;
            for(int row=0;row<ShadowRules.ROWS;row++)
                for(int column=0;column<COLUMNS;column++){
                    if(!isPlayablePlacement(cpu,index,row,column))continue;
                    double value=evaluateCpuPlacement(card,row,column);
                    if(value>bestValue){
                        bestValue=value;bestIndex=index;bestRow=row;bestColumn=column;
                    }
                }
        }
        if(bestIndex<0)return false;
        ShadowData.Card chosen=cpu.hand.get(bestIndex);
        Target target=chooseAI(cpu,chosen,bestRow,bestColumn);
        if(!play(cpu,bestIndex,bestRow,bestColumn,target)){
            log("CPU rejected move: "+lastActionError);
            return false;
        }
        return true;
    }
    /** Headless version used by tests and future difficulty simulators. */
    public void cpuTurn(){
        if(finished||humanTurn)return;
        int safety=40;
        while(safety-->0 && cpuPlayOne()){}
        if(!finished&&!humanTurn)endTurn();
    }
    private double evaluateCpuPlacement(ShadowData.Card card,int row,int column){
        double value=(Math.max(0,card.numericPower())+0.5)/(double)card.numericCost();
        String trigger=card.trigger.toLowerCase(Locale.ROOT);
        if(trigger.isEmpty())trigger=card.effect.toLowerCase(Locale.ROOT);
        if(trigger.contains("front row")&&row==1)value+=2.2;
        if(trigger.contains("back row")&&row==0)value+=2.2;
        if(trigger.contains("front row")&&row!=1)value-=2.8;
        if(trigger.contains("back row")&&row!=0)value-=2.8;
        int lineLength=projectedFactionLine(cpu,card,row,column);
        value+=Math.max(0,lineLength-1)*1.1;
        int needed=requiredFactionLine(card.name);
        if(needed>0 && lineLength>=needed)value+=4.0;
        else if(needed>0)value-=1.5;
        if(cpu.board[1-row][column]!=null)value+=0.9;
        if(column>0&&cpu.board[row][column-1]!=null)value+=0.3;
        if(column<COLUMNS-1&&cpu.board[row][column+1]!=null)value+=0.3;
        if(isTargeted(card) && !targetOptions(cpu,card,row,column).isEmpty())value+=1.3;
        if(isTargeted(card) && targetOptions(cpu,card,row,column).isEmpty())value-=1.0;
        return value+rng.nextDouble()*0.12;
    }
    /** Full deterministic match diagnostic for locally reproducing card/rule problems. */
    public String diagnosticReport(){
        StringBuilder report=new StringBuilder();
        report.append("SHADOWSHAPER DUELS • LOCAL SIMULATOR\n")
          .append("Seed: ").append(seed).append("\n")
          .append("Round: ").append(round).append("/").append(ROUNDS).append("\n")
          .append("Turn: ").append(turnLabel()).append("\n")
          .append("Offensive: ").append(humanFirst?"Player":"CPU").append("\n")
          .append("Score: ").append(score(human)).append(" / ").append(score(cpu)).append("\n")
          .append("Light: player ").append(human.points).append(", CPU ").append(cpu.points).append("\n")
          .append("Pending card effects: ").append(unresolvedEffects).append("\n")
          .append("Match complete: ").append(finished).append("\n\n");
        appendPlayerReport(report,"PLAYER",human);
        appendPlayerReport(report,"CPU",cpu);
        report.append("\nEVENTS\n");
        for(String event:journal)report.append(event).append('\n');
        return report.toString();
    }
    private void appendPlayerReport(StringBuilder report,String label,Player side){
        report.append(label).append(": Deck ").append(side.drawPile.size())
          .append(" / Hand ").append(side.hand.size()).append('\n');
        for(int row=0;row<2;row++){
            report.append(row==0?"BACK: ":"FRONT: ");
            for(int col=0;col<COLUMNS;col++){
                Unit u=side.board[row][col];
                report.append('[').append(col+1).append(' ');
                if(u!=null)report.append(u.card.name).append(" Power=").append(power(u))
                    .append(" Inspired=").append(u.inspired).append(" Suppressed=").append(u.suppressed);
                else report.append("EMPTY");
                report.append("] ");
            }
            report.append('\n');
        }
    }
    public String result(){
        if(!finished)return "";
        int left=score(human),right=score(cpu);
        return ShadowRules.result(left,right);
    }
}
