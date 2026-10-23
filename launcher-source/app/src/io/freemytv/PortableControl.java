package io.freemytv;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Shell-only settings bridge. Protected by the platform DUMP permission in the manifest. */
public class PortableControl extends Activity {
    static String theme(Context c){try{return new String(new AtomicFile(new File(c.getFilesDir(),"launcher-theme")).readFully(),StandardCharsets.UTF_8).trim();}catch(IOException e){return "macos";}}
    private static void theme(Context c,String value)throws IOException{
        if(!Arrays.asList("default","xp","xp-widgets","vista","macos","vista-widgets","macos-widgets").contains(value))throw new IOException("Unknown theme");
        AtomicFile f=new AtomicFile(new File(c.getFilesDir(),"launcher-theme"));FileOutputStream out=f.startWrite();try{out.write(value.getBytes(StandardCharsets.UTF_8));f.finishWrite(out);}catch(IOException e){f.failWrite(out);throw e;}
        Prefs.version++;
    }
    private void apply(JSONObject j)throws Exception{
        if(j.has("theme"))theme(this,j.getString("theme"));
        if(j.has("sounds"))Prefs.setSounds(j.getBoolean("sounds"));
        if(j.has("weather"))Prefs.setWeather(j.getBoolean("weather"));
        if(j.has("quotes"))Prefs.setQuotes(j.getBoolean("quotes"));
        if(j.has("quick_close"))QuickClose.enabled(this,j.getBoolean("quick_close"));
        if(j.has("city")){
            String name=j.getString("city");double lat=j.getDouble("latitude"),lon=j.getDouble("longitude");
            if(name.length()>80||!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)throw new IOException("Invalid city");
            Prefs.setCity(name,String.valueOf(lat),String.valueOf(lon));
        }
        if(j.has("temperature")){String unit=j.getString("temperature");if(!Arrays.asList("auto","C","F").contains(unit))throw new IOException("Invalid unit");Prefs.setTemperature(unit);}
        if(j.has("language")){String lang=j.getString("language");if(!Arrays.asList("auto","en","pt","es","de","ru").contains(lang))throw new IOException("Invalid language");Prefs.setLanguage(lang);}
        if(j.has("appearance")){String value=j.getString("appearance");if(!Arrays.asList("auto","light","dark").contains(value))throw new IOException("Invalid appearance");Prefs.setTheme(value);}
        if(j.has("shelf")){JSONArray list=j.getJSONArray("shelf");if(list.length()>HomeApps.MAX_SHELF)throw new IOException("Too many apps");java.util.List<String> packages=new java.util.ArrayList<>();for(int i=0;i<list.length();i++){String pkg=list.getString(i);if(!pkg.matches("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+"))throw new IOException("Invalid package");packages.add(pkg);}Prefs.setShelf(packages);}
    }
    private JSONObject export()throws Exception{
        JSONObject j=new JSONObject();j.put("schema",1).put("theme",theme(this)).put("sounds",Prefs.sounds()).put("weather",Prefs.weather()).put("quotes",Prefs.quotes()).put("quick_close",QuickClose.enabled(this));
        j.put("city",Prefs.city()).put("latitude",Double.parseDouble(Prefs.latitude())).put("longitude",Double.parseDouble(Prefs.longitude())).put("temperature",Prefs.temperature()).put("language",Prefs.language()).put("appearance",Prefs.theme());
        java.util.List<String> shelf=Prefs.shelf();if(shelf!=null)j.put("shelf",new JSONArray(shelf));
        return j;
    }
    @Override public void onCreate(Bundle state){super.onCreate(state);JSONObject result=new JSONObject();
        try{
            String action=getIntent().getStringExtra("action");
            if("apply".equals(action)||"import".equals(action)){
                File input=new File(getExternalFilesDir(null),"portable-import.json");
                if(input.length()>65536)throw new IOException("Settings file too large");
                byte[] bytes=java.nio.file.Files.readAllBytes(input.toPath());apply(new JSONObject(new String(bytes,StandardCharsets.UTF_8)));input.delete();
            }else if(!"export".equals(action))throw new IOException("Unknown action");
            result=export();result.put("ok",true);
        }catch(Exception e){try{result.put("ok",false).put("error",e.getMessage());}catch(JSONException ignored){}}
        try{result.put("request",getIntent().getStringExtra("request"));File dir=getExternalFilesDir(null);if(dir==null)throw new IOException("External storage unavailable");try(FileOutputStream out=new FileOutputStream(new File(dir,"portable-export.json"))){out.write(result.toString(2).getBytes(StandardCharsets.UTF_8));}}
        catch(Exception e){android.util.Log.e("FreeMyTV","Portable settings: "+e.getMessage());}
        finish();
    }
}
