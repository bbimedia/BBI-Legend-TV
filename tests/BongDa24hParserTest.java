package io.freemytv;
import org.json.*;import java.time.*;import java.nio.file.*;
public class BongDa24hParserTest {
 static String row(String league,String date,String time,String home,String away,String channel,String id){return "<div data-leagueid='"+league+"' class='f-row matchdetail hot'><span class=time>"+time+"</span><span class=date>"+date+"</span><a class='name-club club1'>"+home+"<img src='ignored'></a><a class='name-club club2'>"+away+"</a><span class=item_kenh>"+channel+"</span><p onclick='getMatchinfo("+id+")'><span class=soccer-scores>vs</span></p></div>";}
 public static void main(String[] args)throws Exception{
  long start=LocalDate.of(2026,10,23).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant().toEpochMilli(),end=start+7L*86400000;
  String html=row("1","24/10","02:00","Đội A","Đội B","FPT Play &amp; TV","1")+row("99","24/10","18:00","Việt Nam Nữ","Đội C","","2")+row("999","24/10","20:00","Đội D","Đội E","","3")+row("25","23/10","17:00","Đội F","Đội G","VTV6, TV360","4")+row("1","31/10","02:00","Đội H","Đội I","","5");
  JSONArray rows=BongDa24hParser.parse(html,start,end);if(rows.length()!=3)throw new AssertionError(rows.toString());
  JSONObject first=rows.getJSONObject(0);long expected=LocalDateTime.of(2026,10,24,2,0).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant().toEpochMilli();
  if(first.getLong("at")!=expected||!first.getString("channel").equals("FPT Play & TV"))throw new AssertionError(first.toString());
  if(rows.getJSONObject(1).getInt("priority")!=1||!rows.getJSONObject(1).getString("channel").startsWith("Chưa"))throw new AssertionError();
  try{BongDa24hParser.parse("<html>unexpected challenge</html>",start,end);throw new AssertionError("Markup failure should not become an empty schedule");}catch(java.io.IOException expectedError){}
  long dec=LocalDate.of(2026,12,29).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant().toEpochMilli();
  JSONArray ny=BongDa24hParser.parse(row("1","01/01","01:00","A","B","","6"),dec,dec+7L*86400000);if(ny.length()!=1)throw new AssertionError("Year rollover");
  if(args.length>0){JSONArray live=BongDa24hParser.parse(new String(Files.readAllBytes(Paths.get(args[0])),java.nio.charset.StandardCharsets.UTF_8),start,end);System.out.println("Real page fixture rows selected: "+live.length());if(live.length()==0)throw new AssertionError("Live markup not parsed");for(int i=0;i<Math.min(3,live.length());i++)System.out.println(live.getJSONObject(i).getString("name")+" · "+live.getJSONObject(i).getString("channel"));}
  System.out.println("Parser timezone, Vietnam priority, channels, missing data, year rollover and markup-change checks passed.");
 }
}
