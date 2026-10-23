package io.freemytv;

import android.content.*;
import android.os.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

/** Direct HTTPS feeds on the TV. Independent cached blocks; no PC/server required after install. */
final class XpWidgetsData {
    interface Listener {void changed();}
    static final TimeZone VN=TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
    private final SharedPreferences cache;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService io=Executors.newFixedThreadPool(3);
    private final Set<String> pending=new HashSet<>();
    private final Map<String,Long> attempts=new HashMap<>();
    private final List<Listener> listeners=new ArrayList<>();
    private final Map<String,JSONObject> data=new HashMap<>();
    private boolean closed;
    XpWidgetsData(Context c){cache=c.getSharedPreferences("xp-widgets-v1",Context.MODE_PRIVATE);for(String key:new String[]{"weather","gold","stocks","football"})try{data.put(key,new JSONObject(cache.getString(key,"{}")));}catch(Exception ignored){}}
    JSONObject block(String key){JSONObject b=data.get(key);return b==null?new JSONObject():b;}
    void listen(Listener l){listeners.add(l);}
    void close(){closed=true;listeners.clear();main.removeCallbacksAndMessages(null);io.shutdownNow();}
    void refresh(){for(String key:new String[]{"weather","gold","stocks","football"}){
        if(key.equals("weather")&&!Prefs.weather())continue;
        long ttl=key.equals("football")?6*3600000L:key.equals("weather")?30*60000L:15*60000L;
        long now=System.currentTimeMillis(),last=Math.max(block(key).optLong("fetched"),attempts.containsKey(key)?attempts.get(key):0);
        if(key.equals("football")&&block(key).optInt("scope")!=2)last=0;
        if(key.equals("weather")&&!block(key).optString("place").equals(Prefs.latitude()+","+Prefs.longitude()))last=0;
        if(pending.contains(key)||(last<=now&&now-last<ttl))continue;
        pending.add(key);attempts.put(key,now);final JSONObject previous=block(key);
        io.execute(()->{JSONObject result;try{
            result=key.equals("weather")?weather():key.equals("gold")?gold(previous):key.equals("stocks")?stocks():football();
            result.put("fetched",System.currentTimeMillis());
        }catch(Exception e){try{result=new JSONObject(previous.toString());result.put("error","Không tải được nguồn · giữ bản lưu nếu có");}catch(Exception impossible){result=new JSONObject();}}
            final JSONObject value=result;main.post(()->{if(closed)return;pending.remove(key);data.put(key,value);cache.edit().putString(key,value.toString()).apply();for(Listener l:new ArrayList<>(listeners))l.changed();});
        });
    }}
    static String request(String url)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(9000);c.setReadTimeout(9000);c.setRequestProperty("User-Agent","BBILegendTV/1.3");
        try{if(c.getResponseCode()!=200)throw new IOException("Source unavailable");try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>1500000)throw new IOException("Response too large");out.write(buf,0,n);}return new String(out.toByteArray(),StandardCharsets.UTF_8);
        }}finally{c.disconnect();}
    }
    static String date(long ms,String pattern){SimpleDateFormat f=new SimpleDateFormat(pattern,Locale.forLanguageTag("vi-VN"));f.setTimeZone(VN);return f.format(new Date(ms));}
    static long iso(String text)throws Exception{
        String value=text.trim();
        if(value.endsWith("Z")||value.matches(".*[+-][0-9]{2}:[0-9]{2}$"))return java.time.OffsetDateTime.parse(value).toInstant().toEpochMilli();
        return java.time.LocalDateTime.parse(value).toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
    }
    static boolean valid(double v){return Double.isFinite(v)&&v>0;}
    private JSONObject weather()throws Exception{
        JSONObject j=new JSONObject(request("https://api.open-meteo.com/v1/forecast?latitude="+Prefs.latitude()+"&longitude="+Prefs.longitude()+"&current=temperature_2m,relative_humidity_2m,weather_code&daily=temperature_2m_max,temperature_2m_min&forecast_days=1&timezone=Asia%2FHo_Chi_Minh"));
        JSONObject c=j.getJSONObject("current"),d=j.getJSONObject("daily");
        return new JSONObject().put("temp",c.getDouble("temperature_2m")).put("code",c.getInt("weather_code")).put("humidity",c.getDouble("relative_humidity_2m")).put("max",d.getJSONArray("temperature_2m_max").getDouble(0)).put("min",d.getJSONArray("temperature_2m_min").getDouble(0)).put("observed",c.getString("time")).put("place",Prefs.latitude()+","+Prefs.longitude());
    }
    private JSONObject gold(JSONObject old)throws Exception{
        JSONObject raw=new JSONObject(request("https://taichinh.com/api/v1/gold"));if(!"vnd_per_luong".equals(raw.optString("unit")))throw new IOException("Gold unit changed");
        JSONObject q=raw.getJSONObject("sjc").getJSONObject("doji"),metrics=q.getJSONObject("metrics"),sell=metrics.getJSONObject("sell"),buy=metrics.getJSONObject("buy");
        double s=sell.getDouble("value"),b=buy.getDouble("value");if(!valid(s)||!valid(b))throw new IOException("Invalid quote");
        String stamp=sell.getString("observed_at");long at=iso(stamp);if(at>System.currentTimeMillis()+86400000L)throw new IOException("Future quote");
        if(old.optLong("at")>at)throw new IOException("Source quote older than cache");
        JSONArray history=new JSONArray();JSONArray prev=old.optJSONArray("history");if(prev!=null)for(int i=Math.max(0,prev.length()-239);i<prev.length();i++){JSONObject p=prev.getJSONObject(i);if(p.optLong("at")<at&&p.optLong("at")>at-30L*86400000)history.put(p);}
        history.put(new JSONObject().put("at",at).put("value",s/1000000));
        return new JSONObject().put("buy",b/1000000).put("sell",s/1000000).put("at",at).put("buy_at",iso(buy.getString("observed_at"))).put("publisher",sell.getJSONObject("source").optString("publisher","DOJI")).put("history",history);
    }
    static JSONObject parseStock(JSONObject raw,String symbol,boolean index)throws Exception{
        JSONArray t=raw.getJSONArray("t"),c=raw.getJSONArray("c"),points=new JSONArray();long now=System.currentTimeMillis();
        java.util.TreeMap<Long,Double> sorted=new java.util.TreeMap<>();
        for(int i=0;i<Math.min(t.length(),c.length());i++){double v=c.optDouble(i,Double.NaN),ts=t.optDouble(i,Double.NaN);long at=(long)(ts*1000);if(valid(v)&&Double.isFinite(ts)&&at>0&&at<=now+86400000L)sorted.put(at,v);}
        for(Map.Entry<Long,Double> e:sorted.entrySet())points.put(new JSONObject().put("at",e.getKey()).put("value",e.getValue()));
        if(points.length()==0)throw new IOException("No price bars");JSONObject last=points.getJSONObject(points.length()-1);
        double value=last.getDouble("value"),previous=points.length()>1?points.getJSONObject(points.length()-2).getDouble("value"):value;
        return new JSONObject().put("symbol",symbol).put("index",index).put("value",value).put("change",(value-previous)/previous*100).put("at",last.getLong("at")).put("points",points);
    }
    private JSONObject stocks()throws Exception{
        long now=System.currentTimeMillis()/1000;JSONArray rows=new JSONArray();int failures=0;
        for(String symbol:new String[]{"VNINDEX","VN30","FPT","VCB","HPG"})try{
            boolean index=symbol.startsWith("VN");String url="https://services.entrade.com.vn/chart-api/v2/ohlcs/"+(index?"index":"stock")+"?from="+(now-45L*86400)+"&to="+now+"&symbol="+symbol+"&resolution=1D";
            rows.put(parseStock(new JSONObject(request(url)),symbol,index));
        }catch(Exception e){failures++;}
        if(rows.length()==0)throw new IOException("Stock feed unavailable");return new JSONObject().put("rows",rows).put("partial",failures>0);
    }
    private JSONObject football()throws Exception{
        return new FootballFeed().load(value->main.post(()->{if(closed)return;JSONObject old=block("football");if(value.optJSONArray("events").length()>0||old.optJSONArray("events")==null){data.put("football",value);for(Listener l:new ArrayList<>(listeners))l.changed();}}));
    }
}
