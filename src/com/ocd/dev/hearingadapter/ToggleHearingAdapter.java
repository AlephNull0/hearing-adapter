package com.ocd.dev.hearingadapter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class ToggleHearingAdapter extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		boolean running = prefs.getBoolean(EchoSoundService.PREFS_IS_RUNNING, false);
		Intent echoService = new Intent(context, EchoSoundService.class);
		
		if(running) {
			context.stopService(echoService);
		} else {
			context.startService(echoService);
		}
	}

}
