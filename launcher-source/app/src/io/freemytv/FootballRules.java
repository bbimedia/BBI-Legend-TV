package io.freemytv;
/** Fixed requested scope, independent of translations/provider display names. */
final class FootballRules {
    static final String[] LEAGUES={"4803","4328","4480","4332","4335","4331"};
    static final String[] LABELS={"V.League 1","Ngoại hạng Anh","UEFA Champions League","Serie A · Ý","La Liga · Tây Ban Nha","Bundesliga · Đức"};
    static int priority(String league,String home,String away){
        if("140161".equals(home)||"140161".equals(away))return 0;
        if("144951".equals(home)||"144951".equals(away))return 1;
        for(int i=0;i<LEAGUES.length;i++)if(LEAGUES[i].equals(league))return i+2;
        return -1;
    }
    static String label(int priority){return priority==0?"ĐT Việt Nam · Nam":priority==1?"ĐT Việt Nam · Nữ":priority>=2&&priority<8?LABELS[priority-2]:"Ngoài phạm vi";}
    static boolean vietnamBroadcast(String country){return "Vietnam".equalsIgnoreCase(country)||"Viet Nam".equalsIgnoreCase(country);}
}
