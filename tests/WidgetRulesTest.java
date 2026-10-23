package io.freemytv;
public class WidgetRulesTest {
    private static void lunar(int d,int m,int y,int ld,int lm,int ly,int leap){int[] v=VietnamLunar.fromSolar(d,m,y);if(v[0]!=ld||v[1]!=lm||v[2]!=ly||v[3]!=leap)throw new AssertionError(java.util.Arrays.toString(v));}
    public static void main(String[] args){
        lunar(10,2,2024,1,1,2024,0);lunar(29,1,2025,1,1,2025,0);lunar(17,2,2026,1,1,2026,0);
        lunar(22,3,2023,1,2,2023,1);lunar(25,1,2020,1,1,2020,0);lunar(21,9,2021,15,8,2021,0);
        for(int y=1900;y<=2099;y++)for(int m=1;m<=12;m++){int[] v=VietnamLunar.fromSolar(15,m,y);if(v[0]<1||v[0]>30||v[1]<1||v[1]>12)throw new AssertionError("Invalid lunar date");}
        if(FootballRules.priority("4328","140161","")!=0)throw new AssertionError();
        if(FootballRules.priority("0000","","144951")!=1)throw new AssertionError();
        if(FootballRules.priority("4803","","")!=2)throw new AssertionError();
        if(FootballRules.priority("4328","","")!=3)throw new AssertionError();
        if(FootballRules.priority("9999","","")!=-1)throw new AssertionError();
        if(!FootballRules.vietnamBroadcast("Vietnam")||FootballRules.vietnamBroadcast("United States"))throw new AssertionError();
        System.out.println("Lunar Tet/leap-month/200-year range and football scope/broadcast-region checks passed.");
    }
}
