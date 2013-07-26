package com.ocd.dev.hearingadapter;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.View;

import com.google.glass.location.GlassLocationManager;
import com.google.glass.timeline.TimelineHelper;
import com.google.glass.timeline.TimelineNotificationHelper;
import com.google.glass.timeline.TimelineProvider;
import com.google.glass.util.SettingsSecure;
import com.google.googlex.glass.common.proto.MenuItem;
import com.google.googlex.glass.common.proto.MenuItem.Action;
import com.google.googlex.glass.common.proto.MenuValue;
import com.google.googlex.glass.common.proto.NotificationConfig;
import com.google.googlex.glass.common.proto.NotificationConfig.Level;
import com.google.googlex.glass.common.proto.TimelineItem;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void onButtonClick(View v) {
		GlassLocationManager.init(this);
		
		ContentResolver cr = getContentResolver();
		TimelineHelper tlHelper = new TimelineHelper();

		TimelineItem.Builder ntib = tlHelper.createTimelineItemBuilder(this, new SettingsSecure(cr));

        ntib.setText("Hearing Adapter: Disabled");
        ntib.setNotification(NotificationConfig.newBuilder().setLevel(Level.DEFAULT).build());

        Uri uri = Uri.parse("android.resource://com.ocd.dev.hearingadapter/drawable/ic_volume_full");
        ntib.addMenuItem(MenuItem.newBuilder().setAction(Action.BROADCAST).setBroadcastAction("com.ocd.dev.hearingadapter.action.TOGGLE").addValue(MenuValue.newBuilder().setDisplayName("Enable")
        			.setIconUrl(uri.toString()).build()));
        ntib.addMenuItem(MenuItem.newBuilder().setAction(Action.TOGGLE_PINNED).build());
        ntib.addMenuItem(MenuItem.newBuilder().setAction(Action.DELETE).build());
        
        TimelineItem ti = ntib.build();
        ContentValues vals = TimelineHelper.toContentValues(ti);
    	Uri cardUri = cr.insert(TimelineProvider.TIMELINE_URI, vals);
        TimelineNotificationHelper.notify(this, ti, 1);
        
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString(EchoSoundService.PREFS_CARD_URI, cardUri.toString()).commit();
	}
	
}
