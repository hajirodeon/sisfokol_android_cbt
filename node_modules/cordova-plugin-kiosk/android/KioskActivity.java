package jk.cordova.plugin.kiosk;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import org.apache.cordova.*;

import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.lang.reflect.Method;

public class KioskActivity extends CordovaActivity {

    private static final String PREF_KIOSK_MODE = "pref_kiosk_mode";
    private static final int REQUEST_CODE = 123467;
    public static boolean running = false;
    Object statusBarService;
    ActivityManager am;
    String TAG = "KioskActivity";

    protected void onStart() {
        super.onStart();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        if(Build.VERSION.SDK_INT >= 23) {
            sp.edit().putBoolean(PREF_KIOSK_MODE, false).commit();
            checkDrawOverlayPermission();
        } else {
            sp.edit().putBoolean(PREF_KIOSK_MODE, true).commit();
            addOverlay();
        }
        running = true;
    }
    //http://stackoverflow.com/questions/7569937/unable-to-add-window-android-view-viewrootw44da9bc0-permission-denied-for-t
    @TargetApi(Build.VERSION_CODES.M)
    public void checkDrawOverlayPermission() {
        if (!Settings.canDrawOverlays(this.getApplicationContext())) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode,  Intent data) {
        if (requestCode == REQUEST_CODE) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
            sp.edit().putBoolean(PREF_KIOSK_MODE, true).commit();
            if (Settings.canDrawOverlays(this)) {
                addOverlay();
            }
        }
    }
    //http://stackoverflow.com/questions/25284233/prevent-status-bar-for-appearing-android-modified?answertab=active#tab-top
    public void addOverlay() {
        WindowManager manager = ((WindowManager) getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE));

        WindowManager.LayoutParams localLayoutParams = new WindowManager.LayoutParams();
        localLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        localLayoutParams.gravity = Gravity.TOP;
        localLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|

                // this is to enable the notification to recieve touch events
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |

                // Draws over status bar
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

        localLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        localLayoutParams.height = (int) (50 * getResources()
                .getDisplayMetrics().scaledDensity);
        localLayoutParams.format = PixelFormat.TRANSPARENT;

        CustomViewGroup view = new CustomViewGroup(this);

        manager.addView(view, localLayoutParams);
    }

    protected void onStop() {
        super.onStop();
        running = false;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.init();
        loadUrl(launchUrl);
    }

    private void collapseNotifications()
    {
        try
        {
            if(statusBarService == null) {
                statusBarService = getSystemService("statusbar");
            }

            Class<?> statusBarManager = Class.forName("android.app.StatusBarManager");

            if (Build.VERSION.SDK_INT <= 16)
            {
                Method collapseStatusBar = statusBarManager.getMethod("collapse");
                collapseStatusBar.setAccessible(true);
                collapseStatusBar.invoke(statusBarService);
                return;
            }
            Method collapseStatusBar = statusBarManager.getMethod("collapsePanels");
            collapseStatusBar.setAccessible(true);
            collapseStatusBar.invoke(statusBarService);
        }
        catch (Exception e)
        {
            Log.e(TAG, e.toString());
        }
    }

    public void onPause()
    {
        super.onPause();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        sp.edit().putBoolean(PREF_KIOSK_MODE, true).commit();
        if(!sp.getBoolean(PREF_KIOSK_MODE, false)) {
            return;
        }
        if(am == null) {
            am = ((ActivityManager)getSystemService("activity"));
        }
        am.moveTaskToFront(getTaskId(), 1);
        sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
        collapseNotifications();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        sp.edit().putBoolean(PREF_KIOSK_MODE, true).commit();
        if(!sp.getBoolean(PREF_KIOSK_MODE, false)) {
            return;
        }
        if (!hasFocus) {
            if(am == null) {
                am = ((ActivityManager)getSystemService("activity"));
            }
            am.moveTaskToFront(getTaskId(), 1);
            sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
            collapseNotifications();
        }
    }

    //http://stackoverflow.com/questions/25284233/prevent-status-bar-for-appearing-android-modified?answertab=active#tab-top
    public class CustomViewGroup extends ViewGroup {

        public CustomViewGroup(Context context) {
            super(context);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            return true;
        }
    }
}

