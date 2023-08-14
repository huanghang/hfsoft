package com.dawn.java.ui.homePage;

import static com.dawn.decoderapijni.ServiceTools.MSG_INIT_DONE;
import static com.dawn.decoderapijni.ServiceTools.MSG_INIT_FIRMWARE_UPGRADE;
import static com.dawn.decoderapijni.SoftEngine.SCN_EVENT_DEC_SUCC;
import static com.dawn.decoderapijni.SoftEngine.SCN_EVENT_SCANNER_OVERHEAT;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.dawn.decoderapijni.ServiceTools;
import com.dawn.decoderapijni.SoftEngine;
import com.dawn.java.R;
import com.dawn.java.ui.MyApplication;
import com.dawn.java.ui.activity.BatchScanActivity;
import com.dawn.java.ui.activity.CenterScanSettingActivity;
import com.dawn.java.ui.activity.CodeSetActivity;
import com.dawn.java.ui.activity.DebugAccuracyActivity;
import com.dawn.java.ui.activity.DebugAutoScanActivity;
import com.dawn.java.ui.activity.DebugScanActivity;
import com.dawn.java.ui.activity.DebugUpdateActivity;
import com.dawn.java.ui.activity.DeviceUpdateActivity;
import com.dawn.java.ui.activity.HighLightSettingActivity;
import com.dawn.java.ui.activity.IlluminationSettingActivity;
import com.dawn.java.ui.activity.ImageShowActivity;
import com.dawn.java.ui.activity.VersionShowActivity;
import com.dawn.java.ui.activity.base.DawnActivity;
import com.dawn.java.ui.widget.TopToolbar;
import com.dawn.java.util.HttpURLconnectionClass;
import com.dawn.java.util.IlluminationUtil;
import com.dawn.java.util.ServerDialog;

import java.util.Random;

public class HomeActivity extends DawnActivity implements NavigationView.OnNavigationItemSelectedListener {
    private boolean permissionsAreOk = false;
    private final int HANDLER_SCAN_NEW_MESSAGE = 1;
    private final int HANDLER_SCANNER_OVERHEAT = 2;
    private final int HANDLER_SCANNER_OTHER_ERROR = 3;

    private boolean illuminationDataLoadFlag = false;

    private TopToolbar topBar;
    private Button btn_scan;
    private TextView tv_scanResult, tv_pointResult;

    private Button btn_clear;
    private Button btn_server;
    private Button btn_screen;

    private ServerDialog dialog;


    DrawerLayout drawer;
    NavigationView navigationView;
    private static long decodeTime = 0;

    private ProgressDialog initProgressDialog = null;
    private boolean needToResetScanCallback = false;

    @Override
    public int getLayoutId() {
        return R.layout.activity_navi_home;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IntentFilter filter = new IntentFilter("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        registerReceiver(screenOnOffReceiver, filter);

        initView();
        permissionsAreOk = checkPermissions();
        if (permissionsAreOk) {
            initData();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (illuminationDataLoadFlag == true) {
                        IlluminationUtil.InitSPData(getApplicationContext());
                        IlluminationUtil.DataLoad();
                        break;
                    }
                }
            }
        }).start();
    }

    private void initData() {
        if (MyApplication.systemLocale.getLanguage().equals("zh")) {  //中文
            SoftEngine.getInstance().setNdkSystemLanguage(0);  //中文
        } else {
            SoftEngine.getInstance().setNdkSystemLanguage(1);  //英文
        }
        setScanCallback();
        initProgressDialog = ProgressDialog.show(this, getString(R.string.init_prog_title), getString(R.string.init_prog_start), true, false);
        // 接下来的初始化操作，有可能会自动更新模组固件，需要保持屏幕常亮，避免整机休眠。
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ServiceTools.getInstance().startInit(getDir("nlscan", MODE_PRIVATE).getAbsolutePath(), true, mainThreadHandler);
    }

    private void initView() {
        topBar = findViewById(R.id.topBar);
        topBar.setMainTitle(getResources().getString(R.string.app_name));
        tv_scanResult = findViewById(R.id.tv_scan_result);
        tv_scanResult.setMovementMethod(ScrollingMovementMethod.getInstance());
        tv_pointResult = findViewById(R.id.tv_point_result);
        tv_pointResult.setMovementMethod(ScrollingMovementMethod.getInstance());
        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        topBar.setMenuToolBarListener(new TopToolbar.MenuToolBarListener() {
            @Override
            public void onToolBarClickLeft(View v) {
            }

            @Override
            public void onToolBarClickRight(View v) {
                drawer.openDrawer(GravityCompat.END);
            }
        });
        btn_scan = findViewById(R.id.btn_scan);
        btn_scan.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        Log.d(MyApplication.TAG, "App button touch down");
                        decodeTime = System.currentTimeMillis();
                        ServiceTools.getInstance().startScan();
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        Log.d(MyApplication.TAG, "App button touch up");
                        ServiceTools.getInstance().stopScan();
                        break;
                    default:
                        break;
                }
                return false;
            }
        });


        btn_clear = findViewById(R.id.btn_clear);
        btn_clear.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        //清空信息
                        tv_scanResult.setText("");
                        tv_pointResult.setText("");
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                    default:
                        break;
                }
                return false;
            }
        });

        btn_server = findViewById(R.id.btn_server);
        btn_server.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        //弹窗View框
                        showeditdialog();
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                    default:
                        break;
                }
                return false;
            }
        });

        /**
         * 投屏
         */
        btn_screen = findViewById(R.id.btn_screen);
        btn_screen.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        String res = tv_scanResult.getText().toString();
                        if (res != null) {
                            //if (res.contains("岩心") || res.contains("岩屑") || res.contains("壁心")) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        SharedPreferences s2 = getSharedPreferences("user", Context.MODE_PRIVATE);
                                        String ip = s2.getString("ip", "");
                                        String port = s2.getString("port", "");
                                        if (ip.equals("") || port.equals("")) {
                                            Looper.prepare();
                                            Toast.makeText(HomeActivity.this, "服务地址未配置", Toast.LENGTH_SHORT).show();
                                            Looper.loop();
                                        } else {
                                            String param = new String(Base64.encode(res.getBytes(), Base64.DEFAULT));
                                            String urlPath = "http://" + ip + ":" + port + "/HFchartController/changeAction.form?action=1&param=" + param;
                                            HttpURLconnectionClass.getData(urlPath);
                                        }
                                    } catch (Exception e) {
                                        Looper.prepare();
                                        Toast.makeText(HomeActivity.this, "请检查服务器连接", Toast.LENGTH_SHORT).show();
                                        Looper.loop();
                                    }
                                }
                            }).start();
//                            } else {
//                                Looper.prepare();
//                                Toast.makeText(HomeActivity.this, "请扫描正确的二维码格式", Toast.LENGTH_SHORT).show();
//                                Looper.loop();
//                            }
                        } else {
                            Looper.prepare();
                            Toast.makeText(HomeActivity.this, "请先扫描二维码", Toast.LENGTH_SHORT).show();
                            Looper.loop();
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                    default:
                        break;
                }
                return false;
            }
        });

    }


    public void showeditdialog() {
        dialog = new ServerDialog(this, 0, onclicklistener);
        dialog.create();
        SharedPreferences s2 = getSharedPreferences("user", Context.MODE_PRIVATE);
        String ip = s2.getString("ip", "");
        String port = s2.getString("port", "");
        dialog.text_ip.setText(ip);
        dialog.text_port.setText(port);
        dialog.show();
    }

    private View.OnClickListener onclicklistener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btn_save:
                    String ip = dialog.text_ip.getText().toString().trim();
                    String port = dialog.text_port.getText().toString().trim();
                    if (ip.length() == 0 || port.length() == 0) {
                        Toast.makeText(HomeActivity.this, "请设置服务器IP及端口", Toast.LENGTH_SHORT).show();
                    } else {
                        SharedPreferences sharedPreferences = getSharedPreferences("user", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("ip", ip);
                        editor.putString("port", port);
                        editor.commit();
                        dialog.cancel();
                    }
                    break;
                case R.id.btn_close:
                    dialog.cancel();
                    break;
            }
        }
    };


    private void setScanCallback() {
        SoftEngine.getInstance().setScanningCallback(new SoftEngine.ScanningCallback() {
            @Override
            public int onScanningCallback(int eventCode, int param1, byte[] param2, int length) {
                Log.d(TAG, "onScanningCallback");
                String strResult;
                byte[] data = param2;
                if (data == null) {
                    return 0;
                }
                if (eventCode == SCN_EVENT_DEC_SUCC && data.length > 0) {
                    MyApplication.mSoundPool.play(MyApplication.soundId, 1, 1, 0, 0, (float) 1.0); // 播放声音
                    MyApplication.vibrator.vibrate(100);
                    strResult = new String(param2, 128, length - 128);
                    Log.d(MyApplication.TAG, "strResult:" + strResult);
                    Message msg = new Message();
                    Bundle bundle = new Bundle();
                    bundle.putString("msgResult", strResult);
                    msg.setData(bundle);
                    msg.what = HANDLER_SCAN_NEW_MESSAGE;
                    mainThreadHandler.sendMessage(msg);
                    return 1;
                } else if (eventCode == SCN_EVENT_SCANNER_OVERHEAT) {
                    sendMsg("Scanner is overheat", HANDLER_SCANNER_OVERHEAT);
                } else {
                    sendMsg("Error event code:" + eventCode, HANDLER_SCANNER_OTHER_ERROR);
                }
                return 0;
            }
        });
    }

    private void sendMsg(String data, int flag) {
        Message msg = new Message();
        Bundle bundle = new Bundle();
        bundle.putString("msg_str", data);
        msg.setData(bundle);
        msg.what = flag;
        mainThreadHandler.sendMessage(msg);

    }

    /**
     * 将http请求结果展示出来
     */
    private Handler httpHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            String strResult = message.getData().getString("result");
            if (strResult != null) {
                tv_pointResult.setText(strResult);
            }
            return true;
        }
    });


    private Handler mainThreadHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLER_SCAN_NEW_MESSAGE:
                    String strResult = msg.getData().getString("msgResult");
                    Log.d(TAG, "Result:" + msg.arg2 + " body:" + strResult);
                    if (strResult != null) {
                        Random random = new Random();
                        tv_scanResult.setText(strResult);
                        tv_scanResult.setTextColor(Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256)));
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    SharedPreferences s2 = getSharedPreferences("user", Context.MODE_PRIVATE);
                                    String ip = s2.getString("ip", "");
                                    String port = s2.getString("port", "");
                                    if (ip.equals("") || port.equals("")) {
                                        Looper.prepare();
                                        Toast.makeText(HomeActivity.this, "服务地址未配置", Toast.LENGTH_SHORT).show();
                                        Looper.loop();
                                    } else {
                                        // if (strResult.equals("岩心") || strResult.equals("岩屑") || strResult.equals("壁心")) {

                                        String param = new String(Base64.encode(strResult.getBytes(), Base64.DEFAULT));
                                        String urlPath = "http://" + ip + ":" + port + "/HFchartController/queryAll.form?param=" + param;
                                        String res = HttpURLconnectionClass.getData(urlPath);
                                        //将更改主界面的消息发送给主线程
                                        Message msg = new Message();
                                        Bundle bundle = new Bundle();
                                        bundle.putString("result", res);
                                        msg.setData(bundle);
                                        httpHandler.sendMessage(msg);
//                                        } else {
//                                            Looper.prepare();
//                                            Toast.makeText(HomeActivity.this, "请扫描正确的二维码格式", Toast.LENGTH_SHORT).show();
//                                            Looper.loop();
//                                        }
                                    }
                                } catch (Exception e) {
                                    Looper.prepare();
                                    Toast.makeText(HomeActivity.this, "请检查服务器连接", Toast.LENGTH_SHORT).show();
                                    Looper.loop();
                                }
                            }
                        }).start();
                    }
                    return true;
                case HANDLER_SCANNER_OVERHEAT:
                case HANDLER_SCANNER_OTHER_ERROR:
                    Toast.makeText(HomeActivity.this, msg.getData().getString("msg_str"), Toast.LENGTH_SHORT).show();
                    return true;
                case MSG_INIT_DONE:
                    if (initProgressDialog != null) {
                        initProgressDialog.dismiss();
                        illuminationDataLoadFlag = true;
                        initProgressDialog = null;
                    }
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    return true;
                case MSG_INIT_FIRMWARE_UPGRADE:
                    if (initProgressDialog != null) {
                        initProgressDialog.setMessage(getString(R.string.init_prog_fw_upgrade));
                    }
                    return true;
                default:
                    break;
            }
            return false;
        }
    });

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
            return false;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
            case 2:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionsAreOk = checkPermissions();
                    if (permissionsAreOk) {
                        initData();
                    }
                } else {
                    finish();
                }
                return;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (needToResetScanCallback) {
            setScanCallback();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        needToResetScanCallback = true;
        ServiceTools.getInstance().stopScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(screenOnOffReceiver);
        ServiceTools.getInstance().deInit();
    }

    private BroadcastReceiver screenOnOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.SCREEN_ON".equals(action)) {
                SoftEngine.getInstance().Open();
                IlluminationUtil.DataLoad();
            } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
                SoftEngine.getInstance().Close();
            }
        }
    };

    static boolean BtnFlg = false;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "down code: " + keyCode);
        if (keyCode == 288 || keyCode == 286 || keyCode == 287 || keyCode == 135 || keyCode == 290) {
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (!BtnFlg) {
                        BtnFlg = true;
                        Log.d(MyApplication.TAG, "App button key down");
                        decodeTime = System.currentTimeMillis();
                        ServiceTools.getInstance().startScan();
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    BtnFlg = false;
                    break;
                default:
                    break;
            }
        }

        return super.onKeyDown(keyCode, event);
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        Log.d(TAG, " up code: " + keyCode);
        if (keyCode == 288 || keyCode == 286 || keyCode == 287 || keyCode == 135 || keyCode == 290) {
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_UP:
                    if (BtnFlg) {
                        BtnFlg = false;
                        ServiceTools.getInstance().stopScan();
                    }
                    break;
                default:
                    break;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Log.d(TAG, " onNavigationItemSelected: " + item.getItemId());
        switch (item.getItemId()) {
            case R.id.nav_last_image:
                startActivity(new Intent(HomeActivity.this, ImageShowActivity.class));
                break;
            case R.id.nav_batch_scan:
                startActivity(new Intent(HomeActivity.this, BatchScanActivity.class));
                break;
            case R.id.nav_code_setting:
                startActivity(new Intent(HomeActivity.this, CodeSetActivity.class));
                break;
            case R.id.nav_fw_upgrade:
                startActivity(new Intent(HomeActivity.this, DeviceUpdateActivity.class));
                break;
            case R.id.nav_debug_scan:
                startActivity(new Intent(HomeActivity.this, DebugScanActivity.class));
                break;
            case R.id.nav_debug_accuracy:
                startActivity(new Intent(HomeActivity.this, DebugAccuracyActivity.class));
                break;
            case R.id.nav_debug_auto:
                startActivity(new Intent(HomeActivity.this, DebugAutoScanActivity.class));
                break;
            case R.id.nav_debug_upgrade:
                startActivity(new Intent(HomeActivity.this, DebugUpdateActivity.class));
                break;
            case R.id.nav_version_show:
                startActivity(new Intent(HomeActivity.this, VersionShowActivity.class));
                break;
            case R.id.nav_center_scan:
                startActivity(new Intent(HomeActivity.this, CenterScanSettingActivity.class));
                break;
            case R.id.nav_illumination:
                startActivity(new Intent(HomeActivity.this, IlluminationSettingActivity.class));
                break;
            case R.id.nav_highlight:
                startActivity(new Intent(HomeActivity.this, HighLightSettingActivity.class));
                break;
        }

        drawer.closeDrawer(GravityCompat.END);
        return true;
    }
}
