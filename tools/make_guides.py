"""Generate original, explanatory SVG diagrams; no remote image dependencies."""
from pathlib import Path
from html import escape

OUT = Path(__file__).resolve().parents[1] / 'public' / 'assets'


def text(x, y, value, size=21, color='#dce3f7', weight=400):
    return f'<text x="{x}" y="{y}" fill="{color}" font-size="{size}" font-weight="{weight}" font-family="Arial,sans-serif">{escape(value)}</text>'


def card(name, body):
    svg = f'''<svg xmlns="http://www.w3.org/2000/svg" width="960" height="540" viewBox="0 0 960 540">
    <defs><linearGradient id="wall" x2="1" y2="1"><stop stop-color="#eceaf7"/><stop offset="1" stop-color="#dde4f3"/></linearGradient><linearGradient id="tv" x2="0" y2="1"><stop stop-color="#26334c"/><stop offset="1" stop-color="#152135"/></linearGradient></defs>
    <rect width="960" height="540" fill="url(#wall)"/><ellipse cx="480" cy="490" rx="330" ry="18" fill="#5b649a15"/>
    <rect x="103" y="50" width="754" height="424" rx="18" fill="#192135"/><rect x="112" y="59" width="736" height="399" rx="10" fill="url(#tv)"/>
    <path d="M446 474h68l9 18h45v8H392v-8h45z" fill="#556078"/>
    {body}
    <text x="480" y="468" text-anchor="middle" font-size="6" letter-spacing="3" fill="#747f9a" font-family="Arial">BBI LEGEND TV · HÌNH MINH HỌA</text></svg>'''
    (OUT / name).write_text(svg, encoding='utf-8')


card('guide-about.svg',
     text(153, 112, 'Giới thiệu', 28, '#ffffff', 600)
     + text(154, 154, 'Thông tin thiết bị', 16, '#95a2be')
     + ''.join(text(172, y, label, 21) for y, label in [(211, 'Tên thiết bị                 Android TV'), (260, 'Phiên bản Android       11')])
     + '<rect x="150" y="289" width="657" height="64" rx="10" fill="#7060c9"/>'
     + text(172, 329, 'Bản dựng Android TV OS', 21, '#fff', 600)
     + '<circle cx="755" cy="320" r="25" fill="#faf7ff"/>'
     + text(738, 328, '7×', 21, '#7761d0', 600)
     + text(174, 400, 'Nhấn OK 7 lần để bật chế độ nhà phát triển', 18, '#bdc3d8'))

card('guide-debug.svg',
     text(153, 112, 'Tùy chọn nhà phát triển', 27, '#fff', 600)
     + text(154, 153, 'Gỡ lỗi', 16, '#95a2be')
     + '<rect x="151" y="189" width="657" height="97" rx="11" fill="#3a4665"/>'
     + text(174, 229, 'Gỡ lỗi USB / USB debugging', 23, '#fff', 600)
     + text(174, 263, 'Cho phép kết nối ADB từ máy tính', 16, '#aebbd4')
     + '<rect x="717" y="220" width="62" height="31" rx="16" fill="#9b86ea"/><circle cx="762" cy="235" r="12" fill="#fff"/>'
     + text(174, 344, 'Bật tùy chọn này trên TV của bạn', 21)
     + text(174, 385, 'Tên mục có thể là ADB hoặc Network debugging', 17, '#a3afca'))

card('guide-network.svg',
     text(153, 112, 'Cùng mạng. Sẵn sàng kết nối.', 26, '#fff', 600)
     + '<rect x="195" y="186" width="165" height="97" rx="8" fill="#7285b0"/><rect x="204" y="195" width="147" height="77" rx="3" fill="#b8c9ed"/><path d="M267 283h22v18h-22z" fill="#7285b0"/>'
     + '<rect x="605" y="193" width="151" height="80" rx="7" fill="#9290c6"/><path d="M595 277h172l12 10H585z" fill="#bbb7e7"/>'
     + '<rect x="448" y="210" width="65" height="42" rx="10" fill="#cec6ef"/><path d="M460 210v-26m29 26v-26" stroke="#cec6ef" stroke-width="5" stroke-linecap="round"/>'
     + '<path d="M364 236h79m74 0h79" stroke="#9082d9" stroke-width="3" stroke-dasharray="7 6"/>'
     + text(225, 334, 'TV', 21) + text(447, 290, 'Router', 16, '#a3afca') + text(634, 334, 'Máy tính', 21)
     + '<rect x="238" y="371" width="485" height="52" rx="10" fill="#34425f"/>'
     + text(267, 404, 'IP của TV:  192.168.1.211', 24, '#e0d7ff', 600))

card('guide-allow.svg',
     '<rect x="190" y="126" width="580" height="274" rx="16" fill="#f6f7fc"/>'
     + text(224, 178, 'Cho phép gỡ lỗi USB?', 25, '#34314c', 600)
     + text(224, 222, 'Máy tính này muốn kết nối với TV.', 20, '#76708a')
     + '<rect x="225" y="257" width="24" height="24" rx="5" fill="#8973d8"/><path d="m231 268 5 6 9-12" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round"/>'
     + text(265, 276, 'Luôn cho phép từ máy tính này', 20, '#4a425e')
     + '<rect x="536" y="318" width="195" height="47" rx="9" fill="#8973d8"/>'
     + text(595, 349, 'Cho phép', 19, '#fff', 600)
     + text(447, 349, 'Hủy', 19, '#8c839d')
     + text(220, 436, 'Tick “Luôn cho phép” rồi chọn Cho phép / OK', 19, '#c1bbd5'))

(OUT / 'mark.svg').write_text('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64"><rect width="64" height="64" rx="18" fill="#7761e7"/><rect x="12" y="16" width="40" height="29" rx="6" fill="none" stroke="white" stroke-width="4"/><path d="M23 51h18M27 46v5m10-5v5" stroke="white" stroke-width="3" stroke-linecap="round"/><circle cx="32" cy="30" r="5" fill="white"/></svg>', encoding='utf-8')
print('Created four illustrated guides and app icon.')
