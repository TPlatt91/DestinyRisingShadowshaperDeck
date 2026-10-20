package com.tplatt.shadowshaper;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import android.text.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private final ArrayList<Card> cards = new ArrayList<>();
    private final LinkedHashMap<String,Integer> active = new LinkedHashMap<>();
    private final ArrayList<String> deckNames = new ArrayList<>();
    private final ExecutorService images = Executors.newFixedThreadPool(4);
    private final HashMap<String,Bitmap> memory = new HashMap<>();
    private LinearLayout root, body, top;
    private EditText search;
    private String currentDeck = "My Deck";
    private boolean showingDeck = false;
    private int background = Color.rgb(24,19,18), panel = Color.rgb(55,40,32), accent = Color.rgb(224,184,112);

    static class Card {
        String name, effect, slug, image;
        String faction="", rarity="", cost="", power="", cardNumber="";
        String classification="", trigger="", acquisition="", source="", notes="";
        Card(String n,String e) {
            name=n; effect=e;
            slug=n.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","_").replaceAll("^_|_$","");
            image="https://www.media.todayindestiny.com/images/rising/shadowshaper_duels/card/"+slug+".webp";
            source="https://www.todayindestiny.com/rising/shadowshaper_duels/cards/"+slug;
        }
        String shown(String value) { return value == null || value.trim().isEmpty() ? "Not documented" : value; }
    }
    // Preserve the original TodayInDestiny descriptions for the first 34 cards.
    // Supplemental cards use the community list until primary card-face data can be sourced.
    private void readSupplemental(String filename) {
        try(BufferedReader in=new BufferedReader(new InputStreamReader(getAssets().open(filename),StandardCharsets.UTF_8))){
            String line;
            while((line=in.readLine())!=null) {
                String[] p=line.split("\\|",6);
                if(p.length!=6 || getCard(p[0])!=null) continue;
                Card card=new Card(p[0],p[5]);
                card.rarity=p[1]; card.faction=p[2];card.cost=p[3];card.power=p[4];
                card.image="";
                card.source="https://www.reddit.com/r/ShadowshaperDuels/comments/1nklv84/shadowshaper_card_list/";
                String effect=card.effect;
                int colon=effect.indexOf(':');
                if(colon>=0) {
                    String prefix=effect.substring(0,colon);
                    if(prefix.startsWith("When Played")||prefix.startsWith("Sustained")||
                       prefix.startsWith("Upon")||prefix.startsWith("At the end"))
                        card.trigger=prefix;
                }
                if(card.cost.equalsIgnoreCase("Token"))card.classification="Summoned unit";
                cards.add(card);
            }
        } catch(Exception e){Toast.makeText(this,"Could not load "+filename+": "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private void readDetails(){
        try(BufferedReader in=new BufferedReader(new InputStreamReader(getAssets().open("card_details.tsv"),StandardCharsets.UTF_8))){
            String line;
            while((line=in.readLine())!=null) {
                String[] p=line.split("\\|",-1);
                if(p.length<10)continue;
                Card card=getCard(p[0]);if(card==null)continue;
                card.faction=p[1];card.rarity=p[2];card.cost=p[3];card.power=p[4];
                card.classification=p[5];card.trigger=p[6];card.acquisition=p[7];card.notes=p[8];card.cardNumber=p[9];
            }
        }catch(Exception e){Toast.makeText(this,"Could not load card details: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
        try(BufferedReader in = new BufferedReader(new InputStreamReader(getAssets().open("cards.tsv"), StandardCharsets.UTF_8))) {
            String line; while((line=in.readLine())!=null) {
                String[] p=line.split("\\|",2);
                if(p.length==2)cards.add(new Card(p[0],p[1]));
            }
        }catch(Exception e){Toast.makeText(this,"Catalog load error: "+e.getMessage(),Toast.LENGTH_LONG).show();}
        readSupplemental("community_cards.tsv");
        readSupplemental("expanded_cards.tsv");
        readDetails();
        Collections.sort(cards,(a,b)->a.name.compareToIgnoreCase(b.name));
        loadNames();loadDeck(currentDeck);frame();
        if("deck".equals(getIntent().getStringExtra("start_page")))showDeck();else showCollection();
    }
    private int dp(float d){ return (int)(getResources().getDisplayMetrics().density*d+0.5f); }
    private TextView label(String text,int size,boolean bold) {
        TextView v = new TextView(this);v.setText(text);v.setTextSize(size);v.setTextColor(Color.WHITE);v.setPadding(dp(10),dp(8),dp(10),dp(8));
        if(bold)v.setTypeface(null,1);return v;
    }
    private GradientDrawable bg(int color,int radius) { GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d; }
    private Button button(String text,Runnable action) {
        Button b=new Button(this);b.setText(text);b.setTextColor(Color.WHITE);b.setAllCaps(false);
        b.setBackground(bg(panel,12)); b.setOnClickListener(v->action.run());return b;
    }
    private void frame(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(background);setContentView(root);
        LinearLayout masthead=new LinearLayout(this);masthead.setGravity(Gravity.CENTER_VERTICAL);root.addView(masthead);
        masthead.addView(button("‹ Home",()->finish()),new LinearLayout.LayoutParams(dp(87),dp(48)));
        TextView title=label("SHADOWSHAPER DUELS",18,true);title.setTextColor(accent);masthead.addView(title);
        LinearLayout tabs=new LinearLayout(this);root.addView(tabs);
        Button catalog=button("Card Collection",()->showCollection());tabs.addView(catalog,new LinearLayout.LayoutParams(0,dp(48),1));
        Button decks=button("Deck Builder",()->showDeck());tabs.addView(decks,new LinearLayout.LayoutParams(0,dp(48),1));
        body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
    }
    private void clear(){body.removeAllViews();}
    private ScrollView scroll(LinearLayout parent) {
        ScrollView s=new ScrollView(this);parent.addView(s,new LinearLayout.LayoutParams(-1,0,1));
        return s;
    }
    private LinearLayout column(ScrollView s) {
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(dp(8),dp(4),dp(8),dp(12));s.addView(list);return list;
    }
    private Card getCard(String name){for(Card c:cards)if(c.name.equalsIgnoreCase(name))return c;return null;}
    private String count(){int total=0;for(int n:active.values())total+=n;return String.valueOf(total);}
    private void showCollection(){
        showingDeck=false;clear();
        search=new EditText(this);search.setSingleLine();search.setTextColor(Color.WHITE);search.setHintTextColor(0xffb4bdd4);
        search.setHint("Search name, faction, rarity, or effect…");search.setPadding(dp(16),dp(8),dp(16),dp(8));body.addView(search);
        TextView heading=label(cards.size()+" catalog records  •  select a card to view/add",12,false);body.addView(heading);
        ScrollView s=scroll(body);LinearLayout list=column(s);
        Runnable refresh=()->{
            list.removeAllViews();String q=search.getText().toString().toLowerCase(Locale.ROOT).trim();
            int displayed=0;
            for(Card c:cards)if(c.name.toLowerCase(Locale.ROOT).contains(q)||c.effect.toLowerCase(Locale.ROOT).contains(q)||c.faction.toLowerCase(Locale.ROOT).contains(q)||c.rarity.toLowerCase(Locale.ROOT).contains(q)){
                addCardTile(list,c);displayed++;
            }
            heading.setText(displayed+" / "+cards.size()+" catalog records  •  tap card to view/add");
        };
        search.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int count,int after){}
            public void onTextChanged(CharSequence s,int st,int before,int count){refresh.run();}
            public void afterTextChanged(Editable e){}
        });refresh.run();
    }
    private void addCardTile(LinearLayout list,Card c){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8),dp(7),dp(8),dp(7));row.setBackground(bg(panel,12));
        LinearLayout.LayoutParams outer=new LinearLayout.LayoutParams(-1,dp(140));
        outer.bottomMargin=dp(7);list.addView(row,outer);
        ImageView art=new ImageView(this);art.setScaleType(ImageView.ScaleType.FIT_CENTER);
        row.addView(art,new LinearLayout.LayoutParams(dp(79),dp(118)));loadImage(c,art);
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);
        row.addView(info,new LinearLayout.LayoutParams(0,-2,1));
        info.addView(label(c.name,16,true));
        TextView stats=label(c.shown(c.rarity)+" · "+c.shown(c.faction)+
            " · POW "+c.shown(c.power)+" · COST "+c.shown(c.cost),11,false);
        stats.setTextColor(accent);info.addView(stats);
        TextView effect=label(c.effect,12,false);effect.setTextColor(0xffccd6eb);
        effect.setMaxLines(2);info.addView(effect);
        TextView owned=label("In deck: "+active.getOrDefault(c.name,0)+
            (c.image.isEmpty() && bundledImageName(c).isEmpty() ? "   •   Image needed" : ""),11,false);
        owned.setTextColor(0xffb7c7e0);info.addView(owned);
        row.setOnClickListener(v->inspect(c));
    }
    private TextView section(LinearLayout parent,String heading,String value){
        TextView title=label(heading.toUpperCase(Locale.ROOT),11,true);
        title.setTextColor(accent);parent.addView(title);
        TextView text=label(value,15,false);text.setTextColor(0xffe2e9f6);
        parent.addView(text);return text;
    }
    private void metric(LinearLayout parent,String heading,String value){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(bg(panel,10));box.setPadding(dp(4),dp(3),dp(4),dp(3));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(76),1);
        lp.setMargins(dp(3),0,dp(3),0);parent.addView(box,lp);
        TextView label=label(heading,11,true);label.setTextColor(0xffb9cbec);
        box.addView(label);
        TextView item=label(value,19,true);item.setTextColor(Color.WHITE);
        box.addView(item);
    }
    private void inspect(Card c){
        ScrollView scroller=new ScrollView(this);scroller.setFillViewport(false);
        LinearLayout stack=new LinearLayout(this);stack.setPadding(dp(12),dp(10),dp(12),dp(14));
        stack.setOrientation(LinearLayout.VERTICAL);stack.setBackgroundColor(background);
        scroller.addView(stack);
        if(!c.image.isEmpty()||!bundledImageName(c).isEmpty()){
            ImageView art=new ImageView(this);art.setScaleType(ImageView.ScaleType.FIT_CENTER);
            stack.addView(art,new LinearLayout.LayoutParams(-1,dp(360)));loadImage(c,art);
            TextView imageNote=label("Card-face image · bundled artwork takes priority over online cache",11,false);
            imageNote.setGravity(Gravity.CENTER);imageNote.setTextColor(0xffaab7ca);stack.addView(imageNote);
        } else {
            TextView absent=label("CARD ARTWORK NOT YET VERIFIED\nThis card is documented, but no original image is available yet. Add card_art/" + c.slug + ".webp to this project.",14,true);
            absent.setGravity(Gravity.CENTER);absent.setTextColor(0xffc2cde3);
            absent.setBackground(bg(panel,12));
            stack.addView(absent,new LinearLayout.LayoutParams(-1,dp(180)));
        }
        TextView title=label(c.name,22,true);title.setGravity(Gravity.CENTER);
        stack.addView(title);
        TextView banner=label(c.shown(c.rarity).toUpperCase(Locale.ROOT)+
            "  •  "+c.shown(c.faction),14,true);
        banner.setGravity(Gravity.CENTER);banner.setTextColor(accent);stack.addView(banner);
        LinearLayout metrics=new LinearLayout(this);metrics.setOrientation(LinearLayout.HORIZONTAL);
        stack.addView(metrics,new LinearLayout.LayoutParams(-1,dp(80)));
        metric(metrics,"POWER",c.shown(c.power));metric(metrics,"COST",c.shown(c.cost));
        metric(metrics,"COPIES",String.valueOf(active.getOrDefault(c.name,0)));
        if(!c.cardNumber.isEmpty())section(stack,"Card number",c.cardNumber);
        if(!c.classification.isEmpty())section(stack,"Card class",c.classification);
        section(stack,"Card effect",c.effect.isEmpty() ? "No effect recorded" : c.effect);
        section(stack,"Sim Battle implementation",
            ShadowEffectCoverage.status(c.name,c.effect,c.cost,c.power));
        if(!c.trigger.isEmpty())section(stack,"Effect type / positioning",c.trigger);
        if(!c.acquisition.isEmpty())section(stack,"How to obtain",c.acquisition);
        if(!c.notes.isEmpty())section(stack,"Additional information",c.notes);
        TextView source=label("Reference: "+
            (c.source.contains("todayindestiny.com")?"TodayInDestiny individual card entry":
             "Community Shadowshaper card list; statistics may need game verification"),12,false);
        source.setTextColor(0xffb7c7de);stack.addView(source);
        if(!c.source.isEmpty())stack.addView(button("Open card reference",()->{
            try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(c.source)));}
            catch(Exception ex){toast("Could not open reference");}
        }),new LinearLayout.LayoutParams(-1,dp(46)));
        if(c.cardNumber.isEmpty()){
            TextView number=label("Printed card number: none confirmed by current references.",11,false);
            number.setTextColor(0xffaab5ca);stack.addView(number);
        }
        AlertDialog dialog=new AlertDialog.Builder(this)
          .setTitle("Card details").setView(scroller)
          .setPositiveButton("Add copy",(d,w)->{String problem=addProblem(c);if(!problem.isEmpty()){toast(problem);return;}active.put(c.name,active.getOrDefault(c.name,0)+1);saveDeck();if(showingDeck)showDeck();else showCollection();})
          .setNeutralButton("Remove copy",(d,w)->{int n=active.getOrDefault(c.name,0);if(n<=1)active.remove(c.name);else active.put(c.name,n-1);saveDeck();if(showingDeck)showDeck();else showCollection();})
          .setNegativeButton("Close",null).create();
        dialog.show();
        if(dialog.getWindow()!=null)dialog.getWindow().setLayout(-1,-2);
    }
    private int whole(String value){
        try {return Integer.parseInt(value);}catch(Exception ignored){return -1;}
    }
    private boolean playable(Card card){
        return whole(card.cost)>0&&whole(card.power)>=0 &&
            !"Summoned unit".equalsIgnoreCase(card.classification);
    }
    private int rarityCount(String rarity){
        int count=0;for(Map.Entry<String,Integer> entry:active.entrySet()){
            Card card=getCard(entry.getKey());
            if(card!=null&&card.rarity.equalsIgnoreCase(rarity))count+=entry.getValue();
        }
        return count;
    }
    private String addProblem(Card card){
        if(!playable(card))return card.name+" cannot be decked (token or missing verified stats).";
        if(active.getOrDefault(card.name,0)>=2)return "Maximum 2 copies of a card.";
        int total=0;for(int n:active.values())total+=n;
        if(total>=15)return "A Shadowshaper deck has exactly 15 cards.";
        if(card.rarity.equalsIgnoreCase("Exotic")&&rarityCount("Exotic")>=3)
            return "Maximum 3 Exotic cards per deck.";
        if(card.rarity.equalsIgnoreCase("Mythic")&&rarityCount("Mythic")>=5)
            return "Maximum 5 Mythic cards per deck.";
        return "";
    }
    private String deckStatus(){
        int total=0;for(int n:active.values())total+=n;
        for(Map.Entry<String,Integer> e:active.entrySet()){
            Card card=getCard(e.getKey());
            if(card==null||!playable(card))return "Contains a token or incomplete card: "+e.getKey();
            if(e.getValue()>2)return "Too many copies of "+e.getKey()+" (max 2).";
        }
        int ex=rarityCount("Exotic"),my=rarityCount("Mythic");
        if(ex>3)return ex+" Exotic cards (max 3).";
        if(my>5)return my+" Mythic cards (max 5).";
        if(total!=15)return total+"/15 cards • "+ex+"/3 Exotic • "+my+"/5 Mythic";
        return "LEGAL • 15/15 cards • "+ex+"/3 Exotic • "+my+"/5 Mythic";
    }

    private void addDeckStatistics() {
        int power=0,quantity=0,exotic=0,mythic=0;
        int[] curve=new int[6];
        for(Map.Entry<String,Integer> entry:active.entrySet()){
            Card card=getCard(entry.getKey());
            if(card==null)continue;
            int copies=entry.getValue();
            quantity+=copies;
            power+=Math.max(0,whole(card.power))*copies;
            int cost=whole(card.cost);
            if(cost>=1)curve[Math.min(6,cost)-1]+=copies;
            if(card.rarity.equalsIgnoreCase("Exotic"))exotic+=copies;
            if(card.rarity.equalsIgnoreCase("Mythic"))mythic+=copies;
        }
        LinearLayout totals=new LinearLayout(this);
        totals.setGravity(Gravity.CENTER_VERTICAL);
        totals.setBackground(bg(0xff4c3526,12));
        totals.setPadding(dp(10),dp(5),dp(10),dp(5));
        body.addView(totals,new LinearLayout.LayoutParams(-1,dp(66)));
        LinearLayout powerColumn=new LinearLayout(this);
        powerColumn.setOrientation(LinearLayout.VERTICAL);
        totals.addView(powerColumn,new LinearLayout.LayoutParams(0,-2,1));
        TextView powerNumber=label(power+" POWER",22,true);
        powerNumber.setTextColor(0xfff1c976);powerColumn.addView(powerNumber);
        TextView subtext=label("BASE DECK POWER • "+exotic+"/3 EXOTIC • "+mythic+"/5 MYTHIC",10,false);
        subtext.setTextColor(0xffe1c9aa);powerColumn.addView(subtext);
        TextView countLabel=label(quantity+"/15",21,true);
        countLabel.setTextColor(0xfff1d9b2);countLabel.setGravity(Gravity.CENTER);
        totals.addView(countLabel,new LinearLayout.LayoutParams(dp(71),-2));

        LinearLayout curveRow=new LinearLayout(this);
        curveRow.setBackground(bg(0xff2e231e,10));
        curveRow.setPadding(dp(5),dp(4),dp(5),dp(4));
        body.addView(curveRow,new LinearLayout.LayoutParams(-1,dp(54)));
        for(int n=0;n<curve.length;n++){
            TextView pip=label((n==5?"6+":" "+(n+1))+"\n"+curve[n],12,true);
            pip.setGravity(Gravity.CENTER);
            pip.setTextColor(curve[n]>0?0xfff4d19a:0xffa69684);
            pip.setBackground(bg(curve[n]>0?0xff4e3d30:0xff302b28,9));
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-1,1);
            params.setMargins(dp(2),0,dp(2),0);
            curveRow.addView(pip,params);
        }
    }
    private void showDeck(){
        showingDeck=true;clear();
        LinearLayout actions=new LinearLayout(this);body.addView(actions);
        actions.addView(button("Switch / New",()->chooseDeck()),new LinearLayout.LayoutParams(0,dp(52),1));
        actions.addView(button("Rename",()->renameDeck()),new LinearLayout.LayoutParams(0,dp(52),1));
        actions.addView(button("Delete",()->deleteDeck()),new LinearLayout.LayoutParams(0,dp(52),1));
        TextView title=label(currentDeck+"   •   "+count()+" cards",19,true);title.setTextColor(accent);body.addView(title);
        TextView legality=label(deckStatus(),12,true);legality.setTextColor(deckStatus().startsWith("LEGAL")?0xff9fe4bb:0xffffcc8c);body.addView(legality);
        addDeckStatistics();
        body.addView(button("⚔  Sim Battle with This Deck",()->{
            if(!deckStatus().startsWith("LEGAL")){toast("Cannot start: "+deckStatus());return;}
            android.content.Intent intent=new android.content.Intent(this,BattleActivity.class);
            intent.putExtra("selected_deck",currentDeck);startActivity(intent);
        }),new LinearLayout.LayoutParams(-1,dp(49)));
        LinearLayout io=new LinearLayout(this);body.addView(io);
        io.addView(button("Import text",()->importDeck()),new LinearLayout.LayoutParams(0,dp(50),1));
        io.addView(button("Copy deck",()->copyDeck()),new LinearLayout.LayoutParams(0,dp(50),1));
        io.addView(button("Share",()->shareDeck()),new LinearLayout.LayoutParams(0,dp(50),1));
        ScrollView s=scroll(body);LinearLayout list=column(s);
        if(active.isEmpty())list.addView(label("This deck is empty. Open Card Collection and tap a card to add copies.",14,false));
        for(Map.Entry<String,Integer> e:new ArrayList<>(active.entrySet())){
            Card c=getCard(e.getKey());if(c==null)continue;
            addCardTile(list,c);
        }
        body.addView(button("+ Browse cards",()->showCollection()),new LinearLayout.LayoutParams(-1,dp(54)));
    }
    private String key(String name){return "deck_"+name;}
    private void loadNames(){
        String stored=getPreferences(0).getString("deck_names","[\"My Deck\"]");
        try{JSONArray a=new JSONArray(stored);for(int i=0;i<a.length();i++)deckNames.add(a.getString(i));}catch(Exception ignored){}
        if(deckNames.isEmpty())deckNames.add("My Deck");
        currentDeck=getPreferences(0).getString("selected_deck",deckNames.get(0));
        if(!deckNames.contains(currentDeck))currentDeck=deckNames.get(0);
    }
    private void saveNames(){
        JSONArray a=new JSONArray();for(String n:deckNames)a.put(n);
        getPreferences(0).edit().putString("deck_names",a.toString()).putString("selected_deck",currentDeck).apply();
    }
    private void loadDeck(String name){
        active.clear();currentDeck=name;
        try{JSONObject obj=new JSONObject(getPreferences(0).getString(key(name),"{}"));Iterator<String> it=obj.keys();
            while(it.hasNext()){String n=it.next();int qty=obj.optInt(n,0);if(qty>0&&getCard(n)!=null)active.put(n,qty);}
        }catch(Exception ignored){}saveNames();
    }
    private void saveDeck(){
        JSONObject obj=new JSONObject();try{for(Map.Entry<String,Integer> e:active.entrySet())obj.put(e.getKey(),e.getValue());}catch(Exception ignored){}
        getPreferences(0).edit().putString(key(currentDeck),obj.toString()).apply();
    }
    private void chooseDeck(){
        ArrayList<String> options=new ArrayList<>(deckNames);options.add("+ Create a new deck");
        new AlertDialog.Builder(this).setTitle("Select deck").setItems(options.toArray(new String[0]),(d,w)->{
            if(w==deckNames.size())editName("New deck name","",n->{if(deckNames.contains(n)){toast("A deck with this name exists");return;}deckNames.add(n);loadDeck(n);showDeck();});
            else{loadDeck(deckNames.get(w));showDeck();}
        }).show();
    }
    interface NameHandler{void accept(String name);}
    private void editName(String title,String initial,NameHandler handler){
        EditText entry=new EditText(this);entry.setSingleLine();entry.setText(initial);
        new AlertDialog.Builder(this).setTitle(title).setView(entry).setPositiveButton("Save",(d,w)->{
            String n=entry.getText().toString().trim();
            if(n.isEmpty()||n.length()>60||n.contains("/")||n.contains("\\")||n.contains("|")){toast("Invalid name");return;}handler.accept(n);
        }).setNegativeButton("Cancel",null).show();
    }
    private void renameDeck(){editName("Rename deck",currentDeck,n->{
        if(deckNames.contains(n)&&!n.equals(currentDeck)){toast("Name already exists");return;}
        if(n.equals(currentDeck))return;
        String old=currentDeck;deckNames.set(deckNames.indexOf(old),n);currentDeck=n;saveDeck();
        getPreferences(0).edit().remove(key(old)).apply();saveNames();showDeck();
    });}
    private void deleteDeck(){
        new AlertDialog.Builder(this).setTitle("Delete "+currentDeck+"?").setMessage("This only deletes the saved local deck.")
        .setPositiveButton("Delete",(d,w)->{
            String old=currentDeck;deckNames.remove(old);
            getPreferences(0).edit().remove(key(old)).apply();
            if(deckNames.isEmpty())deckNames.add("My Deck");
            loadDeck(deckNames.get(0));showDeck();
        }).setNegativeButton("Cancel",null).show();
    }
    private String deckText(){
        StringBuilder b=new StringBuilder("# Shadowshaper Duels - "+currentDeck+"\n");
        for(Map.Entry<String,Integer> e:active.entrySet())b.append(e.getValue()).append("x ").append(e.getKey()).append("\n");
        return b.toString();
    }
    private void copyDeck(){
        ((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Shadowshaper deck",deckText()));toast("Deck copied");
    }
    private void shareDeck(){
        Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_SUBJECT,currentDeck);i.putExtra(Intent.EXTRA_TEXT,deckText());startActivity(Intent.createChooser(i,"Export Shadowshaper deck"));
    }
    private void importDeck(){
        EditText input=new EditText(this);input.setMinLines(7);input.setGravity(Gravity.TOP);
        input.setHint("2x Ayuela\\n1x Mu Ying");input.setTextColor(Color.WHITE);
        ScrollView container=new ScrollView(this);container.setPadding(dp(12),0,dp(12),0);container.addView(input);
        new AlertDialog.Builder(this).setTitle("Import deck text").setMessage("Replaces current deck. Unknown card names will be reported.").setView(container)
        .setPositiveButton("Import",(d,w)->{
            LinkedHashMap<String,Integer> incoming=new LinkedHashMap<>();ArrayList<String> unknown=new ArrayList<>();
            for(String line:input.getText().toString().split("\\r?\\n")){
                line=line.trim();if(line.isEmpty()||line.startsWith("#"))continue;
                java.util.regex.Matcher m=java.util.regex.Pattern.compile("^(\\d+)\\s*[xX×]?\\s+(.+)$").matcher(line);
                if(!m.matches()){unknown.add(line);continue;}
                int qty;try{qty=Integer.parseInt(m.group(1));}catch(Exception e){unknown.add(line);continue;}
                Card c=getCard(m.group(2).trim());
                if(c==null||qty<1||qty>999){unknown.add(line);continue;}
                incoming.put(c.name,incoming.getOrDefault(c.name,0)+qty);
            }
            if(!incoming.isEmpty()){
                int count=0,exotic=0,mythic=0;String problem="";
                for(Map.Entry<String,Integer> entry:incoming.entrySet()){
                    Card c=getCard(entry.getKey());int qty=entry.getValue();count+=qty;
                    if(qty>2){problem="No more than 2 copies of "+c.name;break;}
                    if(!playable(c)){problem="Unplayable token or missing stats: "+c.name;break;}
                    if(c.rarity.equalsIgnoreCase("Exotic"))exotic+=qty;
                    if(c.rarity.equalsIgnoreCase("Mythic"))mythic+=qty;
                }
                if(problem.isEmpty()&&count>15)problem="Deck may not exceed 15 cards.";
                if(problem.isEmpty()&&exotic>3)problem="Deck may not exceed 3 Exotic cards.";
                if(problem.isEmpty()&&mythic>5)problem="Deck may not exceed 5 Mythic cards.";
                if(problem.isEmpty()){active.clear();active.putAll(incoming);saveDeck();showDeck();}
                else{toast(problem+" Import not applied.");return;}
            }
            toast("Imported "+incoming.size()+" unique cards; "+unknown.size()+" lines skipped");
            if(!unknown.isEmpty())new AlertDialog.Builder(this).setTitle("Unrecognized lines").setMessage(android.text.TextUtils.join("\n",unknown)).setPositiveButton("OK",null).show();
        }).setNegativeButton("Cancel",null).show();
    }
    private void toast(String msg){Toast.makeText(this,msg,Toast.LENGTH_LONG).show();}
    // Drop official card art in app/src/main/assets/card_art/<card_slug>.webp
    // (png or jpg also supported). Embedded art always takes priority over network art.
    private String bundledImageName(Card c) {
        for(String extension:new String[]{"webp","png","jpg","jpeg"}) {
            String name="card_art/"+c.slug+"."+extension;
            try(InputStream input=getAssets().open(name)) {return name;}
            catch(IOException ignored){}
        }
        return "";
    }
    private void loadImage(Card card,ImageView view){
        view.setImageDrawable(bg(0xff28334f,8));
        view.setContentDescription(card.name);
        view.setTag(card.slug);
        Bitmap cached;
        synchronized(memory){cached=memory.get(card.slug);}
        if(cached!=null){view.setImageBitmap(cached);return;}
        images.execute(()->{
            Bitmap bitmap=null;
            String bundled=bundledImageName(card);
            if(!bundled.isEmpty()) {
                try(InputStream input=getAssets().open(bundled)){
                    bitmap=BitmapFactory.decodeStream(input);
                }catch(IOException ignored){}
            }
            if(bitmap==null && !card.image.isEmpty()){
                File directory=new File(getCacheDir(),"shadowshaper_cards");
                if(!directory.exists())directory.mkdirs();
                File file=new File(directory,card.slug+".webp");
                try {
                    if(file.isFile())bitmap=BitmapFactory.decodeFile(file.getAbsolutePath());
                    if(bitmap==null) {
                        HttpURLConnection conn=(HttpURLConnection)new URL(card.image).openConnection();
                        conn.setConnectTimeout(6000);conn.setReadTimeout(10000);
                        conn.setRequestProperty("User-Agent","ShadowshaperDeck/0.2");
                        try {
                            if(conn.getResponseCode()==HttpURLConnection.HTTP_OK &&
                               conn.getContentType()!=null && conn.getContentType().startsWith("image/")) {
                                try(InputStream input=conn.getInputStream();
                                    FileOutputStream output=new FileOutputStream(file)){
                                    byte[] bytes=new byte[8192];int length;
                                    while((length=input.read(bytes))!=-1)output.write(bytes,0,length);
                                }
                                bitmap=BitmapFactory.decodeFile(file.getAbsolutePath());
                            }
                        }finally {conn.disconnect();}
                        if(bitmap==null)file.delete();
                    }
                }catch(Exception ignored){ /* Image problems must not block card details. */ }
            }
            if(bitmap!=null){
                synchronized(memory){memory.put(card.slug,bitmap);}
                final Bitmap completed=bitmap;
                runOnUiThread(()->{
                    if(card.slug.equals(view.getTag()))view.setImageBitmap(completed);
                });
            }
        });
    }
    @Override protected void onDestroy(){super.onDestroy();images.shutdownNow();}
}
