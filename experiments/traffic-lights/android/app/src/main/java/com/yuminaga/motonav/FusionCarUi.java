package com.yuminaga.motonav;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.*;
import android.util.Log;
import android.view.*;
import java.io.File;
import java.io.IOException;
import java.util.WeakHashMap;

/** Android-only adapter, safe in the provider process without loading MotoNav SDKs.
 * Native initialization may still use an Activity; it is transparent and returned
 * behind the user's task. Explicit first-time setup/diagnostics remains available.
 */
public final class FusionCarUi implements Application.ActivityLifecycleCallbacks {
    private static final String MAP="com.autonavi.amapauto.MainMapActivity";
    private static final String CHANNEL="motonav_junction_service";
    private static final String MARKER="fusion-show-provider-console";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final WeakHashMap<Activity,Boolean> resumed=new WeakHashMap<>();
    private boolean warmed;
    private static File marker(Context c){return new File(c.getFilesDir(),MARKER);}
    private static boolean console(Context c){return marker(c).isFile();}
    public static void install(Application app){app.registerActivityLifecycleCallbacks(new FusionCarUi());}
    /** Only gates the provider's floating WINDOW. Guidance/light callbacks stay enabled. */
    public static boolean allowProviderFloat(){
        try {
            Application app=(Application)Class.forName("android.app.ActivityThread")
                .getMethod("currentApplication").invoke(null);
            return app!=null && console(app);
        } catch(ReflectiveOperationException e){return false;}
    }
    public static void maskProviderFloat(View view,WindowManager.LayoutParams params){
        if(allowProviderFloat())return;
        params.alpha=0f;
        params.flags|=WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        view.setAlpha(0f);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        Log.i("MotoNavProviderUi","provider float masked; native initialization/subscriptions preserved");
    }
    public static void prepare(Context c){
        if(!"com.autonavi.amapauto".equals(c.getPackageName()))return;
        marker(c).delete();
        try {
            Intent i=new Intent().setClassName(c.getPackageName(),
                "com.autonavi.amapauto.background.AutoBackgroundService");
            if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i);else c.startService(i);
            Log.i("MotoNavProviderUi","background service requested");
        } catch(RuntimeException e){Log.w("MotoNavProviderUi","background service unavailable",e);}
    }
    public static void openConsole(Context c){
        try {marker(c).createNewFile();}
        catch(IOException e){Log.w("MotoNavProviderUi","setup marker unavailable",e);return;}
        c.startActivity(new Intent().setClassName(c.getPackageName(),MAP)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP));
    }
    /** Keep the mandatory foreground-service notification; tapping returns MotoNav. */
    public static Notification notification(Context c,Notification original){
        if(!"com.autonavi.amapauto".equals(c.getPackageName()))return original;
        NotificationManager manager=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(
            new NotificationChannel(CHANNEL,"MotoNav 路口信息",NotificationManager.IMPORTANCE_LOW));
        Intent open=new Intent().setClassName(c.getPackageName(),"com.yuminaga.motonav.MainActivity")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent tap=PendingIntent.getActivity(c,7317,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,CHANNEL):new Notification.Builder(c);
        return b.setSmallIcon(android.R.drawable.ic_dialog_map).setContentTitle("MotoNav 正在运行")
            .setContentText("路口信息服务运行中，点此返回 MotoNav").setContentIntent(tap)
            .setOngoing(true).setOnlyAlertOnce(true).setCategory(Notification.CATEGORY_SERVICE).build();
    }
    private boolean target(Activity a){return MAP.equals(a.getClass().getName());}
    private void configure(Activity a){
        if(!target(a))return;
        Window w=a.getWindow();
        if(console(a)){
            int originalTheme=a.getResources().getIdentifier("Theme.Background","style",a.getPackageName());
            if(originalTheme!=0)a.setTheme(originalTheme);
            w.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            WindowManager.LayoutParams p=w.getAttributes();p.alpha=1f;w.setAttributes(p);
            w.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            return;
        }
        int theme=a.getResources().getIdentifier("FusionHiddenCarTheme","style",a.getPackageName());
        if(theme!=0)a.setTheme(theme);
        w.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        WindowManager.LayoutParams p=w.getAttributes();p.alpha=0f;p.windowAnimations=0;w.setAttributes(p);
        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        a.overridePendingTransition(0,0);
    }
    @Override public void onActivityPreCreated(Activity a,Bundle state){configure(a);}
    @Override public void onActivityCreated(Activity a,Bundle state){configure(a);}
    @Override public void onActivityResumed(Activity a){
        if(!target(a))return;
        resumed.put(a,true);configure(a);
        if(console(a)){Log.i("MotoNavProviderUi","explicit setup console visible");return;}
        Log.i("MotoNavProviderUi","provider resumed with transparent non-interactive window");
        handler.postDelayed(()->{
            if(a.isFinishing()||a.isDestroyed()||!Boolean.TRUE.equals(resumed.get(a))||console(a))return;
            configure(a);
            boolean moved=a.moveTaskToBack(true);
            a.overridePendingTransition(0,0);
            warmed=true;
            Log.i("MotoNavProviderUi","provider task backgrounded="+moved);
        },warmed?100:1200);
    }
    @Override public void onActivityPaused(Activity a){resumed.put(a,false);}
    @Override public void onActivityStarted(Activity a){}
    @Override public void onActivityStopped(Activity a){}
    @Override public void onActivitySaveInstanceState(Activity a,Bundle state){}
    @Override public void onActivityDestroyed(Activity a){resumed.remove(a);}
}
