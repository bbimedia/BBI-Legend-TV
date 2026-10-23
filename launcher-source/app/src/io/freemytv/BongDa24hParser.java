package io.freemytv;

import org.json.*;
import java.time.*;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;

/** Reads fixture facts only from the page's matchdetail blocks; never executes HTML/JS. */
final class BongDa24hParser {
    private static final Pattern DIV=Pattern.compile("<div\\b([^>]*)>",Pattern.CASE_INSENSITIVE);
    private static final Pattern ID=Pattern.compile("getMatchinfo\\((\\d+)\\)");
    static String attr(String attrs,String key){Matcher m=Pattern.compile("(?:^|\\s)"+Pattern.quote(key)+"\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))",Pattern.CASE_INSENSITIVE).matcher(attrs);if(!m.find())return "";return m.group(1)!=null?m.group(1):m.group(2)!=null?m.group(2):m.group(3);}
    private static boolean token(String value,String name){return Arrays.asList(value.split("\\s+")).contains(name);}
    static String clean(String html){String value=html.replaceAll("(?s)<[^>]*>"," ").replace("&nbsp;"," ").replace("&amp;","&").replace("&quot;","\"").replace("&#39;","'").replace("&ndash;","–").replace("&mdash;","—");Matcher m=Pattern.compile("&#(x[0-9a-fA-F]+|[0-9]+);").matcher(value);StringBuffer out=new StringBuffer();while(m.find()){String code=m.group(1);try{int cp=Integer.parseInt(code.startsWith("x")?code.substring(1):code,code.startsWith("x")?16:10);m.appendReplacement(out,Matcher.quoteReplacement(new String(Character.toChars(cp))));}catch(Exception e){m.appendReplacement(out,"");}}m.appendTail(out);return out.toString().replaceAll("\\s+"," ").trim();}
    static String field(String block,String tag,String cls){Matcher m=Pattern.compile("<"+tag+"\\b([^>]*)>(.*?)</"+tag+">",Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(block);while(m.find())if(token(attr(m.group(1),"class"),cls))return clean(m.group(2));return "";}
    static String normalized(String v){return Normalizer.normalize(v,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).replace('đ','d');}
    static int priority(String id,String home,String away){String teams=normalized(home+" "+away);if(teams.contains("viet nam")||teams.contains("vietnam"))return teams.matches(".*\\bnu\\b.*")||teams.contains("women")?1:0;String[] ids={"25","1","7","3","5","4"};for(int i=0;i<ids.length;i++)if(ids[i].equals(id))return i+2;return -1;}
    static JSONArray parse(String html,long from,long until)throws Exception{
        List<Integer> starts=new ArrayList<>(),ends=new ArrayList<>();List<String> attrs=new ArrayList<>();Matcher div=DIV.matcher(html);
        while(div.find())if(token(attr(div.group(1),"class"),"matchdetail")){starts.add(div.start());ends.add(div.end());attrs.add(div.group(1));if(starts.size()>2000)throw new java.io.IOException("Too many fixture rows");}
        if(starts.isEmpty()&&!html.contains("showgiai")&&!html.contains("football-match")&&!normalized(clean(html)).contains("khong co"))throw new java.io.IOException("Schedule markup changed");
        JSONArray result=new JSONArray();int year=Instant.ofEpochMilli(from).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).getYear();
        for(int i=0;i<starts.size();i++)try{
            String block=html.substring(ends.get(i),i+1<starts.size()?starts.get(i+1):html.length());String home=field(block,"a","club1"),away=field(block,"a","club2"),league=attr(attrs.get(i),"data-leagueid");int rank=priority(league,home,away);if(rank<0||home.isEmpty()||away.isEmpty())continue;
            String date=field(block,"span","date"),time=field(block,"span","time");if(date.isEmpty())date=attr(attrs.get(i),"data-date").replace('-','/');Matcher dm=Pattern.compile("^(\\d{1,2})/(\\d{1,2})(?:/(\\d{4}))?$").matcher(date);if(!dm.matches())continue;
            Matcher hm=Pattern.compile("^([01]?\\d|2[0-3]):([0-5]\\d)$").matcher(time);boolean known=hm.matches();int hour=known?Integer.parseInt(hm.group(1)):0,minute=known?Integer.parseInt(hm.group(2)):0;
            long at=0;for(int candidate=year-1;candidate<=year+1;candidate++){if(dm.group(3)!=null&&Integer.parseInt(dm.group(3))!=candidate)continue;try{long value=LocalDate.of(candidate,Integer.parseInt(dm.group(2)),Integer.parseInt(dm.group(1))).atTime(hour,minute).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant().toEpochMilli();if(value>=from&&value<until){at=value;break;}}catch(Exception ignored){}}
            if(at==0)continue;Matcher match=ID.matcher(block);String id=match.find()?match.group(1):league+"-"+at+"-"+home+"-"+away;String channel=field(block,"span","item_kenh");if(channel.length()>200)channel=channel.substring(0,200);String score=field(block,"span","soccer-scores");Matcher scores=Pattern.compile("^(\\d+)\\s*[-:]\\s*(\\d+)$").matcher(score);boolean scored=scores.matches();
            result.put(new JSONObject().put("id",id).put("at",at).put("name",home+" vs "+away).put("league",FootballRules.label(rank)).put("priority",rank).put("time_known",known).put("postponed",normalized(time).contains("hoan")).put("home",scored?scores.group(1):"").put("away",scored?scores.group(2):"").put("channel",channel.isEmpty()?"Chưa có thông tin kênh tại Việt Nam":channel).put("channel_source",channel.isEmpty()?"":"Bongda24h · kênh ghi trong từng trận").put("url","https://bongda24h.vn/bong-da/lich-thi-dau.html"));
        }catch(org.json.JSONException|IllegalArgumentException ignored){}
        return result;
    }
}
