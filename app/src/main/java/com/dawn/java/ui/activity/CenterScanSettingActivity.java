package com.dawn.java.ui.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import com.dawn.decoderapijni.SoftEngine;
import com.dawn.java.R;
import com.dawn.java.ui.MyApplication;
import com.dawn.java.ui.widget.TopToolbar;

public class CenterScanSettingActivity extends AppCompatActivity {
    private Button btnCalibration;
//    private Button btnEnable;
//    private Button btnDisable;
    private TopToolbar topBar;
    private static final String TAG = "ScanJni DLScan";
    private Switch switch_enable;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_center_scan_setting);
        initView();
        initData();
    }

    private void initView() {
        topBar = findViewById(R.id.topBar);
        topBar.setMenuToolBarListener(new TopToolbar.MenuToolBarListener() {
            @Override
            public void onToolBarClickLeft(View v) {
                finish();
            }

            @Override
            public void onToolBarClickRight(View v) {
            }
        });
        switch_enable = findViewById(R.id.switch_enable);
        switch_enable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Log.v(TAG, "onCheckedChanged");
                if (b) {
                    SoftEngine.getInstance().setFocusDecodeEnable(1);
                    btnCalibration.setVisibility(View.VISIBLE);
                }else{
                    SoftEngine.getInstance().setFocusDecodeEnable(0);
                    btnCalibration.setVisibility(View.INVISIBLE);
                }
            }
        });

        btnCalibration = findViewById(R.id.btn_calibration);
        btnCalibration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SoftEngine.getInstance().setFocusDecodeCalibration();
            }
        });
    }

    private void initData(){
        int rc = SoftEngine.getInstance().getFocusDecodeEnable();
        if (rc == 1){
            switch_enable.setChecked(true);
            btnCalibration.setVisibility(View.VISIBLE);
        }else{
            btnCalibration.setVisibility(View.INVISIBLE);
        }
    }

}
