package me.hexian000.callrecorder;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.nio.file.Paths;

import static me.hexian000.callrecorder.CallRecorder.LOG_TAG;

public class CallReceiver extends BroadcastReceiver {
	private static String lastState = "";
	private static MediaRecorder recorder = null;
	private final Handler handler = new Handler();
	private Context context;
	private String filePath;
	private int trial = 0;

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

	private static String makeRecordingFileName(final String contact, final String number) {
		String name = Utils.nowISO8601() + "_" + number;
		if (contact != null) {
			name += "_" + contact;
		}
		name += ".amr";
		return name;
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
		this.context = context;

		final String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
		final boolean start = TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state);
		Log.d(LOG_TAG, "PHONE_STATE " + state + " " + number + " " + start);

		if (start) {
			try {
				filePath = prepareRecording(context, number);
			} catch (Exception ex) {
				Log.e(LOG_TAG, "fatal exception: ", ex);
				return;
			}
			handler.postDelayed(this::tryStartRecording, 1000);
		} else {
			try {
				stopRecording();
			} catch (Exception ex) {
				Log.e(LOG_TAG, "unexpected exception: ", ex);
			}
		}
	}

	private String prepareRecording(final Context context, final String number) throws IOException {
		final String dirPath = Utils.makePath(Paths.get(
				Environment.getExternalStorageDirectory().getAbsolutePath(),
				"CallRecorder"
		).toString());

		return Paths.get(
				dirPath,
				makeRecordingFileName(
						getContactDisplayNameByNumber(
								context.getContentResolver(),
								number
						),
						number
				)
		).toString();
	}

	private void tryStartRecording() {
		try {
			startRecording();
			trial = 0;
		} catch (Exception ex) {
			Log.e(LOG_TAG, "unexpected exception: ", ex);
			if (recorder != null) {
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
			trial++;
			if (trial < 3) {
				handler.postDelayed(this::tryStartRecording, 1000);
			} else {
				Log.wtf(LOG_TAG, "all attempts failed.");
				trial = 0;
			}
		}
	}

	private void startRecording() {
		if (recorder != null) {
			Log.w(LOG_TAG, "startRecording when already recording");
			return;
		}

		recorder = new MediaRecorder();
		recorder.setAudioChannels(1);
		recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
		recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_WB);
		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
		recorder.setOutputFile(filePath);

		recorder.setOnErrorListener((mr, what, extra) ->
				Log.e(LOG_TAG, "MediaRecorder.onError " + what + " " + extra));
		recorder.setOnInfoListener((mr, what, extra) ->
				Log.i(LOG_TAG, "MediaRecorder.onInfo " + what + " " + extra));

		try {
			recorder.prepare();
		} catch (IOException e) {
			Log.e(LOG_TAG, "MediaRecorder.prepare()", e);
		}

		Log.i(LOG_TAG, "start: " + filePath);
		recorder.start();
		recorder.getMaxAmplitude();
		Toast.makeText(context, R.string.record_begin, Toast.LENGTH_SHORT).show();
	}

	private void stopRecording() {
		if (recorder == null) {
			Log.w(LOG_TAG, "stopRecording when not recording");
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
		Toast.makeText(context, context.getResources().getString(R.string.record_end), Toast.LENGTH_SHORT).show();
	}
}
