/*
 * Copyright 2010, 2011 Ali Piccioni & Francois Poizat
 *
 * This program is distributed under the terms of the GNU General Public License
 *
 *  This file is part of Team Liquid Android App.
 *
 *  Team Liquid Android App is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Team Liquid Android App is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Team Liquid Android App.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.opensourcetlapp.tl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.SimpleXmlSerializer;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.htmlcleaner.XmlSerializer;
import org.opensourcetlapp.tl.R;
import org.opensourcetlapp.tl.Adapters.ForumAdapter;
import org.opensourcetlapp.tl.Structs.PostInfo;


import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class ShowForum extends ListActivity implements Runnable{
	private String forumURL;
	private String forumName;
	private static final String TAG = "ShowForum";
	private static final String FORUM_ENTRIES_XPATH = "//table[@class='solid']/tbody/tr[position()>1]";
	private int pageNumber = 1;
	private int forumCode;
	
	
	private ProgressDialog progressDialog;
	private HtmlCleaner cleaner;
	private static TagNode node;
	private Context context;
	
	private List <PostInfo> postInfoList; 
		
	private TLHandler handler;
	
	private void showDate(){
		Date date = new Date();
		Log.d(TAG, date.toGMTString());
	}
	
    @Override 
    public void onConfigurationChanged(Configuration newConfig) { 
        super.onConfigurationChanged(newConfig); 
        //---code to redraw your activity here---
        //...
    }
    
    private void parseForumCode(){
    	String showPartString = "show_part=";
    	int start = forumURL.indexOf(showPartString);
    	if (start == -1){
    		forumCode = -1;
    		return;
    	}
    	
    	int offset = start + showPartString.length();
    	int end = forumURL.indexOf('&', start);
    	if (end == -1){
    		end = forumURL.length();
    	}
    	
    	try {
    		forumCode = Integer.parseInt(forumURL.substring(offset, end));
    	} catch (NumberFormatException e){
    		e.printStackTrace();
    		forumCode = -1;
    	}
    }
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_forum);
		
		Bundle extras = getIntent().getExtras();
		forumURL = extras.getString("forumURL");
		forumName = extras.getString("forumName");
		parseForumCode();
		Log.d(TAG, String.format("Forum Code: %d", forumCode));
		setTitle(TLLib.makeActivityTitle(forumName));
		context = this;
		refresh();
	}
	
	private void refresh(){
		progressDialog = ProgressDialog.show(this, null, "Loading...", true, true);
		handler = new ShowForumHandler(progressDialog, this);
		new Thread(this).start();
	}
	
	private String makeUrlWithPageNumber(){
		String temp;
		if (!forumURL.contains("?")){
			temp = forumURL + "?" + String.format("currentpage=%d",pageNumber) + "&viewdays=0";
		}
		else {
			temp = forumURL + "&" +  String.format("currentpage=%d",pageNumber) + "&viewdays=0";
		}
		return temp;
	}
	
	private void render(){
		cleaner = TLLib.buildDefaultHtmlCleaner();
		
		String absoluteForumURL = TLLib.getAbsoluteURL(makeUrlWithPageNumber());
		try {
			URL url = new URL(absoluteForumURL);
			node = TLLib.TagNodeFromURLShowForum(cleaner, url, handler, context);
			postInfoList = new ArrayList<PostInfo>(35);
			
			handler.sendEmptyMessage(TLHandler.PROGRESS_RENDERING);
			Object[] forumTable = node.evaluateXPath(FORUM_ENTRIES_XPATH);
			for (Object o : forumTable){
				TagNode entry = (TagNode) o;
				postInfoList.add(PostInfo.buildPostInfoFromForumEntry(entry));
			}	
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			handler.progressStatus = TLHandler.PROGRESS_NETWORK_DOWN;
		} catch (XPatherException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected void onListItemClick(ListView l, View v, int position, long id) {
		PostInfo postInfo = postInfoList.get((int) id);
		String postURL = postInfo.topicURL;
		String postTopic = postInfo.topicString;
		boolean postLocked = postInfo.locked;
		
		Intent intent = new Intent().setClass(this, ShowThread.class);
		intent.putExtra("postURL", postURL);
		intent.putExtra("postTopic", postTopic);
		intent.putExtra("postLocked", postLocked);
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.show_forum_menu, menu);
	    return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh:
			refresh();
			break;
		case R.id.prev:
			if (pageNumber > 1){
				pageNumber--;
				refresh();
			}
			break;
		case R.id.next:
			pageNumber++;
			refresh();
			break;
		case R.id.post:
			Intent intent = new Intent();
			intent.setClass(this, CreateThread.class);
			intent.putExtra("forumCode", forumCode);
			intent.putExtra("forumName", forumName);
			startActivity(intent);
			break;
		}
		return true;
	}
	
	@Override
	public void run() {
		render();
		handler.sendEmptyMessage(0);
	}
	
	private class ShowForumHandler extends TLHandler {
		public ShowForumHandler(ProgressDialog progressDialog, Context context) {
			super(progressDialog, context);
		}
		
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 0 && this.progressStatus == TLHandler.PROGRESS_OKAY){
				progressDialog.setMessage("Rendering...");
				getListView().setAdapter(new ForumAdapter(postInfoList, context));
				progressDialog.dismiss();
			}
			else {
				super.handleMessage(msg);
			}
		}
		
	}
}
