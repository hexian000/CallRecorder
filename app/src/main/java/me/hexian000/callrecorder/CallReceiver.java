package me.hexian000.callrecorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static me.hexian000.callrecorder.CallRecorder.LOG_TAG;

public class CallReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		final CallRecorder app = (CallRecorder) context.getApplicationContext();
		final String number;
		final boolean start;
		if ("android.intent.action.PHONE_STATE".equals(intent.getAction())) {
			final String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
			number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
			Log.d(LOG_TAG, "PHONE_STATE " + state + " " + number);
			start = TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state);
		} else {
			return;
		}

		if (start) {
			if (app.mediaRecorder != null) {
				return;
			}

			final MediaRecorder recorder = new MediaRecorder();
			recorder.setAudioChannels(1);
			recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
			recorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
			recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

			File dir = new File(
					Environment.getExternalStorageDirectory() + "/CallRecorder");
			if (!dir.exists()) {
				if (!dir.mkdirs()) {
					Log.e(LOG_TAG, "cannot mkdirs: " + dir.getAbsolutePath());
					return;
				}
			}

			final String file = dir.getAbsolutePath() + "/" +
					new SimpleDateFormat("yyyy-MM-dd'T'HH_mm_ss.SSSZZ", Locale.getDefault())
							.format(new Date()) + "_" + number + ".aac";
			recorder.setOutputFile(file);

			try {
				recorder.prepare();
			} catch (IOException e) {
				Log.e(LOG_TAG, "MediaRecorder.prepare()", e);
			}
			Log.i(LOG_TAG, "start: " + file);
			recorder.start();
			app.mediaRecorder = recorder;
		} else {
			if (app.mediaRecorder != null) {
				app.mediaRecorder.stop();
				app.mediaRecorder.release();
				app.mediaRecorder = null;
				Log.i(LOG_TAG, "stop");
			}
		}
	}
}
