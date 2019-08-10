package me.hexian000.callrecorder;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;

import static me.hexian000.callrecorder.CallRecorder.LOG_TAG;

public class AudioRecordService extends Service {
	public static final String EXTRA_NUMBER = "number";
	private static final String ACTION_STOP = "stop";
	private final Handler handler = new Handler();
	private NotificationManager notificationManager;
	private Notification.Builder builder;
	private CallRecorder app;
	private int startId;
	private long startTimeMillis;
	private MediaRecorder recorder;
	private String number;

	public boolean isRecording() {
		return recorder != null;
	}

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not supported");
	}

	private void notifyStart() {
		if (builder == null) {
			builder = new Notification.Builder(getApplicationContext(),
					CallRecorder.CHANNEL_RECORDING);
		}

		final Intent stopRecIntent = new Intent(app, AudioRecordService.class);
		stopRecIntent.setAction(ACTION_STOP);
		final PendingIntent stopRec = PendingIntent.getForegroundService(app, 0,
				stopRecIntent, PendingIntent.FLAG_ONE_SHOT);
		builder.setContentText(getResources().getText(R.string.notification_ongoing))
		       .setSubText(Utils.formatDuration(0))
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
		handler.postDelayed(this::notifyUpdate, 1000);
	}

	private void notifyUpdate() {
		if (!isRecording()) {
			return;
		}

		builder.setSubText(Utils.formatDuration(System.currentTimeMillis() - startTimeMillis));
		notificationManager.notify(startId, builder.build());

		handler.postDelayed(this::notifyUpdate, 1000);
	}

	private void notifyStop() {
		final String text = getResources().getString(R.string.record_end);
		final Notification.Builder builder = new Notification.Builder(app,
				CallRecorder.CHANNEL_RECORDING);
		builder.setContentText(text)
		       .setStyle(new Notification.BigTextStyle().bigText(text))
		       .setSmallIcon(R.drawable.ic_mic_black_24dp)
		       .setWhen(System.currentTimeMillis())
		       .setVisibility(Notification.VISIBILITY_SECRET)
		       .setChannelId(CallRecorder.CHANNEL_RECORDING)
		       .setTimeoutAfter(10000);

		notificationManager.notify(startId, builder.build());
	}

	private void startRecording(@NonNull final String filePath) throws IOException {
		recorder.setOutputFile(filePath);

		recorder.setOnErrorListener((mr, what, extra) ->
				Log.e(LOG_TAG, "MediaRecorder.onError " + what + " " + extra));
		recorder.setOnInfoListener((mr, what, extra) ->
				Log.i(LOG_TAG, "MediaRecorder.onInfo " + what + " " + extra));

		recorder.prepare();
		Log.i(LOG_TAG, "start: " + filePath);
		recorder.start();
		startTimeMillis = System.currentTimeMillis();
		recorder.getMaxAmplitude();
	}

	private void stopRecording() {
		recorder.stop();
		final int maxAmplitude = recorder.getMaxAmplitude();
		if (maxAmplitude > 0) {
			Log.i(LOG_TAG, "stop, maxAmplitude=" + maxAmplitude);
		} else {
			Log.w(LOG_TAG, "stop, maxAmplitude=" + maxAmplitude);
		}
		recorder.reset();
		recorder.release();
		recorder = null;
	}

	private void abortRecording() {
		if (recorder != null) {
			Log.w(LOG_TAG, "abort");
			try {
				recorder.reset();
			} catch (Exception ignored) {
			}
			try {
				recorder.release();
			} catch (Exception ignored) {
			}
			recorder = null;
		}
	}

	@Override
	public void onCreate() {
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		app = (CallRecorder) getApplicationContext();
		app.audioRecordService = this;
	}

	private void onIntent(@NonNull final Intent intent) {
		if (ACTION_STOP.equals(intent.getAction())) {
			if (isRecording()) {
				final String number = intent.getStringExtra(EXTRA_NUMBER);
				if ((number == null && this.number == null) ||
						(number != null && number.equals(this.number))) {
					stopRecording();
					notifyStop();
				}
			}
			return;
		}
		if (isRecording()) {
			Log.w(LOG_TAG, "trying to start when busy");
			return;
		}
		number = intent.getStringExtra(EXTRA_NUMBER);

		try {
			if (number != null) { /* Call Recording */
				recorder = CallRecorder.newCallRecorder();
				startRecording(Utils.makeCallFilePath(this, number));
			} else {
				recorder = CallRecorder.newMicRecorder();
				startRecording(Utils.makeMicFilePath());
			}
		} catch (Exception ex) {
			abortRecording();
			Log.e(LOG_TAG, "AudioRecordService.startRecording", ex);
			return;
		}
		notifyStart();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		this.startId = startId;
		onIntent(intent);
		handler.post(this::release);
		return START_NOT_STICKY;
	}

	private void release() {
		if (!isRecording()) {
			stopSelf();
		}
	}

	@Override
	public void onDestroy() {
		if (isRecording()) {
			stopRecording();
			notifyStop();
		}
		app.audioRecordService = null;
	}
}
