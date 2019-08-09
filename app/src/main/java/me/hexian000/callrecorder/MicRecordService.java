package me.hexian000.callrecorder;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Locale;

import static me.hexian000.callrecorder.CallRecorder.LOG_TAG;

public class MicRecordService extends Service {
	private static final String ACTION_STOP = "stop";

	private final Handler handler = new Handler();

	private NotificationManager notificationManager;
	private Notification.Builder builder;
	private CallRecorder app;
	private boolean started = false;
	private int startId;
	private long startTimeMillis;

	private MediaRecorder recorder = null;

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not supported");
	}

	private void initNotification() {
		if (builder == null) {
			builder = new Notification.Builder(getApplicationContext(),
					CallRecorder.CHANNEL_RECORDING);
		}

		final Intent stopRecIntent = new Intent(app, MicRecordService.class);
		stopRecIntent.setAction(ACTION_STOP);
		final PendingIntent stopRec = PendingIntent.getForegroundService(app, 0,
				stopRecIntent, PendingIntent.FLAG_ONE_SHOT);
		startTimeMillis = System.currentTimeMillis();
		builder.setContentText(getResources().getText(R.string.notification_ongoing))
				.setSubText(formatDuration(0))
				.setSmallIcon(R.drawable.ic_mic_black_24dp)
				.setWhen(startTimeMillis)
				.setOngoing(true)
				.setOnlyAlertOnce(true)
				.setVisibility(Notification.VISIBILITY_SECRET)
				.addAction(new Notification.Action.Builder(
						Icon.createWithResource(this, R.drawable.ic_stop_black_24dp),
						getString(R.string.notification_action_stop),
						stopRec).build());

		CallRecorder.createNotificationChannels(notificationManager, getResources());
		builder.setChannelId(CallRecorder.CHANNEL_RECORDING);

		startForeground(startId, builder.build());
		handler.postDelayed(this::updateNotification, 1000);
	}

	private String formatDuration(long millis) {
		final long seconds = millis / 1000;
		if (seconds < 3600) {
			return String.format(Locale.getDefault(), "%02d:%02d",
					seconds / 60, seconds % 60);
		}
		return String.format(Locale.getDefault(), "%d:%02d:%02d",
				seconds / 3600, (seconds % 3600) / 60, seconds % 60);
	}

	private void updateNotification() {
		if (!started) {
			return;
		}

		builder.setSubText(formatDuration(System.currentTimeMillis() - startTimeMillis));
		notificationManager.notify(startId, builder.build());

		handler.postDelayed(this::updateNotification, 1000);
	}

	private void startMicRecord() throws IOException {
		if (recorder != null) {
			Log.w(LOG_TAG, "startMicRecord when already recording");
			return;
		}

		recorder = new MediaRecorder();
		recorder.setAudioChannels(1);
		recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		recorder.setAudioSamplingRate(48000);
		recorder.setAudioEncodingBitRate(192000);

		final String dirPath = Utils.makePath(Paths.get(
				Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
						.getAbsolutePath(), "Recorder").toString());

		final String fileName = Utils.sanitizeFileName(Utils.nowISO8601() + ".m4a");
		final String fullPath = Paths.get(dirPath, fileName).toString();
		recorder.setOutputFile(fullPath);

		recorder.setOnErrorListener((mr, what, extra) ->
				Log.e(LOG_TAG, "MediaRecorder.onError " + what + " " + extra));
		recorder.setOnInfoListener((mr, what, extra) ->
				Log.i(LOG_TAG, "MediaRecorder.onInfo " + what + " " + extra));

		recorder.prepare();
		Log.i(LOG_TAG, "start: " + fullPath);
		recorder.start();
		recorder.getMaxAmplitude();
		Toast.makeText(this, R.string.record_begin, Toast.LENGTH_SHORT).show();
	}

	private void stopMicRecord() {
		if (recorder == null) {
			Log.w(LOG_TAG, "stopMicRecord when not recording");
			return;
		}

		recorder.stop();
		final int maxAmplitude = recorder.getMaxAmplitude();
		if (maxAmplitude > 0) {
			Log.i(LOG_TAG, "stop, maxAmplitude=" + maxAmplitude);
		} else {
			Log.w(LOG_TAG, "stop, maxAmplitude=" + maxAmplitude);
		}
		recorder.release();
		recorder = null;

		final String text = getResources().getString(R.string.record_end);
		final Notification.Builder builder = new Notification.Builder(app,
				CallRecorder.CHANNEL_RECORDING);
		builder.setContentText(text)
				.setStyle(new Notification.BigTextStyle().bigText(text))
				.setSmallIcon(R.drawable.ic_mic_black_24dp)
				.setWhen(System.currentTimeMillis())
				.setVisibility(Notification.VISIBILITY_SECRET)
				.setChannelId(CallRecorder.CHANNEL_RECORDING);

		notificationManager.notify(startId, builder.build());
	}

	@Override
	public void onCreate() {
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		app = (CallRecorder) getApplicationContext();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (ACTION_STOP.equals(intent.getAction())) {
			stopSelf();
			return START_NOT_STICKY;
		}
		if (started) {
			return START_NOT_STICKY;
		}

		this.startId = startId;
		try {
			startMicRecord();
		} catch (Exception ex) {
			if (recorder != null) {
				recorder.reset();
				recorder.release();
				recorder = null;
			}
			Log.e(LOG_TAG, "MicRecordService.startMicRecord", ex);
			stopSelf();
			return START_NOT_STICKY;
		}
		initNotification();
		started = true;
		app.micRecordService = this;
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		stopMicRecord();
		app.micRecordService = null;
		started = false;
		final MainActivity mainActivity = app.mainActivity;
		if (mainActivity != null) {
			mainActivity.update();
		}
	}
}
