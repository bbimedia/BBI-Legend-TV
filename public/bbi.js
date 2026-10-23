'use strict';
// BBI MEDIA contact and donation details; release history belongs to BBI Legend TV.
(() => {
  const VERSION = '1.3.1';
  const FEATURES = [
    ['⌁','Kết nối & kiểm tra TV','Đọc thông tin, kiểm tra Android TV / Google TV và hướng dẫn chuẩn bị bằng hình.'],
    ['◈','Năm diện mạo','Free My TV mặc định, XP, XP Widgets, Vista và macOS trong cùng một bản cài.'],
    ['↶','Sao lưu & khôi phục','Tự backup trước thay đổi, kiểm tra toàn vẹn và khôi phục đúng TV.'],
    ['⌂','Home 3 lần','Đóng hẳn ứng dụng vừa dùng; giữ dữ liệu và bảo vệ dịch vụ hệ thống.'],
    ['♫','Cá nhân hóa nhanh','Âm thanh điều hướng, thời tiết, thành phố và các thao tác nhanh trên TV.'],
    ['▦','Quản lý ứng dụng','Xem app đang mở/chạy nền, RAM ước lượng và chụp màn hình TV.']
  ];
  const RELEASES = [
    {version:'v1.3.1',date:'23/10/2026',title:'Lịch Bongda24h & nhận diện BBI Legend TV trên launcher',highlights:[
      {title:'Lịch bóng đá và kênh phát từ Bongda24h',desc:'Đọc lịch 7 ngày của trang nguồn, giữ giờ Việt Nam và tên kênh từng trận, ưu tiên Việt Nam. Làm mới thủ công và giữ bản lưu khi mất mạng hoặc cấu trúc trang đổi.'},
      {title:'Cài đặt launcher mang tên BBI Legend TV',desc:'Cập nhật tên app, logo BBI MEDIA, màn hình cài đặt và nhãn dịch vụ. Giữ một dòng ghi nguồn Free My TV / MIT ở cuối trang cài đặt.'},
      {title:'Kiểm tra thực tế trên Xiaomi Android 11',desc:'Đã cài thử, kiểm tra widget thật, phím remote, lịch tháng và bộ lọc bóng đá. Sao lưu trước mỗi lần cài, giữ cấu hình hiện có.'}
    ]},
    {version:'v1.3.0',date:'23/10/2026',title:'XP Widgets · Một màn hình, nhiều thông tin',highlights:[
      {title:'Theme XP Widgets riêng',desc:'Giữ theme XP cổ điển; thêm bố cục TV với đồng hồ, lịch âm/dương UTC+7, lịch tháng và thời tiết theo thành phố.'},
      {title:'Bóng đá và kênh tại Việt Nam',desc:'Hôm nay/7 ngày: ưu tiên ĐT Việt Nam nam/nữ, V.League; NHA, C1, Serie A, La Liga và Bundesliga. Chỉ ghép kênh theo trận/quốc gia, ghi rõ khi chưa có; nguồn miễn phí giới hạn phạm vi.'},
      {title:'Vàng và chứng khoán Việt Nam',desc:'Giá DOJI/SJC, biểu đồ giá bán tích lũy trên TV; VNINDEX, VN30, FPT, VCB và HPG với biểu đồ đóng cửa theo phiên. Có mốc nguồn, cache và trạng thái mất mạng; không dùng giá giả.'}
    ]},
    {version:'v1.2.0',date:'23/10/2026',title:'Thông báo phiên bản mới & thông tin sử dụng',highlights:[
      {title:'Theo dõi GitHub Releases',desc:'Kiểm tra bản ổn định mới mỗi 24 giờ, có kiểm tra thủ công, công tắc tự động và liên kết tải; dùng bình thường khi mất mạng.'},
      {title:'Thông tin tương thích và quyền sở hữu',desc:'Bổ sung giới hạn tương thích từng TV, phạm vi backup, miễn trừ trách nhiệm và quyền đối với hình ảnh, nhãn hiệu của bên thứ ba.'}
    ]},
    {version:'v1.1.1',date:'22/10/2026',title:'BBI Legend TV · Nhận diện mới và không gian BBI MEDIA',highlights:[
      {title:'Tên gọi BBI Legend TV',desc:'Cập nhật tên ứng dụng web, logo, tiêu đề trình duyệt, hướng dẫn khởi chạy và gói portable.'},
      {title:'Giới thiệu BBI MEDIA',desc:'Tích hợp thiết kế nhận diện, thông tin liên hệ BBI MEDIA, cộng đồng Tôi là Vibe’r và mã VietQR ủng hộ.'},
      {title:'Nhật ký riêng cho BBI Legend TV',desc:'Theo dõi các phiên bản và tính năng của BBI Legend TV, kèm tìm kiếm nội dung cập nhật.'}
    ]},
    {version:'v1.0.0',date:'22/10/2026',title:'Công cụ web portable cho Android TV & Google TV',highlights:[
      {title:'Hướng dẫn và kiểm tra tương thích',desc:'Bốn hình minh họa chuẩn bị TV, kết nối ADB nội bộ, đọc model/Android và ghép nối không dây.'},
      {title:'Kho giao diện trong một APK',desc:'Giao diện Free My TV mặc định, Windows XP, Vista và macOS; đổi theme trong cùng ứng dụng, giữ các tùy chọn.'},
      {title:'Backup trước thay đổi',desc:'Lưu APK và các tùy chọn do ứng dụng quản lý. Kiểm tra SHA-256, ngăn restore sang TV khác và tạo bản dự phòng trước khi khôi phục.'},
      {title:'Điều khiển và tiện ích',desc:'Home 3 lần, âm thanh khi di chuyển, ngày giờ tự động, cài đặt mạng/Trợ năng, danh sách app/RAM và chụp màn hình.'},
      {title:'Windows, Linux và macOS',desc:'Dịch vụ web cục bộ bằng Python, công cụ ADB và file khởi chạy cho từng hệ điều hành. Đã thử trên Windows với Xiaomi MiTV-MOOR2 / Android 11; Linux/macOS chưa chạy thực tế.'}
    ]}
  ];
  const make=(tag,cls='',value='')=>{const e=document.createElement(tag);e.className=cls;if(value)e.textContent=value;return e;};
  const link=(name,url,description='')=>{const a=make('a','about-link');a.href=url;if(url.startsWith('https://')){a.target='_blank';a.rel='noreferrer';}a.append(make('strong','',name));if(description)a.append(make('small','',description));return a;};
  function showTimeline(releases){const host=document.querySelector('#changelog-list');host.replaceChildren();for(const [index,release] of releases.entries()){const row=make('article','release-row');const meta=make('div','release-meta');meta.append(make('strong','',release.version),make('span','',release.date));const card=make('div','panel release-card');const heading=make('div','release-heading');heading.append(make('h2','',release.title));if(index===0)heading.append(make('span','release-badge',release===RELEASES[0]?'Mới nhất':'Phiên bản trước'));card.append(heading);const grid=make('div','release-grid');for(const item of release.highlights){const cell=make('div','release-highlight');cell.append(make('span','release-symbol','✦'));const copy=make('div');copy.append(make('h3','',item.title),make('p','',item.desc));cell.append(copy);grid.append(cell);}card.append(grid);row.append(meta,card);host.append(row);}document.querySelector('#changelog-source-note').textContent='Các thay đổi của BBI Legend TV.';}
  function showThanks(groups,host){host.replaceChildren();for(const [name,items] of groups){const block=make('section','thanks-group');block.append(make('h3','',name));const grid=make('div','thanks-grid');for(const [title,url,description] of items)grid.append(link(title,url,description));block.append(grid);host.append(block);}}
  async function init(){
    const response=await fetch('/assets/bbi-content.json');if(!response.ok)throw new Error('Không tải được nội dung BBI.');const data=await response.json();
    const company=data.company,donation=data.donation,community=data.community;
    document.querySelector('#about-tagline').textContent=company.tagline;
    document.querySelector('#about-company-kind').textContent='Phát triển bởi '+company.name+' — '+company.kind;
    const contact=document.querySelector('#bbi-contact');contact.append(link(company.hotline,'tel:'+company.hotline.replaceAll(' ',''),'Hotline'),link(company.email,'mailto:'+company.email,'Email'),link(company.website,'https://'+company.website,'Website'));
    const groups=document.querySelector('#bbi-community');groups.append(link('Nhóm Zalo · '+community.name,community.zalo,'Hỏi đáp, trao đổi và góp ý'),link('Nhóm Facebook · '+community.name,community.facebook,'Kết nối cộng đồng'));
    document.querySelector('#donate-account').textContent=donation.account;document.querySelector('#donate-holder').textContent=donation.holder;document.querySelector('#donate-bank').textContent=donation.bank;
    document.querySelector('#copy-account').addEventListener('click',async()=>{try{await navigator.clipboard.writeText(donation.account);document.querySelector('#copy-account').textContent='✓ Đã chép';setTimeout(()=>document.querySelector('#copy-account').textContent='Sao chép',1600);}catch(_e){document.querySelector('#donate-copy-status').textContent='Bạn có thể chọn và sao chép số tài khoản ở trên.';}});
    const features=document.querySelector('#legend-features');for(const [icon,title,description] of FEATURES){const card=make('div','about-feature');card.append(make('span','',icon));const copy=make('div');copy.append(make('h3','',title),make('p','',description));card.append(copy);features.append(card);}
    showThanks([['Nền tảng của BBI Legend TV',[
      ['Free My TV','https://github.com/simbasjungles/free-my-tv','Launcher Android TV, mã nguồn MIT và cơ chế khôi phục.'],
      ['Android Platform Tools','https://developer.android.com/tools/releases/platform-tools','ADB chính thức để kết nối và thao tác với TV.'],
      ['Python','https://www.python.org/','Dịch vụ web chạy cục bộ trên máy tính.'],
      ['Inter','https://rsms.me/inter/','Phông chữ giao diện, giấy phép SIL OFL.']
    ]]],document.querySelector('#legend-thanks'));
    document.querySelector('#about-copyright').textContent='© '+new Date().getFullYear()+' '+company.name+'. BBI Legend TV · Mã nguồn mở theo giấy phép MIT.';
    const search=document.querySelector('#changelog-search');
    function update(){const q=search.value.trim().toLocaleLowerCase('vi');const filtered=RELEASES.filter(r=>!q||[r.version,r.date,r.title,...r.highlights.flatMap(h=>[h.title,h.desc])].join(' ').toLocaleLowerCase('vi').includes(q));showTimeline(filtered);document.querySelector('#release-count').textContent=filtered.length+' phiên bản';if(!filtered.length)document.querySelector('#changelog-list').append(make('div','panel empty-state','Không có bản cập nhật khớp từ khóa.'));}
    search.addEventListener('input',update);update();
    document.querySelectorAll('[data-legend-version]').forEach(e=>e.textContent=VERSION);
  }
  init().catch(error=>{document.querySelector('#about-load-status').textContent=error.message;document.querySelector('#changelog-source-note').textContent=error.message;});
})();
