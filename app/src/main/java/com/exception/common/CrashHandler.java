package com.exception.common;


import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.exception.common.MainActivity;
import com.exception.common.MainApplication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @ClassName: CrashHandler
 * @Description: 系统默认的UncaughtException处理类的实现类 用于当程序异常的时候 由该类接管程序并且发送错误信息
 */
public class CrashHandler implements UncaughtExceptionHandler {


    public static final String TAG = "CrashHandler";
    PendingIntent restartIntent;
    //系统默认的UncaughtException处理异常 
    private UncaughtExceptionHandler mDefaultHandler;
    //CrashHandler实例  
    private static CrashHandler INSTANCE = new CrashHandler();
    private Context mContext;

    private Application mapplication;
    private String  Exception_log ;


    //用来存储设备信息和异常信息
    private Map<String, String> infos = new HashMap<String, String>();

    //用于格式化日期作为日志文件名的 时间
    private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

    private CrashHandler() {
    }

    /**
     * 获取CrashHandler实例 ,单例模式
     */
    public static CrashHandler getInstance() {
        return INSTANCE;
    }

    /**
     * @param @param context    设定文件
     * @return void    返回类型
     * @throws
     * @Title: init
     * @Description: 初始化方法
     */
    public void init(Context context, Application application,String mException_log) {
        mContext = context;
        mapplication=application;
        Exception_log=mException_log;
        //获取系统默认的UncaughtException处理 
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        //设置该CrashHandler为程序的默认处理异常
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * 当UncaughtException发生时会转入该函数来处理
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!handleException(ex) && mDefaultHandler != null) {
            //如果用户没有处理则让系统默认的异常处理器来处处理 
            //mDefaultHandler.uncaughtException(thread, ex);  
        } else {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Log.e(TAG, "error : " + e);
            }
        }
    }

    /**
     * @param @param  ex
     * @param @return 设定文件
     * @return boolean    返回类型
     * @throws
     * @Title: handleException
     * @Description:自定义错误处 理，搜集错误信息发送和错误报告等操作都是在这里完成
     */
    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }

        //收集设备参数信息
        collectDeviceInfo(mContext);
        //保存日志文件
        String logFilePath = saveCrashInfo2File(ex);
        //使用Toast来显示异常信息
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                Toast.makeText(mContext, "发生异常,应用即将关闭!", Toast.LENGTH_SHORT).show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //写完日志信息在重启 虽然考虑到会照成ARN
                        Intent intent = new Intent(mapplication, MainActivity.class);
                        PendingIntent restartIntent = PendingIntent.getActivity(mapplication, 0, intent, Intent.FLAG_ACTIVITY_NEW_TASK);
                        Thread.setDefaultUncaughtExceptionHandler(restartHandler);
//                        AlarmManager mgr = (AlarmManager) AppApplicationContext.context.getSystemService(Context.ALARM_SERVICE);
//                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 10,
//                                restartIntent); // 1秒钟后重启应用
//                        Logger.d(TAG, "执行了重启");
                        new ActivityContrl().finishProgram();
                    }
                }, 500);
                Looper.loop();
            }
        }.start();
        return true;
    }

    /**
     * @param @param ctx    设定文件
     * @return void    返回类型
     * @throws
     * @Title: collectDeviceInfo
     * @Description: 收集设备参数信息
     */
    public void collectDeviceInfo(Context ctx) {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode = pi.versionCode + "";
                infos.put("versionName", versionName);
                infos.put("versionCode", versionCode);
            }
        } catch (NameNotFoundException e) {
            Log.e(TAG, "an error occured when collect package info" + e);
        }
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                infos.put(field.getName(), field.get(null).toString());
                Log.d(TAG, field.getName() + " : " + field.get(null));
            } catch (Exception e) {
                Log.e(TAG, "an error occured when collect crash info" + e);
            }
        }
    }

    /**
     * @param @param  ex
     * @param @return 返回文件名称,便于将文件传送到服务器
     * @return String    返回类型
     * @throws
     * @Title: saveCrashInfo2File
     * @Description: 保存错误信息到文件中
     */
    private String saveCrashInfo2File(Throwable ex) {

        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : infos.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key + "=" + value + "\n");
        }

        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        sb.append(result);
        try {
            long timestamp = System.currentTimeMillis();
            String time = formatter.format(new Date());
            String fileName = "Crash-" + time + "-" + timestamp + ".txt";
            String path =  Exception_log;
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                File dir = new File(path);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(path + fileName);
                fos.write(sb.toString().getBytes());
                fos.close();
            }


            return path + fileName;
        } catch (Exception e) {
            Log.e(TAG, "an error occured while writing file..." + e);
        }
        return null;
    }


    public UncaughtExceptionHandler restartHandler = new UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            Log.d(TAG, "进入了重启");
            AlarmManager mgr = (AlarmManager) mapplication.getSystemService(Context.ALARM_SERVICE);
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000,
                    restartIntent); // 1秒钟后重启应用
            Log.d(TAG, "执行了重启");
            new ActivityContrl().finishProgram(); // 自定义方法，关闭当前打开的所有avtivity
        }
    };

    public class ActivityContrl {
        private List<Activity> activityList = new ArrayList<Activity>();

        public void remove(Activity activity) {
            activityList.remove(activity);
        }

        public void add(Activity activity) {
            activityList.add(activity);
        }

        public void finishProgram() {
            for (Activity activity : activityList) {
                if (null != activity) {
                    activity.finish();
                }
            }
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }
    /**
     * 解决安卓6.0以上版本不能读取外部存储权限的问题
     * @param activity
     * @return
     */
    public static boolean isGrantExternalRW(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity.checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 1);

            return false;
        }

        return true;
    }
}  