package cn.qianlan.klinedemo;

import android.app.Application;

public class AppContext extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        OkHttpUtil.initOkHttp();
    }
}
