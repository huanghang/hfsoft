package com.dawn.java.util;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.dawn.java.R;


public class ServerDialog extends Dialog {
    Activity context;
    private Button btn_save;
    private Button btn_close;
    public EditText text_ip;
    public EditText text_port;

    private View.OnClickListener mClickListener;

    public ServerDialog(Activity context) {
        super(context);
        this.context = context;
    }

    public ServerDialog(Activity context, int theme, View.OnClickListener clickListenerl) {
        super(context, theme);
        this.context = context;
        this.mClickListener = clickListenerl;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.setContentView(R.layout.dialog);
        text_ip = findViewById(R.id.text_ip);
        text_port = findViewById(R.id.text_port);
        btn_save = findViewById(R.id.btn_save);
        btn_close = findViewById(R.id.btn_close);
        btn_save.setOnClickListener(mClickListener);
        btn_close.setOnClickListener(mClickListener);
        this.setCancelable(true);
    }
}
