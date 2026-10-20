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
    private static final int BG=0xff0d151e, TABLE=0xff1d232a, WOOD=0xff32291f;
    private static final int GOLD=0xffdfb970, BLUE=0xff86baf0, MUTED=0xffadbaa9;
    private static final int CREAM=0xfff5ead4, ENEMY=0xffbd847d;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private ShadowEngine game;
    private LinkedHashMap<String,ShadowData.Card> cards;
    private LinearLayout root,board,hand,sidePanel;
    private TextView roundText,scoreText,turnText,pointsText,deckText,hintText,events,opponentText;
    private String enemyFaction="Mixed";
    private Button endButton;
    private int selectedHand=-1,lastShownLog=0;
    private boolean starting=false,dialogShowing=false;
    private final Runnable cpuAction=()->{
        if(game!=null&&!game.finished&&!game.humanTurn) {
            game.cpuTurn();selectedHand=-1;refresh();finishIfOver();
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
        top.setBackground(tint(0xff323237,0xff171f29,0));
        root.addView(top,new LinearLayout.LayoutParams(-1,dp(46)));
        top.addView(button("‹",()->pauseMenu()),new LinearLayout.LayoutParams(dp(46),-1));
        TextView title=text("SHADOWSHAPER  •  DUELS",17,true,CREAM);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(dp(14),0,0,0);
        top.addView(title,new LinearLayout.LayoutParams(0,-1,1));
        turnText=text("SOLO PRACTICE",11,true,MUTED);
        top.addView(turnText,new LinearLayout.LayoutParams(dp(170),-1));
        top.addView(button("☰",()->pauseMenu()),new LinearLayout.LayoutParams(dp(48),-1));

        LinearLayout middle=new LinearLayout(this);
        middle.setGravity(Gravity.CENTER_VERTICAL);middle.setBackgroundColor(TABLE);
        root.addView(middle,new LinearLayout.LayoutParams(-1,0,1));
        buildLeftHud(middle);
        board=new LinearLayout(this);board.setOrientation(LinearLayout.VERTICAL);
        board.setBackground(tint(0xff40352b,0xff22282d,4));
        board.setPadding(dp(4),dp(3),dp(4),dp(3));
        LinearLayout.LayoutParams fieldParams=new LinearLayout.LayoutParams(0,-1,1);
        fieldParams.setMargins(dp(5),dp(4),dp(5),dp(4));
        middle.addView(board,fieldParams);
        buildRightHud(middle);

        LinearLayout bottom=new LinearLayout(this);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setBackground(tint(0xff282d32,0xff101b27,0));
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
        endButton.setBackground(tint(0xff927042,0xff57422b,9));
        action.addView(endButton,new LinearLayout.LayoutParams(-1,0,1));
        hintText=text("Tap a card or drag it onto your field",10,false,MUTED);
        action.addView(hintText,new LinearLayout.LayoutParams(-1,dp(38)));
    }
    private void buildLeftHud(LinearLayout parent){
        LinearLayout hud=new LinearLayout(this);hud.setOrientation(LinearLayout.VERTICAL);
        hud.setPadding(dp(5),dp(9),dp(4),dp(9));
        hud.setBackground(tint(0xff202d39,0xff151f2a,5));
        parent.addView(hud,new LinearLayout.LayoutParams(dp(111),-1));
        TextView e=text("OPPONENT",11,true,ENEMY);
        hud.addView(e,new LinearLayout.LayoutParams(-1,dp(28)));
        opponentText=text("CPU",16,true,CREAM);hud.addView(opponentText,new LinearLayout.LayoutParams(-1,0,1));
        roundText=text("ROUND\n1 / 6",18,true,GOLD);
        roundText.setBackground(panel(0xff263440,0xff87734e,11));
        LinearLayout.LayoutParams roundParams=new LinearLayout.LayoutParams(-1,dp(88));
        roundParams.setMargins(0,dp(6),0,dp(6));hud.addView(roundText,roundParams);
        TextView me=text("YOU",16,true,CREAM);hud.addView(me,new LinearLayout.LayoutParams(-1,0,1));
        hud.addView(text("OFFENSIVE / DEFENSIVE",9,false,BLUE),new LinearLayout.LayoutParams(-1,dp(20)));
    }
    private void buildRightHud(LinearLayout parent){
        sidePanel=new LinearLayout(this);sidePanel.setOrientation(LinearLayout.VERTICAL);
        sidePanel.setPadding(dp(5),dp(8),dp(6),dp(8));
        sidePanel.setBackground(tint(0xff192b3a,0xff101d29,5));
        parent.addView(sidePanel,new LinearLayout.LayoutParams(dp(151),-1));
        scoreText=text("0    :    0",24,true,GOLD);
        scoreText.setBackground(panel(0xff212e38,0xff705a3c,9));
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
          .setNegativeButton(first?"Home":"Keep Playing",(d,w)->{if(first)finish();})
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
        selectedHand=-1;hintText.setText("Tap to select • drag to place • hold to inspect");
        opponentText.setText("CPU\n"+enemyFaction.toUpperCase(Locale.ROOT));
        refresh();
        showCoin();
    }
    private void showCoin(){
        new AlertDialog.Builder(this)
          .setTitle(game.humanFirst?"OFFENSIVE • YOU GO FIRST":"DEFENSIVE • CPU GOES FIRST")
          .setMessage("Six rounds • 15-card decks\n"+
              "Build the greatest total Power on your side.\n"+
              "Opening hand: "+game.human.hand.size()+" cards.")
          .setPositiveButton("START DUEL",(d,w)->scheduleCpu())
          .show();
    }
    private void scheduleCpu(){
        handler.removeCallbacks(cpuAction);
        if(game!=null&&!game.finished&&!game.humanTurn)handler.postDelayed(cpuAction,560);
    }
    private void endTurn(){
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
        roundText.setText("ROUND\n"+game.round+" / "+ShadowEngine.ROUNDS);
        scoreText.setText(game.score(game.human)+"   :   "+game.score(game.cpu));
        turnText.setText(game.finished?"DUEL COMPLETE":
           game.humanTurn?"YOUR TURN  •  "+(game.humanFirst?"OFFENSIVE":"DEFENSIVE"):
           "CPU TURN  •  "+(game.humanFirst?"DEFENSIVE":"OFFENSIVE"));
        pointsText.setText(game.human.points+" / "+game.round+"\nLIGHT");
        deckText.setText("DECK "+game.human.drawPile.size()+"  •  HAND "+game.human.hand.size());
        endButton.setEnabled(!game.finished&&game.humanTurn);
        endButton.setAlpha(game.humanTurn&&!game.finished?1f:0.48f);
        board.removeAllViews();
        addBoardRow("ENEMY BACK",game.cpu,0);
        addBoardRow("ENEMY FRONT",game.cpu,1);
        addBoardRow("YOUR FRONT",game.human,1);
        addBoardRow("YOUR BACK",game.human,0);
        renderHand();
        renderEvents();
    }
    private void addBoardRow(String title,ShadowEngine.Player owner,int index){
        boolean mine=owner==game.human;
        LinearLayout strip=new LinearLayout(this);
        strip.setGravity(Gravity.CENTER_VERTICAL);
        strip.setPadding(dp(1),dp(1),dp(1),dp(1));
        strip.setBackground(tint(mine?0xff273848:0xff382e2b,0xff1c232a,4));
        LinearLayout.LayoutParams height=new LinearLayout.LayoutParams(-1,0,1);
        height.setMargins(0,dp(1),0,dp(1));board.addView(strip,height);
        TextView rowLabel=text(title.replace(" ","\n"),8,true,mine?BLUE:ENEMY);
        strip.addView(rowLabel,new LinearLayout.LayoutParams(dp(42),-1));
        for(int col=0;col<ShadowEngine.COLUMNS;col++){
            final int row=index,column=col;
            final ShadowEngine.Unit unit=owner.board[row][col];
            FrameLayout slot=new FrameLayout(this);
            boolean highlighted=mine && unit==null && game.humanTurn && selectedHand>=0 &&
                selectedHand<game.human.hand.size()&&game.affordable(game.human,selectedHand);
            slot.setBackground(panel(unit==null?(highlighted?0xff426c75:0xff182533):0xff2e3e4c,
                highlighted?0xffb1d8cf:0xff715843,5));
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
                power.setBackground(panel(0xff386495,0xffa3cde6,6));
                FrameLayout.LayoutParams upper=new FrameLayout.LayoutParams(dp(27),dp(21),Gravity.TOP|Gravity.LEFT);
                slot.addView(power,upper);
                TextView cost=text(unit.card.cost,10,true,GOLD);
                cost.setBackground(panel(0xff20252e,0xffb6975d,6));
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
                slot.setOnClickListener(v->inspectUnit(unit));
                if(game.lastPlacedSide==(mine?0:1)&&game.lastPlacedRow==row&&game.lastPlacedColumn==column){
                    slot.setScaleX(.88f);slot.setScaleY(.88f);
                    slot.animate().scaleX(1f).scaleY(1f).setDuration(170).start();
                }
            }
            if(mine&&unit==null){
                slot.setOnClickListener(v->{
                    if(selectedHand>=0)attemptPlay(selectedHand,row,column);
                });
                slot.setOnDragListener((v,drag)->{
                    if(game==null||game.finished||!game.humanTurn)return false;
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
            frame.setBackground(panel(chosen?0xff566886:playable?0xff364557:0xff212c38,
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
                selectedHand=(selectedHand==cardIndex?-1:cardIndex);
                hintText.setText(selectedHand<0?"Select card to play":"Tap or drag to an empty YOUR row slot");
                refresh();
            });
            frame.setOnLongClickListener(v->{inspectCard(card);return true;});
            frame.setOnTouchListener(new View.OnTouchListener(){
                float downX,downY;boolean dragged;
                @Override public boolean onTouch(View view,MotionEvent event){
                    switch(event.getActionMasked()){
                        case MotionEvent.ACTION_DOWN:
                            downX=event.getRawX();downY=event.getRawY();dragged=false;
                            return false; // Let View dispatch click and native long-press detection.
                        case MotionEvent.ACTION_MOVE:
                            if(!dragged && game!=null&&game.humanTurn&&!game.finished &&
                                game.affordable(game.human,cardIndex) &&
                                Math.hypot(event.getRawX()-downX,event.getRawY()-downY)>dp(10)){
                                dragged=true;selectedHand=cardIndex;
                                ClipData dragData=ClipData.newPlainText("shadow_hand",String.valueOf(cardIndex));
                                view.startDragAndDrop(dragData,new View.DragShadowBuilder(view),null,0);
                                return true;
                            }
                            return dragged; // Non-drag moves still permit long-press inspection.
                        case MotionEvent.ACTION_UP:
                            return dragged; // Native click fires for non-drag taps.
                        case MotionEvent.ACTION_CANCEL:return dragged;
                    }return false;
                }
            });
        }
        if(game.human.hand.isEmpty())hand.addView(text("EMPTY HAND",12,true,MUTED));
    }
    private void attemptPlay(int handIndex,int row,int col){
        if(game==null||game.finished||!game.humanTurn||handIndex<0||handIndex>=game.human.hand.size())return;
        ShadowData.Card card=game.human.hand.get(handIndex);
        if(!game.affordable(game.human,handIndex)){
            hintText.setText("Not enough Light to play "+card.name);return;
        }
        List<ShadowEngine.Target> options=game.targetOptions(game.human,card,row,col);
        if(game.isTargeted(card)&&!options.isEmpty()){
            String[] labels=new String[options.size()];
            for(int i=0;i<options.size();i++){
                ShadowEngine.Target target=options.get(i);
                ShadowEngine.Unit unit=target.unit();
                labels[i]=unit.card.name+"  •  "+game.power(unit)+" Power"+
                    "  •  "+(target.row==1?"Front":"Back")+" "+(target.column+1);
            }
            new AlertDialog.Builder(this)
                .setTitle(card.name+" • Choose "+(options.get(0).owner==game.human?"ally":"target"))
                .setItems(labels,(d,index)->commitPlay(handIndex,row,col,options.get(index)))
                .setNegativeButton("CANCEL",null).show();
        }else{
            commitPlay(handIndex,row,col,null);
        }
    }
    private void commitPlay(int handIndex,int row,int col,ShadowEngine.Target target){
        if(game.play(game.human,handIndex,row,col,target)){
            selectedHand=-1;
            refresh();finishIfOver();
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
            .setPositiveButton("CLOSE",null).show();
    }
    private void pauseMenu(){
        String[] items={"Resume Duel","Restart / Change Deck","Rules","Return Home"};
        new AlertDialog.Builder(this).setTitle("SHADOWSHAPER DUELS")
            .setItems(items,(d,selection)->{
                switch(selection){
                    case 1: selectDeck(false);break;
                    case 2: startActivity(new Intent(this,RulesActivity.class));break;
                    case 3: new AlertDialog.Builder(this).setTitle("Leave the match?")
                        .setMessage("The current match will be lost.")
                        .setPositiveButton("LEAVE",(dialog,which)->finish())
                        .setNegativeButton("STAY",null).show();break;
                    default:break;
                }
            }).show();
    }
    @Override public void onBackPressed(){pauseMenu();}
    @Override protected void onDestroy(){handler.removeCallbacks(cpuAction);super.onDestroy();}
}
