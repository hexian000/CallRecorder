package me.hexian000.callrecorder;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.MediaRecorder;
import android.provider.Settings;

public class CallRecorder extends Application {
	public static final String LOG_TAG = CallRecorder.class.getSimpleName();
	public final static String CHANNEL_RECORDING = "recording";

	static void createNotificationChannels(final NotificationManager manager, final Resources res) {
		final NotificationChannel channel = new NotificationChannel(CHANNEL_RECORDING,
				res.getString(R.string.notification_channel), NotificationManager.IMPORTANCE_LOW);
		channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
		channel.enableLights(false);
		channel.enableVibration(false);
		channel.setSound(null, null);

		manager.createNotificationChannel(channel);
	}

	static MediaRecorder newCallRecorder() {
		final MediaRecorder recorder = new MediaRecorder();
		recorder.setAudioChannels(1);
		recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
		recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_WB);
		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
		return recorder;
	}

	static MediaRecorder newMicRecorder() {
		final MediaRecorder recorder = new MediaRecorder();
		recorder.setAudioChannels(1);
		recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		recorder.setAudioSamplingRate(44100);
		recorder.setAudioEncodingBitRate(192000);
		return recorder;
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

	public boolean isAccessServiceEnabled() {
		String prefString = Settings.Secure.getString(getContentResolver(),
				Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

		return prefString != null && prefString.contains(
				getPackageName() + "/" + AudioRecordService.class.getName());
	}
}
