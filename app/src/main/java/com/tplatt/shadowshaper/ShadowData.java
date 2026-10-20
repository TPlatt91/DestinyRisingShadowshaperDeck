package com.tplatt.shadowshaper;

import android.content.Context;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Shared data model for the existing deck editor and the standalone AI battle.
 * No network is required to load the card statistics.
 */
public final class ShadowData {
    private ShadowData(){}
    public static final int DECK_SIZE=15, COPIES_LIMIT=2, EXOTIC_LIMIT=3, MYTHIC_LIMIT=5;
    public static class Card {
        public String name="", rarity="", faction="", cost="", power="", effect="", classification="", trigger="";
        public boolean onlineArt=false;
        public int numericCost(){try{return Integer.parseInt(cost);}catch(Exception ignored){return -1;}}
        public int numericPower(){try{return Integer.parseInt(power);}catch(Exception ignored){return -1;}}
        public boolean isPlayable(){return numericCost()>0 && numericPower()>=0 &&
            !("Summoned unit".equalsIgnoreCase(classification)) &&
            !("Token".equalsIgnoreCase(cost));}
    }
    public static LinkedHashMap<String,Card> load(Context context){
        LinkedHashMap<String,Card> result=new LinkedHashMap<>();
        readCatalog(context,result,"cards.tsv",false);
        readCatalog(context,result,"community_cards.tsv",true);
        readCatalog(context,result,"expanded_cards.tsv",true);
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(context.getAssets().open("card_details.tsv"),StandardCharsets.UTF_8))){
            String line;
            while((line=reader.readLine())!=null){
                String[] fields=line.split("\\|",-1);
                if(fields.length!=10)continue;
                Card card=result.get(fields[0]);
                if(card==null)continue;
                card.faction=fields[1];card.rarity=fields[2];
                card.cost=fields[3];card.power=fields[4];
                card.classification=fields[5];card.trigger=fields[6];
            }
        }catch(Exception ignored){}
        return result;
    }
    private static void readCatalog(Context context,LinkedHashMap<String,Card> map,String filename,boolean expanded){
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(context.getAssets().open(filename),StandardCharsets.UTF_8))){
            String line;
            while((line=reader.readLine())!=null){
                String[] fields=line.split("\\|",expanded?6:2);
                if(fields.length!=(expanded?6:2))continue;
                if(map.containsKey(fields[0]))continue;
                Card card=new Card();card.name=fields[0];card.onlineArt=!expanded;
                if(expanded){card.rarity=fields[1];card.faction=fields[2];card.cost=fields[3];card.power=fields[4];card.effect=fields[5];}
                else card.effect=fields[1];
                map.put(card.name,card);
            }
        }catch(Exception ignored){}
    }
    public static LinkedHashMap<String,Integer> readDeck(Context context,String name){
        LinkedHashMap<String,Integer> cards=new LinkedHashMap<>();
        String raw=context.getSharedPreferences("MainActivity",Context.MODE_PRIVATE).getString("deck_"+name,"{}");
        try {
            JSONObject value=new JSONObject(raw);
            java.util.Iterator<String> iterator=value.keys();
            while(iterator.hasNext()){
                String key=iterator.next(),safe=key.trim();
                int count=value.optInt(key,0);
                if(count>0 && !safe.isEmpty())cards.put(safe,count);
            }
        }catch(Exception ignored){}
        return cards;
    }
    public static ArrayList<String> deckNames(Context context){
        ArrayList<String> names=new ArrayList<>();
        String json=context.getSharedPreferences("MainActivity",Context.MODE_PRIVATE).getString("deck_names","[\"My Deck\"]");
        try{
            org.json.JSONArray array=new org.json.JSONArray(json);
            for(int i=0;i<array.length();i++)names.add(array.getString(i));
        }catch(Exception ignored){}
        if(names.isEmpty())names.add("My Deck");
        return names;
    }
    public static String selectedDeck(Context context){
        return context.getSharedPreferences("MainActivity",Context.MODE_PRIVATE).getString("selected_deck","My Deck");
    }
    public static String validate(Map<String,Integer> deck,Map<String,Card> catalog){
        int total=0,exotic=0,mythic=0;
        for(Map.Entry<String,Integer> entry:deck.entrySet()){
            Card card=catalog.get(entry.getKey());
            if(card==null)return "Unknown card: "+entry.getKey();
            int amount=entry.getValue();
            if(amount<1||amount>COPIES_LIMIT)return "Maximum two copies of "+card.name;
            if(!card.isPlayable())return card.name+" is a token or missing its Power / cost.";
            total+=amount;
            if(card.rarity.equalsIgnoreCase("Exotic"))exotic+=amount;
            if(card.rarity.equalsIgnoreCase("Mythic"))mythic+=amount;
        }
        if(total!=DECK_SIZE)return "A playable deck needs exactly 15 cards ("+total+"/15).";
        if(exotic>EXOTIC_LIMIT)return "No more than 3 Exotic cards (currently "+exotic+").";
        if(mythic>MYTHIC_LIMIT)return "No more than 5 Mythic cards (currently "+mythic+").";
        return "";
    }
    public static ArrayList<Card> expanded(Map<String,Integer> deck,Map<String,Card> catalog){
        ArrayList<Card> output=new ArrayList<>();
        for(Map.Entry<String,Integer> e:deck.entrySet()){
            Card c=catalog.get(e.getKey());
            if(c!=null)for(int n=0;n<e.getValue();n++)output.add(c);
        }
        return output;
    }
    public static LinkedHashMap<String,Integer> opponentDeck(Map<String,Card> catalog){
        // Default CPU practice build. Same legality restrictions as player decks.
        LinkedHashMap<String,Integer> result=new LinkedHashMap<>();
        String[] presets={"Dreg","Resilient Dreg","Novice Marauder","Acolyte","Thrall",
            "Legionary","War Beast","Vandal","Rock Pile","Shank","Marauder","Guerrilla",
            "Zhiyao","Shinka Legionary","Donovan-17","Mu Ying","Sparrow","Resilient Captain",
            "Scorpius","Knight","Carrier"};
        for(String name:presets){
            Card c=catalog.get(name);
            if(c==null||!c.isPlayable())continue;
            if(c.rarity.equalsIgnoreCase("Exotic")||c.rarity.equalsIgnoreCase("Mythic"))continue;
            result.put(c.name,1);
            if(result.size()==DECK_SIZE)break;
        }
        if(result.size()!=DECK_SIZE)return new LinkedHashMap<>();
        return result;
    }
}
