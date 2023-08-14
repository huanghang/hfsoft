package com.dawn.java.ui.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.dawn.decoderapijni.ServiceTools;
import com.dawn.decoderapijni.SoftEngine;
import com.dawn.java.R;
import com.dawn.java.ui.MyApplication;
import com.dawn.java.ui.activity.base.DawnActivity;
import com.dawn.java.ui.widget.TopToolbar;
import com.leon.lfilepickerlibrary.LFilePicker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

public class DebugUpdateActivity extends DawnActivity implements View.OnClickListener {

    private final int MSG_UPDATE_FINISH = 0x01;
    private final int MSG_GET_UPDATE_PROGRESS_VALUE = 0x02;
    private final int REQ_CODE_SELECT_FW_1 = 0x1823;
    private final int REQ_CODE_SELECT_FW_2 = 0x1824;

    private TopToolbar topToolbar;
    private Button btn_start_update, btn_select_fw1, btn_select_fw2;
//    private Button btn_stop_update;

    private TextView tvFwPath1, tvFwPath2, tvCurrVer;
    private ProgressDialog progressDialogForUpdate = null;
    private ProgressDialog progressCurUpdate = null;


    private boolean isTesting = false;
    private String newFwPath1 = null;
    private String newFwPath2 = null;
    private int gCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initData();
        setUpgradeCallback();
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_debug_update;
    }

    public void initView() {
        tvCurrVer = findViewById(R.id.tv_curr_ver);
        tvFwPath1 = findViewById(R.id.tv_fw_path_1);
        tvFwPath2 = findViewById(R.id.tv_fw_path_2);
        topToolbar = findViewById(R.id.tb_topToolBar);
        topToolbar.setMenuToolBarListener(new TopToolbar.MenuToolBarListener() {
            @Override
            public void onToolBarClickLeft(View v) {
                finish();
            }

            @Override
            public void onToolBarClickRight(View v) {

            }
        });

        btn_start_update = findViewById(R.id.btn_start_update);
        btn_start_update.setOnClickListener(this);

        btn_select_fw1 = findViewById(R.id.btn_select_fw1);
        btn_select_fw1.setOnClickListener(this);
        btn_select_fw2 = findViewById(R.id.btn_select_fw2);
        btn_select_fw2.setOnClickListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_CODE_SELECT_FW_1 && resultCode == RESULT_OK) {
            List<String> list = data.getStringArrayListExtra("paths");
            if (list.size() > 0) {
                newFwPath1 = list.get(0);
                tvFwPath1.setText(newFwPath1);
            }
        } else if (requestCode == REQ_CODE_SELECT_FW_2 && resultCode == RESULT_OK) {
            List<String> list = data.getStringArrayListExtra("paths");
            if (list.size() > 0) {
                newFwPath2 = list.get(0);
                tvFwPath2.setText(newFwPath2);
            }
        }
    }

    public void initData() {
        showCurrVersion();
    }

    private void showCurrVersion() {
        try {
            tvCurrVer.setText(SoftEngine.getInstance().getScannerVersion());
        } catch (Exception e) {
            tvCurrVer.setText(R.string.update_unknown_version);
        }
    }

    private void startUpdate() {
        if (newFwPath1 == null || newFwPath2 == null) {
            Toast.makeText(this, R.string.update_choose_fw_first, Toast.LENGTH_SHORT).show();
            return;
        }
        if (progressDialogForUpdate == null) {
            progressDialogForUpdate = new ProgressDialog(this);
            progressDialogForUpdate.setCancelable(false);
            progressDialogForUpdate.setTitle(R.string.dialog_please_wait);
            progressDialogForUpdate.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);// 设置水平进度条
            progressDialogForUpdate.setMax(1);
            progressDialogForUpdate.setMessage("Updating scanner...\n Count:" + gCount);
            progressDialogForUpdate.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(R.string.update_choose_fw_first),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // TODO Auto-generated method stub
                            isTesting = false;
                            progressCurUpdate = new ProgressDialog(DebugUpdateActivity.this);
                            progressCurUpdate.setCancelable(false);
                            progressCurUpdate.setTitle(R.string.update_wait_update_finish);
                            progressCurUpdate.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);// 设置水平进度条
                            progressCurUpdate.setMax(1);
                            progressCurUpdate.show();
                        }
                    });
            progressDialogForUpdate.show();

            final String currFwPath;
            if (gCount % 2 == 0) {
                currFwPath = newFwPath1;
            } else {
                currFwPath = newFwPath2;
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final boolean updateIsOk = ServiceTools.getInstance().updateFirmware(currFwPath);
                    try {
                        // Make sure scanner reboot already
                        Thread.sleep(1000);

                        Message msg = new Message();
                        Bundle bundle = new Bundle();
                        bundle.putBoolean("updateIsOk", updateIsOk);
                        msg.setData(bundle);
                        msg.what = MSG_UPDATE_FINISH;
                        handler.sendMessage(msg);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_GET_UPDATE_PROGRESS_VALUE:
                    if (progressDialogForUpdate != null && progressDialogForUpdate.isShowing()) {
                        progressDialogForUpdate.setProgress(msg.arg1);
                        progressDialogForUpdate.setMax(msg.arg2);
                    }

                    if (progressCurUpdate != null && progressCurUpdate.isShowing()) {
                        progressCurUpdate.setProgress(msg.arg1);
                        progressCurUpdate.setMax(msg.arg2);
                    }
                    break;
                case MSG_UPDATE_FINISH:
                    gCount++;
                    Bundle bundle = msg.getData();
                    boolean updateIsOk = bundle.getBoolean("updateIsOk");

                    if (progressDialogForUpdate != null) {
                        progressDialogForUpdate.dismiss();
                        progressDialogForUpdate = null;
                    }
                    if (progressCurUpdate != null) {
                        progressCurUpdate.dismiss();
                        progressCurUpdate = null;
                    }
                    Toast.makeText(DebugUpdateActivity.this,
                            updateIsOk ? R.string.update_success : R.string.update_failure,
                            Toast.LENGTH_LONG).show();
                    showCurrVersion();


                    if (isTesting) {
                        startUpdate();
                    } else {
                        gCount = 0;
                        btn_start_update.setEnabled(true);

                    }

                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start_update:
                isTesting = true;
                btn_start_update.setEnabled(false);
//                btn_stop_update.setEnabled(true);
                startUpdate();
                break;
            case R.id.btn_select_fw1:
                Toast.makeText(DebugUpdateActivity.this, "Loading", Toast.LENGTH_SHORT).show();
                // use https://github.com/leonHua/LFilePicker
                new LFilePicker()
                        .withActivity(DebugUpdateActivity.this)
                        .withRequestCode(REQ_CODE_SELECT_FW_1)
                        .withStartPath("/sdcard")
                        .withFileFilter(new String[]{".bin"})
                        .withMutilyMode(false)
                        .start();
                break;
            case R.id.btn_select_fw2:
                Toast.makeText(DebugUpdateActivity.this, "Loading", Toast.LENGTH_SHORT).show();
                // use https://github.com/leonHua/LFilePicker
                new LFilePicker()
                        .withActivity(DebugUpdateActivity.this)
                        .withRequestCode(REQ_CODE_SELECT_FW_2)
                        .withStartPath("/sdcard")
                        .withFileFilter(new String[]{".bin"})
                        .withMutilyMode(false)
                        .start();
                break;
            default:
                break;
        }
    }

    private void setUpgradeCallback() {
        SoftEngine.getInstance().setUpgradeCallback(new SoftEngine.UpgradeProgressCallback() {
            @Override
            public void onUpgradeCallback(int progressValue, int totalValue) {
                Message msg = new Message();
                msg.what = MSG_GET_UPDATE_PROGRESS_VALUE;
                msg.arg1 = progressValue;
                msg.arg2 = totalValue;
                handler.sendMessage(msg);
            }
        });
    }
}
