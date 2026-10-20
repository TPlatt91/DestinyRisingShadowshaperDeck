package com.tplatt.shadowshaper;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class RulesActivity extends Activity {
    private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView label(String value,int size,boolean bold,int color){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        t.setPadding(dp(2),dp(6),dp(2),dp(6));return t;
    }
    private void section(LinearLayout stack,String title,String body){
        TextView heading=label(title,20,true,0xffa8c6ff);
        LinearLayout.LayoutParams h=new LinearLayout.LayoutParams(-1,-2);h.topMargin=dp(18);stack.addView(heading,h);
        TextView copy=label(body,15,false,0xffdfebff);copy.setLineSpacing(dp(3),1.1f);stack.addView(copy);
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);getWindow().setStatusBarColor(0xff0c1324);
        getWindow().setNavigationBarColor(0xff0c1324);
        ScrollView s=new ScrollView(this);s.setBackgroundColor(0xff0c1324);setContentView(s);
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20),dp(22),dp(20),dp(35));s.addView(content);
        TextView header=label("SHADOWSHAPER RULES",27,true,Color.WHITE);content.addView(header);
        content.addView(label("Destiny: Rising • In-game duel basics",13,false,0xffa1b6d7));
        section(content,"Objective","Score more total Power than your opponent after round 6. Your score is the sum of surviving unit Power on your side of the battlefield. A tied Power total is a draw.");
        section(content,"Deck Construction","Exactly 15 cards per deck. Maximum 2 copies of a card with the same name, maximum 3 Exotic cards, and maximum 5 Mythic cards. Summoned-only tokens cannot be placed directly in your constructed deck.");
        section(content,"Rounds & Play Points","The game runs for six rounds. Round 1 grants 1 play point, round 2 grants 2, and so on through round 6. Each card has a cost. You can play multiple cards if you can afford them. Unspent play points do not carry into the next round.");
        section(content,"First / Second Player","A coin toss determines who acts first. Both players take a turn in every round. The second player receives the final action in round 6.");
        section(content,"Front & Back Row","Place a card into an empty slot in either row of your battlefield. Many effects activate only in a specific row; placing a card elsewhere does not satisfy that condition.");
        section(content,"Adjacency, Columns & Lines","Left and right are the neighboring slots in the SAME row. The column is the front/back pair at the same horizontal position. A same-faction Line requires the specified number of matching faction units in a row. The simulator currently treats a line as uninterrupted adjacent units; exact spacing is being checked against in-game examples.");
        section(content,"Targeting","Effects that select a unit require a valid target. Friendly buffs cannot select an enemy, and opposing damage effects cannot select your own unit. If an ability's condition has not been met, it cannot trigger. No card, Light, or target is consumed by an illegal placement.");
        section(content,"End Turn","Choose End Turn after playing cards. Unspent Light expires and each side takes one turn per round. Round 6's second player always takes the final action before the final Power score is calculated.");
        section(content,"Power & Removal","The top-left number on the card is Power. The top-right number is play cost. If a unit is reduced to 0 Power, it is destroyed. Positive and negative changes to Power affect the total battlefield score.");
        section(content,"Ability Keywords","When Played — triggers when a card enters the field.\nSustained — remains active while its unit is in play, unless suppressed.\nInspired — gains 1 Power at the end of its controller's turn.\nSuppressed — disables a unit's sustained/special effects and removes bonuses.\nFront Row / Back Row — effects with placement requirements.");
        section(content,"Simulator Accuracy","The six-round scoring, deck limits, faction/row restrictions, Light expenditure and suppression definitions follow documented Shadowshaper gameplay. Opening hand sizes (5 offensive / 4 defensive) are supported by player reports. The six-slot rows, contiguous Line interpretation, and one-card draw each new round are provisional. Individual complex card abilities remain under implementation; unresolved effects are identified in the battle log.");
        section(content,"Sources","These rules are based on Game8's Shadowshaper Duels guide and TodayInDestiny's Shadowshaper keywords. In-game updates take precedence over these references.");
        Button source=new Button(this);source.setText("Open gameplay reference");source.setAllCaps(false);
        content.addView(source);source.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://game8.co/games/Destiny-Rising/archives/546603"))));
    }
}
