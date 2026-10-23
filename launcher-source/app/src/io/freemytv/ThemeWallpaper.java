package io.freemytv;
import android.graphics.*;
/** Original vectors shared by desktop and settings so the theme wallpaper stays identical. */
final class ThemeWallpaper {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path hill=new Path(),ribbon=new Path(),curve=new Path();
    boolean draw(Canvas c,String theme,float width,float height){
        p.reset();p.setAntiAlias(true);p.setColor(Color.WHITE);
        if("xp".equals(theme))xp(c,width,height);
        else if("vista".equals(theme)||"vista-widgets".equals(theme))vista(c,width,height);
        else if("macos".equals(theme)||"macos-widgets".equals(theme))macos(c,width,height);
        else if("xp-widgets".equals(theme))xp_widgets(c,width,height);
        else return false;
        return true;
    }
    private void xp(Canvas c,float width,float height){float w=width,h=height;c.save();c.scale(w/1280f,h/720f);
            p.setShader(new LinearGradient(0,0,0,500,new int[]{0xff0860bb,0xff58afea,0xffc2e4fa},null,Shader.TileMode.CLAMP));c.drawRect(0,0,1280,720,p);p.setShader(null);
            p.setColor(0x99ffffff);c.drawOval(720,100,1060,145,p);c.drawOval(810,75,990,145,p);c.drawOval(930,105,1165,148,p);p.setColor(0x77ffffff);c.drawOval(250,173,500,201,p);c.drawOval(330,150,455,198,p);
            p.setAlpha(255);hill.reset();hill.moveTo(0,490);hill.cubicTo(330,410,720,350,1280,495);hill.lineTo(1280,720);hill.lineTo(0,720);hill.close();p.setShader(new LinearGradient(0,390,0,720,0xff8dcc37,0xff377f13,Shader.TileMode.CLAMP));c.drawPath(hill,p);
            hill.reset();hill.moveTo(0,590);hill.cubicTo(400,630,900,395,1280,470);hill.lineTo(1280,720);hill.lineTo(0,720);hill.close();p.setShader(new LinearGradient(0,460,0,720,0xff67b82b,0xff2d721a,Shader.TileMode.CLAMP));c.drawPath(hill,p);p.setShader(null);c.restore();}
    private void vista(Canvas c,float width,float height){c.save();c.scale(width/1280f,height/720f);
            p.setShader(new LinearGradient(0,0,1100,720,new int[]{0xff061226,0xff124959,0xff1c6380,0xff070e25},null,Shader.TileMode.CLAMP));c.drawRect(0,0,1280,720,p);
            p.setShader(new RadialGradient(950,365,650,new int[]{0x885dd1ba,0x3343a3bb,0},null,Shader.TileMode.CLAMP));c.drawRect(0,0,1280,720,p);
            ribbon.reset();ribbon.moveTo(-100,730);ribbon.cubicTo(350,650,460,180,1380,80);ribbon.cubicTo(890,230,720,550,1280,670);ribbon.lineTo(1280,820);ribbon.close();p.setShader(new LinearGradient(300,700,1000,150,new int[]{0x003fb9ee,0x7743b8e5,0x4478d5b1,0x0055cdaf},null,Shader.TileMode.CLAMP));c.drawPath(ribbon,p);
            ribbon.reset();ribbon.moveTo(-60,675);ribbon.cubicTo(350,640,650,245,1370,140);p.setShader(null);p.setColor(0x5587e6f9);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawPath(ribbon,p);
            ribbon.reset();ribbon.moveTo(220,780);ribbon.cubicTo(500,490,750,440,1320,570);p.setColor(0x335beab8);p.setStrokeWidth(3);c.drawPath(ribbon,p);p.setStyle(Paint.Style.FILL);c.restore();}
    private void macos(Canvas c,float width,float height){c.save();c.scale(width/1280f,height/720f);
            p.setShader(new LinearGradient(100,0,1100,720,new int[]{0xff122a61,0xff566fbb,0xffe6adbe},null,Shader.TileMode.CLAMP));c.drawRect(0,0,1280,720,p);
            p.setShader(new RadialGradient(1020,110,700,new int[]{0xffffe9d4,0x99f79ca8,0},null,Shader.TileMode.CLAMP));c.drawRect(0,0,1280,720,p);
            curve.reset();curve.moveTo(-100,610);curve.cubicTo(120,0,500,-110,865,110);curve.cubicTo(520,150,420,450,500,820);curve.lineTo(-100,820);curve.close();p.setShader(new LinearGradient(100,200,700,700,new int[]{0xff203e94,0xff6b8fe8,0xffa6cdf5},null,Shader.TileMode.CLAMP));c.drawPath(curve,p);
            curve.reset();curve.moveTo(250,780);curve.cubicTo(500,260,880,160,1360,350);curve.lineTo(1360,780);curve.close();p.setShader(new LinearGradient(600,280,1100,800,new int[]{0xffbdc8f6,0xff788bd0,0xff242a64},null,Shader.TileMode.CLAMP));c.drawPath(curve,p);
            curve.reset();curve.moveTo(490,780);curve.cubicTo(780,330,1020,400,1350,550);curve.lineTo(1350,780);curve.close();p.setShader(new LinearGradient(800,430,1100,760,0xfff6d2dd,0xff9b79b2,Shader.TileMode.CLAMP));c.drawPath(curve,p);p.setShader(null);c.restore();}
    private void xp_widgets(Canvas c,float width,float height){float w=width,h=height;p.setShader(new LinearGradient(0,0,0,h,0xff1866b7,0xffa6d8f5,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);p.setColor(0x44ffffff);c.drawOval(.6f*w,.03f*h,.94f*w,.1f*h,p);p.setAlpha(255);Path hill=new Path();hill.moveTo(0,h*.7f);hill.cubicTo(w*.35f,h*.45f,w*.65f,h*.6f,w,h*.72f);hill.lineTo(w,h);hill.lineTo(0,h);hill.close();p.setShader(new LinearGradient(0,h*.6f,0,h,0xff6caa35,0xff2e7020,Shader.TileMode.CLAMP));c.drawPath(hill,p);p.setShader(null);}
}
