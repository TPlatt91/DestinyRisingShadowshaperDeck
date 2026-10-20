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
    private int background = Color.rgb(12,17,31), panel = Color.rgb(25,34,56), accent = Color.rgb(128,173,255);

    static class Card {
        String name, effect, slug, image;
        Card(String n,String e) {
            name=n; effect=e;
            slug=n.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","_").replaceAll("^_|_$","");
            image="https://www.media.todayindestiny.com/images/rising/shadowshaper_duels/card/"+slug+".webp";
        }
    }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
        try(BufferedReader in = new BufferedReader(new InputStreamReader(getAssets().open("cards.tsv"), StandardCharsets.UTF_8))) {
            String line; while((line=in.readLine())!=null) { String[] p=line.split("\\|",2); if(p.length==2)cards.add(new Card(p[0],p[1])); }
        } catch(Exception e) { Toast.makeText(this,"Catalog load error: "+e.getMessage(),Toast.LENGTH_LONG).show(); }
        loadNames(); loadDeck(currentDeck); frame(); showCollection();
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
        TextView title=label("SHADOWSHAPER  •  DECK ARCHIVE",19,true);title.setTextColor(accent);root.addView(title);
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
        search.setHint("Search cards and abilities…");search.setPadding(dp(16),dp(8),dp(16),dp(8));body.addView(search);
        TextView heading=label(cards.size()+" catalog records  •  select a card to view/add",12,false);body.addView(heading);
        ScrollView s=scroll(body);LinearLayout list=column(s);
        Runnable refresh=()->{
            list.removeAllViews();String q=search.getText().toString().toLowerCase(Locale.ROOT).trim();
            int displayed=0;
            for(Card c:cards)if(c.name.toLowerCase(Locale.ROOT).contains(q)||c.effect.toLowerCase(Locale.ROOT).contains(q)){
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
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(8),dp(7),dp(8),dp(7));row.setBackground(bg(panel,12));
        LinearLayout.LayoutParams outer=new LinearLayout.LayoutParams(-1,dp(126));outer.bottomMargin=dp(7);list.addView(row,outer);
        ImageView art=new ImageView(this);art.setScaleType(ImageView.ScaleType.FIT_CENTER);row.addView(art,new LinearLayout.LayoutParams(dp(76),dp(112)));loadImage(c,art);
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);row.addView(info,new LinearLayout.LayoutParams(0,-2,1));
        info.addView(label(c.name,16,true));TextView effect=label(c.effect,12,false);effect.setTextColor(0xffccd6eb);effect.setMaxLines(3);info.addView(effect);
        TextView owned=label("In deck: "+active.getOrDefault(c.name,0),12,false);owned.setTextColor(accent);info.addView(owned);
        row.setOnClickListener(v->inspect(c));
    }
    private void inspect(Card c){
        LinearLayout stack=new LinearLayout(this);stack.setPadding(dp(12),dp(8),dp(12),dp(8));stack.setOrientation(LinearLayout.VERTICAL);
        ImageView art=new ImageView(this);art.setScaleType(ImageView.ScaleType.FIT_CENTER);stack.addView(art,new LinearLayout.LayoutParams(-1,dp(350)));loadImage(c,art);
        stack.addView(label(c.effect,14,false));stack.addView(label("Current deck: "+currentDeck+" ("+active.getOrDefault(c.name,0)+" copies)",12,false));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(c.name).setView(stack)
          .setPositiveButton("Add copy",(d,w)->{active.put(c.name,active.getOrDefault(c.name,0)+1);saveDeck(); if(showingDeck)showDeck();else showCollection();})
          .setNeutralButton("Remove copy",(d,w)->{int n=active.getOrDefault(c.name,0);if(n<=1)active.remove(c.name);else active.put(c.name,n-1);saveDeck();if(showingDeck)showDeck();else showCollection();})
          .setNegativeButton("Close",null).create();
        dialog.show();
    }
    private void showDeck(){
        showingDeck=true;clear();
        LinearLayout actions=new LinearLayout(this);body.addView(actions);
        actions.addView(button("Switch / New",()->chooseDeck()),new LinearLayout.LayoutParams(0,dp(52),1));
        actions.addView(button("Rename",()->renameDeck()),new LinearLayout.LayoutParams(0,dp(52),1));
        actions.addView(button("Delete",()->deleteDeck()),new LinearLayout.LayoutParams(0,dp(52),1));
        TextView title=label(currentDeck+"   •   "+count()+" cards",19,true);title.setTextColor(accent);body.addView(title);
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
        input.setHint("2x Ayuela\\n1x Mu Ying");input.setTextColor(Color.BLACK);
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
            if(!incoming.isEmpty()){active.clear();active.putAll(incoming);saveDeck();showDeck();}
            toast("Imported "+incoming.size()+" unique cards; "+unknown.size()+" lines skipped");
            if(!unknown.isEmpty())new AlertDialog.Builder(this).setTitle("Unrecognized lines").setMessage(android.text.TextUtils.join("\n",unknown)).setPositiveButton("OK",null).show();
        }).setNegativeButton("Cancel",null).show();
    }
    private void toast(String msg){Toast.makeText(this,msg,Toast.LENGTH_LONG).show();}
    private void loadImage(Card card,ImageView view){
        view.setImageDrawable(bg(0xff28334f,8));view.setContentDescription(card.name);
        Bitmap cached; synchronized(memory){cached=memory.get(card.slug);}
        if(cached!=null){view.setImageBitmap(cached);return;}
        view.setTag(card.slug);
        images.execute(()->{
            Bitmap b=null;File directory=new File(getCacheDir(),"shadowshaper_cards");directory.mkdirs();
            File file=new File(directory,card.slug+".webp");
            try{
                if(file.exists())b=BitmapFactory.decodeFile(file.getAbsolutePath());
                if(b==null){
                    HttpURLConnection conn=(HttpURLConnection)new URL(card.image).openConnection();
                    conn.setConnectTimeout(7000);conn.setReadTimeout(10000);
                    conn.setRequestProperty("User-Agent","ShadowshaperDeck/0.1");
                    if(conn.getResponseCode()==200){
                        try(InputStream in=conn.getInputStream();FileOutputStream out=new FileOutputStream(file)){
                            byte[] bytes=new byte[8192];int n;while((n=in.read(bytes))!=-1)out.write(bytes,0,n);
                        }
                        b=BitmapFactory.decodeFile(file.getAbsolutePath());
                        if(b==null)file.delete();
                    }conn.disconnect();
                }
            }catch(Exception ignored){}
            if(b!=null){synchronized(memory){memory.put(card.slug,b);}Bitmap done=b;
                runOnUiThread(()->{if(card.slug.equals(view.getTag()))view.setImageBitmap(done);});
            }
        });
    }
    @Override protected void onDestroy(){super.onDestroy();images.shutdownNow();}
}
