package io.freemytv;

import org.json.*;
import java.text.*;
import java.util.*;

/** TheSportsDB official v1 endpoints. Global request pacing stays below 30/minute.
 * No artwork, guessed rights-holder channels, overseas broadcast lists or streams.
 */
final class FootballFeed {
    private static final String API="https://www.thesportsdb.com/api/v1/json/123/";
    private long lastRequest;
    private int failed;
    private final Map<String,JSONObject> events=new HashMap<>();
    private final Map<String,Set<String>> channels=new HashMap<>();
    private long from,until;
    interface Progress {void update(JSONObject block);}
    private JSONObject get(String path)throws Exception{
        if(Thread.currentThread().isInterrupted())throw new InterruptedException();
        long wait=2150-(System.currentTimeMillis()-lastRequest);if(wait>0)Thread.sleep(wait);
        lastRequest=System.currentTimeMillis();return new JSONObject(XpWidgetsData.request(API+path));
    }
    private void add(JSONArray rows)throws Exception{
        if(rows==null)return;
        for(int i=0;i<rows.length();i++){
            JSONObject e=rows.optJSONObject(i);if(e==null||!"Soccer".equals(e.optString("strSport")))continue;
            int rank=FootballRules.priority(e.optString("idLeague"),e.optString("idHomeTeam"),e.optString("idAwayTeam"));if(rank<0)continue;
            long at=0;String stamp=e.optString("strTimestamp");try{
                if(!stamp.isEmpty()&&!stamp.equals("null"))at=XpWidgetsData.iso(stamp);
                else if(e.optString("strTime").matches("[0-9]{2}:[0-9]{2}:[0-9]{2}"))at=XpWidgetsData.iso(e.getString("dateEvent")+"T"+e.getString("strTime"));
            }catch(Exception ignored){}
            if(at<from||at>=until)continue;String id=e.optString("idEvent");if(!id.matches("[0-9]+"))continue;
            JSONObject item=new JSONObject().put("id",id).put("at",at).put("name",e.optString("strEvent")).put("league",FootballRules.label(rank)).put("priority",rank)
                .put("status",e.optString("strStatus")).put("postponed","yes".equalsIgnoreCase(e.optString("strPostponed")))
                .put("home",e.optString("intHomeScore","")).put("away",e.optString("intAwayScore",""));
            events.put(id,item);
        }
    }
    private JSONObject snapshot(boolean loading)throws Exception{
        List<JSONObject> list=new ArrayList<>(events.values());Collections.sort(list,(a,b)->{
            int rank=Integer.compare(a.optInt("priority"),b.optInt("priority"));return rank!=0?rank:Long.compare(a.optLong("at"),b.optLong("at"));});
        JSONArray out=new JSONArray();for(JSONObject original:list){JSONObject item=new JSONObject(original.toString());Set<String> names=channels.get(item.optString("id"));item.put("channel",names==null||names.isEmpty()?"Chưa có thông tin kênh tại Việt Nam":android.text.TextUtils.join(" · ",names));item.put("channel_source",names==null?"":"TheSportsDB · lịch phát tại Việt Nam");out.put(item);}
        return new JSONObject().put("events",out).put("from",from).put("partial",failed>0).put("limited",true).put("loading",loading).put("scope",2);
    }
    JSONObject load(Progress progress)throws Exception{
        Calendar start=Calendar.getInstance(XpWidgetsData.VN);start.set(Calendar.HOUR_OF_DAY,0);start.set(Calendar.MINUTE,0);start.set(Calendar.SECOND,0);start.set(Calendar.MILLISECOND,0);from=start.getTimeInMillis();until=from+7L*86400000;
        SimpleDateFormat utc=new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT);utc.setTimeZone(TimeZone.getTimeZone("UTC"));
        // Prioritize national teams and V.League before fetching the five European leagues.
        for(String team:new String[]{"140161","144951"})for(String endpoint:new String[]{"eventsnext.php","eventslast.php"})try{JSONObject j=get(endpoint+"?id="+team);add(j.optJSONArray("events"));add(j.optJSONArray("results"));}catch(InterruptedException e){throw e;}catch(Exception e){failed++;}
        progress.update(snapshot(true));
        for(String league:FootballRules.LEAGUES){
            for(int day=0;day<8;day++)try{String d=utc.format(new Date(from+day*86400000L));add(get("eventsday.php?d="+d+"&l="+league+"&s=Soccer").optJSONArray("events"));}catch(InterruptedException e){throw e;}catch(Exception e){failed++;}
            progress.update(snapshot(true));
        }
        for(int day=0;day<8;day++)try{
            String d=utc.format(new Date(from+day*86400000L));JSONArray tv=get("eventstv.php?d="+d+"&s=Soccer&a=Vietnam").optJSONArray("tvevents");if(tv==null)continue;
            for(int i=0;i<tv.length();i++){JSONObject row=tv.optJSONObject(i);if(row==null||!FootballRules.vietnamBroadcast(row.optString("strCountry")))continue;String id=row.optString("idEvent"),name=row.optString("strChannel").trim();if(!events.containsKey(id)||name.isEmpty()||name.equals("null"))continue;Set<String> set=channels.get(id);if(set==null){set=new TreeSet<>();channels.put(id,set);}set.add(name);}
        }catch(InterruptedException e){throw e;}catch(Exception e){failed++;}
        if(failed>=60&&events.isEmpty())throw new java.io.IOException("Football sources unavailable");return snapshot(false);
    }
}
