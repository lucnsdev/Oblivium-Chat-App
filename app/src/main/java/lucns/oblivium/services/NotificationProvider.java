package lucns.oblivium.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;

import lucns.oblivium.R;

public class NotificationProvider {

    public interface Callback {
        void onButtonClick();
    }

    private final int NOTIFICATION_CODE = 1234;
    private final String silent = "Silent";
    private final String alert = "Alert";
    private final String BUTTON_CLICK = "button_click";
    private final Context context;
    private final Callback callback;
    private final NotificationManager notificationManager;
    private Notification.Builder builderProgress;
    private Notification notification;
    private boolean isShowing;
    private Class<?> activityClass;

    public NotificationProvider(Context context, Callback callback) {
        this.context = context;
        this.callback = callback;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createChannels();
    }

    private void createChannels() {
        NotificationChannel builderChannel = new NotificationChannel(silent, context.getString(R.string.notification_silent), NotificationManager.IMPORTANCE_DEFAULT);
        builderChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        builderChannel.enableLights(false);
        builderChannel.enableVibration(false);
        builderChannel.setSound(null, null);
        notificationManager.createNotificationChannel(builderChannel);

        AudioAttributes.Builder audioAttributes = new AudioAttributes.Builder();
        audioAttributes.setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_ALL);
        audioAttributes.setLegacyStreamType(AudioManager.STREAM_NOTIFICATION);
        audioAttributes.setUsage(AudioAttributes.USAGE_NOTIFICATION);

        builderChannel = new NotificationChannel(alert, context.getString(R.string.notification_alert), NotificationManager.IMPORTANCE_HIGH);
        builderChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        builderChannel.enableLights(true);
        builderChannel.enableVibration(true);
        builderChannel.setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + R.raw.unsure), audioAttributes.build());
        builderChannel.setLightColor(Color.argb(255, 255, 255, 255));
        builderChannel.setVibrationPattern(new long[]{250, 250, 250, 250});
        notificationManager.createNotificationChannel(builderChannel);
    }

    public void showProgress(int title, int text) {
        showProgress(context.getString(title), context.getString(text), null, null, 100);
    }

    public void showProgress(String title, String text, String sub, int maximum) {
        showProgress(title, text, sub, null, maximum);
    }

    public void showProgress(String title, String text, String sub, String action, int maximum) { // action button only works if app is running
        isShowing = true;
        PendingIntent pendingIntent = null;
        if (activityClass != null) {
            Intent resultIntent = new Intent(context, activityClass);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addNextIntentWithParentStack(resultIntent);
            pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }
        builderProgress = new Notification.Builder(context, silent);
        builderProgress.setAutoCancel(false);
        builderProgress.setOngoing(true);
        builderProgress.setShowWhen(true);
        builderProgress.setColorized(false);
        //builderProgress.setColor(context.getColor(R.color.accent));
        builderProgress.setTicker(title);
        builderProgress.setContentTitle(title);
        if (text != null) builderProgress.setContentText(text);
        if (sub != null) builderProgress.setSubText(sub);
        if (pendingIntent != null) builderProgress.setContentIntent(pendingIntent);
        if (action != null) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
                pendingIntent = PendingIntent.getBroadcast(context, NOTIFICATION_CODE, new Intent(BUTTON_CLICK), PendingIntent.FLAG_IMMUTABLE);
            } else {
                pendingIntent = PendingIntent.getBroadcast(context, NOTIFICATION_CODE, new Intent(BUTTON_CLICK), PendingIntent.FLAG_UPDATE_CURRENT);
            }
            builderProgress.addAction(new Notification.Action.Builder(Icon.createWithResource(context, R.drawable.icon_close_18), action, pendingIntent).build());
            IntentFilter filter = new IntentFilter();
            filter.addAction(BUTTON_CLICK);
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        }
        builderProgress.addAction(new Notification.Action.Builder(Icon.createWithResource(context, R.drawable.icon_close_18), context.getString(android.R.string.cancel), pendingIntent).build());
        builderProgress.setProgress(maximum, 0, false);
        builderProgress.setSmallIcon(Icon.createWithResource(context, R.drawable.icon_notification));

        builderProgress.setCategory(Notification.CATEGORY_SERVICE);
        notification = builderProgress.build();
        notificationManager.notify(NOTIFICATION_CODE, notification);
    }

    public int getNotificationCode() {
        return NOTIFICATION_CODE;
    }

    public Notification getNotification() {
        return notification;
    }

    public void setActivityClass(Class<?> activityToOpen) {
        activityClass = activityToOpen;
    }

    public void updateProgress(String title, String text, int progress, int maximum) {
        builderProgress.setContentTitle(title);
        if (text != null) builderProgress.setContentText(text);
        builderProgress.setProgress(maximum, progress, false);
        notification = builderProgress.build();
        notificationManager.notify(NOTIFICATION_CODE, notification);
    }

    public void updateProgress(int progress, int maximum) {
        builderProgress.setProgress(maximum, progress, false);
        notification = builderProgress.build();
        notificationManager.notify(NOTIFICATION_CODE, notification);
    }

    public void showAlert(int title, int text) {
        showAlert(context.getString(title), context.getString(text), null, null);
    }

    public void showAlert(String title, String text, String sub) {
        showAlert(title, text, sub, null);
    }

    public void showAlert(String title, String text, String sub, String action) { // action button only works if app is running
        isShowing = true;
        PendingIntent pendingIntent = null;
        if (activityClass != null) {
            Intent resultIntent = new Intent(context, activityClass);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addNextIntentWithParentStack(resultIntent);
            pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }

        Notification.Builder builder = new Notification.Builder(context, alert);
        builder.setAutoCancel(true);
        builder.setOngoing(false);
        builder.setShowWhen(true);
        builder.setColorized(false);
        //builder.setColor(context.getColor(R.color.accent));
        builder.setTicker(title);
        builder.setContentTitle(title);
        if (text != null) builder.setContentText(text);
        if (sub != null) builder.setSubText(sub);
        builder.setSmallIcon(Icon.createWithResource(context, R.drawable.icon_notification));
        builder.setCategory(Notification.CATEGORY_SERVICE);
        if (pendingIntent != null) builder.setContentIntent(pendingIntent);
        if (action != null) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
                pendingIntent = PendingIntent.getBroadcast(context, NOTIFICATION_CODE, new Intent(BUTTON_CLICK), PendingIntent.FLAG_IMMUTABLE);
            } else {
                pendingIntent = PendingIntent.getBroadcast(context, NOTIFICATION_CODE, new Intent(BUTTON_CLICK), PendingIntent.FLAG_UPDATE_CURRENT);
            }
            builder.addAction(new Notification.Action.Builder(Icon.createWithResource(context, R.drawable.icon_close_18), action, pendingIntent).build());
            IntentFilter filter = new IntentFilter();
            filter.addAction(BUTTON_CLICK);
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        }
        notification = builder.build();
        notificationManager.notify(NOTIFICATION_CODE, notification);
    }

    public void show(int title, int text, int sub) {
        show(context.getString(title), context.getString(text), context.getString(sub), null);
    }

    public void show(int title, int text, int sub, int action) {
        show(context.getString(title), context.getString(text), context.getString(sub), context.getString(action));
    }

    public void show(String title, String text, String sub, String action) { // action button only works if app is running
        isShowing = true;
        PendingIntent pendingIntent = null;
        if (activityClass != null) {
            Intent resultIntent = new Intent(context, activityClass);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addNextIntentWithParentStack(resultIntent);
            pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }
        Notification.Builder builder = new Notification.Builder(context, silent);
        builder.setAutoCancel(true);
        builder.setOngoing(false);
        builder.setShowWhen(true);
        builder.setColorized(false);
        //builder.setColor(context.getColor(R.color.accent));
        builder.setTicker(title);
        builder.setContentTitle(title);
        if (text != null) builder.setContentText(text);
        if (sub != null) builder.setSubText(sub);
        if (pendingIntent != null) builder.setContentIntent(pendingIntent);
        if (action != null) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
                pendingIntent = PendingIntent.getBroadcast(context, NOTIFICATION_CODE, new Intent(BUTTON_CLICK), PendingIntent.FLAG_IMMUTABLE);
            } else {
                pendingIntent = PendingIntent.getBroadcast(context, NOTIFICATION_CODE, new Intent(BUTTON_CLICK), PendingIntent.FLAG_UPDATE_CURRENT);
            }
            builder.addAction(new Notification.Action.Builder(Icon.createWithResource(context, R.drawable.icon_close_18), action, pendingIntent).build());
            IntentFilter filter = new IntentFilter();
            filter.addAction(BUTTON_CLICK);
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        }
        builder.setSmallIcon(Icon.createWithResource(context, R.drawable.icon_notification));
        builder.setCategory(Notification.CATEGORY_SERVICE);
        notification = builder.build();
        notificationManager.notify(NOTIFICATION_CODE, notification);
    }

    public void hide() {
        isShowing = false;
        notificationManager.cancelAll();
        try {
            context.unregisterReceiver(receiver);
        } catch (Exception ignore) {
        }
    }

    public boolean isShowing() {
        return isShowing;
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent == null || intent.getAction() == null ? "" : intent.getAction();
            hide();
            if (action.equals(BUTTON_CLICK)) callback.onButtonClick();
        }
    };
}
