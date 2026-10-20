package com.tplatt.shadowshaper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Button;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Landscape tabletop sim. This recreates the functional layout and feel of
 * in-game Shadowshaper Duels without claiming copied proprietary UI assets.
 * All state mutations and card abilities live in ShadowEngine.
 */
public final class BattleActivity extends Activity {
    private static final int BG=0xff13100f, TABLE=0xff241b17, WOOD=0xff3a2920;
    private static final int GOLD=0xffe5bb70, BLUE=0xff8bc5ee, MUTED=0xffc3ae92;
    private static final int CREAM=0xfff7e9cb, ENEMY=0xffd09b8a;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private ShadowEngine game;
    private LinkedHashMap<String,ShadowData.Card> cards;
    private LinearLayout root,board,hand,sidePanel;
    private TextView roundText,scoreText,turnText,pointsText,deckText,hintText,events,opponentText,opponentHandText;
    private LinearLayout roundMarkers;
    private String enemyFaction="Mixed";
    private Button endButton;
    private int selectedHand=-1,lastShownLog=0;
    private int lastVisualRevision=-1,lastRoundShown=-1;
    private boolean animateNewestPlacement=false;
    private int pendingHand=-1,pendingRow=-1,pendingColumn=-1;
    private final ArrayList<ShadowEngine.Target> pendingTargets=new ArrayList<>();
    private boolean starting=false,dialogShowing=false;
    private final Runnable cpuAction=()->{
        if(game==null||game.finished||game.humanTurn||dialogShowing)return;
        if(game.cpuPlayOne()){
            selectedHand=-1;
            refresh(); // Show the CPU card and its Power before the next play.
            handler.postDelayed(cpuAction,560);
        }else{
            game.endTurn();
            refresh();finishIfOver();
            if(game!=null&&!game.finished&&!game.humanTurn)scheduleCpu();
        }
    };
    private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable panel(int fill,int outline,int radius){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{fill,fill});
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1),outline);return d;
    }
    private GradientDrawable tint(int start,int finish,int radius){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{start,finish});d.setCornerRadius(dp(radius));
        d.setStroke(dp(1),0xff68513b);return d;
    }
    private TextView text(String value,int size,boolean bold,int color){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);
        t.setGravity(Gravity.CENTER);t.setIncludeFontPadding(false);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private Button button(String value,Runnable action){
        Button b=new Button(this);b.setText(value);b.setTextColor(CREAM);
        b.setAllCaps(false);b.setTextSize(12);b.setPadding(dp(8),0,dp(8),0);
        b.setBackground(tint(0xff4c4a40,0xff282e35,8));
        b.setOnClickListener(v->action.run());return b;
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        cards=ShadowData.load(this);
        buildUi();selectDeck(true);
    }
    private void buildUi(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);setContentView(root);

        LinearLayout top=new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(5),dp(3),dp(6),dp(3));
        top.setBackground(tint(0xff4a3020,0xff241713,0));
        root.addView(top,new LinearLayout.LayoutParams(-1,dp(46)));
        top.addView(button("‹",()->pauseMenu()),new LinearLayout.LayoutParams(dp(46),-1));
        TextView title=text("SHADOWSHAPER  •  DUELS",17,true,CREAM);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(dp(14),0,0,0);
        top.addView(title,new LinearLayout.LayoutParams(0,-1,1));
        opponentHandText=text("CPU  ▤ 0",11,true,MUTED);
        top.addView(opponentHandText,new LinearLayout.LayoutParams(dp(93),-1));
        turnText=text("SOLO PRACTICE",11,true,GOLD);
        top.addView(turnText,new LinearLayout.LayoutParams(dp(170),-1));
        top.addView(button("☰",()->pauseMenu()),new LinearLayout.LayoutParams(dp(48),-1));

        LinearLayout middle=new LinearLayout(this);
        middle.setGravity(Gravity.CENTER_VERTICAL);middle.setBackgroundColor(TABLE);
        root.addView(middle,new LinearLayout.LayoutParams(-1,0,1));
        buildLeftHud(middle);
        board=new LinearLayout(this);board.setOrientation(LinearLayout.VERTICAL);
        board.setBackground(tint(0xff493226,0xff261f1b,4));
        board.setPadding(dp(4),dp(3),dp(4),dp(3));
        LinearLayout.LayoutParams fieldParams=new LinearLayout.LayoutParams(0,-1,1);
        fieldParams.setMargins(dp(5),dp(4),dp(5),dp(4));
        middle.addView(board,fieldParams);
        buildRightHud(middle);

        LinearLayout bottom=new LinearLayout(this);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setBackground(tint(0xff3b2821,0xff160e0e,0));
        root.addView(bottom,new LinearLayout.LayoutParams(-1,dp(116)));
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(5),dp(5),dp(5),dp(5));
        bottom.addView(info,new LinearLayout.LayoutParams(dp(116),-1));
        pointsText=text("1 / 1\nLIGHT",20,true,GOLD);info.addView(pointsText,new LinearLayout.LayoutParams(-1,0,1));
        deckText=text("DECK 15  •  HAND 5",10,false,MUTED);
        info.addView(deckText,new LinearLayout.LayoutParams(-1,dp(24)));
        HorizontalScrollView scroll=new HorizontalScrollView(this);
        scroll.setFillViewport(false);scroll.setHorizontalScrollBarEnabled(false);
        bottom.addView(scroll,new LinearLayout.LayoutParams(0,-1,1));
        hand=new LinearLayout(this);hand.setPadding(dp(10),dp(3),dp(16),dp(3));
        hand.setClipChildren(false);scroll.addView(hand);
        LinearLayout action=new LinearLayout(this);action.setOrientation(LinearLayout.VERTICAL);
        action.setPadding(dp(6),dp(7),dp(8),dp(7));
        bottom.addView(action,new LinearLayout.LayoutParams(dp(139),-1));
        endButton=button("END TURN  ›",()->endTurn());
        endButton.setBackground(tint(0xffb58c46,0xff654623,9));
        action.addView(endButton,new LinearLayout.LayoutParams(-1,0,1));
        hintText=text("Tap a card or drag it onto your field",10,false,MUTED);
        action.addView(hintText,new LinearLayout.LayoutParams(-1,dp(38)));
    }
    private void buildLeftHud(LinearLayout parent){
        LinearLayout hud=new LinearLayout(this);hud.setOrientation(LinearLayout.VERTICAL);
        hud.setPadding(dp(5),dp(9),dp(4),dp(9));
        hud.setBackground(tint(0xff3b291d,0xff191614,5));
        parent.addView(hud,new LinearLayout.LayoutParams(dp(111),-1));
        TextView e=text("OPPONENT",11,true,ENEMY);
        hud.addView(e,new LinearLayout.LayoutParams(-1,dp(28)));
        opponentText=text("CPU",16,true,CREAM);hud.addView(opponentText,new LinearLayout.LayoutParams(-1,0,1));
        roundText=text("ROUND\n1 / 6",18,true,GOLD);
        roundText.setBackground(panel(0xff30251e,0xffb5945c,11));
        LinearLayout.LayoutParams roundParams=new LinearLayout.LayoutParams(-1,dp(88));
        roundParams.setMargins(0,dp(6),0,dp(6));hud.addView(roundText,roundParams);
        roundMarkers=new LinearLayout(this);
        roundMarkers.setGravity(Gravity.CENTER);
        hud.addView(roundMarkers,new LinearLayout.LayoutParams(-1,dp(22)));
        TextView me=text("YOU",16,true,CREAM);hud.addView(me,new LinearLayout.LayoutParams(-1,0,1));
        hud.addView(text("OFFENSIVE / DEFENSIVE",9,false,BLUE),new LinearLayout.LayoutParams(-1,dp(20)));
    }
    private void buildRightHud(LinearLayout parent){
        sidePanel=new LinearLayout(this);sidePanel.setOrientation(LinearLayout.VERTICAL);
        sidePanel.setPadding(dp(5),dp(8),dp(6),dp(8));
        sidePanel.setBackground(tint(0xff38281d,0xff201713,5));
        parent.addView(sidePanel,new LinearLayout.LayoutParams(dp(151),-1));
        scoreText=text("0    :    0",24,true,GOLD);
        scoreText.setBackground(panel(0xff312820,0xffad894f,9));
        sidePanel.addView(scoreText,new LinearLayout.LayoutParams(-1,dp(58)));
        sidePanel.addView(text("YOUR POWER   :   CPU POWER",10,true,MUTED),new LinearLayout.LayoutParams(-1,dp(25)));
        TextView label=text("MATCH EVENTS",10,true,BLUE);label.setGravity(Gravity.CENTER_VERTICAL);
        sidePanel.addView(label,new LinearLayout.LayoutParams(-1,dp(23)));
        ScrollView logScroll=new ScrollView(this);
        sidePanel.addView(logScroll,new LinearLayout.LayoutParams(-1,0,1));
        events=text("Ready to duel.",10,false,CREAM);events.setGravity(Gravity.TOP|Gravity.LEFT);
        events.setPadding(dp(5),dp(4),dp(5),dp(4));
        logScroll.addView(events);
        sidePanel.addView(button("LOG  /  DETAILS",()->fullLog()),new LinearLayout.LayoutParams(-1,dp(35)));
    }
    private void selectDeck(boolean first){
        handler.removeCallbacks(cpuAction);dialogShowing=false;
        ArrayList<String> titles=new ArrayList<>();
        ArrayList<LinkedHashMap<String,Integer>> lists=new ArrayList<>();
        LinkedHashMap<String,Integer> practice=ShadowData.opponentDeck(cards);
        if(ShadowData.validate(practice,cards).isEmpty()){
            titles.add("Starter Practice Deck (15 cards)");lists.add(practice);
        }
        for(String name:ShadowData.deckNames(this)){
            LinkedHashMap<String,Integer> deck=ShadowData.readDeck(this,name);
            if(ShadowData.validate(deck,cards).isEmpty()){
                titles.add(name);lists.add(deck);
            }
        }
        String requested=getIntent().getStringExtra("selected_deck");
        if(requested!=null){
            int index=titles.indexOf(requested);
            getIntent().removeExtra("selected_deck");
            if(index>=0){begin(titles.get(index),lists.get(index));return;}
        }
        if(titles.isEmpty()){
            new AlertDialog.Builder(this).setTitle("Deck Construction")
                .setMessage("No legal practice or saved deck is available yet.")
                .setPositiveButton("Build Deck",(d,w)->{
                    Intent i=new Intent(this,MainActivity.class);
                    i.putExtra("start_page","deck");startActivity(i);finish();
                }).setNegativeButton("Home",(d,w)->finish()).show();return;
        }
        AlertDialog chooser=new AlertDialog.Builder(this)
          .setTitle(first?"Choose a Shadowshaper deck":"Start a new duel")
          .setItems(titles.toArray(new String[0]),(dialog,which)->begin(titles.get(which),lists.get(which)))
          .setNegativeButton(first?"Home":"Keep Playing",(d,w)->{if(first)finish();else {dialogShowing=false;scheduleCpu();}})
          .create();
        chooser.setCanceledOnTouchOutside(!first);chooser.show();
    }
    private void begin(String title,LinkedHashMap<String,Integer> deck){
        String[] opponents={"Mixed Practice","Fallen","Hive","Cabal","Humans"};
        new AlertDialog.Builder(this).setTitle("CHOOSE CPU OPPONENT")
          .setItems(opponents,(dialog,index)->{
              String selected=index==0?"Mixed":opponents[index];
              launchMatch(title,deck,selected);
          }).setNegativeButton("Back",(d,w)->selectDeck(false)).show();
    }
    private void launchMatch(String title,LinkedHashMap<String,Integer> deck,String faction){
        enemyFaction=faction;
        LinkedHashMap<String,Integer> enemy=ShadowData.opponentDeck(cards,faction);
        game=new ShadowEngine(cards,System.nanoTime());
        try{game.start(deck,enemy);}catch(Exception error){
            new AlertDialog.Builder(this).setTitle("Unable to start duel")
              .setMessage(error.getMessage()).setPositiveButton("Choose Deck",(d,w)->selectDeck(true)).show();return;
        }
        clearTargetSelection();
        lastVisualRevision=-1;lastRoundShown=-1;
        selectedHand=-1;hintText.setText("Tap to select • drag to place • hold to inspect");
        opponentText.setText("CPU\n"+enemyFaction.toUpperCase(Locale.ROOT));
        refresh();
        showCoin();
    }
    private void showCoin(){
        dialogShowing=true;
        handler.removeCallbacks(cpuAction);
        new AlertDialog.Builder(this)
          .setTitle(game.humanFirst?"OFFENSIVE • YOU GO FIRST":"DEFENSIVE • CPU GOES FIRST")
          .setMessage("Six rounds • 15-card decks\n"+
              "Build the greatest total Power on your side.\n"+
              "Opening hand: "+game.human.hand.size()+" cards.")
          .setPositiveButton("START DUEL",(d,w)->{dialogShowing=false;scheduleCpu();})
          .setCancelable(false)
          .show();
    }
    private void scheduleCpu(){
        handler.removeCallbacks(cpuAction);
        if(game!=null&&!game.finished&&!game.humanTurn&&!dialogShowing)handler.postDelayed(cpuAction,560);
    }
    private void endTurn(){
        if(game!=null && game.pendingMovedUnit!=null){
            hintText.setText("Move "+game.pendingMovedUnit.card.name+" to an empty gold-highlighted slot.");
            return;
        }
        if(!pendingTargets.isEmpty()){
            clearTargetSelection();
            hintText.setText("Target selection canceled.");
            refresh();
            return;
        }
        if(game==null||game.finished||!game.humanTurn)return;
        boolean playable=false;
        for(int i=0;i<game.human.hand.size();i++)
            if(game.affordable(game.human,i)){playable=true;break;}
        if(game.human.points>0 && playable){
            new AlertDialog.Builder(this)
                .setTitle("End turn with Light remaining?")
                .setMessage("You still have "+game.human.points+
                    " Light and a playable card in hand. Unspent Light does not carry over.")
                .setPositiveButton("END TURN",(d,w)->finishTurn())
                .setNegativeButton("KEEP PLAYING",null).show();
            return;
        }
        finishTurn();
    }
    private void finishTurn(){
        if(game==null||game.finished||!game.humanTurn)return;
        selectedHand=-1;game.endTurn();refresh();finishIfOver();scheduleCpu();
    }
    private void finishIfOver(){
        if(game==null||!game.finished)return;
        handler.removeCallbacks(cpuAction);
        new AlertDialog.Builder(this).setTitle(game.result())
          .setMessage("FINAL POWER\nYOU: "+game.score(game.human)+"\nCPU: "+game.score(game.cpu)+
              "\n\nUnverified effects encountered: "+game.unresolvedEffects+
              "\n\nResult is preliminary if any effects were unresolved.")
          .setPositiveButton("NEW MATCH",(d,w)->selectDeck(false))
          .setNeutralButton("REVIEW FIELD",(d,w)->refresh())
          .setNegativeButton("HOME",(d,w)->finish()).show();
    }
    private void refresh(){
        if(game==null)return;
        animateNewestPlacement=game.placementRevision!=lastVisualRevision;
        if(lastRoundShown!=game.round){
            lastRoundShown=game.round;
            roundText.setScaleX(1.08f);roundText.setScaleY(1.08f);
            roundText.animate().scaleX(1f).scaleY(1f).setDuration(310).start();
        }
        roundText.setText("ROUND\n"+game.round+" / "+ShadowEngine.ROUNDS);
        roundMarkers.removeAllViews();
        for(int n=1;n<=ShadowRules.ROUNDS;n++){
            TextView pip=text(String.valueOf(n),10,n==game.round,n==game.round?0xff1a1715:GOLD);
            pip.setBackground(panel(n==game.round?GOLD:0xff3d2d22,0xffba975e,12));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(14),dp(18));
            lp.setMargins(dp(1),0,dp(1),0);roundMarkers.addView(pip,lp);
        }
        opponentHandText.setText("CPU  ▤ "+game.cpu.hand.size());
        scoreText.setText(game.score(game.human)+"   :   "+game.score(game.cpu));
        turnText.setText(game.finished?"DUEL COMPLETE":
           game.humanTurn?"YOUR TURN  •  "+(game.humanFirst?"OFFENSIVE":"DEFENSIVE"):
           "CPU TURN  •  "+(game.humanFirst?"DEFENSIVE":"OFFENSIVE"));
        pointsText.setText(game.human.points+" / "+game.round+"\nLIGHT");
        deckText.setText("DECK "+game.human.drawPile.size()+"  •  HAND "+game.human.hand.size());
        endButton.setText(game.pendingMovedUnit!=null?"CHOOSE DESTINATION":
            pendingTargets.isEmpty()?"END TURN  ›":"CANCEL TARGET");
        endButton.setEnabled(!game.finished&&game.humanTurn&&game.pendingMovedUnit==null);
        if(game.pendingMovedUnit!=null){
            hintText.setText("Xuan Wei: move "+game.pendingMovedUnit.card.name+" to an empty slot.");
        }
        endButton.setAlpha(game.humanTurn&&!game.finished?1f:0.48f);
        board.removeAllViews();
        addBoardRow("ENEMY BACK",game.cpu,0);
        addBoardRow("ENEMY FRONT",game.cpu,1);
        addBoardRow("YOUR FRONT",game.human,1);
        addBoardRow("YOUR BACK",game.human,0);
        renderHand();
        renderEvents();
        lastVisualRevision=game.placementRevision;
        animateNewestPlacement=false;
    }
    private void addBoardRow(String title,ShadowEngine.Player owner,int index){
        boolean mine=owner==game.human;
        LinearLayout strip=new LinearLayout(this);
        strip.setGravity(Gravity.CENTER_VERTICAL);
        strip.setPadding(dp(1),dp(1),dp(1),dp(1));
        strip.setBackground(tint(mine?0xff3c2d29:0xff382720,mine?0xff241b1e:0xff231816,4));
        LinearLayout.LayoutParams height=new LinearLayout.LayoutParams(-1,0,1);
        height.setMargins(0,dp(1),0,dp(1));board.addView(strip,height);
        TextView rowLabel=text(title.replace(" ","\n"),8,true,mine?BLUE:ENEMY);
        strip.addView(rowLabel,new LinearLayout.LayoutParams(dp(42),-1));
        for(int col=0;col<ShadowEngine.COLUMNS;col++){
            final int row=index,column=col;
            final ShadowEngine.Unit unit=owner.board[row][col];
            FrameLayout slot=new FrameLayout(this);
            final boolean relocationSlot=game.pendingMovedUnit!=null &&
                game.pendingMoveCaster==game.human &&
                owner==game.pendingMovedUnit.owner && unit==null;
            final boolean targetHighlighted=isPendingTarget(owner,row,column);
            boolean highlighted=pendingTargets.isEmpty()&&mine && selectedHand>=0 &&
                game.isPlayablePlacement(game.human,selectedHand,row,column);
            slot.setBackground(panel((targetHighlighted||relocationSlot)?0xff557058:
                unit==null?(highlighted?0xff435a53:0xff211b1b):0xff4a3531,
                (targetHighlighted||relocationSlot)?0xffffe2a2:highlighted?0xffe4bf73:0xff9b774e,5));
            LinearLayout.LayoutParams width=new LinearLayout.LayoutParams(0,-1,1);
            width.setMargins(dp(2),dp(1),dp(2),dp(1));strip.addView(slot,width);
            if(unit==null){
                TextView empty=text(mine?"＋":"•",14,false,highlighted?BLUE:0xff536070);
                slot.addView(empty,new FrameLayout.LayoutParams(-1,-1));
            }else{
                ImageView art=new ImageView(this);
                art.setScaleType(ImageView.ScaleType.FIT_CENTER);
                FrameLayout.LayoutParams image=new FrameLayout.LayoutParams(-1,-1);
                image.setMargins(dp(2),dp(2),dp(2),dp(15));
                slot.addView(art,image);CardArt.bind(this,unit.card,art);
                TextView power=text(String.valueOf(game.power(unit)),11,true,CREAM);
                power.setBackground(panel(0xff465d79,0xffb4d6f4,13));
                FrameLayout.LayoutParams upper=new FrameLayout.LayoutParams(dp(27),dp(21),Gravity.TOP|Gravity.LEFT);
                slot.addView(power,upper);
                TextView cost=text(unit.card.cost,10,true,GOLD);
                cost.setBackground(panel(0xff356c96,0xffb4d6f4,13));
                FrameLayout.LayoutParams costPos=new FrameLayout.LayoutParams(dp(25),dp(21),Gravity.TOP|Gravity.RIGHT);
                slot.addView(cost,costPos);
                TextView footer=text(unit.card.name,9,true,CREAM);
                footer.setSingleLine(true);footer.setEllipsize(android.text.TextUtils.TruncateAt.END);
                footer.setBackgroundColor(0xcc151820);
                slot.addView(footer,new FrameLayout.LayoutParams(-1,dp(15),Gravity.BOTTOM));
                if(unit.inspired&&!unit.suppressed){
                    TextView star=text("✦",11,true,GOLD);
                    FrameLayout.LayoutParams mark=new FrameLayout.LayoutParams(dp(21),dp(18),Gravity.RIGHT|Gravity.BOTTOM);
                    mark.bottomMargin=dp(16);slot.addView(star,mark);
                }
                if(unit.suppressed)slot.setAlpha(0.62f);
                slot.setOnClickListener(v->{
                    if(targetHighlighted){
                        commitPlay(pendingHand,pendingRow,pendingColumn,
                            new ShadowEngine.Target(owner,row,column));
                    }else inspectUnit(unit);
                });
                slot.setOnLongClickListener(v->{inspectUnit(unit);return true;});
                if(animateNewestPlacement&&game.lastPlacedSide==(mine?0:1)&&game.lastPlacedRow==row&&game.lastPlacedColumn==column){
                    slot.setScaleX(.88f);slot.setScaleY(.88f);
                    slot.animate().scaleX(1f).scaleY(1f).setDuration(170).start();
                }
            }
            if(relocationSlot){
                slot.setOnClickListener(v->{
                    if(game.resolveMove(game.human,row,column)){
                        hintText.setText("Unit moved. Continue your turn.");
                        refresh();
                    }else hintText.setText(game.lastActionError);
                });
            }else if(mine&&unit==null){
                slot.setOnClickListener(v->{
                    if(!pendingTargets.isEmpty())return;
                    if(selectedHand>=0)attemptPlay(selectedHand,row,column);
                    else hintText.setText("Select a card in your hand before choosing a position.");
                });
                slot.setOnDragListener((v,drag)->{
                    if(game==null||game.finished||!game.humanTurn||!pendingTargets.isEmpty())return false;
                    switch(drag.getAction()){
                        case DragEvent.ACTION_DRAG_STARTED:
                            return drag.getClipDescription()!=null &&
                                drag.getClipDescription().hasMimeType("text/plain");
                        case DragEvent.ACTION_DRAG_ENTERED:
                            v.setBackground(panel(0xff52797f,0xffe6dd98,6));return true;
                        case DragEvent.ACTION_DRAG_EXITED:
                            v.setBackground(panel(0xff182533,0xff715843,5));return true;
                        case DragEvent.ACTION_DROP:
                            try{
                                int handIndex=Integer.parseInt(drag.getClipData().getItemAt(0).getText().toString());
                                attemptPlay(handIndex,row,column);
                            }catch(Exception ignored){}
                            return true;
                        case DragEvent.ACTION_DRAG_ENDED:return true;
                    }
                    return true;
                });
            }
        }
    }
    private void renderHand(){
        hand.removeAllViews();
        for(int i=0;i<game.human.hand.size();i++){
            final int cardIndex=i;
            ShadowData.Card card=game.human.hand.get(i);
            boolean chosen=i==selectedHand,playable=game.humanTurn&&game.affordable(game.human,i);
            FrameLayout frame=new FrameLayout(this);
            frame.setBackground(panel(chosen?0xff6a503b:playable?0xff453526:0xff2d2421,
                chosen?GOLD:playable?BLUE:0xff5a504a,7));
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(dp(83),dp(106));
            params.leftMargin=i==0?dp(0):-dp(9);params.rightMargin=dp(3);
            params.topMargin=chosen?0:dp(7);
            hand.addView(frame,params);
            ImageView art=new ImageView(this);
            art.setScaleType(ImageView.ScaleType.FIT_CENTER);
            FrameLayout.LayoutParams ip=new FrameLayout.LayoutParams(-1,-1);
            ip.setMargins(dp(4),dp(3),dp(4),dp(20));frame.addView(art,ip);
            CardArt.bind(this,card,art);
            TextView stats=text(card.power+"          "+card.cost,11,true,CREAM);
            stats.setBackgroundColor(0x993e312c);
            frame.addView(stats,new FrameLayout.LayoutParams(-1,dp(22),Gravity.TOP));
            TextView name=text(card.name,10,true,CREAM);
            name.setSingleLine(true);name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            name.setBackgroundColor(0xe5161f2b);
            frame.addView(name,new FrameLayout.LayoutParams(-1,dp(22),Gravity.BOTTOM));
            frame.setContentDescription(card.name+"; cost "+card.cost+"; Power "+card.power);
            frame.setOnClickListener(v->{
                if(game.finished||!game.humanTurn){inspectCard(card);return;}
                clearTargetSelection();
                selectedHand=(selectedHand==cardIndex?-1:cardIndex);
                hintText.setText(selectedHand<0?"Select card to play":"Tap or drag to an empty YOUR row slot");
                refresh();
            });
            frame.setOnLongClickListener(v->{inspectCard(card);return true;});
            frame.setOnTouchListener(new View.OnTouchListener(){
                float downX,downY;
                boolean dragged,longPressed,moved;
                Runnable pendingLongPress;
                final int slop=ViewConfiguration.get(BattleActivity.this).getScaledTouchSlop();
                private void cancelPress(){
                    if(pendingLongPress!=null){
                        handler.removeCallbacks(pendingLongPress);pendingLongPress=null;
                    }
                }
                private void resetView(View v){
                    v.animate().translationY(0f).scaleX(1f).scaleY(1f).setDuration(110).start();
                }
                @Override public boolean onTouch(View view,MotionEvent event){
                    switch(event.getActionMasked()){
                        case MotionEvent.ACTION_DOWN:
                            downX=event.getRawX();downY=event.getRawY();
                            dragged=false;longPressed=false;moved=false;
                            view.animate().translationY(-dp(7)).scaleX(1.04f).scaleY(1.04f)
                                .setDuration(120).start();
                            pendingLongPress=()->{
                                if(!dragged&&!moved){
                                    longPressed=true;resetView(view);view.performLongClick();
                                }
                            };
                            handler.postDelayed(pendingLongPress,ViewConfiguration.getLongPressTimeout());
                            return true; // The view must own DOWN to receive MOVE for dragging.
                        case MotionEvent.ACTION_MOVE:
                            float dx=event.getRawX()-downX,dy=event.getRawY()-downY;
                            if(!dragged&&!longPressed && Math.hypot(dx,dy)>slop){
                                moved=true;cancelPress();
                                if(game!=null&&game.humanTurn&&!game.finished&&
                                    game.affordable(game.human,cardIndex)){
                                    dragged=true;selectedHand=cardIndex;resetView(view);
                                    ClipData payload=ClipData.newPlainText(
                                        "shadow_hand",String.valueOf(cardIndex));
                                    view.startDragAndDrop(payload,new View.DragShadowBuilder(view),null,0);
                                }else resetView(view);
                            }
                            return true;
                        case MotionEvent.ACTION_UP:
                            cancelPress();resetView(view);
                            if(!dragged&&!longPressed&&!moved)view.performClick();
                            return true;
                        case MotionEvent.ACTION_CANCEL:
                            cancelPress();resetView(view);return true;
                    }
                    return true;
                }
            });
        }
        if(game.human.hand.isEmpty())hand.addView(text("EMPTY HAND",12,true,MUTED));
    }
    /** Target directly from the battlefield rather than using an obstructive list. */
    private boolean isPendingTarget(ShadowEngine.Player owner,int row,int column){
        for(ShadowEngine.Target target:pendingTargets)
            if(target.owner==owner&&target.row==row&&target.column==column)return true;
        return false;
    }
    private void clearTargetSelection(){
        pendingHand=-1;pendingRow=-1;pendingColumn=-1;
        pendingTargets.clear();
    }
    private void attemptPlay(int handIndex,int row,int col){
        if(game==null||game.finished||!game.humanTurn||handIndex<0||handIndex>=game.human.hand.size())return;
        if(game.pendingMovedUnit!=null){
            hintText.setText("Finish relocating "+game.pendingMovedUnit.card.name+" first.");
            return;
        }
        ShadowData.Card card=game.human.hand.get(handIndex);
        if(!game.isPlayablePlacement(game.human,handIndex,row,col)){
            String why=game.validatePlay(game.human,handIndex,row,col,null);
            hintText.setText(why.isEmpty()?"Cannot place card here.":why);
            return;
        }
        List<ShadowEngine.Target> options=game.targetOptions(game.human,card,row,col);
        if(game.isTargeted(card)&&!options.isEmpty()){
            clearTargetSelection();
            pendingHand=handIndex;pendingRow=row;pendingColumn=col;
            pendingTargets.addAll(options);
            selectedHand=handIndex;
            hintText.setText("Choose a gold-highlighted "+
                (options.get(0).owner==game.human?"ally":"opposing card")+
                " for "+card.name);
            refresh();
        }else{
            commitPlay(handIndex,row,col,null);
        }
    }
    private void commitPlay(int handIndex,int row,int col,ShadowEngine.Target target){
        if(game.play(game.human,handIndex,row,col,target)){
            clearTargetSelection();selectedHand=-1;
            hintText.setText("Card played • choose next card or end turn");
            refresh();finishIfOver();
        }else{
            hintText.setText(game.lastActionError.isEmpty()?
                "Unable to play card.":game.lastActionError);
        }
    }
    private void inspectUnit(ShadowEngine.Unit unit){
        if(game==null)return;
        String status=(unit.inspired?"✦ Inspired   ":"")+(unit.suppressed?"Suppressed  ":"");
        inspect(unit.card,"CURRENT POWER: "+game.power(unit)+"    "+
            "BONUSES: +"+unit.bonus+"\n"+status);
    }
    private void inspectCard(ShadowData.Card card){
        inspect(card,"Power "+card.power+"  •  Cost "+card.cost);
    }
    private void inspect(ShadowData.Card card,String state){
        ScrollView scroll=new ScrollView(this);
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16),dp(10),dp(16),dp(10));
        content.setBackground(tint(0xff252d35,0xff131b24,8));scroll.addView(content);
        ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        content.addView(image,new LinearLayout.LayoutParams(-1,dp(212)));
        CardArt.bind(this,card,image);
        TextView title=text(card.name,23,true,GOLD);content.addView(title);
        content.addView(text(card.rarity+"   •   "+card.faction,13,true,BLUE),
            new LinearLayout.LayoutParams(-1,dp(28)));
        content.addView(text(state,15,true,CREAM),new LinearLayout.LayoutParams(-1,dp(35)));
        TextView effect=text(card.effect,15,false,CREAM);effect.setGravity(Gravity.LEFT);
        effect.setPadding(dp(5),dp(8),dp(5),dp(14));content.addView(effect);
        AlertDialog d=new AlertDialog.Builder(this).setView(scroll)
            .setPositiveButton("CLOSE",null).create();d.show();
        if(d.getWindow()!=null)d.getWindow().setLayout(dp(435),-2);
    }
    private void renderEvents(){
        StringBuilder b=new StringBuilder();
        int start=Math.max(0,game.journal.size()-7);
        for(int i=start;i<game.journal.size();i++)
            b.append(game.journal.get(i)).append("\n");
        if(game.unresolvedEffects>0)
            b.append("\n⚠ ").append(game.unresolvedEffects).append(" effects pending.");
        events.setText(b.toString());
    }
    private void fullLog(){
        if(game==null)return;
        ScrollView scroll=new ScrollView(this);
        TextView all=text(android.text.TextUtils.join("\n",game.journal),13,false,CREAM);
        all.setGravity(Gravity.LEFT|Gravity.TOP);all.setPadding(dp(16),dp(12),dp(16),dp(12));
        scroll.addView(all);
        new AlertDialog.Builder(this).setTitle("Duel events").setView(scroll)
            .setNeutralButton("COPY REPORT",(d,w)->{
                android.content.ClipboardManager clipboard=
                    (android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("Shadowshaper match report",
                    game.diagnosticReport()));
                android.widget.Toast.makeText(this,"Match report copied",android.widget.Toast.LENGTH_SHORT).show();
            })
            .setPositiveButton("SHARE REPORT",(d,w)->{
                Intent share=new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_SUBJECT,"Shadowshaper duel test report");
                share.putExtra(Intent.EXTRA_TEXT,game.diagnosticReport());
                startActivity(Intent.createChooser(share,"Share match report"));
            })
            .setNegativeButton("CLOSE",null).show();
    }
    private void pauseMenu(){
        if(dialogShowing)return;
        dialogShowing=true;
        handler.removeCallbacks(cpuAction);
        final boolean[] resumeOnDismiss={true};
        String[] items={"Resume Duel","Restart / Change Deck","Rules","Return Home"};
        AlertDialog menu=new AlertDialog.Builder(this).setTitle("SHADOWSHAPER DUELS")
            .setItems(items,(d,selection)->{
                switch(selection){
                    case 1:
                        resumeOnDismiss[0]=false;
                        selectDeck(false);
                        break;
                    case 2:
                        resumeOnDismiss[0]=false;
                        startActivity(new Intent(this,RulesActivity.class));
                        dialogShowing=false; // onPause cancels AI; onResume will resume after Rules
                        break;
                    case 3:
                        resumeOnDismiss[0]=false;
                        new AlertDialog.Builder(this).setTitle("Leave the match?")
                            .setMessage("The current match will be lost.")
                            .setPositiveButton("LEAVE",(dialog,which)->finish())
                            .setNegativeButton("STAY",(dialog,which)->{
                                dialogShowing=false;scheduleCpu();
                            }).show();
                        break;
                    default:break;
                }
            }).create();
        menu.setOnDismissListener(d->{
            if(resumeOnDismiss[0]){
                dialogShowing=false;
                if(!isFinishing())scheduleCpu();
            }
        });
        menu.show();
    }
    @Override protected void onPause(){
        handler.removeCallbacks(cpuAction);
        super.onPause();
    }
    @Override protected void onResume(){
        super.onResume();
        if(game!=null&&!dialogShowing&&!game.finished)scheduleCpu();
    }
    @Override public void onBackPressed(){pauseMenu();}
    @Override protected void onDestroy(){handler.removeCallbacks(cpuAction);super.onDestroy();}
}
