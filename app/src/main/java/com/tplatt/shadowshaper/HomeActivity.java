package com.tplatt.shadowshaper;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class HomeActivity extends Activity {
    private final int ink=0xff171210, panel=0xff382820, pale=0xffe0cdb0, accent=0xfff0c678;
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private GradientDrawable background(int color,int radius){
        GradientDrawable result=new GradientDrawable();
        result.setColor(color);result.setCornerRadius(dp(radius));
        return result;
    }
    private TextView text(String value,int size,boolean strong,int color){
        TextView view=new TextView(this);view.setText(value);view.setTextSize(size);
        view.setTextColor(color);
        if(strong)view.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return view;
    }
    private void destination(LinearLayout root,String glyph,String name,String subtitle,Class<?> cls,String section){
        LinearLayout item=new LinearLayout(this);item.setGravity(Gravity.CENTER_VERTICAL);
        item.setOrientation(LinearLayout.HORIZONTAL);item.setPadding(dp(18),dp(19),dp(15),dp(19));
        item.setBackground(background(panel,15));
        LinearLayout.LayoutParams margin=new LinearLayout.LayoutParams(-1,dp(112));margin.bottomMargin=dp(13);
        root.addView(item,margin);
        TextView number=text(glyph,24,true,accent);number.setGravity(Gravity.CENTER);
        number.setBackground(background(0xff614631,12));
        item.addView(number,new LinearLayout.LayoutParams(dp(52),dp(54)));
        LinearLayout details=new LinearLayout(this);details.setPadding(dp(15),0,0,0);details.setOrientation(LinearLayout.VERTICAL);
        item.addView(details,new LinearLayout.LayoutParams(0,-2,1));
        details.addView(text(name,19,true,Color.WHITE));
        TextView caption=text(subtitle,12,false,pale);caption.setPadding(0,dp(5),0,0);
        details.addView(caption);
        TextView arrow=text("›",32,false,accent);item.addView(arrow);
        item.setContentDescription(name+". "+subtitle);
        item.setOnClickListener(v->{
            Intent intent=new Intent(this,cls);
            if(section!=null)intent.putExtra("start_page",section);
            startActivity(intent);
        });
    }
    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(ink);getWindow().setNavigationBarColor(ink);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(ink);
        setContentView(scroll);
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(17),dp(28),dp(17),dp(20));scroll.addView(content);
        TextView eyebrow=text("DESTINY: RISING",12,true,accent);eyebrow.setLetterSpacing(0.19f);
        content.addView(eyebrow);
        TextView title=text("SHADOWSHAPER\nDUELS",35,true,Color.WHITE);title.setLineSpacing(0,1.05f);
        LinearLayout.LayoutParams titleParams=new LinearLayout.LayoutParams(-1,-2);
        titleParams.topMargin=dp(10);content.addView(title,titleParams);
        TextView subtitle=text("DECK ARCHIVE & SINGLE-PLAYER SIMULATOR",12,false,pale);
        subtitle.setPadding(0,dp(10),0,dp(30));content.addView(subtitle);

        destination(content,"⚔","Sim Battle","Six-round solo match against AI",BattleActivity.class,null);
        destination(content,"▤","Deck Construction","Build, save, import and export decks",MainActivity.class,"deck");
        destination(content,"◈","Card Collection","Explore the Shadowshaper card archive",MainActivity.class,"collection");
        destination(content,"≡","Rules","Deck limits, scoring and game mechanics",RulesActivity.class,null);

        TextView footer=text("Fan-made companion • Offline deck data • Solo vs AI",11,false,0xffbaa389);
        footer.setGravity(Gravity.CENTER);footer.setPadding(0,dp(14),0,dp(10));content.addView(footer);
    }
}
