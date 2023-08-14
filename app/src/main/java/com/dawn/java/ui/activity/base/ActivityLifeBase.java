package com.dawn.java.ui.activity.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

/**
 * Activity 生命周期基类，管理Activity
 * activity 堆栈 管理 添加/删除
 */
public class ActivityLifeBase extends FragmentActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

    }
}
