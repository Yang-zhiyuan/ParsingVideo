package com.example.parse;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.didikee.donate.AlipayDonate;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.util.VideoUtil;

public class MainActivity extends AppCompatActivity {
    private String payCode = "fkx09508sza6amko2gs8wf6";
    private static final int PERMISSIONS_REQUEST_CAMERA = 454;
    private EditText etUrl;
    private Button btnParse;
    private TextView tvPath;
    private Button btnClean;
    private Button btnGood;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!VideoUtil.getInstance().checkStoragePermission(this)) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_CAMERA);
        }
        if (!VideoUtil.getInstance().checkAlertWindowsPermission(this)) {
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), 0);
        } else {
            showAlertWindows();
        }
        etUrl = (EditText) findViewById(R.id.etUrl);
        btnParse = (Button) findViewById(R.id.btnParse);
        tvPath = (TextView) findViewById(R.id.tvPath);
        btnClean = (Button) findViewById(R.id.btnClean);
        btnGood = (Button) findViewById(R.id.btnGood);
        tvPath.setText(tvPath.getText() + VideoUtil.FILE_PATH);
        btnParse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = etUrl.getText().toString();
                if (!url.isEmpty()) {
                    VideoUtil.getInstance().parse(MainActivity.this, url);
                }
            }
        });
        btnClean.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etUrl.setText("");
            }
        });
        btnGood.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                donate();
            }
        });
    }

    class FloatingOnTouchListener implements View.OnTouchListener {
        private int x, y, lastX, lastY;
        private WindowManager windowManager;
        private WindowManager.LayoutParams layoutParams;

        FloatingOnTouchListener(WindowManager windowManager, WindowManager.LayoutParams layoutParams) {
            this.windowManager = windowManager;
            this.layoutParams = layoutParams;
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    ((ImageView) view).setImageAlpha(128);
                    x = (int) event.getRawX();
                    y = (int) event.getRawY();
                    lastX = x;
                    lastY = y;
                    break;
                case MotionEvent.ACTION_MOVE:
                    int nowX = (int) event.getRawX();
                    int nowY = (int) event.getRawY();
                    int movedX = nowX - x;
                    int movedY = nowY - y;
                    x = nowX;
                    y = nowY;
                    layoutParams.x = layoutParams.x + movedX;
                    layoutParams.y = layoutParams.y + movedY;
                    // 更新悬浮窗控件布局
                    windowManager.updateViewLayout(view, layoutParams);
                    break;
                case MotionEvent.ACTION_UP:
                    ((ImageView) view).setImageAlpha(255);
                    VideoUtil.getInstance().writeXY(MainActivity.this, layoutParams.x, layoutParams.y);
                    if (Math.abs(event.getRawX() - lastX) < 5 && Math.abs(event.getRawY() - lastY) < 5) {
                        try {
                            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                            ClipData data = cm.getPrimaryClip();
                            ClipData.Item item = data.getItemAt(0);
                            String url = item.getText().toString();
                            VideoUtil.getInstance().parse(MainActivity.this, url);
                            if (cm != null) {
                                cm.setPrimaryClip(cm.getPrimaryClip());
                                cm.setText(null);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "请先复制短视频分享链接", Toast.LENGTH_SHORT).show();
                        }
                    }
                default:
                    break;
            }
            return false;
        }
    }

    private void showAlertWindows() {
        if (VideoUtil.getInstance().checkAlertWindowsPermission(this)) {
            final WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            // 新建悬浮窗控件
            ImageView img = new ImageView(getApplicationContext());
            img.setImageResource(R.mipmap.ic_launcher);
            // 设置LayoutParam
            final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
            }
            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            layoutParams.format = PixelFormat.RGBA_8888;
            layoutParams.width = 96;
            layoutParams.height = 96;
            layoutParams.x = VideoUtil.getInstance().getX(this);
            layoutParams.y = VideoUtil.getInstance().getY(this);
            // 将悬浮窗控件添加到WindowManager
            windowManager.addView(img, layoutParams);
            img.setOnTouchListener(new FloatingOnTouchListener(windowManager, layoutParams));

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, 1, 0, "捐赠作者").setIcon(R.mipmap.ic_launcher);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        donate();
        return true;
    }

    private void donate() {
        boolean hasInstalledAlipayClient = AlipayDonate.hasInstalledAlipayClient(this);
        if (hasInstalledAlipayClient) {
            AlipayDonate.startAlipayClient(this, payCode);
        } else {
            Toast.makeText(this, "手机未安装支付宝程序", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            if (!VideoUtil.getInstance().checkAlertWindowsPermission(this)) {
                Toast.makeText(this, "悬浮窗权限获取失败,如需使用悬浮框功能请手动开启", Toast.LENGTH_LONG).show();
            } else {
                showAlertWindows();
                Toast.makeText(this, "悬浮窗权限获取成功", Toast.LENGTH_SHORT).show();
            }
        }
    }


}
