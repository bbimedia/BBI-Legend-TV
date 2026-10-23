"""Original vector layout preview; optional source snapshot, never synthetic market data."""
from pathlib import Path
from html import escape
import json

root = Path(__file__).resolve().parents[1]
parts = ['''<svg xmlns="http://www.w3.org/2000/svg" width="1280" height="720" viewBox="0 0 1280 720"><defs><linearGradient id="sky" x2="0" y2="1"><stop stop-color="#1866b7"/><stop offset="1" stop-color="#a6d8f5"/></linearGradient><linearGradient id="bar" x2="0" y2="1"><stop stop-color="#4098fc"/><stop offset=".48" stop-color="#1971ee"/><stop offset="1" stop-color="#0957cd"/></linearGradient><linearGradient id="green" x2="0" y2="1"><stop stop-color="#75ba62"/><stop offset="1" stop-color="#23792d"/></linearGradient><filter id="shadow"><feDropShadow dx="0" dy="5" stdDeviation="6" flood-opacity=".22"/></filter></defs><rect width="1280" height="720" fill="url(#sky)"/><ellipse cx="1000" cy="50" rx="210" ry="30" fill="#ffffff" opacity=".25"/><path d="M0 500 Q430 310 1280 540V720H0Z" fill="#74ae3c"/><path d="M0 600 Q700 390 1280 520V720H0Z" fill="#478529"/><g font-family="Arial,sans-serif">''']
def text(x, y, value, size=16, color='#254d76', bold=False):
    parts.append(f'<text x="{x}" y="{y}" fill="{color}" font-size="{size}" font-weight="{700 if bold else 400}">{escape(str(value))}</text>')
def box(x,y,w,h,title):
    parts.append(f'<g filter="url(#shadow)"><rect x="{x}" y="{y}" width="{w}" height="{h}" rx="7" fill="#1557c3"/><rect x="{x+3}" y="{y+3}" width="{w-6}" height="34" rx="5" fill="url(#bar)"/><path d="M{x+3} {y+37}H{x+w-3}V{y+h-3}H{x+3}Z" fill="#f9faf3"/></g>')
    text(x+14,y+26,title,16,'white',True)
def button(x,y,w,label):
    parts.append(f'<rect x="{x}" y="{y}" width="{w}" height="30" rx="4" fill="#e7edf0" stroke="#bcc9d2"/>');text(x+10,y+20,label,14)
text(30,55,'BBI Legend TV',28,'white',True)
text(192,88,'XP Widgets / Ngày mới trên màn hình lớn',17,'#eef6ff')
text(1060,55,'↻ Làm mới',17,'white')
for i,(symbol,label,color) in enumerate([('▶','YouTube','#d83b36'),('N','Netflix','#252c3b'),('▦','Ứng dụng','#2770cc'),('↔','HDMI','#239b80'),('⚙','Cài đặt','#7e8b9d')]):
    y=112+i*88;parts.append(f'<rect x="60" y="{y}" width="70" height="48" rx="12" fill="{color}" stroke="#ffffff55"/>');text(82,y+33,symbol,28,'white',True);text(47,y+73,label,15,'white',True)
button(25,589,140,'▦  Ứng dụng')
box(192,106,330,234,'◷  Thời gian');text(210,222,'09:41',64,'#16458c');text(210,255,'Thứ Sáu, 23/10/2026',18);text(210,289,'Âm lịch 13/9 · Bính Ngọ',19,'#166149',True);text(210,319,'GIỜ VIỆT NAM · UTC+7',12,'#7b8793')
box(538,106,330,234,'☀  Thời tiết');text(558,214,'--°C',52,'#1762a0',True);text(558,253,'Hồ Chí Minh',20);text(558,285,'Nhiệt độ · cao/thấp · độ ẩm',15,'#64788e');text(558,319,'Open-Meteo · tải dữ liệu trên TV',12,'#718092')
box(884,106,354,234,'▦  Lịch dương / âm');text(902,169,'Tháng 10 · 2026',16,'#1d4e96',True)
for col,name in enumerate(['T2','T3','T4','T5','T6','T7','CN']):text(907+col*46,195,name,12,'#72869c')
import calendar
weeks=calendar.monthcalendar(2026,10)
for row,week in enumerate(weeks):
    for col,day in enumerate(week):
        if not day:continue
        x=910+col*46;y=220+row*24
        if day==23:parts.append(f'<rect x="{x-7}" y="{y-17}" width="32" height="23" rx="4" fill="#2871d1"/>')
        text(x,y,day,14,'white' if day==23 else '#284d72')
box(192,358,330,278,'⚽  Bóng đá');button(209,406,295,'Hôm nay  ⇄  7 ngày tới')
text(209,466,'Ưu tiên Việt Nam nam / nữ',18,'#234f80',True);text(209,491,'V.League · NHA · C1',17);text(209,518,'Serie A · La Liga · Bundesliga',16);text(209,554,'Giờ Việt Nam · Kênh phát theo trận',14,'#62788e');text(209,582,'OK xem lịch / bộ lọc / kênh VN',13,'#62788e');text(209,616,'TheSportsDB · lịch và kênh có giới hạn',11,'#718092')
box(538,358,330,278,'◈  Vàng miếng SJC · DOJI');text(556,438,'Mua  --  /  Bán  --',24,'#9a6c06',True);text(556,465,'Triệu đồng mỗi lượng',13,'#6c7280')
for yy in (493,526,559):parts.append(f'<path d="M556 {yy}H848" stroke="#dfe5e9"/>')
text(568,532,'Biểu đồ giá bán tích lũy trên TV',14,'#82909c');text(556,594,'Cần ≥ 2 mốc công bố để vẽ đường giá',12,'#718092');text(556,616,'DOJI qua taichinh.com · có mốc thời gian',11,'#718092')
box(884,358,354,278,'↗  Chứng khoán Việt Nam');button(902,403,316,'VNINDEX   ›');text(902,471,'-- điểm  /  --%',24,'#17694c',True)
for yy in (494,526,559):parts.append(f'<path d="M902 {yy}H1218" stroke="#dfe5e9"/>')
text(916,534,'Biểu đồ giá đóng cửa theo phiên',14,'#82909c');text(902,591,'VNINDEX · VN30 · FPT · VCB · HPG',12,'#718092');text(902,616,'DNSE · giá tham khảo, có thể trễ',11,'#718092')
parts.append('<rect x="0" y="664" width="1280" height="56" fill="url(#bar)"/><rect x="0" y="664" width="154" height="56" rx="8" fill="url(#green)"/>')
text(22,702,'▦  start',27,'white',True);text(182,699,'▣  BBI Today · XP Widgets',17,'white');text(787,699,'Minh họa bố cục · số liệu tải trên TV',13,'#d9ebff');text(1169,699,'09:41',18,'white',True)
parts.append('</g></svg>');(root/'public/assets/xp-widgets.svg').write_text(''.join(parts),encoding='utf-8')
