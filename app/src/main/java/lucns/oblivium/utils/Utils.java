package lucns.oblivium.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.CombinedVibration;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.VibratorManager;
import android.util.Log;

import java.util.Locale;

import lucns.oblivium.R;

public class Utils {

    private static VibratorManager vibrator;

    static {
        init();
    }

    private static void init() {
        Context context = App.getContext();
        vibrator = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
    }

    public static void vibrate(int duration) {
        if (duration > 0) {
            vibrator.cancel();
            vibrator.vibrate(CombinedVibration.createParallel(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)));
        }
    }

    public static void vibrate() {
        vibrate(50);
    }

    public static void vibrate(long duration) {
        vibrator.cancel();
        vibrator.vibrate(CombinedVibration.createParallel(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)));
    }

    public static void singlePulse() {
        vibrator.cancel();
        long[] timings = new long[]{0, 25};
        int[] amplitudes = new int[]{0, 128};
        VibrationEffect effect = VibrationEffect.createWaveform(timings, amplitudes, -1);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(CombinedVibration.createParallel(effect), VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH));
            return;
        }
        VibrationAttributes.Builder builder = new VibrationAttributes.Builder();
        builder.setUsage(VibrationAttributes.USAGE_TOUCH);
        vibrator.vibrate(CombinedVibration.createParallel(effect), builder.build());
    }

    public static void pulsate() {
        vibrator.cancel();
        long[] timings = new long[]{0, 25, 75, 10};
        int[] amplitudes = new int[]{0, 128, 0, 255};
        VibrationEffect effect = VibrationEffect.createWaveform(timings, amplitudes, -1);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(CombinedVibration.createParallel(effect), VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH));
            return;
        }
        VibrationAttributes.Builder builder = new VibrationAttributes.Builder();
        builder.setUsage(VibrationAttributes.USAGE_TOUCH);
        vibrator.vibrate(CombinedVibration.createParallel(effect), builder.build());
    }

    public static boolean hasInternetConnection() {
        ConnectivityManager connectivity = (ConnectivityManager) App.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = connectivity.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities capabilities = connectivity.getNetworkCapabilities(network);
        if (capabilities == null) return false;
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    public static String retrieveTime(long time) {
        time = System.currentTimeMillis() - time;
        Context c = App.getContext();
        if (time < 60) {
            return String.format(Locale.getDefault(), c.getString(R.string.format_few_ago), c.getString(R.string.seconds));
        } else if (time < 3600) {
            return String.format(Locale.getDefault(), c.getString(R.string.format_few_ago), c.getString(R.string.minutes));
        } else if (time < 86400) {
            return String.format(Locale.getDefault(), c.getString(R.string.format_few_ago), c.getString(R.string.hours));
        } else if (time < 172800) {
            return c.getString(R.string.yesterday);
        } else {
            return c.getString(R.string.several_days);
        }
    }

    public static String getDateTime(long timestamp) {
        Context c = App.getContext();
        long difference = (System.currentTimeMillis() - timestamp) / 1000;
        if (difference < 60) return c.getString(R.string.few_seconds);
        else if (difference < 3600) return String.format(Locale.getDefault(), c.getString(R.string.format_minutes), difference / 60, difference / 60 == 1 ? "" : "s");
        else if (difference < 86400) return String.format(Locale.getDefault(), c.getString(R.string.format_hours), difference / 3600, difference / 3600 == 1 ? "" : "s");
        else return String.format(Locale.getDefault(), c.getString(R.string.format_days), difference / 86400, difference / 86400 == 1 ? "" : "s");
    }
}
