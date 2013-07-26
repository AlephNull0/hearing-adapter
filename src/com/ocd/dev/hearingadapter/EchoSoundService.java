package com.ocd.dev.hearingadapter;

import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.IBinder;
import android.os.Process;
import android.preference.PreferenceManager;

import com.google.glass.timeline.TimelineHelper;
import com.google.glass.timeline.TimelineHelper.Update;
import com.google.googlex.glass.common.proto.MenuItem;
import com.google.googlex.glass.common.proto.MenuItem.Action;
import com.google.googlex.glass.common.proto.MenuValue;
import com.google.googlex.glass.common.proto.TimelineItem;

public class EchoSoundService extends Service {
	public static final String PREFS_IS_RUNNING = "is_running";
	public static final String PREFS_CARD_URI = "card_uri";
	public static final int NOTIFICATION_ID = 1425;
	private Thread mThread;
	private AudioRecord mAudioRecord;
	private AudioTrack mAudioTrack;
	private AtomicBoolean mRecording;
	
	@Override
	public void onCreate() {
		super.onCreate();
		mRecording = new AtomicBoolean();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		makeForegroundService();
		
		startEcho();
		
		return START_STICKY;
	}

	private void makeForegroundService() {
		Notification notification = new Notification.Builder(this)
				.setContentTitle("Hearing Adapter")
				.setContentText("Echoing sound...")
				.setSmallIcon(R.drawable.ic_launcher)
				.getNotification();
		
		startForeground(NOTIFICATION_ID, notification);
	}
	
	private void startEcho() {
		mRecording.set(false);
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
		editor.putBoolean(PREFS_IS_RUNNING, true);
		editor.commit();
		updateTimelineCard(true);
		mThread = new Thread(mEchoRunnable);
		mThread.start();
	}
	
	private void updateTimelineCard(boolean active) {
		final boolean a = active;
		final String t = (active)? "Hearing Adapter: Enabled" : "Hearing Adapter: Disabled";
		final String uri = PreferenceManager.getDefaultSharedPreferences(this).getString(PREFS_CARD_URI, null);
        final Uri iconUri = (active)?
        		Uri.parse("android.resource://com.ocd.dev.hearingadapter/drawable/ic_volume_muted") :
        		Uri.parse("android.resource://com.ocd.dev.hearingadapter/drawable/ic_volume_full");
        			
		
		if(uri != null) {
			final TimelineHelper helper = new TimelineHelper();
			TimelineHelper.atomicUpdateTimelineItemAsync(new Update() {
				
				@Override
				public TimelineItem onExecute() {
					TimelineItem item = helper.queryTimelineItem(getContentResolver(), Uri.parse(uri));
					TimelineItem.Builder builder = TimelineItem.newBuilder(item).setText(t);
					String menuItemText = null;
					if(a) {
						builder.removeMenuItem(2);
						menuItemText = "Disable";
					} else {
						builder.addMenuItem(2, MenuItem.newBuilder().setAction(Action.DELETE));
						menuItemText = "Enable";
					}

					builder.removeMenuItem(0)
							.addMenuItem(0, MenuItem.newBuilder()
							.setAction(Action.BROADCAST)
							.setBroadcastAction("com.ocd.dev.hearingadapter.action.TOGGLE")
							.addValue(MenuValue.newBuilder().setDisplayName(menuItemText)
							.setIconUrl(iconUri.toString()).build()));
					
					item = builder.build();

					return helper.updateTimelineItem(EchoSoundService.this, item, null, false, false);
				}
			});
		}
	}
	
	private Runnable mEchoRunnable = new Runnable() {
		
		@Override
		public void run() {
			Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
			
			int sampleRate = 11025;
			int channelInConfig = AudioFormat.CHANNEL_IN_MONO;
			int channelOutConfig = AudioFormat.CHANNEL_OUT_MONO;
			int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
			int bufferSize = AudioRecord.getMinBufferSize(11025, channelInConfig, AudioFormat.ENCODING_PCM_16BIT);
			
			mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelInConfig, audioFormat, bufferSize);
			mAudioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, sampleRate, channelOutConfig, audioFormat, bufferSize, AudioTrack.MODE_STREAM);
			mAudioTrack.setPlaybackRate(sampleRate);
			
			byte[] buffer = new byte[bufferSize];
			mAudioRecord.startRecording();
			mAudioTrack.play();
			
			mRecording.set(true);
			
			while(mRecording.get()) {
				mAudioRecord.read(buffer, 0, bufferSize);
				mAudioTrack.write(buffer, 0, buffer.length);
			}
			
			mAudioRecord.release();
			mAudioTrack.release();
			
		}
	};
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onDestroy() {
		mRecording.set(false);
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
		editor.putBoolean(PREFS_IS_RUNNING, false);
		editor.commit();
		
		updateTimelineCard(false);
		
		super.onDestroy();
	}

}
