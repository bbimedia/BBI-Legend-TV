package io.freemytv;

import android.app.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

/** Shared widget desktop and data behavior, with XP, Vista Aero and macOS skins. */
final class WidgetHomeView extends FrameLayout {
    private final Activity activity;
    private final String theme;
    private final boolean mac,vista;
    private final float scale;
    private final FrameLayout canvas;
    private final Handler handler=new Handler();
    private final Click sound;
    private final XpWidgetsData data;
    private final ExecutorService appsLoader=Executors.newSingleThreadExecutor();
    private final List<HomeApps.Item> apps=new ArrayList<>();
    private final LinearLayout rail,footballRows,monthGrid;
    private final TextView clock,date,lunar,weather,weatherInfo,weatherSource,footballSource,goldValue,goldSource,stockValue,stockSource,taskClock;
    private final Chart goldChart,stockChart;
    private final Button footballTab,stockTab;
    private boolean week,alive=true,active,navigationKey;
    private int stockIndex;
    private View firstApp;
    private String monthKey="";
    private final android.view.ViewTreeObserver.OnGlobalFocusChangeListener focusSound=(oldView,newView)->{if(navigationKey&&oldView!=null&&newView!=null)soundMove();};
    private final Runnable ticker=new Runnable(){public void run(){if(!active)return;updateClock();data.refresh();handler.postDelayed(this,30000);}};
    WidgetHomeView(Activity a,String selectedTheme){
        super(a);theme=selectedTheme;mac="macos-widgets".equals(theme);vista="vista-widgets".equals(theme);activity=a;sound=new Click(a);data=new XpWidgetsData(a);
        scale=Math.min(a.getResources().getDisplayMetrics().widthPixels/1280f,a.getResources().getDisplayMetrics().heightPixels/720f);
        setBackgroundColor(0xff17467d);canvas=new FrameLayout(a);addView(canvas,new LayoutParams(s(1280),s(720),Gravity.CENTER));
        canvas.addView(new Landscape(a),new LayoutParams(-1,-1));
        Button refresh=button("↻ Làm mới");place(refresh,1060,70,180,30);refresh.setOnClickListener(v->{sound.open();data.requestRefresh();Toast.makeText(a,"Đang làm mới dữ liệu",Toast.LENGTH_SHORT).show();});
        rail=new LinearLayout(a);rail.setOrientation(LinearLayout.VERTICAL);place(rail,24,106,145,470);
        Button all=button("▦  Ứng dụng");place(all,24,589,145,45);all.setOnClickListener(v->programs());
        LinearLayout timeBody=window("◷  Thời gian",192,106,330,234);
        clock=text("--:--",61,0xff16458c,true);clock.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));timeBody.addView(clock);
        date=text("",18,0xff405670,false);timeBody.addView(date);
        lunar=text("",19,0xff166149,true);lunar.setPadding(0,s(10),0,0);timeBody.addView(lunar);
        TextView zone=text("GIỜ VIỆT NAM · UTC+7",12,0xff7b8793,true);zone.setPadding(0,s(8),0,0);timeBody.addView(zone);
        LinearLayout weatherBody=window("☀  Thời tiết",538,106,330,234);
        weather=text("Đang tải…",36,0xff1762a0,true);weatherBody.addView(weather);
        weatherInfo=text("",18,0xff405670,false);weatherBody.addView(weatherInfo);
        weatherSource=text("Open-Meteo · theo thành phố đã chọn",12,0xff6d7e91,false);weatherSource.setPadding(0,s(14),0,0);weatherBody.addView(weatherSource);
        LinearLayout calendarBody=window("▦  Lịch dương / âm",884,106,354,234);calendarBody.setPadding(s(10),s(6),s(10),s(4));
        monthGrid=new LinearLayout(a);monthGrid.setOrientation(LinearLayout.VERTICAL);calendarBody.addView(monthGrid,new LinearLayout.LayoutParams(-1,-1));
        calendarBody.setFocusable(true);focus(calendarBody);calendarBody.setOnClickListener(v->calendarDialog(0));
        LinearLayout footballBody=window("⚽  Bóng đá",192,358,330,278);
        footballTab=button("Hôm nay  ⇄  7 ngày tới");footballBody.addView(footballTab,new LinearLayout.LayoutParams(-1,s(34)));footballTab.setOnClickListener(v->{week=!week;renderFootball();sound.open();});
        footballRows=new LinearLayout(a);footballRows.setOrientation(LinearLayout.VERTICAL);footballBody.addView(footballRows,new LinearLayout.LayoutParams(-1,0,1));footballRows.setFocusable(true);focus(footballRows);footballRows.setOnClickListener(v->footballDialog());
        footballSource=text("VN ưu tiên · NHA / C1 / Ý / TBN / Đức",11,0xff718092,false);footballBody.addView(footballSource);
        LinearLayout goldBody=window("◈  Vàng miếng SJC · DOJI",538,358,330,278);
        goldValue=text("Đang tải giá…",21,0xff9a6c06,true);goldBody.addView(goldValue);
        TextView units=text("Mua / Bán · triệu đồng mỗi lượng",13,0xff6c7280,false);goldBody.addView(units);
        goldChart=new Chart(a,0xffc68b13);goldBody.addView(goldChart,new LinearLayout.LayoutParams(-1,0,1));
        goldSource=text("DOJI qua taichinh.com",11,0xff718092,false);goldBody.addView(goldSource);
        goldBody.setFocusable(true);focus(goldBody);goldBody.setOnClickListener(v->goldDialog());
        LinearLayout stockBody=window("↗  Chứng khoán Việt Nam",884,358,354,278);
        stockTab=button("VNINDEX  ›");stockBody.addView(stockTab,new LinearLayout.LayoutParams(-1,s(32)));stockTab.setOnClickListener(v->{stockIndex++;renderStocks();sound.open();});
        stockValue=text("Đang tải…",22,0xff17694c,true);stockBody.addView(stockValue);
        stockChart=new Chart(a,0xff258569);stockBody.addView(stockChart,new LinearLayout.LayoutParams(-1,0,1));
        stockSource=text("DNSE · giá đóng cửa theo phiên",11,0xff718092,false);stockBody.addView(stockSource);
        stockChart.setFocusable(true);focus(stockChart);stockChart.setOnClickListener(v->stockDialog());
        LinearLayout bar=new LinearLayout(a);bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(s(mac?14:0),s(mac?5:0),s(mac?14:0),s(mac?5:0));
        bar.setBackground(gradient(mac?0xddffffff:vista?0xdd203e53:0xff388bff,mac?0xbbe0e9fa:vista?0xee081b30:0xff1453d0,mac?22:0));
        bar.setElevation(s(12));place(bar,mac?192:0,664,mac?1046:1280,56);
        Button start=button(mac?"▦  Apps":vista?"◉  Bắt đầu":"▦  start");
        if(!mac){start.setTextColor(Color.WHITE);start.setTypeface(Typeface.DEFAULT,Typeface.BOLD_ITALIC);start.setBackground(gradient(vista?0xff5bb8d8:0xff69b853,vista?0xff125274:0xff23792d,vista?28:8));}
        bar.addView(start,new LinearLayout.LayoutParams(s(154),-1));start.setOnClickListener(v->programs());
        TextView task=text("",17,mac?0xff263e5a:Color.WHITE,true);bar.addView(task,new LinearLayout.LayoutParams(0,-2,1));
        Button settings=button("Cài đặt");bar.addView(settings,new LinearLayout.LayoutParams(s(170),s(38)));settings.setOnClickListener(v->launch(HomeApps.settings()));
        taskClock=text("",18,mac?0xff263e5a:Color.WHITE,true);taskClock.setGravity(Gravity.CENTER);bar.addView(taskClock,new LinearLayout.LayoutParams(s(115),-1));
        data.listen(this::render);getViewTreeObserver().addOnGlobalFocusChangeListener(focusSound);
        appsLoader.execute(()->{List<HomeApps.Item> result=HomeApps.shelf(a,false);result.addAll(HomeApps.others(a));handler.post(()->{if(!alive)return;apps.addAll(result);populateApps();});});
        updateClock();render();setFocusable(false);
    }
    private String edition(){return mac?"macOS Widgets":vista?"Vista Widgets":"XP Widgets";}
    private int s(float value){return Math.round(value*scale);}
    private void place(View v,int x,int y,int w,int h){LayoutParams p=new LayoutParams(s(w),s(h));p.leftMargin=s(x);p.topMargin=s(y);canvas.addView(v,p);}
    private GradientDrawable gradient(int top,int bottom,int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{top,bottom});d.setCornerRadius(s(radius));return d;}
    private TextView text(String label,int size,int color,boolean bold){TextView t=new TextView(activity);t.setText(label);t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,s(size));t.setTextColor(color);t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));return t;}
    private int radius(){return mac?16:vista?9:4;}
    private int focusTop(){return mac?0xffedf6ff:vista?0xffd5f5ff:0xffffedaa;}
    private int focusBottom(){return mac?0xffa9d2ff:vista?0xff86c9e8:0xffffca59;}
    private void focus(View v){final android.graphics.drawable.Drawable original=v.getBackground();v.setSoundEffectsEnabled(false);v.setOnFocusChangeListener((view,yes)->{view.setElevation(yes?s(8):0);view.setBackground(yes?gradient(focusTop(),focusBottom(),radius()):original);});}
    private Button button(String label){Button b=new Button(activity);b.setText(label);b.setAllCaps(false);b.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,s(15));b.setTextColor(0xff164379);b.setPadding(s(6),0,s(6),0);b.setMinHeight(0);b.setMinimumHeight(0);b.setSoundEffectsEnabled(false);b.setBackground(gradient(0xfffafaf2,0xffdce6ec,radius()));b.setClipToOutline(true);b.setOnFocusChangeListener((v,f)->b.setBackground(gradient(f?focusTop():0xfffafaf2,f?focusBottom():0xffdce6ec,radius())));return b;}
    private LinearLayout window(String label,int x,int y,int w,int h){
        LinearLayout frame=new LinearLayout(activity);frame.setOrientation(LinearLayout.VERTICAL);frame.setPadding(s(mac?1:3),s(mac?1:3),s(mac?1:3),s(mac?1:3));
        frame.setBackground(gradient(mac?0xeef6f8ff:vista?0xcc80c4da:0xff2779ef,mac?0xeedfe9f6:vista?0xdd174259:0xff1046b5,mac?18:vista?12:7));frame.setClipToOutline(true);frame.setElevation(s(mac?10:7));place(frame,x,y,w,h);
        LinearLayout header=new LinearLayout(activity);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(s(10),0,s(8),0);header.setBackground(gradient(mac?0xfff0f3fa:vista?0xaa76b8cf:0xff4098fc,mac?0xffdce4ef:vista?0xcc255569:0xff0960dd,mac?16:vista?8:5));
        if(mac){for(int color:new int[]{0xffff6058,0xffffbd2e,0xff28c840}){View dot=new View(activity);GradientDrawable d=new GradientDrawable();d.setShape(GradientDrawable.OVAL);d.setColor(color);dot.setBackground(d);LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(s(8),s(8));dp.rightMargin=s(4);header.addView(dot,dp);}}
        TextView title=text(label,mac?14:16,mac?0xff324663:Color.WHITE,true);title.setPadding(s(mac?6:0),0,0,0);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));frame.addView(header,new LinearLayout.LayoutParams(-1,s(34)));
        LinearLayout body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);body.setBackgroundColor(mac?0xf2f5f8ff:vista?0xeef0f8fc:0xfff9faf3);body.setPadding(s(16),s(10),s(16),s(10));frame.addView(body,new LinearLayout.LayoutParams(-1,0,1));return body;
    }
    private void populateApps(){rail.removeAllViews();firstApp=null;Set<String> seen=new HashSet<>();for(HomeApps.Item item:apps){if(item.kind!=HomeApps.APP||!seen.add(item.key))continue;if(rail.getChildCount()>=5)break;LinearLayout tile=new LinearLayout(activity);tile.setOrientation(LinearLayout.VERTICAL);tile.setGravity(Gravity.CENTER);tile.setFocusable(true);tile.setContentDescription(item.label);focus(tile);ImageView icon=new RoundedAppIcon(activity);icon.setScaleType(ImageView.ScaleType.FIT_CENTER);if(item.logo!=null)icon.setImageBitmap(item.logo);else if(item.tile!=null)icon.setImageBitmap(item.tile);GradientDrawable shape=gradient(0x22ffffff,0x11ffffff,12);icon.setBackground(shape);icon.setClipToOutline(true);tile.addView(icon,new LinearLayout.LayoutParams(s(76),s(48)));TextView name=text(item.label,14,Color.WHITE,true);name.setMaxLines(1);name.setEllipsize(android.text.TextUtils.TruncateAt.END);name.setGravity(Gravity.CENTER);tile.addView(name,new LinearLayout.LayoutParams(-1,s(27)));tile.setOnClickListener(v->launch(item));rail.addView(tile,new LinearLayout.LayoutParams(-1,s(88)));if(firstApp==null)firstApp=tile;}if(firstApp!=null)firstApp.requestFocus();}
    private void launch(HomeApps.Item item){sound.open();HomeApps.open(activity,item,this,0,0,getWidth(),getHeight());}
    private void programs(){sound.open();AppCatalogActivity.open(activity,false);}

    private void dialog(String title,View content){sound.open();ScrollView scroll=new ScrollView(activity);scroll.setPadding(s(18),s(10),s(18),s(10));scroll.addView(content);AlertDialog d=new AlertDialog.Builder(activity,android.R.style.Theme_Material_Light_Dialog_Alert).setTitle(title).setView(scroll).setPositiveButton("Đóng",null).create();d.show();d.getWindow().setLayout(s(950),s(580));}
    private void message(String title,String body){TextView t=text(body,20,0xff263e5a,false);t.setPadding(s(10),s(8),s(10),s(8));dialog(title,t);}
    private void updateClock(){long now=System.currentTimeMillis();clock.setText(XpWidgetsData.date(now,"HH:mm"));taskClock.setText(XpWidgetsData.date(now,"HH:mm"));date.setText(XpWidgetsData.date(now,"EEEE, dd/MM/yyyy"));Calendar c=Calendar.getInstance(XpWidgetsData.VN);try{int[] l=VietnamLunar.fromSolar(c.get(Calendar.DAY_OF_MONTH),c.get(Calendar.MONTH)+1,c.get(Calendar.YEAR));lunar.setText("Âm lịch  "+l[0]+" / "+l[1]+(l[3]==1?" nhuận":"")+" · "+VietnamLunar.yearName(l[2]));}catch(Exception e){lunar.setText("Âm lịch hỗ trợ 1900–2099");}String key=XpWidgetsData.date(now,"yyyy-MM-dd");if(!key.equals(monthKey)){monthKey=key;drawMonth(monthGrid,0,false);renderFootball();}}
    private void drawMonth(LinearLayout host,int offset,boolean large){host.removeAllViews();Calendar c=Calendar.getInstance(XpWidgetsData.VN);c.set(Calendar.DAY_OF_MONTH,1);c.add(Calendar.MONTH,offset);int year=c.get(Calendar.YEAR),month=c.get(Calendar.MONTH),today=Calendar.getInstance(XpWidgetsData.VN).get(Calendar.DAY_OF_MONTH);TextView label=text("Tháng "+(month+1)+" · "+year+(large?"  (số nhỏ: âm lịch)":""),large?24:15,0xff1d4e96,true);host.addView(label);String[] days={"T2","T3","T4","T5","T6","T7","CN"};LinearLayout names=new LinearLayout(activity);for(String name:days){TextView t=text(name,large?18:11,0xff72869c,true);t.setGravity(Gravity.CENTER);names.addView(t,new LinearLayout.LayoutParams(0,large?s(32):s(20),1));}host.addView(names);int first=(c.get(Calendar.DAY_OF_WEEK)+5)%7,max=c.getActualMaximum(Calendar.DAY_OF_MONTH);for(int row=0;row<6;row++){LinearLayout line=new LinearLayout(activity);for(int col=0;col<7;col++){int day=row*7+col-first+1;String value="";if(day>=1&&day<=max){value=String.valueOf(day);if(large)try{int[] l=VietnamLunar.fromSolar(day,month+1,year);value+="\n"+l[0]+"/"+l[1]+(l[3]==1?"n":"");}catch(Exception ignored){}}TextView cell=text(value,large?21:14,col==6?0xffc35945:0xff284d72,false);cell.setGravity(Gravity.CENTER);if(offset==0&&day==today){cell.setBackground(gradient(0xff458fed,0xff2265c7,4));cell.setTextColor(Color.WHITE);}line.addView(cell,new LinearLayout.LayoutParams(0,large?s(57):s(24),1));}host.addView(line);}}
    private void calendarDialog(int offset){LinearLayout body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);LinearLayout controls=new LinearLayout(activity),grid=new LinearLayout(activity);grid.setOrientation(LinearLayout.VERTICAL);int[] page={offset};Button prev=button("‹ Tháng trước"),next=button("Tháng sau ›");controls.addView(prev,new LinearLayout.LayoutParams(0,s(44),1));controls.addView(next,new LinearLayout.LayoutParams(0,s(44),1));prev.setOnClickListener(v->drawMonth(grid,--page[0],true));next.setOnClickListener(v->drawMonth(grid,++page[0],true));body.addView(controls);body.addView(grid);drawMonth(grid,offset,true);dialog("Lịch dương / âm Việt Nam · UTC+7",body);}
    private String source(JSONObject b,String label){if(b.optBoolean("loading"))return label+" · đang tải "+b.optInt("days_loaded")+"/7 ngày";long at=b.optLong("fetched");return label+(at>0?" · tải "+XpWidgetsData.date(at,"HH:mm dd/MM"):" · chưa có dữ liệu")+(b.has("error")?" · bản lưu / lỗi mạng":"");}
    private void render(){renderWeather();renderFootball();renderGold();renderStocks();}
    private void renderWeather(){JSONObject b=data.block("weather");if(!Prefs.weather()){weather.setText("Đã tắt thời tiết");weatherInfo.setText("Bật trong Cài đặt nhanh");return;}double temp=b.optDouble("temp",Double.NaN);boolean f="F".equals(Prefs.temperature());weather.setText(Double.isFinite(temp)?String.format(Locale.ROOT,"%.0f°%s  ·  %s",f?temp*1.8+32:temp,f?"F":"C",condition(b.optInt("code"))):"Chưa có dữ liệu");weatherInfo.setText(Prefs.city()+(Double.isFinite(temp)?String.format(Locale.ROOT,"\nCao %.0f° · Thấp %.0f° · Ẩm %.0f%%",f?b.optDouble("max")*1.8+32:b.optDouble("max"),f?b.optDouble("min")*1.8+32:b.optDouble("min"),b.optDouble("humidity")):"\nKiểm tra kết nối Internet"));weatherSource.setText(source(b,"Open-Meteo")+(!b.optString("observed").isEmpty()?"\nQuan trắc "+b.optString("observed").replace('T',' '):""));}
    private String condition(int code){if(code==0)return "Nắng";if(code<=3&&code>=0)return "Có mây";if(code==45||code==48)return "Sương mù";if(code>=95)return "Dông";if(code>=71&&code<=77)return "Tuyết";return code>=0?"Mưa":"--";}
    private List<JSONObject> events(){List<JSONObject> result=new ArrayList<>();JSONArray rows=data.block("football").optJSONArray("events");String today=XpWidgetsData.date(System.currentTimeMillis(),"yyyy-MM-dd");Calendar c=Calendar.getInstance(XpWidgetsData.VN);c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);long from=c.getTimeInMillis(),until=from+(week?7L:1L)*86400000;if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject e=rows.optJSONObject(i);if(e!=null&&e.optLong("at")>=from&&e.optLong("at")<until)result.add(e);}return result;}
    private String eventLine(JSONObject e){String score=e.optString("home"),away=e.optString("away");boolean scored=!score.isEmpty()&&!score.equals("null")&&!away.isEmpty()&&!away.equals("null");return (e.optBoolean("time_known",true)?XpWidgetsData.date(e.optLong("at"),week?"dd/MM HH:mm":"HH:mm"):XpWidgetsData.date(e.optLong("at"),"dd/MM")+" chưa chốt giờ")+"  "+e.optString("name")+(scored?"  "+score+"–"+away:"");}
    private void renderFootball(){JSONObject b=data.block("football");footballTab.setText(week?"7 ngày tới  ⇄  Hôm nay":"Hôm nay  ⇄  7 ngày tới");footballRows.removeAllViews();List<JSONObject> rows=events();for(int i=0;i<Math.min(2,rows.size());i++){JSONObject e=rows.get(i);LinearLayout entry=new LinearLayout(activity);entry.setOrientation(LinearLayout.VERTICAL);entry.setPadding(0,s(5),0,0);TextView t=text(eventLine(e),14,0xff234f80,true);t.setMaxLines(1);t.setEllipsize(android.text.TextUtils.TruncateAt.END);TextView channel=text(e.optString("channel","Chưa có thông tin kênh tại Việt Nam"),12,0xff6c7e91,false);channel.setMaxLines(1);channel.setEllipsize(android.text.TextUtils.TruncateAt.END);entry.addView(t);entry.addView(channel);footballRows.addView(entry,new LinearLayout.LayoutParams(-1,s(60)));}if(rows.isEmpty())footballRows.addView(text("Chưa có trận trong dữ liệu nhận được.\nOK để xem chi tiết nguồn.",16,0xff6c7c91,false));footballSource.setText(source(b,"Bongda24h · lịch / kênh theo từng trận")+(b.optBoolean("loading")?" · đang tải thêm":"")+"\nOK danh sách · Giờ Việt Nam");}
    private void footballDialog(){
        LinearLayout body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);
        TextView note=text("Giờ Việt Nam · ĐT nam/nữ & V.League ưu tiên. Kênh là lịch phát từng trận ở Việt Nam, không phải suy đoán theo bản quyền.",16,0xff5a7188,false);body.addView(note);
        LinearLayout filters=new LinearLayout(activity),list=new LinearLayout(activity);list.setOrientation(LinearLayout.VERTICAL);
        String[] names={"Tất cả","Việt Nam","NHA","C1","Ý","TBN","Đức"};
        for(int i=0;i<names.length;i++){final int choice=i;Button b=button(names[i]);filters.addView(b,new LinearLayout.LayoutParams(0,s(42),1));b.setOnClickListener(v->fillFootballList(list,choice));}
        body.addView(filters);body.addView(list);
        LinearLayout official=new LinearLayout(activity);for(String[] entry:new String[][]{{"Lịch Bongda24h", "https://bongda24h.vn/bong-da/lich-thi-dau.html"},{"Lịch VPF", "https://vpf.vn/"},{"Đội tuyển · VFF", "https://vff.org.vn/"}}){Button b=button(entry[0]);official.addView(b,new LinearLayout.LayoutParams(0,s(44),1));b.setOnClickListener(v->{try{activity.startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(entry[1])));}catch(Exception e){Toast.makeText(activity,"TV chưa có ứng dụng mở trang web",Toast.LENGTH_SHORT).show();}});}body.addView(official);fillFootballList(list,0);dialog("Lịch bóng đá · "+(week?"7 ngày tới":"Hôm nay"),body);
    }
    private void fillFootballList(LinearLayout host,int choice){host.removeAllViews();int count=0;JSONObject b=data.block("football");for(JSONObject e:events()){
        int rank=e.optInt("priority",99);boolean accepted=choice==0||(choice==1&&rank<=2)||(choice>=2&&rank==choice+1);if(!accepted)continue;count++;
        String line=eventLine(e)+(e.optBoolean("postponed")?" · HOÃN":"")+"\n"+e.optString("league")+"\nKênh VN: "+e.optString("channel","Chưa có thông tin kênh tại Việt Nam");TextView t=text(line,20,0xff254d76,false);t.setPadding(s(8),s(12),s(8),s(12));host.addView(t);
    }if(count==0)host.addView(text("Chưa có trận trong phần dữ liệu nhận được; không có nghĩa là không có trận đấu.",19,0xff6c7e91,false));
        host.addView(text("Nguồn: bongda24h.vn/bong-da/lich-thi-dau.html. Giờ và kênh được đọc từ từng trận; không suy đoán kênh. Chỉ hiện các giải đã chọn và các trận Việt Nam nam/nữ có trong lịch nguồn. Lịch/kênh có thể đổi hoặc còn thiếu; không cung cấp luồng phát.\n"+source(b,"Lần tải")+(b.optBoolean("loading")?" · đang tải tiếp, mở lại sau khoảng 20 giây":"")+(b.optBoolean("partial")?" · một số nguồn tải lỗi":""),14,0xff738497,false));
    }
    private void renderGold(){JSONObject b=data.block("gold");double buy=b.optDouble("buy",Double.NaN),sell=b.optDouble("sell",Double.NaN);goldValue.setText(Double.isFinite(sell)?String.format(Locale.ROOT,"%.2f  /  %.2f",buy,sell):"Chưa có giá");goldChart.points=b.optJSONArray("history");goldChart.invalidate();goldSource.setText("DOJI qua taichinh.com"+(b.optLong("at")>0?"\nGiá bán "+XpWidgetsData.date(b.optLong("at"),"HH:mm dd/MM/yyyy"):"\nChưa có dữ liệu")+(b.has("error")?" · bản lưu":"")+"\nBiểu đồ bán · tích lũy trên TV · OK chi tiết");}
    private void goldDialog(){JSONObject b=data.block("gold");message("Vàng miếng SJC · DOJI",goldValue.getText()+" triệu đồng/lượng (mua / bán)\n\nNguồn công bố: "+b.optString("publisher","DOJI")+" · truy cập qua taichinh.com/api/v1/gold.\nGiá mua: "+(b.optLong("buy_at")>0?XpWidgetsData.date(b.optLong("buy_at"),"HH:mm dd/MM/yyyy"):"chưa có")+"\nGiá bán: "+(b.optLong("at")>0?XpWidgetsData.date(b.optLong("at"),"HH:mm dd/MM/yyyy"):"chưa có")+"\n\nĐường giá bán tích lũy các mốc công bố nhận được trên TV, tối đa 30 ngày/240 điểm; không phải lịch sử đầy đủ. Cần ít nhất hai mốc để vẽ. Chưa có dữ liệu không được thay bằng giá mẫu.\n\n"+source(b,"Lần tải")+"\nChỉ tham khảo, có thể trễ; không phải tư vấn đầu tư.");}
    private JSONObject stock(){JSONArray rows=data.block("stocks").optJSONArray("rows");return rows==null||rows.length()==0?new JSONObject():rows.optJSONObject(Math.floorMod(stockIndex,rows.length()));}
    private void renderStocks(){JSONObject b=data.block("stocks"),q=stock();boolean index=q.optBoolean("index");double value=q.optDouble("value",Double.NaN),change=q.optDouble("change");stockTab.setText(q.optString("symbol","VNINDEX")+"   ›");stockValue.setText(Double.isFinite(value)?String.format(Locale.ROOT,"%,.2f %s  %+.2f%%",value,index?"điểm":"nghìn đ",change):"Chưa có giá");stockValue.setTextColor(change<0?0xffb84942:0xff17694c);stockChart.points=q.optJSONArray("points");stockChart.invalidate();stockSource.setText("DNSE · đóng cửa từng phiên"+(q.optLong("at")>0?"\nPhiên "+XpWidgetsData.date(q.optLong("at"),"dd/MM/yyyy"):"\nChưa có dữ liệu")+(b.has("error")?" · bản lưu":"")+(b.optBoolean("partial")?" · thiếu mã":"")+"\n30 phiên gần · OK biểu đồ để xem chi tiết");}
    private void stockDialog(){JSONObject b=data.block("stocks"),q=stock();message("Thị trường Việt Nam · "+q.optString("symbol"),stockValue.getText()+"\n\nNguồn: DNSE / Entrade Chart API. Giá đóng cửa, không phải bảng điện trực tiếp. Chỉ số tính bằng điểm; cổ phiếu nghìn đồng. Phần trăm so với phiên trước trong chuỗi, có thể chịu ảnh hưởng điều chỉnh giá.\nPhiên mới nhất: "+(q.optLong("at")>0?XpWidgetsData.date(q.optLong("at"),"dd/MM/yyyy"):"chưa có")+"\n"+source(b,"Tải nguồn")+"\n\nChọn nút tên mã để chuyển VNINDEX, VN30, FPT, VCB, HPG.\nChỉ tham khảo, dữ liệu có thể trễ hoặc gián đoạn; không phải tư vấn đầu tư.");}
    void resume(){active=true;handler.removeCallbacks(ticker);ticker.run();}
    void pause(){active=false;handler.removeCallbacks(ticker);}
    void destroy(){alive=false;active=false;handler.removeCallbacksAndMessages(null);data.close();appsLoader.shutdownNow();getViewTreeObserver().removeOnGlobalFocusChangeListener(focusSound);sound.release();}
    void reset(boolean focus){if(focus&&firstApp!=null)firstApp.requestFocus();}
    private void soundMove(){sound.move();}
    boolean onKey(KeyEvent e){int key=e.getKeyCode();if(key>=KeyEvent.KEYCODE_DPAD_UP&&key<=KeyEvent.KEYCODE_DPAD_RIGHT){navigationKey=e.getAction()==KeyEvent.ACTION_DOWN;return false;}navigationKey=false;if(key==KeyEvent.KEYCODE_BACK||key==KeyEvent.KEYCODE_MENU){if(e.getAction()==KeyEvent.ACTION_DOWN&&e.getRepeatCount()==0)programs();return true;}return false;}
    /** Clip the fitted image itself so letterboxed app banners also get rounded corners. */
    private final class RoundedAppIcon extends ImageView {
        private final Path rounded = new Path();
        private final RectF imageBounds = new RectF();
        RoundedAppIcon(Activity a) { super(a); }
        @Override protected void onDraw(Canvas canvas) {
            android.graphics.drawable.Drawable drawable = getDrawable();
            if (drawable == null || drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
                super.onDraw(canvas); return;
            }
            float fit = Math.min((float)getWidth()/drawable.getIntrinsicWidth(), (float)getHeight()/drawable.getIntrinsicHeight());
            float w = drawable.getIntrinsicWidth()*fit, h = drawable.getIntrinsicHeight()*fit;
            imageBounds.set((getWidth()-w)/2f,(getHeight()-h)/2f,(getWidth()+w)/2f,(getHeight()+h)/2f);
            float radius = Math.min(s(13),h*0.24f);
            rounded.reset(); rounded.addRoundRect(imageBounds,radius,radius,Path.Direction.CW);
            int save=canvas.save(); canvas.clipPath(rounded); super.onDraw(canvas); canvas.restoreToCount(save);
        }
    }

    private final class Chart extends View {
        JSONArray points;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final int color;
        Chart(Activity a,int c){super(a);color=c;}
        protected void onDraw(Canvas canvas){super.onDraw(canvas);float w=getWidth(),h=getHeight();if(w<=0||h<=0)return;paint.setTypeface(Typeface.DEFAULT);paint.setTextSize(s(11));paint.setColor(0xff82909c);int count=points==null?0:points.length();if(count<2){canvas.drawText(count==1?"Đang tích lũy · cần thêm mốc giá":"Chưa có dữ liệu biểu đồ",s(4),h/2,paint);return;}
            int begin=Math.max(0,count-30);double lo=Double.MAX_VALUE,hi=-Double.MAX_VALUE;long first=points.optJSONObject(begin).optLong("at"),last=points.optJSONObject(count-1).optLong("at");for(int i=begin;i<count;i++){double v=points.optJSONObject(i).optDouble("value");lo=Math.min(lo,v);hi=Math.max(hi,v);}double range=hi-lo;if(range<.0001)range=Math.max(1,Math.abs(hi)*.005);float top=s(18),bottom=h-s(25),left=s(5),right=w-s(6);if(bottom<=top)return;paint.setStrokeWidth(s(1));paint.setColor(0xffe0e6e9);for(int i=0;i<3;i++){float y=top+(bottom-top)*i/2;canvas.drawLine(left,y,right,y,paint);}Path line=new Path();for(int i=begin;i<count;i++){JSONObject p=points.optJSONObject(i);float x=last>first?(float)(left+(right-left)*(p.optLong("at")-first)/(double)(last-first)):(left+right)/2;float y=(float)(bottom-(bottom-top)*(p.optDouble("value")-lo)/range);if(i==begin)line.moveTo(x,y);else line.lineTo(x,y);}paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(s(2));canvas.drawPath(line,paint);paint.setStyle(Paint.Style.FILL);paint.setColor(0xff64788e);paint.setTextSize(s(10));canvas.drawText(String.format(Locale.ROOT,"%.2f – %.2f",lo,hi),left,s(11),paint);canvas.drawText(XpWidgetsData.date(first,"dd/MM"),left,h-s(6),paint);String end=XpWidgetsData.date(last,"dd/MM");canvas.drawText(end,right-paint.measureText(end),h-s(6),paint);
        }
    }
    /** Original vector hills and clouds; no Microsoft wallpaper assets. */
    private final class Landscape extends View {private final ThemeWallpaper shared=new ThemeWallpaper();private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);Landscape(Activity a){super(a);}protected void onDraw(Canvas c){shared.draw(c,theme,getWidth(),getHeight());}}
}
