package io.freemytv;

import java.util.*;
import java.util.regex.*;

/** Pure parsers: missing or unsupported readings stay unknown, never become a measured zero. */
final class AppMetrics {
    static Map<String,List<Integer>> processes(String text){
        Map<String,List<Integer>> out=new HashMap<>();
        Matcher m=Pattern.compile("(?m)^\\s*(\\d+)\\s+([A-Za-z0-9_.]+(?::[^\\s]+)?)\\s*$").matcher(text);
        while(m.find()){String pkg=m.group(2).split(":",2)[0];if(!pkg.contains("."))continue;List<Integer> ids=out.get(pkg);if(ids==null){ids=new ArrayList<>();out.put(pkg,ids);}ids.add(Integer.parseInt(m.group(1)));}
        return out;
    }
    static Long pss(String text){
        Matcher m=Pattern.compile("TOTAL PSS:\\s*([0-9,]+)").matcher(text);long sum=0;boolean found=false;
        while(m.find()){sum+=Long.parseLong(m.group(1).replace(",",""));found=true;}
        if(!found){m=Pattern.compile("(?m)^\\s*TOTAL\\s+([0-9,]+)\\s").matcher(text);while(m.find()){sum+=Long.parseLong(m.group(1).replace(",",""));found=true;}}
        return found?sum*1024:null;
    }
    static long rate(long previous,long current,long millis){return previous<0||current<previous||millis<=0?-1:Math.round((current-previous)*1000d/millis);}
    static String bytes(long n){if(n<0)return "Chưa đọc được";if(n<1024)return n+" B";double d=n;String[] units={"B","KB","MB","GB","TB"};int i=0;while(d>=1024&&i<units.length-1){d/=1024;i++;}return String.format(Locale.ROOT,"%.1f %s",d,units[i]);}
}
