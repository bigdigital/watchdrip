package com.thatguysservice.huami_xdrip.models;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;

import com.activeandroid.ActiveAndroid;
import com.thatguysservice.huami_xdrip.HuamiXdrip;
import com.thatguysservice.huami_xdrip.PreferenceActivity;
import com.thatguysservice.huami_xdrip.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import static android.content.Context.ALARM_SERVICE;

/**
 * Created by jamorham on 06/01/16.
 * <p>
 * lazy helper class for utilities
 */
public class Helper {
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private final static String TAG = "Helper";
    private final static boolean debug_wakelocks = false;
    private static final Map<String, Long> rateLimits = new HashMap<>();
    // singletons to avoid repeated allocation
    private static DecimalFormatSymbols dfs;
    private static DecimalFormat df;

    public static boolean buggy_samsung = false; // flag set when we detect samsung devices which do not perform to android specifications

    public static void startService(final Class c, final String... args) {
        startService(c, null, args);
    }

    public static void startService(final Class c, final byte[] bytes, final String... args) {
        final Intent intent = new Intent(HuamiXdrip.getAppContext(), c);
        if (bytes != null) {
            intent.putExtra("bytes_payload", bytes);
        }
        if (args.length % 2 == 1) {
            throw new RuntimeException("Odd number of args for JoH.startService");
        }
        for (int i = 0; i < args.length; i += 2) {
            intent.putExtra(args[i], args[i + 1]);
        }
        HuamiXdrip.getAppContext().startService(intent);
    }

    public static String qs(double x, int digits) {

        if (digits == -1) {
            digits = 0;
            if (((int) x != x)) {
                digits++;
                if ((((int) x * 10) / 10 != x)) {
                    digits++;
                    if ((((int) x * 100) / 100 != x)) digits++;
                }
            }
        }

        if (dfs == null) {
            final DecimalFormatSymbols local_dfs = new DecimalFormatSymbols();
            local_dfs.setDecimalSeparator('.');
            dfs = local_dfs; // avoid race condition
        }

        final DecimalFormat this_df;
        // use singleton if on ui thread otherwise allocate new as DecimalFormat is not thread safe
        if (Thread.currentThread().getId() == 1) {
            if (df == null) {
                final DecimalFormat local_df = new DecimalFormat("#", dfs);
                local_df.setMinimumIntegerDigits(1);
                df = local_df; // avoid race condition
            }
            this_df = df;
        } else {
            this_df = new DecimalFormat("#", dfs);
        }

        this_df.setMaximumFractionDigits(digits);
        return this_df.format(x);
    }

    public static double ts() {
        return new Date().getTime();
    }

    public static long tsl() {
        return System.currentTimeMillis();
    }

    public static long uptime() {
        return SystemClock.uptimeMillis();
    }

    public static boolean upForAtLeastMins(int mins) {
        return uptime() > Constants.MINUTE_IN_MS * mins;
    }

    public static long msSince(long when) {
        return (tsl() - when);
    }

    public static long msSince(long end, long start) {
        return (end - start);
    }

    public static long msTill(long when) {
        return (when - tsl());
    }

    public static long absMsSince(long when) {
        return Math.abs(tsl() - when);
    }

    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "<empty>";
        final char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            final int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    // Convert a stream of bytes to a mac format (i.e: 12:34:AB:BC:DE:FC)
    public static String bytesToHexMacFormat(final byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "NoMac";
        }
        final String str = bytesToHex(bytes);
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i += 2) {
            if (sb.length() > 0) {
                sb.append(":");
            }
            sb.append(str.substring(i, i + 2));
        }
        return sb.toString();
    }

    public static byte[] reverseBytes(byte[] source) {
        byte[] dest = new byte[source.length];
        for (int i = 0; i < source.length; i++) {
            dest[(source.length - i) - 1] = source[i];
        }
        return dest;
    }

    public static byte[] tolerantHexStringToByteArray(String str) {
        return hexStringToByteArray(str.toUpperCase().replaceAll("[^A-F0-9]", ""));
    }

    public static byte[] hexStringToByteArray(String str) {
        try {
            str = str.toUpperCase().trim();
            if (str.length() == 0) return null;
            final int len = str.length();
            byte[] data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(str.charAt(i), 16) << 4) + Character.digit(str.charAt(i + 1), 16));
            }
            return data;
        } catch (Exception e) {
            Log.e(TAG, "Exception processing hexString: " + e);
            return null;
        }
    }

    public static String macFormat(final String unformatted) {
        if (unformatted == null) return null;
        try {
            return unformatted.replaceAll("[^a-fA-F0-9]", "").replaceAll("(.{2})", "$1:").substring(0, 17);
        } catch (Exception e) {
            return null;
        }
    }

    public static String base64encode(String input) {
        try {
            return new String(Base64.encode(input.getBytes("UTF-8"), Base64.NO_WRAP), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Got unsupported encoding: " + e);
            return "encode-error";
        }
    }

    public static String base64decode(String input) {
        try {
            return new String(Base64.decode(input.getBytes("UTF-8"), Base64.NO_WRAP), "UTF-8");
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            Log.e(TAG, "Got unsupported encoding: " + e);
            return "decode-error";
        }
    }

    public static String base64encodeBytes(byte[] input) {
        try {
            return new String(Base64.encode(input, Base64.NO_WRAP), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Got unsupported encoding: " + e);
            return "encode-error";
        }
    }

    public static byte[] base64decodeBytes(String input) {
        try {
            return Base64.decode(input.getBytes("UTF-8"), Base64.NO_WRAP);
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            Log.e(TAG, "Got unsupported encoding: " + e);
            return new byte[0];
        }
    }

    public static String ucFirst(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }


    public static String backTrace() {
        return backTrace(1);
    }

    public static String backTrace(int depth) {
        try {
            StackTraceElement stack = new Exception().getStackTrace()[2 + depth];
            StackTraceElement stackb = new Exception().getStackTrace()[3 + depth];
            String[] stackclassa = stack.getClassName().split("\\.");
            String[] stackbclassa = stackb.getClassName().split("\\.");

            return stackbclassa[stackbclassa.length - 1] + "::" + stackb.getMethodName()
                    + " -> " + stackclassa[stackclassa.length - 1] + "::" + stack.getMethodName();
        } catch (Exception e) {
            return "unknown backtrace: " + e.toString();
        }
    }

    public static String backTraceShort(int depth) {
        try {
            final StackTraceElement stackb = new Exception().getStackTrace()[3 + depth];
            return stackb.getMethodName();
        } catch (Exception e) {
            return "unknown backtrace: " + e.toString();
        }
    }


    public static synchronized void clearRatelimit(final String name) {
        if (PersistentStore.getLong(name) > 0) {
            PersistentStore.setLong(name, 0);
        }
        if (rateLimits.containsKey(name)) {
            rateLimits.remove(name);
        }
    }

    // return true if below rate limit (persistent version)
    public static synchronized boolean pratelimit(String name, int seconds) {
        // check if over limit
        final long time_now = Helper.tsl();
        final long rate_time;
        if (!rateLimits.containsKey(name)) {
            rate_time = PersistentStore.getLong(name); // 0 if undef
        } else {
            rate_time = rateLimits.get(name);
        }
        if ((rate_time > 0) && (time_now - rate_time) < (seconds * 1000L)) {
            Log.d(TAG, name + " rate limited: " + seconds + " seconds");
            return false;
        }
        // not over limit
        rateLimits.put(name, time_now);
        PersistentStore.setLong(name, time_now);
        return true;
    }

    // return true if below rate limit
    public static synchronized boolean ratelimit(String name, int seconds) {
        // check if over limit
        if ((rateLimits.containsKey(name)) && (Helper.tsl() - rateLimits.get(name) < (seconds * 1000L))) {
            Log.d(TAG, name + " rate limited: " + seconds + " seconds");
            return false;
        }
        // not over limit
        rateLimits.put(name, Helper.tsl());
        return true;
    }

    // return true if below rate limit
    public static synchronized boolean quietratelimit(String name, int seconds) {
        // check if over limit
        if ((rateLimits.containsKey(name)) && (Helper.tsl() - rateLimits.get(name) < (seconds * 1000))) {
            return false;
        }
        // not over limit
        rateLimits.put(name, Helper.tsl());
        return true;
    }

    // return true if below rate limit
    public static synchronized boolean ratelimitmilli(String name, int milliseconds) {
        // check if over limit
        if ((rateLimits.containsKey(name)) && (Helper.tsl() - rateLimits.get(name) < (milliseconds))) {
            Log.d(TAG, name + " rate limited: " + milliseconds + " milliseconds");
            return false;
        }
        // not over limit
        rateLimits.put(name, Helper.tsl());
        return true;
    }

    public static String getDeviceDetails() {
        final String manufacturer = Build.MANUFACTURER.replace(" ", "_");
        final String model = Build.MODEL.replace(" ", "_");
        final String version = Integer.toString(Build.VERSION.SDK_INT) + " " + Build.VERSION.RELEASE + " " + Build.VERSION.INCREMENTAL;
        return manufacturer + " " + model + " " + version;
    }

    public static String getVersionDetails() {
        try {
            return HuamiXdrip.getAppContext().getPackageManager().getPackageInfo(HuamiXdrip.getAppContext().getPackageName(), PackageManager.GET_META_DATA).versionName;
        } catch (Exception e) {
            return "Unknown version";
        }
    }

    public static boolean isOldVersion(Context context) {
        try {
            final Signature[] pinfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES).signatures;
            if (pinfo.length == 1) {
                final Checksum s = new CRC32();
                final byte[] ba = pinfo[0].toByteArray();
                s.update(ba, 0, ba.length);
                if (s.getValue() == 2009579833) return true;
            }
        } catch (Exception e) {
            Log.d(TAG, "exception: " + e);
        }
        return false;
    }

    public static String hourMinuteString() {
        // Date date = new Date();
        // SimpleDateFormat sd = new SimpleDateFormat("HH:mm");
        //  return sd.format(date);
        return hourMinuteString(Helper.tsl());
    }

    public static String hourMinuteString(long timestamp) {
        return android.text.format.DateFormat.format("kk:mm", timestamp).toString();
    }

    public static String dateTimeText(long timestamp) {
        return android.text.format.DateFormat.format("yyyy-MM-dd kk:mm:ss", timestamp).toString();
    }

    public static String dateText(long timestamp) {
        return android.text.format.DateFormat.format("yyyy-MM-dd", timestamp).toString();
    }

    public static long getTimeZoneOffsetMs() {
        return new GregorianCalendar().getTimeZone().getRawOffset();
    }

    public static String niceTimeSince(long t) {
        return niceTimeScalar(msSince(t));
    }

    public static String niceTimeTill(long t) {
        return niceTimeScalar(-msSince(t));
    }

    // temporary
    public static String niceTimeScalar(long t) {
        String unit = HuamiXdrip.getAppContext().getString(R.string.unit_second);
        t = t / 1000;
        if (t != 1) unit =  HuamiXdrip.getAppContext().getString(R.string.unit_seconds);
        if (t > 59) {
            unit =  HuamiXdrip.getAppContext().getString(R.string.unit_minute);
            t = t / 60;
            if (t != 1) unit =  HuamiXdrip.getAppContext().getString(R.string.unit_minutes);
            if (t > 59) {
                unit =  HuamiXdrip.getAppContext().getString(R.string.unit_hour);
                t = t / 60;
                if (t != 1) unit =  HuamiXdrip.getAppContext().getString(R.string.unit_hours);
                if (t > 24) {
                    unit =  HuamiXdrip.getAppContext().getString(R.string.unit_day);
                    t = t / 24;
                    if (t != 1) unit =  HuamiXdrip.getAppContext().getString(R.string.unit_days);
                    if (t > 28) {
                        unit =  HuamiXdrip.getAppContext().getString(R.string.unit_week);
                        t = t / 7;
                        if (t != 1) unit =  HuamiXdrip.getAppContext().getString(R.string.unit_weeks);
                    }
                }
            }
        }
        //if (t != 1) unit = unit + "s"; //implemented plurality in every step, because in other languages plurality of time is not every time adding the same character
        return qs((double) t, 0) + " " + unit;
    }

    public static String niceTimeScalar(double t, int digits) {
        String unit =  HuamiXdrip.getAppContext().getString(R.string.unit_second);
        t = t / 1000;
        if (t != 1) unit =  HuamiXdrip.getAppContext().getString(R.string.unit_seconds);
        if (t > 59) {
            unit =  HuamiXdrip.getAppContext().getString(R.string.unit_minute);
            t = t / 60;
            if (t != 1) unit =  HuamiXdrip.getAppContext().getString(R.string.unit_minutes);
            if (t > 59) {
                unit =  HuamiXdrip.getAppContext().getString(R.string.unit_hour);
                t = t / 60;
                if (t != 1) unit =  HuamiXdrip.getAppContext().getString(R.string.unit_hours);
                if (t > 24) {
                    unit =  HuamiXdrip.getAppContext().getString(R.string.unit_day);
                    t = t / 24;
                    if (t != 1) unit =  HuamiXdrip.getAppContext().getString(R.string.unit_days);
                    if (t > 28) {
                        unit =  HuamiXdrip.getAppContext().getString(R.string.unit_week);
                        t = t / 7;
                        if (t != 1) unit =  HuamiXdrip.getAppContext().getString(R.string.unit_weeks);
                    }
                }
            }
        }
        //if (t != 1) unit = unit + "s"; //implemented plurality in every step, because in other languages plurality of time is not every time adding the same character
        return qs(t, digits) + " " + unit;
    }

    public static String niceTimeScalarNatural(long t) {
        if (t > 3000000) t = t + 10000; // round up by 10 seconds if nearly an hour
        if ((t > Constants.DAY_IN_MS) && (t < Constants.WEEK_IN_MS * 2)) {
            final SimpleDateFormat df = new SimpleDateFormat("EEEE", Locale.getDefault());
            final String day = df.format(new Date(Helper.tsl() + t));
            return ((t > Constants.WEEK_IN_MS) ? "next " : "") + day;
        } else {
            return niceTimeScalar(t);
        }
    }

    public static String niceTimeScalarRedux(long t) {
        return niceTimeScalar(t).replaceFirst("^1 ", "");
    }

    public static String niceTimeScalarShort(long t) {
        return niceTimeScalar(t).replaceFirst("([A-z]).*$", "$1");
    }

    public static String niceTimeScalarShortWithDecimalHours(long t) {
        if (t > Constants.HOUR_IN_MS) {
            return niceTimeScalar(t, 1).replaceFirst("([A-z]).*$", "$1");
        } else {
            return niceTimeScalar(t).replaceFirst("([A-z]).*$", "$1");
        }
    }

    public static double tolerantParseDouble(String str) throws NumberFormatException {
        return Double.parseDouble(str.replace(",", "."));
    }

    public static double tolerantParseDouble(final String str, final double def) {
        if (str == null) return def;
        try {
            return Double.parseDouble(str.replace(",", "."));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static int tolerantParseInt(final String str, final int def) {
        if (str == null) return def;
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static long tolerantParseLong(final String str, final long def) {
        if (str == null) return def;
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static String getRFC822String(long timestamp) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        return dateFormat.format(new Date(timestamp));
    }

    public static PowerManager.WakeLock getWakeLock(final String name, int millis) {
        final PowerManager pm = (PowerManager)  HuamiXdrip.getAppContext().getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, name);
        wl.acquire(millis);
        if (debug_wakelocks) Log.d(TAG, "getWakeLock: " + name + " " + wl.toString());
        return wl;
    }

    public static synchronized void releaseWakeLock(final PowerManager.WakeLock wl) {
        if (debug_wakelocks) Log.d(TAG, "releaseWakeLock: " + wl.toString());
        if (wl == null) return;
        if (wl.isHeld()) {
            try {
                wl.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing wakelock: " + e);
            }
        }
    }

    public static PowerManager.WakeLock fullWakeLock(final String name, long millis) {
        final PowerManager pm = (PowerManager)  HuamiXdrip.getAppContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, name);
        wl.acquire(millis);
        if (debug_wakelocks) Log.d(TAG, "fullWakeLock: " + name + " " + wl.toString());
        return wl;
    }

    public static void fullDatabaseReset() {
        try {
            clearCache();
            ActiveAndroid.dispose();
            ActiveAndroid.initialize( HuamiXdrip.getAppContext());
        } catch (Exception e) {
            Log.e(TAG, "Error restarting active android db");
        }
    }

    public static void clearCache() {
        try {
            ActiveAndroid.clearCache();
        } catch (Exception e) {
            Log.e(TAG, "Error clearing active android cache: " + e);
        }
    }

    public static boolean isScreenOn() {
        final PowerManager pm = (PowerManager)  HuamiXdrip.getAppContext().getSystemService(Context.POWER_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return pm.isInteractive();
        } else {
            return pm.isScreenOn();
        }
    }

    public static boolean isOngoingCall() {
        try {
            AudioManager manager = (AudioManager)  HuamiXdrip.getAppContext().getSystemService(Context.AUDIO_SERVICE);
            return (manager.getMode() == AudioManager.MODE_IN_CALL);
            // possibly should have MODE_IN_COMMUNICATION as well
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean runOnUiThread(Runnable theRunnable) {
        final Handler mainHandler = new Handler( HuamiXdrip.getAppContext().getMainLooper());
        return mainHandler.post(theRunnable);
    }

    public static boolean runOnUiThreadDelayed(Runnable theRunnable, long delay) {
        final Handler mainHandler = new Handler( HuamiXdrip.getAppContext().getMainLooper());
        return mainHandler.postDelayed(theRunnable, delay);
    }

    public static void removeUiThreadRunnable(Runnable theRunnable) {
        final Handler mainHandler = new Handler( HuamiXdrip.getAppContext().getMainLooper());
        mainHandler.removeCallbacks(theRunnable);
    }

    public static void hardReset() {
        try {
            android.os.Process.killProcess(android.os.Process.myPid());
        } catch (Exception e) {
            // not much to do
        }
    }

    public static void show_ok_dialog(final Activity activity, final String title, final String message, final Runnable runnable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(activity, R.style.Theme_Huamixdrip));
                    builder.setTitle(title);
                    builder.setMessage(message);
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                dialog.dismiss();
                            } catch (Exception e) {
                                //
                            }
                            if (runnable != null) {
                                runOnUiThreadDelayed(runnable, 10);
                            }
                        }
                    });

                    builder.create().show();
                } catch (Exception e) {
                    Log.wtf(TAG, "show_dialog exception: " + e);
                    static_toast_long(message);
                }
            }
        });
    }

    public static void static_toast(final Context context, final String msg, final int length) {
        try {
            if (!runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Toast.makeText(context, msg, length).show();
                        Log.i(TAG, "Displaying toast using fallback");
                    } catch (Exception e) {
                        Log.e(TAG, "Exception processing runnable toast ui thread: " + e);
                        PreferenceActivity.toastStatic(msg);
                    }
                }
            })) {
                Log.e(TAG, "Couldn't display toast via ui thread: " + msg);
                PreferenceActivity.toastStatic(msg);
            }
        } catch (Exception e) {
            Log.e(TAG, "Couldn't display toast due to exception: " + msg + " e: " + e.toString());
            PreferenceActivity.toastStatic(msg);
        }
    }

    public static void static_toast_long(final String msg) {
        static_toast( HuamiXdrip.getAppContext(), msg, Toast.LENGTH_LONG);
    }

    public static void static_toast_short(final String msg) {
        static_toast( HuamiXdrip.getAppContext(), msg, Toast.LENGTH_SHORT);
    }

    public static void static_toast_long(Context context, final String msg) {
        static_toast(context, msg, Toast.LENGTH_LONG);
    }


    public static String getResourceURI(int id) {
        return ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + HuamiXdrip.getAppContext().getPackageName() + "/" + id;
    }

    public static boolean validateMacAddress(final String mac) {
        return mac != null && mac.length() == 17 && mac.matches("([\\da-fA-F]{1,2}(?:\\:|$)){6}");
    }

    public static String urlEncode(String source) {
        try {
            return URLEncoder.encode(source, "UTF-8");
        } catch (Exception e) {
            return "encoding-exception";
        }
    }

    public static void bitmapToFile(Bitmap bitmap, String path, String fileName) {

        if (bitmap == null) return;
        Log.d(TAG, "bitmapToFile: " + bitmap.getWidth() + "x" + bitmap.getHeight());
        File dir = new File(path);
        if (!dir.exists())
            dir.mkdirs();
        final File file = new File(path, fileName);
        try {
            FileOutputStream output = new FileOutputStream(file);
            final boolean result = bitmap.compress(Bitmap.CompressFormat.PNG, 80, output);
            output.flush();
            output.close();
            Log.d(TAG, "Bitmap compress result: " + result);
        } catch (Exception e) {
            Log.e(TAG, "Got exception writing bitmap to file: " + e);
        }
    }

    public static void shareImage(Context context, File file) {
        Uri uri = Uri.fromFile(file);
        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("image/*");
        intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, "");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        try {
            context.startActivity(Intent.createChooser(intent, "Share"));
        } catch (ActivityNotFoundException e) {
            static_toast_long("No suitable app to show an image!");
        }
    }

    public static void releaseOrientation(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public static void lockOrientation(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        int rotation = display.getRotation();
        int height;
        int width;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR2) {
            height = display.getHeight();
            width = display.getWidth();
        } else {
            Point size = new Point();
            display.getSize(size);
            height = size.y;
            width = size.x;
        }
        switch (rotation) {
            case Surface.ROTATION_90:
                if (width > height)
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                else
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                break;
            case Surface.ROTATION_180:
                if (height > width)
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                else
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                break;
            case Surface.ROTATION_270:
                if (width > height)
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                else
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            default:
                if (height > width)
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                else
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    public static boolean isAirplaneModeEnabled(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    public static boolean refreshDeviceCache(String thisTAG, BluetoothGatt gatt) {
        try {
            final Method method = gatt.getClass().getMethod("refresh", new Class[0]);
            if (method != null) {
                return (Boolean) method.invoke(gatt, new Object[0]);
            }
        } catch (Exception e) {
            Log.e(thisTAG, "An exception occured while refreshing gatt device cache: " + e);
        }
        return false;
    }

    public static boolean createSpecialBond(final String thisTAG, final BluetoothDevice device) {
        try {
            Log.e(thisTAG, "Attempting special bond");
            Class[] argTypes = new Class[]{int.class};
            final Method method = device.getClass().getMethod("createBond", argTypes);
            if (method != null) {
                return (Boolean) method.invoke(device, 2);
            } else {
                Log.e(thisTAG, "CANNOT FIND SPECIAL BOND METHOD!!");
            }
        } catch (Exception e) {
            Log.e(thisTAG, "An exception occured while creating special bond: " + e);
        }
        return false;
    }

    public static boolean isBluetoothEnabled(final Context context) {
        try {
            final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            final BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter(); // local scope only
            return mBluetoothAdapter.isEnabled();
        } catch (Exception e) {
            UserError.Log.d(TAG, "isBluetoothEnabled() exception: " + e);
        }
        return false;
    }

    public synchronized static void setBluetoothEnabled(Context context, boolean state) {
        try {

            if (isAirplaneModeEnabled(context)) {
                UserError.Log.e(TAG, "Not setting bluetooth to state: " + state + " due to airplane mode being enabled");
                return;
            }

            if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {

                final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
                if (bluetoothManager == null) {
                    UserError.Log.e(TAG, "Couldn't get bluetooth in setBluetoothEnabled");
                    return;
                }
                BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter(); // local scope only
                if (mBluetoothAdapter == null) {
                    UserError.Log.e(TAG, "Couldn't get bluetooth adapter in setBluetoothEnabled");
                    return;
                }
                try {
                    if (state) {
                        UserError.Log.i(TAG, "Setting bluetooth enabled");
                        mBluetoothAdapter.enable();
                    } else {
                        UserError.Log.i(TAG, "Setting bluetooth disabled");
                        mBluetoothAdapter.disable();

                    }
                } catch (Exception e) {
                    UserError.Log.e(TAG, "Exception when enabling/disabling bluetooth: " + e);
                }
            } else {
                UserError.Log.e(TAG, "Bluetooth low energy not supported");
            }
        } finally {
            //
        }
    }

    public static void niceRestartBluetooth(Context context) {
        if (!isOngoingCall()) {
            if (ratelimit("joh-restart-bluetooth", 600)) {
                restartBluetooth(context);
            }
        }
    }

    public synchronized static void restartBluetooth(final Context context) {
        restartBluetooth(context, 0);
    }

    public synchronized static void restartBluetooth(final Context context, final int startInMs) {
        new Thread() {
            @Override
            public void run() {
                final PowerManager.WakeLock wl = getWakeLock("restart-bluetooth", 60000);
                Log.d(TAG, "Restarting bluetooth");
                try {
                    if (startInMs > 0) {
                        try {
                            Thread.sleep(startInMs);
                        } catch (InterruptedException e) {
                            Log.d(TAG, "Got interrupted waiting to start resetBluetooth");
                        }
                    }
                    setBluetoothEnabled(context, false);
                    try {
                        Thread.sleep(6000);
                    } catch (InterruptedException e) {
                        Log.d(TAG, "Got interrupted in resetBluetooth");
                    }
                    setBluetoothEnabled(context, true);
                } finally {
                    releaseWakeLock(wl);
                }
            }
        }.start();
    }

    public static Map<String, String> bundleToMap(Bundle bundle) {
        final HashMap<String, String> map = new HashMap<>();
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            if (value != null) {
                map.put(key, value.toString());
            }
        }
        return map;
    }

    public static void threadSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            //
        }
    }

    public static ByteBuffer bArrayAsBuffer(byte[] bytes) {
        final ByteBuffer bb = ByteBuffer.allocate(bytes.length);
        bb.put(bytes);
        return bb;
    }

    public static long checksum(byte[] bytes) {
        if (bytes == null) return 0;
        final CRC32 crc = new CRC32();
        crc.update(bytes);
        return crc.getValue();
    }

    public static long checksum(byte[] bytes, int offset, int length) {
        CRC32 crc = new CRC32();
        crc.update(bytes, offset, length);
        return (int) (crc.getValue());
    }
    public static int parseIntWithDefault(String number, int radix, int defaultVal) {
        try {
            return Integer.parseInt(number, radix);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing integer number = " + number + " radix = " + radix);
            return defaultVal;
        }
    }

    public static double roundDouble(final double value, int places) {
        if (places < 0) throw new IllegalArgumentException("Invalid decimal places");
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static float roundFloat(final float value, int places) {
        if (places < 0) throw new IllegalArgumentException("Invalid decimal places");
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    public static boolean isServiceRunningInForeground(Class<?> serviceClass) {
        final ActivityManager manager = (ActivityManager) HuamiXdrip.getAppContext().getSystemService(Context.ACTIVITY_SERVICE);
        try {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return service.foreground;
                }
            }
            return false;
        } catch (NullPointerException e) {
            return false;
        }
    }

    public static boolean emptyString(final String str) {
        return str == null || str.length() == 0;
    }

    public static class DecimalKeyListener extends DigitsKeyListener {
        private final char[] acceptedCharacters =
                new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                        new DecimalFormat().getDecimalFormatSymbols().getDecimalSeparator()};

        @Override
        protected char[] getAcceptedChars() {
            return acceptedCharacters;
        }

        public int getInputType() {
            return InputType.TYPE_CLASS_NUMBER;
        }

    }
    public static void cancelAlarm(Context context, PendingIntent serviceIntent) {
        // do we want a try catch block here?
        final AlarmManager alarm = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        if (serviceIntent != null) {
            Log.d(TAG, "Cancelling alarm " + serviceIntent.getCreatorPackage());
            alarm.cancel(serviceIntent);
        } else {
            Log.d(TAG, "Cancelling alarm: serviceIntent is null");
        }
    }

    public static long wakeUpIntent(Context context, long delayMs, PendingIntent pendingIntent) {
        final long wakeTime = Helper.tsl() + delayMs;
        if (pendingIntent != null) {
            Log.d(TAG, "Scheduling wakeup intent: " + dateTimeText(wakeTime));
            final AlarmManager alarm = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            try {
                alarm.cancel(pendingIntent);
            } catch (Exception e) {
                Log.e(TAG, "Exception cancelling alarm in wakeUpIntent: " + e);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (buggy_samsung && Pref.getBoolean("allow_samsung_workaround", true)) {
                    alarm.setAlarmClock(new AlarmManager.AlarmClockInfo(wakeTime, null), pendingIntent);
                } else {
                    alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, wakeTime, pendingIntent);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarm.setExact(AlarmManager.RTC_WAKEUP, wakeTime, pendingIntent);
            } else
                alarm.set(AlarmManager.RTC_WAKEUP, wakeTime, pendingIntent);
        } else {
            Log.e(TAG, "wakeUpIntent - pending intent was null!");
        }
        return wakeTime;
    }

}
