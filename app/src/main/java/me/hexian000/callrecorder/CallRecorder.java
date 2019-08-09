package me.hexian000.callrecorder;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;

public class CallRecorder extends Application {
	public static final String LOG_TAG = CallRecorder.class.getSimpleName();
	public final static String CHANNEL_RECORDING = "recording";
	public MicRecordService micRecordService = null;
	public MainActivity mainActivity = null;

	static void createNotificationChannels(final NotificationManager manager, final Resources res) {
		final NotificationChannel channel = new NotificationChannel(CHANNEL_RECORDING,
				res.getString(R.string.notification_channel), NotificationManager.IMPORTANCE_LOW);
		channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
		channel.enableLights(false);
		channel.enableVibration(false);
		channel.setSound(null, null);

		manager.createNotificationChannel(channel);
	}

	public boolean isEnabled() {
		final Context context = getApplicationContext();
		final ComponentName receiverName = new ComponentName(context, CallReceiver.class);
		return PackageManager.COMPONENT_ENABLED_STATE_ENABLED == context
				.getPackageManager()
				.getComponentEnabledSetting(receiverName);
	}

	public void setEnabled(boolean enabled) {
		final Context context = getApplicationContext();
		final ComponentName receiverName = new ComponentName(context, CallReceiver.class);
		final PackageManager pm = context.getPackageManager();
		pm.setComponentEnabledSetting(receiverName,
				enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
						PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				PackageManager.DONT_KILL_APP);
	}
}
