package io.freemytv;

/** Vietnamese astronomical lunar calendar, local civil days at UTC+7.
 * New moon and apparent solar longitude are evaluated using low-order Meeus series.
 * Supported UI range: 1900..2099. No network or third-party calendar service.
 */
final class VietnamLunar {
    private static final double PI=Math.PI, SYNODIC=29.530588853;
    static int julian(int day,int month,int year){
        int a=(14-month)/12,y=year+4800-a,m=month+12*a-3;
        int jd=day+(153*m+2)/5+365*y+y/4-y/100+y/400-32045;
        if(jd<2299161)jd=day+(153*m+2)/5+365*y+y/4-32083;
        return jd;
    }
    private static double sin(double degrees){return Math.sin(degrees*PI/180);}
    private static int moon(int k){
        double t=k/1236.85,t2=t*t,t3=t2*t;
        double jd=2415020.75933+SYNODIC*k+.0001178*t2-.000000155*t3+.00033*sin(166.56+132.87*t-.009173*t2);
        double m=359.2242+29.10535608*k-.0000333*t2-.00000347*t3;
        double p=306.0253+385.81691806*k+.0107306*t2+.00001236*t3;
        double f=21.2964+390.67050646*k-.0016528*t2-.00000239*t3;
        double corr=(.1734-.000393*t)*sin(m)+.0021*sin(2*m)-.4068*sin(p)+.0161*sin(2*p)-.0004*sin(3*p)
            +.0104*sin(2*f)-.0051*sin(m+p)-.0074*sin(m-p)+.0004*sin(2*f+m)-.0004*sin(2*f-m)
            -.0006*sin(2*f+p)+.0010*sin(2*f-p)+.0005*sin(2*p+m);
        double dt=t< -11?.001+.000839*t+.0002261*t2-.00000845*t3-.000000081*t*t3:-.000278+.000265*t+.000262*t2;
        return (int)Math.floor(jd+corr-dt+.5+7.0/24);
    }
    private static int solar(int jd){
        double t=(jd-2451545.5-7.0/24)/36525,t2=t*t;
        double m=357.52910+35999.05030*t-.0001559*t2-.00000048*t*t2;
        double l=280.46645+36000.76983*t+.0003032*t2;
        l+=(1.914600-.004817*t-.000014*t2)*sin(m)+(.019993-.000101*t)*sin(2*m)+.000290*sin(3*m);
        l=(l%360+360)%360;return (int)Math.floor(l/30);
    }
    private static int month11(int year){int k=(int)Math.floor((julian(31,12,year)-2415021.076998695)/SYNODIC);int n=moon(k);return solar(n)>=9?moon(k-1):n;}
    private static int leapOffset(int a){int k=(int)Math.floor((a-2415021.076998695)/SYNODIC+.5),last=-1,arc=solar(moon(k+1)),i=1;while(arc!=last&&i<14){last=arc;i++;arc=solar(moon(k+i));}return i-1;}
    /** day, month, year, leap flag. */
    static int[] fromSolar(int day,int month,int year){
        if(year<1900||year>2099)throw new IllegalArgumentException("Calendar supports 1900..2099");
        int jd=julian(day,month,year),k=(int)Math.floor((jd-2415021.076998695)/SYNODIC);
        int start=moon(k+1);if(start>jd)start=moon(k);
        int a=month11(year),b=a,ly;
        if(a>=start){ly=year;a=month11(year-1);}else{ly=year+1;b=month11(year+1);}
        int ld=jd-start+1,diff=(int)Math.floor((start-a)/29.0),lm=diff+11,leap=0;
        if(b-a>365){int off=leapOffset(a);if(diff>=off){lm=diff+10;if(diff==off)leap=1;}}
        if(lm>12)lm-=12;if(lm>=11&&diff<4)ly--;
        return new int[]{ld,lm,ly,leap};
    }
    static String yearName(int year){String[] stems={"Giáp","Ất","Bính","Đinh","Mậu","Kỷ","Canh","Tân","Nhâm","Quý"};String[] branches={"Tý","Sửu","Dần","Mão","Thìn","Tỵ","Ngọ","Mùi","Thân","Dậu","Tuất","Hợi"};return stems[Math.floorMod(year+6,10)]+" "+branches[Math.floorMod(year+8,12)];}
}
