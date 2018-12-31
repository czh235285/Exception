package com.exception.common;

import android.app.Application;

/**
 * Created by wuaomall@gmail.com on 2018/12/31.
 */
public class MainApplication extends Application {


    private static MainApplication mainApplication;

    @Override
    public void onCreate() {
        super.onCreate();

        init();

    }


    private void init() {
        //保存日志到本地 并且崩溃重启
//        if (AppUtils.isDebugMode(getApplicationContext())){
//            CrashHandler crashHandler = CrashHandler.getInstance();
//            crashHandler.init(getApplicationContext(),MainApplication.getInstance(),"/sdcard/app名字/ExceptionLog/")  ;
//        }
    }



    public static   MainApplication getInstance(){
        if (mainApplication == null) {
            mainApplication = new MainApplication();
        }

        return  mainApplication;
    }
}
