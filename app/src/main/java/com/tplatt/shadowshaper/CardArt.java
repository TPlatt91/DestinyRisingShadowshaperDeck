package com.tplatt.shadowshaper;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Asynchronous original card face images, no made-up replacement art. */
public final class CardArt {
    private CardArt(){}
    private static final ExecutorService workers=Executors.newFixedThreadPool(3);
    private static final ConcurrentHashMap<String,Bitmap> memory=new ConcurrentHashMap<>();
    private static String slug(String name){
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","_").replaceAll("^_|_$","");
    }
    public static void bind(Activity activity,ShadowData.Card card,ImageView view){
        String id=slug(card.name);
        view.setTag(id);
        view.setContentDescription(card.name+" card art");
        view.setImageResource(android.R.color.transparent);
        Bitmap available=memory.get(id);
        if(available!=null){view.setImageBitmap(available);return;}
        Context app=activity.getApplicationContext();
        workers.execute(()->{
            Bitmap image=null;
            for(String extension:new String[]{"webp","png","jpg","jpeg"}){
                try(InputStream in=app.getAssets().open("card_art/"+id+"."+extension)){
                    image=BitmapFactory.decodeStream(in);
                    if(image!=null)break;
                }catch(Exception ignored){}
            }
            if(image==null&&card.onlineArt){
                File folder=new File(app.getCacheDir(),"shadowshaper_cards");
                if(!folder.exists())folder.mkdirs();
                File file=new File(folder,id+".webp");
                try{
                    if(file.isFile())image=BitmapFactory.decodeFile(file.getAbsolutePath());
                    if(image==null){
                        URL url=new URL("https://www.media.todayindestiny.com/images/rising/shadowshaper_duels/card/"+id+".webp");
                        HttpURLConnection connection=(HttpURLConnection)url.openConnection();
                        connection.setConnectTimeout(6000);connection.setReadTimeout(9000);
                        try{
                            String type=connection.getContentType();
                            if(connection.getResponseCode()==200 && type!=null&&type.startsWith("image/")){
                                try(InputStream in=connection.getInputStream();FileOutputStream out=new FileOutputStream(file)){
                                    byte[] bytes=new byte[8192];int count;
                                    while((count=in.read(bytes))!=-1)out.write(bytes,0,count);
                                }
                                image=BitmapFactory.decodeFile(file.getAbsolutePath());
                            }
                        }finally{connection.disconnect();}
                        if(image==null)file.delete();
                    }
                }catch(Exception ignored){}
            }
            if(image!=null){
                memory.put(id,image);
                Bitmap result=image;
                activity.runOnUiThread(()->{
                    if(id.equals(view.getTag())&&!activity.isFinishing())view.setImageBitmap(result);
                });
            }
        });
    }
}
