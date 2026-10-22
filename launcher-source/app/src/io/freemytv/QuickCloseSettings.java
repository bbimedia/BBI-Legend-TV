package io.freemytv;
import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import android.graphics.Color;
public class QuickCloseSettings extends Activity {
    private Button toggle;private TextView status;
    @Override public void onCreate(Bundle state){super.onCreate(state);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(48,32,48,24);page.setBackgroundColor(0xff14263e);
        TextView title=new TextView(this);title.setText("Đóng hẳn ứng dụng bằng 3 lần Home");title.setTextSize(25);title.setTextColor(Color.WHITE);page.addView(title);
        TextView help=new TextView(this);help.setText("Nhấn Home 3 lần trong 1,8 giây để buộc dừng ứng dụng vừa dùng.\nMột lần Home vẫn về màn hình chính bình thường.\n\nỨng dụng bị đóng sẽ dừng phát và các tác vụ nền cho đến khi mở lại. Không xóa dữ liệu hay tài khoản. Dịch vụ hệ thống được bảo vệ.\n\nCần bật dịch vụ ‘Free My TV — Đóng ứng dụng’ trong Trợ năng để nhận biết ứng dụng vừa mở. Dịch vụ chỉ đọc tên gói của cửa sổ đang hoạt động, không đọc nội dung.");help.setTextColor(0xffd9e8fa);help.setTextSize(17);help.setPadding(0,16,0,16);page.addView(help);
        toggle=new Button(this);toggle.setAllCaps(false);toggle.setOnClickListener(v->{QuickClose.enabled(this,!QuickClose.enabled(this));refresh();});page.addView(toggle);
        Button permission=new Button(this);permission.setText("Mở Trợ năng để cấp quyền");permission.setAllCaps(false);permission.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));page.addView(permission);
        status=new TextView(this);status.setTextColor(0xffb8dfff);status.setTextSize(16);page.addView(status);setContentView(page);toggle.requestFocus();refresh();
    }
    @Override public void onResume(){super.onResume();if(toggle!=null)refresh();}
    private void refresh(){toggle.setText(QuickClose.enabled(this)?"Đang BẬT — Nhấn OK để tắt":"Đang TẮT — Nhấn OK để bật");String enabled=Settings.Secure.getString(getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);String component=new ComponentName(this,QuickCloseService.class).flattenToString();boolean granted=enabled!=null&&(enabled.contains(component)||enabled.contains(getPackageName()+"/.QuickCloseService"));status.setText(granted?"Quyền Trợ năng: đã bật":"Quyền Trợ năng: chưa bật");}
}
