package com.zengge.catchexception;

import android.app.Application;
import android.content.Context;
import android.widget.Toast;
import android.support.multidex.MultiDex;

public class CrashApp extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public void onCreate() {
        super.onCreate();
        //CrashHandler.getInstance().init(getApplicationContext());
        try {
            System.loadLibrary("function");
        } catch(UnsatisfiedLinkError u) {
            u.printStackTrace();
            Toast.makeText(getApplicationContext(), "ZGNB管理器功能库无法加载,程序即将退出!", Toast.LENGTH_LONG).show();
            try {
                Thread.sleep(3000);
            } catch(InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            System.exit(-1);
        }
    }
}
