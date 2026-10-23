package io.freemytv;

import org.json.*;
import java.text.*;
import java.util.*;

/** Seven daily schedule fragments used by Bongda24h's own public calendar. */
final class FootballFeed {
    interface Progress {void update(JSONObject block);}
    JSONObject load(Progress progress)throws Exception{
        Calendar start=Calendar.getInstance(XpWidgetsData.VN);start.set(Calendar.HOUR_OF_DAY,0);start.set(Calendar.MINUTE,0);start.set(Calendar.SECOND,0);start.set(Calendar.MILLISECOND,0);long from=start.getTimeInMillis(),until=from+7L*86400000;
        SimpleDateFormat fmt=new SimpleDateFormat("dd-MM-yyyy",Locale.ROOT);fmt.setTimeZone(XpWidgetsData.VN);Map<String,JSONObject> byId=new HashMap<>();int failed=0;
        for(int day=0;day<7;day++){
            if(Thread.currentThread().isInterrupted())throw new InterruptedException();if(day>0)Thread.sleep(1500);
            try{String html=XpWidgetsData.request("https://bongda24h.vn/Schedules/AjaxSchedules?date="+fmt.format(new Date(from+day*86400000L))+"&leagueId=0");JSONArray parsed=BongDa24hParser.parse(html,from,until);for(int i=0;i<parsed.length();i++){JSONObject row=parsed.getJSONObject(i);byId.put(row.getString("id"),row);}}
            catch(InterruptedException e){throw e;}catch(Exception e){failed++;android.util.Log.w("BBIWidgets","Bongda24h day "+day+": "+e.getClass().getSimpleName()+" "+e.getMessage());}
            progress.update(snapshot(byId,from,failed,true,day+1));
        }
        if(failed==7)throw new java.io.IOException("Bongda24h unavailable or markup changed");return snapshot(byId,from,failed,false,7);
    }
    private JSONObject snapshot(Map<String,JSONObject> rows,long from,int failed,boolean loading,int days)throws Exception{
        List<JSONObject> list=new ArrayList<>(rows.values());Collections.sort(list,(a,b)->{int order=Integer.compare(a.optInt("priority"),b.optInt("priority"));return order!=0?order:Long.compare(a.optLong("at"),b.optLong("at"));});JSONArray result=new JSONArray();for(JSONObject row:list)result.put(new JSONObject(row.toString()));
        return new JSONObject().put("events",result).put("from",from).put("partial",failed>0).put("loading",loading).put("days_loaded",days).put("scope",3).put("source","Bongda24h");
    }
}
