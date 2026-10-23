"""Generate UI-native SVG catalog previews; placeholder data and no private network names."""
from pathlib import Path
import base64
import xml.etree.ElementTree as E
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'public/assets'
NS='http://www.w3.org/2000/svg'
E.register_namespace('',NS)
def node(parent,tag,**attrs):return E.SubElement(parent,'{'+NS+'}'+tag,{k.replace('_','-'):str(v) for k,v in attrs.items()})
def rect(g,x,y,w,h,fill,r=0,**attrs):return node(g,'rect',x=x,y=y,width=w,height=h,fill=fill,rx=r,**attrs)
def text(g,value,x,y,size=17,color='white',**attrs):
 e=node(g,'text',x=x,y=y,font_size=size,fill=color,**attrs);e.text=value;return e
def gradient(defs,key,colors,x1=0,y1=0,x2=0,y2=720):
 g=node(defs,'linearGradient',id=key,gradientUnits='userSpaceOnUse',x1=x1,y1=y1,x2=x2,y2=y2)
 for i,color in enumerate(colors):node(g,'stop',offset=i/(len(colors)-1),stop_color=color)
 return 'url(#'+key+')'
def wallpaper(g,defs,theme,widgets):
 if theme=='xp':
  sky=gradient(defs,'sky',['#1866b7','#a6d8f5']) if widgets else gradient(defs,'sky',['#0860bb','#58afea','#c2e4fa'],y2=500)
  rect(g,0,0,1280,720,sky)
  node(g,'ellipse',cx=990,cy=50,rx=215,ry=27,fill='white',opacity='.27')
  hill=gradient(defs,'hill',['#6caa35','#2e7020'],y1=430)
  node(g,'path',d='M0 504 C448 324 832 432 1280 518V720H0Z' if widgets else 'M0 490C330 410 720 350 1280 495V720H0Z',fill=hill)
  if not widgets:node(g,'path',d='M0 590C400 630 900 395 1280 470V720H0Z',fill=gradient(defs,'hill2',['#67b82b','#2d721a'],y1=460))
 elif theme=='vista':
  rect(g,0,0,1280,720,gradient(defs,'sky',['#061226','#124959','#1c6380','#070e25'],x2=1100))
  node(g,'path',d='M-100 730C350 650 460 180 1380 80C890 230 720 550 1280 670V820Z',fill=gradient(defs,'aero',['#277a9a','#62b9ba'],x2=1000,y1=720,y2=150),opacity='.4')
  node(g,'path',d='M-60 675C350 640 650 245 1370 140M220 780C500 490 750 440 1320 570',fill='none',stroke='#87e6f9',stroke_width=2,opacity='.35')
 elif theme=='ubuntu':
  rect(g,0,0,1280,720,gradient(defs,'sky',['#1d0618','#3d0d33','#6a1d55','#2a0820'],x2=1280,y2=720))
  node(g,'circle',cx=1080,cy=640,r=620,fill=gradient(defs,'glow',['#f26a2e','#e95420','#2a0820'],x1=1080,y1=460,x2=520,y2=180),opacity='.55')
  node(g,'path',d='M-80 560C260 420 520 640 840 470C1030 370 1170 250 1380 290V420C1160 390 1040 520 850 600C540 730 280 540 -80 690Z',fill=gradient(defs,'rib1',['#e95420','#f07a3c','#ff9a5c'],x1=120,y1=600,x2=1220,y2=320),opacity='.42')
  node(g,'path',d='M-60 360C300 250 600 420 900 300C1080 230 1200 130 1360 150V215C1190 215 1070 330 880 385C590 470 300 330 -60 440Z',fill=gradient(defs,'rib2',['#c34fa0','#e95420'],x1=180,y1=400,x2=1180,y2=150),opacity='.3')
  node(g,'path',d='M-60 630C280 480 540 690 850 535C1040 440 1170 320 1360 350M-60 400C300 290 600 445 890 340C1070 270 1190 175 1360 182',fill='none',stroke='#ffd2b8',stroke_width=2,opacity='.35')
 else:
  rect(g,0,0,1280,720,gradient(defs,'sky',['#122a61','#566fbb','#e6adbe'],x1=100,x2=1100))
  node(g,'path',d='M-100 610C120 0 500-110 865 110C520 150 420 450 500 820H-100Z',fill=gradient(defs,'curve1',['#203e94','#6b8fe8','#a6cdf5'],x1=100,y1=200,x2=700,y2=700))
  node(g,'path',d='M250 780C500 260 880 160 1360 350V780Z',fill=gradient(defs,'curve2',['#bdc8f6','#788bd0','#242a64'],x1=600,y1=280,x2=1100,y2=800))
  node(g,'path',d='M490 780C780 330 1020 400 1350 550V780Z',fill=gradient(defs,'curve3',['#f6d2dd','#9b79b2'],x1=800,y1=430,x2=1100,y2=760))
def quickbar(g,defs,theme):
 palettes={'xp':('#4e9eff','#0753cb','#4998ff','#1461d1','#86b9fb','white',0,4),'vista':('#85c6d1','#173445','#7ad0e0','#334f62','#95d0df','#f3fbff',10,7),'macos':('#ffffff','#dce6f8','#ffffff','#edf4ff','#596d90','#253c59',18,12),'ubuntu':('#141414','#0a0a0a','#2c2c2c','#202020','#3a3a3a','#f2f2f2',0,18),'default':('#223751','#102035','#3b5874','#233a52','#526e8d','white',0,9)}
 a,b,c,d,stroke,ink,r,br=palettes[theme];inset=12 if theme in ('vista','macos') else 0;top=5 if inset else 0;h=56 if inset else 64
 rect(g,inset,top,1280-inset*2,h,gradient(defs,'bar',[a,b],y2=64),r,fill_opacity='.86' if inset else 1,stroke=stroke,stroke_opacity='.5')
 x=inset+18
 for w,label in [(584-inset*2,'Wi-Fi    ↓ —    ↑ —'),(170,'⇄ Đổi nguồn'),(145,'▣ Video'),(160,'▦ Ứng dụng'),(145,'⚙ Cài đặt')]:
  rect(g,x,top+6,w,h-12,gradient(defs,'button'+str(x),[c,d],y2=64),br,fill_opacity='.22' if inset else 1,stroke=stroke,stroke_opacity='.45');text(g,label,x+w/2,top+h/2+5,15,ink,text_anchor='middle');x+=w+10
APPS=[('Netflix','#b91323','N'),('YouTube','#e52c2e','▶'),('CH Play','#f6f9fc','▷'),('FPT Play','#fb7820','FPT'),('SCTV Online','#202839','SCTV')]
def app(g,x,y,label,color,mark,w=100,h=54,label_color='white'):
 rect(g,x,y,w,h,color,12);text(g,mark,x+w/2,y+h*.7,27,'#228569' if label=='CH Play' else 'white',text_anchor='middle',font_weight='bold');text(g,label,x+w/2,y+h+22,15,label_color,text_anchor='middle')

def showcase(g, defs, theme, x, y, w, h):
    xp = theme == 'xp'
    vista = theme == 'vista'
    mac = theme == 'macos'
    ubuntu = theme == 'ubuntu'
    bg = '#ffffff' if mac else '#1b1b1b' if ubuntu else '#0e2330' if vista else '#eef4fb'
    stroke = '#ffffff' if mac else '#3a3a3a' if ubuntu else '#5aaec4' if vista else '#7ba4e0'
    text_c = '#253c59' if mac else '#ffffff' if (ubuntu or vista) else '#153c76'
    sub_c = '#596d90' if mac else '#a0a0a0' if ubuntu else '#88b5c7' if vista else '#456a99'
    accent = '#007aff' if mac else '#e95420' if ubuntu else '#008eb5' if vista else '#0d4fb5'

    rect(g, x, y, w, h, bg, 16 if mac else 12 if ubuntu else 8, fill_opacity='.88' if (ubuntu or vista) else '.94', stroke=stroke, stroke_width=1)

    # Header
    rect(g, x + 16, y + 14, 40, 28, '#e50914', 6)
    text(g, 'N', x + 36, y + 34, 18, 'white', text_anchor='middle', font_weight='bold')
    text(g, 'GỢI Ý NỘI DUNG TỪ NETFLIX', x + 66, y + 28, 14, text_c, font_weight='bold')
    text(g, '6 nội dung nổi bật & được xem nhiều nhất hôm nay', x + 66, y + 43, 11, sub_c)
    rect(g, x + w - 105, y + 16, 88, 22, accent, 6)
    text(g, 'THỊNH HÀNH', x + w - 61, y + 31, 10, 'white', text_anchor='middle', font_weight='bold')

    # 6 Real Content Cards in 2 rows x 3 columns
    items = [
        ('Stranger Things 5', 'Viễn tưởng • ★ 8.9', '4K HDR', '#6d0e0e', '#2a0808'),
        ('Squid Game 2', 'Sinh tồn • ★ 8.5', 'HOT', '#0f4f46', '#09231f'),
        ('One Piece Live Action', 'Phiêu lưu • ★ 8.6', '4K', '#5c2f0e', '#2a1608'),
        ('Wednesday Season 2', 'Bí ẩn • ★ 8.4', 'MỚI', '#2b1b54', '#12093a'),
        ('The Witcher S4', 'Hành động • ★ 8.2', 'TOP 10', '#1c2e3d', '#0c1012'),
        ('Red Notice 2', 'Bom tấn • ★ 7.9', 'BOM TẤN', '#4a1515', '#210505'),
    ]

    cw = int((w - 48) / 3)
    card_h = int((h - 96) / 2)

    for i, (title, meta, badge, ctop, cbot) in enumerate(items):
        r = i // 3
        c_idx = i % 3
        cx = x + 16 + c_idx * (cw + 8)
        cy = y + 54 + r * (card_h + 8)

        # Card container
        rect(g, cx, cy, cw, card_h, '#000000', 8, fill_opacity='.35', stroke=accent if i==0 else 'none', stroke_width=2 if i==0 else 0)
        # Poster photo simulation (16:9)
        img_h = card_h - 32
        rect(g, cx + 2, cy + 2, cw - 4, img_h, ctop, 6)
        rect(g, cx + 2, cy + img_h - 18, cw - 4, 20, '#000000', 0, fill_opacity='.75')

        # Badge
        bcol = '#e50914' if 'HOT' in badge or 'LIVE' in badge else '#004d40' if '4K' in badge else '#e65100'
        rect(g, cx + cw - 48, cy + 6, 42, 16, bcol, 4)
        text(g, badge, cx + cw - 27, cy + 18, 8, 'white', text_anchor='middle', font_weight='bold')

        # Title overlaid on poster
        text(g, title, cx + 8, cy + img_h - 4, 10, 'white', font_weight='bold')
        # Meta below
        text(g, meta, cx + 6, cy + card_h - 8, 9, sub_c)

    text(g, 'Chọn thẻ và nhấn OK trên remote để xem ngay ▶', x + 18, y + h - 10, 10, sub_c)
def classic(g,defs,theme):
 if theme=='macos':
  rect(g,150,578,980,122,'#e4eefa',28,fill_opacity='.75');text(g,'▦ Apps',185,644,22,'#253c59')
  for i,item in enumerate(APPS):app(g,315+i*154,596,*item,82,54,label_color='#203448')
  showcase(g,defs,theme,40,66,1200,500)
 elif theme=='ubuntu':
  rect(g,0,64,124,656,'#1b1b1b',0,fill_opacity='.92')
  rect(g,10,76,104,76,'#ffffff',14,fill_opacity='.18');rect(g,9,103,5,22,'#e95420',2.5)
  for i,(label,color,mark) in enumerate(APPS):
   y=89+i*82;rect(g,20,y,84,50,color,12);text(g,mark,62,y+35,24,'#228569' if label=='CH Play' else 'white',text_anchor='middle',font_weight='bold')
  rect(g,26,618,72,1,'#ffffff',fill_opacity='.2')
  for r in range(3):
   for c in range(3):node(g,'circle',cx=47+c*15,cy=655+r*15,r=3,fill='#f2f2f2')
  rect(g,134,95,94,38,'#303030',10,fill_opacity='.95');text(g,'Netflix',181,120,16,'#f7f7f7',text_anchor='middle')
  showcase(g,defs,theme,125,66,1140,595)
 else:
  for i,item in enumerate(APPS):app(g,65,100+i*108,*item)
  if theme=='xp':showcase(g,defs,theme,245,66,1000,590)
  elif theme=='vista':showcase(g,defs,theme,245,66,785,590)
  rect(g,0,664,1280,56,gradient(defs,'task',['#3889ff','#1357d4'] if theme=='xp' else ['#516e7e','#0b1823'],y1=664,y2=720))
  rect(g,0,664,154 if theme=='xp' else 72,56,'#3a8f39' if theme=='xp' else '#2780aa',9 if theme=='xp' else 28);text(g,'▦ start' if theme=='xp' else '⊞',24,701,23);text(g,'09:41',1170,700,20)
  if theme=='vista':
   rect(g,1048,85,206,425,'#102536',10,fill_opacity='.45');node(g,'circle',cx=1151,cy=190,r=76,fill='#f0f4f5');node(g,'path',d='M1151 135V190L1186 212',fill='none',stroke='#263b49',stroke_width=5);text(g,'Thứ Sáu',1151,315,22,text_anchor='middle');text(g,'23 tháng 10',1151,345,22,text_anchor='middle')
def widgets(g,defs,theme):
 mac=theme=='macos';vista=theme=='vista';ubuntu=theme=='ubuntu';pill=mac or ubuntu;edge='#e1eaf8' if mac else '#d4d4d4' if ubuntu else '#448498' if vista else '#1557c3';body='#f5f8ff' if mac else '#fafafa' if ubuntu else '#eaf5fa' if vista else '#f9faf3';title='#324663' if mac else '#303030' if ubuntu else 'white';header=gradient(defs,'header',['#f0f3fa','#dce4ef'] if mac else ['#f2f2f2','#e6e6e6'] if ubuntu else ['#80c4d3','#255569'] if vista else ['#4098fc','#0960dd'],y2=34)
 if ubuntu:rect(g,0,64,192,656,'#1b1b1b',0,fill_opacity='.9')
 for i,item in enumerate(APPS):app(g,58,115+i*88,*item,76,48)
 rect(g,24,589,145,45,'#f0f4f9',16 if mac else 12 if ubuntu else 5);text(g,'▦ Ứng dụng',96,618,16,'#2f2f2f' if ubuntu else '#164379',text_anchor='middle');rect(g,1060,70,180,30,'#f3f7fa',15 if mac else 12 if ubuntu else 5);text(g,'↻ Làm mới',1150,91,15,'#2f2f2f' if ubuntu else '#164379',text_anchor='middle')
 labels=['◷ Thời gian','☀ Thời tiết','▦ Lịch dương / âm','⚽ Bóng đá','◈ Vàng SJC · DOJI','↗ Chứng khoán Việt Nam']
 for i,label in enumerate(labels):
  x=[192,538,884][i%3];y=106 if i<3 else 358;w=354 if i%3==2 else 330;h=234 if i<3 else 278
  rect(g,x,y,w,h,edge,18 if mac else 14 if ubuntu else 10 if vista else 7);rect(g,x+3,y+3,w-6,h-6,body,16 if mac else 12 if ubuntu else 6)
  # Header uses its own transformed gradient, like the native window title.
  head=node(g,'g',transform=f'translate({x+3},{y+3})');rect(head,0,0,w-6,32,header,16 if mac else 12 if ubuntu else 5)
  if mac:
   for dx,c in [(12,'#ff6058'),(24,'#ffbd2e'),(36,'#28c840')]:node(head,'circle',cx=dx,cy=16,r=4,fill=c)
  elif ubuntu:
   node(head,'circle',cx=w-22,cy=16,r=9,fill='#d9d9d9');text(head,'×',w-22,20,13,'#3d3d3d',text_anchor='middle',font_weight='bold')
  text(head,label,48 if mac else 12 if ubuntu else 10,22,14 if (mac or ubuntu) else 16,title,font_weight='bold')
 text(g,'09:41',210,222,61,'#16458c');text(g,'Thứ Sáu, 23/10/2026',210,258,18,'#405670');text(g,'Âm lịch 14 / 9 · Bính Ngọ',210,300,18,'#166149');text(g,'GIỜ VIỆT NAM · UTC+7',210,326,12,'#7b8793')
 text(g,'--°C',556,217,38,'#1762a0');text(g,'Theo thành phố đã chọn',556,255,18,'#405670');text(g,'Open-Meteo · tải dữ liệu trên TV',556,305,12,'#718092')
 text(g,'Tháng 10 · 2026',900,169,17,'#1d4e96')
 for col,day in enumerate(['T2','T3','T4','T5','T6','T7','CN']):text(g,day,915+col*45,194,12,'#72869c',text_anchor='middle')
 for day in range(1,32):
  n=day+2;col=n%7;row=n//7;x=915+col*45;y=220+row*23
  if day==23:rect(g,x-17,y-17,34,22,'#e95420' if ubuntu else '#367ce1',4)
  text(g,str(day),x,y,14,'white' if day==23 else '#284d72',text_anchor='middle')
 for x,y,w,label in [(210,400,294,'Hôm nay ⇄ 7 ngày tới'),(902,400,318,'VNINDEX  ›')]:rect(g,x,y,w,32,'#ececec' if ubuntu else '#e6edf4',14 if mac else 10 if ubuntu else 5);text(g,label,x+w/2,y+22,15,'#2f2f2f' if ubuntu else '#164379',text_anchor='middle')
 text(g,'Lịch thi đấu & kênh phát',210,470,17,'#234f80');text(g,'Việt Nam · NHA · C1 · Ý · TBN · Đức',210,505,13,'#6c7e91');text(g,'Bongda24h · dữ liệu tải trên TV',210,615,12,'#718092')
 text(g,'--  /  --',556,430,25,'#9a6c06');text(g,'Mua / Bán · triệu đồng mỗi lượng',556,454,13,'#6c7280');text(g,'-- điểm',902,462,25,'#17694c')
 for x,w,label in [(556,294,'DOJI/SJC · dữ liệu tải trên TV'),(902,318,'DNSE · giá đóng cửa theo phiên')]:
  for y in [490,530,570]:node(g,'path',d=f'M{x} {y}h{w}',stroke='#dce4eb',stroke_width=1)
  text(g,'Biểu đồ cập nhật trên TV',x+12,535,13,'#82909c');text(g,label,x,615,12,'#718092')
 rect(g,192 if pill else 0,664,1046 if pill else 1280,56,'#e4eafa' if mac else '#1e1e1e' if ubuntu else '#14354c' if vista else '#236be0',22 if pill else 0,fill_opacity='.9' if ubuntu else '.85');rect(g,205 if pill else 0,669,186 if ubuntu else 154,46,'#f0f5fb' if mac else '#e95420' if ubuntu else '#25805a',16 if mac else 20 if ubuntu else 6);text(g,'▦ Apps' if mac else '▦ Hiện ứng dụng' if ubuntu else '▦ Bắt đầu',225 if pill else 20,700,18 if ubuntu else 19,'#253c59' if mac else 'white');text(g,'Cài đặt',1030,700,18,'#253c59' if mac else 'white');text(g,'09:41',1150,700,18,'#253c59' if mac else 'white')
def generate():
 for theme in ['xp','vista','macos','ubuntu','default']:
  for variant in (['classic','widgets'] if theme!='default' else ['classic']):
   root=E.Element('{'+NS+'}svg',{'width':'1280','height':'720','viewBox':'0 0 1280 720','role':'img'});node(root,'title').text='Minh họa giao diện '+theme+' '+variant;defs=node(root,'defs');g=node(root,'g',font_family='Arial,sans-serif')
   if theme=='default':node(g,'image',x=0,y=64,width=1280,height=656,preserveAspectRatio='none',href='data:image/webp;base64,'+base64.b64encode((ASSETS/'default.webp').read_bytes()).decode())
   else:wallpaper(g,defs,theme,variant=='widgets');(widgets if variant=='widgets' else classic)(g,defs,theme)
   quickbar(g,defs,theme);E.ElementTree(root).write(ASSETS/(theme+'-'+variant+'-clean.svg'),encoding='utf-8',xml_declaration=True)
if __name__=='__main__':generate()
