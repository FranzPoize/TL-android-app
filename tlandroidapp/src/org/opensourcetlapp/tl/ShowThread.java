package org.opensourcetlapp.tl;

import android.app.ListActivity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class ShowThread extends ListActivity {

	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_post);

		forumPostView = (LinearLayout) findViewById(R.id.forumPost);
		scrollView = (ScrollView) findViewById(R.id.scroll);

		Bundle extras = getIntent().getExtras();
		postURL = extras.getString("postURL");
		postTopic = extras.getString("postTopic");	// TODO: Make the app pull the topic from the HTML
		postLocked = extras.getBoolean("postLocked"); // TODO: This only works properly when opening this activity is opened from the ShowForum. Add closed-thread detection. 
		
		parsePostURL();
		//setTitle(TLLib.makeActivityTitle(String.format("(%d) %s", currentPage, postTopic)));

		cleaner = TLLib.buildDefaultHtmlCleaner();
		context = this;

		renderUBB = new RenderUBB(this, null);
		refresh(true);
	}
}
