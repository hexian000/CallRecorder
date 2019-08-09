package me.hexian000.callrecorder;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static me.hexian000.callrecorder.CallRecorder.LOG_TAG;

public class CallReceiver extends BroadcastReceiver {
	private static String lastState = "";
	private static MediaRecorder mediaRecorder = null;

	private static String sanitizeFileName(final String desiredName) {
		return desiredName.replaceAll("[\\\\/:*?\"<>|]", "_");
	}

	private static String nowISO8601() {
		return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ", Locale.getDefault())
				.format(new Date());
	}

	private static String getContactDisplayNameByNumber(final ContentResolver resolver,
	                                                    final String number) {
		final Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
				Uri.encode(number));
		try (final Cursor contactLookup = resolver.query(uri, new String[]{
				BaseColumns._ID, ContactsContract.PhoneLookup.DISPLAY_NAME
		}, null, null, null)) {
			if (contactLookup != null && contactLookup.getCount() > 0) {
				contactLookup.moveToNext();
				return contactLookup.getString(contactLookup.getColumnIndex(
						ContactsContract.Data.DISPLAY_NAME));
			}
		}

		return null;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (!TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
			return;
		}

		final String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
		if (lastState.equals(state)) {
			return;
		}
		lastState = state;

		final String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
		final boolean start = TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state);

		Log.d(LOG_TAG, "PHONE_STATE " + state + " " + number + " " + start);

		try {
			if (start) {
				startRecording(context, number);
			} else {
				stopRecording(context);
			}
		} catch (Exception ex) {
			Log.e(LOG_TAG, "unexpected exception: ", ex);
		}
	}

	private void startRecording(final Context context, final String number) {
		if (mediaRecorder != null) {
			Log.w(LOG_TAG, "startRecording when already recording");
			return;
		}

		mediaRecorder = new MediaRecorder();
		mediaRecorder.setAudioChannels(1);
		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
		mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_WB);
		mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);

		final String dirPath;
		{
			final File dir = new File(
					Environment.getExternalStorageDirectory() + "/CallRecorder");
			if (!dir.exists()) {
				if (!dir.mkdirs()) {
					Log.e(LOG_TAG, "cannot mkdirs: " + dir.getAbsolutePath());
					return;
				}
			}
			dirPath = dir.getAbsolutePath();
		}

		final String file;
		{
			String name = nowISO8601() + "_" + number;

			final String contact = getContactDisplayNameByNumber(context.getContentResolver(),
					number);
			if (contact != null) {
				name += "_" + contact;
			}

			name += ".amr";
			file = dirPath + "/" + sanitizeFileName(name);
		}
		mediaRecorder.setOutputFile(file);

		mediaRecorder.setOnErrorListener((mr, what, extra) ->
				Log.e(LOG_TAG, "MediaRecorder.onError " + what + " " + extra));
		mediaRecorder.setOnInfoListener((mr, what, extra) ->
				Log.i(LOG_TAG, "MediaRecorder.onInfo " + what + " " + extra));

		try {
			mediaRecorder.prepare();
		} catch (IOException e) {
			Log.e(LOG_TAG, "MediaRecorder.prepare()", e);
		}

		Log.i(LOG_TAG, "start: " + file);
		for (int retry = 0; ; retry++) {
			try {
				mediaRecorder.start();
				mediaRecorder.getMaxAmplitude();
				break;
			} catch (Exception ex) {
				Log.e(LOG_TAG, "MediaRecorder.start()", ex);
			}
			if (retry < 3) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ex) {
					Log.w(LOG_TAG, "Thread.sleep()", ex);
					return;
				}
			} else {
				Log.e(LOG_TAG, "MediaRecorder.start() failed over 3 times");
				return;
			}
		}
		Toast.makeText(context, R.string.record_begin, Toast.LENGTH_SHORT).show();
	}

	private void stopRecording(final Context context) {
		if (mediaRecorder == null) {
			Log.w(LOG_TAG, "stopRecording when not recording");
			return;
		}
		mediaRecorder.stop();
		final int maxAmplitude = mediaRecorder.getMaxAmplitude();
		if (maxAmplitude > 0) {
			Log.i(LOG_TAG, "stop, maxAmplitude=" + maxAmplitude);
		} else {
			Log.w(LOG_TAG, "stop, maxAmplitude=" + maxAmplitude);
		}
		mediaRecorder.release();
		mediaRecorder = null;
		Toast.makeText(context, context.getResources().getString(R.string.record_end), Toast.LENGTH_SHORT).show();
	}
}
