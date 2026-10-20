package com.tplatt.shadowshaper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class BattleActivity extends Activity {
    // Prototype board layout: six positions per row, pending confirmation from the game UI.
    private static final int SLOTS=6, ROUNDS=6;
    private final int dark=0xff0c1324, panel=0xff21314e, accent=0xffa8c6ff;
    private final Random random=new Random();
    private final StringBuilder events=new StringBuilder();
    private LinkedHashMap<String,ShadowData.Card> catalog;
    private String deckName="";
    private Side player,enemy;
    private int round=1,turnsFinished=0, pendingEffects=0;
    private boolean playerFirst=true, playerTurn=true, finished=false, matchStarted=false;
    private LinearLayout root;
    private TextView status, score, notice, log;
    private LinearLayout field, hand;
    private Button endTurnButton;

    private static class Unit {
        ShadowData.Card card;
        int power;
        boolean inspired=false;
        Unit(ShadowData.Card c){card=c;power=Math.max(0,c.numericPower());}
    }
    private static class Side {
        ArrayList<ShadowData.Card> deck=new ArrayList<>(), hand=new ArrayList<>();
        Unit[][] board=new Unit[2][SLOTS]; // 0=back, 1=front
        int points=0;
    }
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable background(int color,int radius){
        GradientDrawable shape=new GradientDrawable();shape.setColor(color);
        shape.setCornerRadius(dp(radius));return shape;
    }
    private TextView label(String value,int size,boolean bold,int color){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);t.setPadding(dp(7),dp(6),dp(7),dp(6));
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private Button button(String value,Runnable onClick){
        Button b=new Button(this);b.setText(value);b.setTextColor(Color.WHITE);
        b.setAllCaps(false);b.setTextSize(13);b.setBackground(background(panel,10));
        b.setOnClickListener(v->onClick.run());return b;
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(dark);getWindow().setNavigationBarColor(dark);
        catalog=ShadowData.load(this);
        buildLayout();
        selectDeck();
    }
    private void buildLayout(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(dark);
        setContentView(root);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(header,new LinearLayout.LayoutParams(-1,dp(52)));
        header.addView(button("‹ Home",()->finish()),new LinearLayout.LayoutParams(dp(92),-1));
        TextView heading=label("SHADOWSHAPER • SIM BATTLE",16,true,accent);
        header.addView(heading,new LinearLayout.LayoutParams(0,-1,1));
        status=label("Choose a 15-card deck",13,true,Color.WHITE);
        root.addView(status);
        score=label("Player 0  •  CPU 0",18,true,Color.WHITE);score.setGravity(Gravity.CENTER);
        root.addView(score);
        ScrollView scroll=new ScrollView(this);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        field=new LinearLayout(this);field.setOrientation(LinearLayout.VERTICAL);
        field.setPadding(dp(6),dp(7),dp(6),dp(12));scroll.addView(field);
        notice=label("Selected abilities are implemented. Other card effects are marked as pending.",12,false,0xffffdbaa);
        root.addView(notice);
        TextView handTitle=label("YOUR HAND • Tap card to play",12,true,accent);
        root.addView(handTitle);
        HorizontalScrollView handScroll=new HorizontalScrollView(this);
        handScroll.setHorizontalScrollBarEnabled(false);
        root.addView(handScroll,new LinearLayout.LayoutParams(-1,dp(122)));
        hand=new LinearLayout(this);hand.setPadding(dp(4),dp(2),dp(4),dp(2));
        handScroll.addView(hand);
        LinearLayout actions=new LinearLayout(this);root.addView(actions,new LinearLayout.LayoutParams(-1,dp(53)));
        endTurnButton=button("End Turn",()->endPlayerTurn());
        actions.addView(endTurnButton,new LinearLayout.LayoutParams(0,-1,2));
        actions.addView(button("New Match",()->selectDeck()),new LinearLayout.LayoutParams(0,-1,1));
    }
    private void selectDeck(){
        ArrayList<String> legal=new ArrayList<>();
        ArrayList<String> names=ShadowData.deckNames(this);
        for(String name:names)if(ShadowData.validate(ShadowData.readDeck(this,name),catalog).isEmpty())legal.add(name);
        if(legal.isEmpty()){
            matchStarted=false;finished=true;
            status.setText("No legal saved deck");
            field.removeAllViews();
            field.addView(label("Create a legal 15-card deck to start Sim Battle.\n\nExactly 15 cards • maximum 2 copies each • 3 Exotic • 5 Mythic.",16,false,Color.WHITE));
            endTurnButton.setEnabled(false);hand.removeAllViews();
            new AlertDialog.Builder(this).setTitle("A deck is required")
              .setMessage("No valid saved deck found. Open Deck Construction and make a deck with exactly 15 playable cards.")
              .setPositiveButton("Open Deck Construction",(d,w)->{
                  Intent intent=new Intent(this,MainActivity.class);intent.putExtra("start_page","deck");startActivity(intent);
              }).setNegativeButton("Back to home",(d,w)->finish()).show();
            return;
        }
        String requested=getIntent().getStringExtra("selected_deck");
        if(requested!=null && legal.contains(requested)){
            getIntent().removeExtra("selected_deck");
            startMatch(requested);
            return;
        }
        String[] choices=legal.toArray(new String[0]);
        new AlertDialog.Builder(this).setTitle("Select your deck").setItems(choices,(d,i)->startMatch(choices[i]))
            .setNegativeButton(matchStarted?"Keep current battle":"Cancel",(d,w)->{if(!matchStarted)finish();}).show();
    }
    private void startMatch(String name){
        deckName=name;player=new Side();enemy=new Side();
        player.deck.addAll(ShadowData.expanded(ShadowData.readDeck(this,name),catalog));
        LinkedHashMap<String,Integer> cpu=ShadowData.opponentDeck(catalog);
        if(cpu.isEmpty()){
            matchStarted=false;status.setText("CPU deck unavailable");
            new AlertDialog.Builder(this).setTitle("Unable to start").setMessage("Not enough playable card records for the practice opponent.")
                .setPositiveButton("Back", (d,w)->finish()).show();return;
        }
        enemy.deck.addAll(ShadowData.expanded(cpu,catalog));
        Collections.shuffle(player.deck,random);Collections.shuffle(enemy.deck,random);
        playerFirst=random.nextBoolean();round=1;turnsFinished=0;pendingEffects=0;
        finished=false;matchStarted=true;events.setLength(0);
        draw(player,playerFirst?5:4);
        draw(enemy,playerFirst?4:5);
        playerTurn=playerFirst;
        startRound(true);
        event(deckName+" vs CPU • "+(playerFirst?"You act first.":"CPU acts first."));
        if(!playerTurn)runCpu();
        render();
    }
    private void startRound(boolean first){
        player.points=round;enemy.points=round;
        if(!first){draw(player,1);draw(enemy,1);}
        event("Round "+round+": both players have "+round+" play point"+(round==1?"":"s")+".");
    }
    private void draw(Side side,int count){
        for(int k=0;k<count&&!side.deck.isEmpty();k++)side.hand.add(side.deck.remove(side.deck.size()-1));
    }
    private void event(String message){
        if(events.length()>3500)events.delete(0,Math.min(1300,events.length()));
        events.append("• ").append(message).append("\n");
    }
    private int totalPower(Side side){
        int sum=0;for(Unit[] units:side.board)for(Unit u:units)if(u!=null)sum+=Math.max(0,u.power);
        return sum;
    }
    private boolean free(Side s,int row,int column){
        return row>=0&&row<2&&column>=0&&column<SLOTS&&s.board[row][column]==null;
    }
    private boolean play(Side side,Side opponent,int handIndex,int row,int slot){
        if(finished||handIndex<0||handIndex>=side.hand.size()||!free(side,row,slot))return false;
        ShadowData.Card card=side.hand.get(handIndex);
        int cost=card.numericCost();
        if(cost<0||cost>side.points)return false;
        side.points-=cost;side.hand.remove(handIndex);
        Unit unit=new Unit(card);side.board[row][slot]=unit;
        event((side==player?"You":"CPU")+" played "+card.name+" in "+(row==1?"Front":"Back")+
            " row, slot "+(slot+1)+".");
        applyAbility(side,opponent,unit,row,slot);
        cleanup(side);cleanup(opponent);
        return true;
    }
    private boolean hasFront(Side side){
        for(Unit unit:side.board[1])if(unit!=null)return true;return false;
    }
    private void damage(Side side,int row,int value){
        Unit best=null;
        if(row>=0){for(Unit u:side.board[row])if(u!=null&&(best==null||u.power>best.power))best=u;}
        else {for(Unit[] units:side.board)for(Unit u:units)if(u!=null&&(best==null||u.power>best.power))best=u;}
        if(best!=null){
            best.power=Math.max(0,best.power-value);
            event(best.card.name+" takes "+value+" damage.");
        }
    }
    private boolean noEffect(String value){
        return value==null||value.trim().isEmpty()||
            value.toLowerCase(Locale.ROOT).contains("no special effect");
    }
    private void applyAbility(Side own,Side other,Unit unit,int row,int column){
        String card=unit.card.name;
        switch(card){
            case "Sparrow":
                if(row==1){unit.power+=3;event("Sparrow gains +3 Power (Front Row).");}return;
            case "Resilient Captain":
                if(row==1){unit.power+=3;event("Resilient Captain gains +3 Power (Front Row).");}return;
            case "Elite Vandal":
                if(row==0&&hasFront(other)){unit.power+=3;event("Elite Vandal gains +3 Power (Back Row).");}return;
            case "Hexguard":
                unit.inspired=true;event("Hexguard becomes Inspired.");return;
            case "Tenacious Shielded Knight":
                int inspired=0;
                for(int col:new int[]{column-1,column+1})if(col>=0&&col<SLOTS){
                    Unit u=own.board[row][col];
                    if(u!=null && "Hive".equalsIgnoreCase(u.card.faction)){
                        u.inspired=true;inspired++;
                    }
                }
                event("Inspired granted to "+inspired+" adjacent Hive units.");return;
            case "Tracer Shank":
                if(row==0)damage(other,-1,3);else markPending(card,"requires Back Row");return;
            case "Vandal":
                damage(other,1,2);return;
            case "Wretch":
                if(row==1)damage(other,1,1);else markPending(card,"requires Front Row");return;
            case "Incendior":
                damage(other,1,3);return;
            case "Inquisitor":
                if(row==0)damage(other,-1,4);else markPending(card,"requires Back Row");return;
            default:
                if(!noEffect(unit.card.effect))markPending(card,"complex/targeted ability not yet simulated");
        }
    }
    private void markPending(String card,String reason){
        pendingEffects++;
        event("PENDING EFFECT: "+card+" ("+reason+").");
    }
    private void cleanup(Side side){
        for(int r=0;r<2;r++)for(int c=0;c<SLOTS;c++){
            Unit u=side.board[r][c];
            if(u!=null&&u.power<=0){event(u.card.name+" was destroyed.");side.board[r][c]=null;}
        }
    }
    private void applyEndTurn(Side side){
        for(Unit[] units:side.board)for(Unit u:units)
            if(u!=null&&u.inspired){u.power+=1;event(u.card.name+" gains +1 Power (Inspired).");}
    }
    private void endPlayerTurn(){
        if(finished||!matchStarted||!playerTurn)return;
        applyEndTurn(player);nextTurn();
    }
    private void nextTurn(){
        turnsFinished++;
        if(turnsFinished==2){
            if(round==ROUNDS){
                finished=true;int me=totalPower(player),cpu=totalPower(enemy);
                event("FINAL: "+me+" - "+cpu+" • "+(me>cpu?"VICTORY":me<cpu?"DEFEAT":"DRAW"));
                render();new AlertDialog.Builder(this)
                  .setTitle(me>cpu?"Victory!":me<cpu?"Defeat":"Draw")
                  .setMessage("Final Power\nYou: "+me+"\nCPU: "+cpu+"\n\nEffects pending implementation: "+pendingEffects)
                  .setPositiveButton("New Match",(d,w)->selectDeck())
                  .setNegativeButton("Review Field",null).show();return;
            }
            round++;turnsFinished=0;startRound(false);
            playerTurn=playerFirst;
        }else playerTurn=!playerTurn;
        if(!playerTurn)runCpu();
        render();
    }
    private int[] cpuSlot(ShadowData.Card card){
        String condition=card.trigger.toLowerCase(Locale.ROOT);
        if(condition.isEmpty()&&card.effect.toLowerCase(Locale.ROOT).startsWith("when played ("))
            condition=card.effect.substring(0,Math.min(40,card.effect.length())).toLowerCase(Locale.ROOT);
        boolean back=condition.contains("back row");
        boolean front=condition.contains("front row")&&!back;
        int[] preference=back?new int[]{0,1}:front?new int[]{1,0}:new int[]{1,0};
        for(int row:preference)for(int col=0;col<SLOTS;col++)if(free(enemy,row,col))return new int[]{row,col};
        return null;
    }
    private void runCpu(){
        if(finished)return;
        int safety=30;
        while(safety-->0){
            int best=-1;double bestValue=-1;
            for(int index=0;index<enemy.hand.size();index++){
                ShadowData.Card card=enemy.hand.get(index);
                int cost=card.numericCost();
                if(cost<=0||cost>enemy.points||cpuSlot(card)==null)continue;
                double value=card.numericPower()/(double)cost + random.nextDouble()*0.3;
                if(value>bestValue){bestValue=value;best=index;}
            }
            if(best<0)break;
            ShadowData.Card card=enemy.hand.get(best);
            int[] spot=cpuSlot(card);
            if(spot==null||!play(enemy,player,best,spot[0],spot[1]))break;
        }
        applyEndTurn(enemy);nextTurn();
    }
    private void choosePlacement(int handIndex){
        if(!playerTurn||finished||handIndex>=player.hand.size())return;
        ShadowData.Card card=player.hand.get(handIndex);
        if(card.numericCost()>player.points){
            new AlertDialog.Builder(this).setTitle(card.name).setMessage("This card costs "+card.cost+
                " but you have "+player.points+" play points remaining.")
                .setPositiveButton("OK",null).show();return;
        }
        ArrayList<String> locations=new ArrayList<>();
        ArrayList<int[]> slots=new ArrayList<>();
        for(int row:new int[]{1,0})for(int col=0;col<SLOTS;col++)if(free(player,row,col)){
            locations.add((row==1?"FRONT":"BACK")+" ROW · SLOT "+(col+1));
            slots.add(new int[]{row,col});
        }
        if(locations.isEmpty()){event("Your battlefield has no empty slots.");render();return;}
        new AlertDialog.Builder(this).setTitle("Place "+card.name+" ("+card.cost+" cost)")
        .setItems(locations.toArray(new String[0]),(d,selected)->{
            int[] slot=slots.get(selected);
            if(play(player,enemy,handIndex,slot[0],slot[1]))render();
        }).setNegativeButton("Cancel",null).show();
    }
    private void inspectUnit(Unit unit){
        new AlertDialog.Builder(this).setTitle(unit.card.name+" • "+unit.power+" Power")
          .setMessage("Faction: "+unit.card.faction+"\nRarity: "+unit.card.rarity+
          "\nOriginal Power: "+unit.card.power+"\nCost: "+unit.card.cost+
          "\nInspired: "+(unit.inspired?"Yes":"No")+
          "\n\n"+unit.card.effect)
          .setPositiveButton("Close",null).show();
    }
    private void renderRow(LinearLayout stack,String title,Side side,int row,boolean yours){
        TextView titleView=label(title,12,true,yours?accent:0xffffd3a2);stack.addView(titleView);
        HorizontalScrollView scroller=new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        stack.addView(scroller,new LinearLayout.LayoutParams(-1,dp(120)));
        LinearLayout tiles=new LinearLayout(this);scroller.addView(tiles);
        for(int i=0;i<SLOTS;i++){
            Unit unit=side.board[row][i];
            LinearLayout tile=new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setBackground(background(unit==null?0xff192640:0xff324767,9));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(57),dp(112));
            lp.setMargins(dp(2),dp(2),dp(2),dp(2));tiles.addView(tile,lp);
            if(unit!=null){
                ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                tile.addView(image,new LinearLayout.LayoutParams(-1,dp(75)));
                CardArt.bind(this,unit.card,image);
                TextView stats=label(unit.power+" • "+unit.card.name,9,true,Color.WHITE);
                stats.setGravity(Gravity.CENTER);stats.setMaxLines(2);
                tile.addView(stats);
                tile.setOnClickListener(v->inspectUnit(unit));
            }else{
                TextView empty=label("＋\nEMPTY",10,false,0xff7f92b5);
                empty.setGravity(Gravity.CENTER);tile.addView(empty);
            }
        }
    }
    private void render(){
        field.removeAllViews();hand.removeAllViews();
        if(!matchStarted)return;
        int me=totalPower(player),cpu=totalPower(enemy);
        status.setText("Round "+round+"/"+ROUNDS+"   •   "+(finished?"Match finished":
            playerTurn?"YOUR TURN":"CPU TURN")+"   •   "+(playerFirst?"Offensive":"Defensive"));
        score.setText("YOU "+me+"  ⚔  "+cpu+" CPU");
        TextView opponentInfo=label("CPU  •  "+enemy.hand.size()+" in hand  •  "+
            enemy.deck.size()+" in deck  •  "+enemy.points+" points",13,true,0xffffd3a2);
        field.addView(opponentInfo);
        renderRow(field,"CPU BACK ROW",enemy,0,false);
        renderRow(field,"CPU FRONT ROW",enemy,1,false);
        TextView divider=label("──── BATTLEFIELD ────",13,true,accent);
        divider.setGravity(Gravity.CENTER);field.addView(divider);
        renderRow(field,"YOUR FRONT ROW",player,1,true);
        renderRow(field,"YOUR BACK ROW",player,0,true);
        TextView playerInfo=label("YOU  •  "+player.hand.size()+" in hand  •  "+
            player.deck.size()+" in deck  •  "+player.points+"/"+round+" points",13,true,accent);
        field.addView(playerInfo);
        log=label(events.toString(),11,false,0xffc2d0e8);
        log.setBackground(background(0xff17253d,8));field.addView(log);
        for(int i=0;i<player.hand.size();i++){
            final int handIndex=i;ShadowData.Card card=player.hand.get(i);
            LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setBackground(background(
                playerTurn && card.numericCost()<=player.points ? 0xff364f78 : panel,10));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(102),dp(110));
            lp.setMargins(dp(3),dp(4),dp(3),dp(4));hand.addView(tile,lp);
            ImageView art=new ImageView(this);art.setScaleType(ImageView.ScaleType.FIT_CENTER);
            tile.addView(art,new LinearLayout.LayoutParams(-1,dp(69)));CardArt.bind(this,card,art);
            TextView title=label(card.name+"  •  "+card.cost+"/"+card.power,10,true,Color.WHITE);
            title.setGravity(Gravity.CENTER);title.setMaxLines(2);tile.addView(title);
            tile.setOnClickListener(v->choosePlacement(handIndex));
            tile.setOnLongClickListener(v->{
                new AlertDialog.Builder(this).setTitle(card.name)
                  .setMessage("Faction: "+card.faction+"\nRarity: "+card.rarity+
                    "\nCost: "+card.cost+"\nPower: "+card.power+"\n\n"+card.effect)
                  .setPositiveButton("Close",null).show();
                return true;
            });
        }
        if(player.hand.isEmpty())hand.addView(label("No cards in hand",13,false,0xffc4d0e3));
        endTurnButton.setEnabled(!finished&&playerTurn);
        endTurnButton.setAlpha(!finished&&playerTurn?1f:0.5f);
        notice.setText("Effects pending: "+pendingEffects+" • Basic play and scoring operational; "+
            "unimplemented effects appear in the event log.");
    }
}
