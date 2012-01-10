/*
 * Copyright 2010, 2011 Ali Piccioni
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

package com.thoughtmetric.tl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ShowForumList extends ListActivity implements Runnable  {
	public static final String TAG = "main";
	private static TagNode node;
	private static final String FORUM_NAME_XPATH = "//h2/a[@class='forummsginfo']";
	private DBHelper db;
	private Cursor forumsCursor;
	private static final int PROGRESS_DIALOG_KEY = 1;
	
	private Context context;
	
	ProgressDialog progressDialog;
	
	private static final String [] HARD_CODED_FORUM_NAMES = {};
	private static final String [] HARD_CODED_FORUM_URLS = {};
	
	private TLHandler handler;
	private static final String ACTIVITY_SUBTITLE = "Forums";
	
    @Override 
    public void onConfigurationChanged(Configuration newConfig) { 
        super.onConfigurationChanged(newConfig); 
        //---code to redraw your activity here---
        //...
    }
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_forum_list);
		setTitle(TLLib.makeActivityTitle(ACTIVITY_SUBTITLE));
		db = new DBHelper(this);
		context = this;
		doThreadStuff();
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		forumsCursor.close();
		db.close();
	}
	
	private void renderForumList() throws IOException{
		forumsCursor = db.getForums();
		forumsCursor.moveToFirst();
		if (forumsCursor.isAfterLast()){
			fetchForumInfo();
			forumsCursor.requery();
		}
	}
	
	private void fetchForumInfo() throws IOException{
		HtmlCleaner cleaner = TLLib.buildDefaultHtmlCleaner();
		String forumURL = TLLib.getAbsoluteURL(Config.FORUM_LIST);
		try {
			node = TLLib.TagNodeFromURLShowForumList(cleaner, new URL(forumURL), handler, context);
			
			handler.sendEmptyMessage(TLHandler.PROGRESS_SEARCHING);
			Object[] forumNameNodes = node.evaluateXPath(FORUM_NAME_XPATH);
			for (Object forumNameObject : forumNameNodes){
				TagNode forumNameNode = (TagNode)forumNameObject;
				String fname = HtmlTools.unescapeHtml(forumNameNode.getChildren().iterator().next().toString().trim());
				String furl = HtmlTools.unescapeHtml(forumNameNode.getAttributeByName("href"));
				db.insertForum(fname, furl);
			}
			
			for (int i = 0; i < HARD_CODED_FORUM_NAMES.length; i++){
				db.insertForum(HARD_CODED_FORUM_NAMES[i], HARD_CODED_FORUM_URLS[i]);
			}
			
			handler.sendEmptyMessage(TLHandler.PROGRESS_RENDERING);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XPatherException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	protected void onListItemClick(ListView l, View v, int position, long id) {
		String forumURLString = forumsCursor.getString(forumsCursor.getColumnIndex("url"));
		String forumName = forumsCursor.getString(forumsCursor.getColumnIndex("name"));
		Intent intent = new Intent().setClass(this, ShowForum.class);
		intent.putExtra("forumURL", forumURLString);
		intent.putExtra("forumName", forumName);
		startActivity(intent);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.show_forums_menu, menu);
	    return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh:
			db.clear();
			doThreadStuff();
			//renderForumList();
			break;
		}
		return true;
	}

	@Override
	public void run() {
		try {
			renderForumList();
		} catch (IOException e){
			handler.progressStatus = TLHandler.PROGRESS_NETWORK_DOWN;
			Log.d(TAG, "Cannot establish connection");	
		}
		handler.sendEmptyMessage(0);
	}
	
	private void doThreadStuff(){ // Find a better name
		progressDialog = ProgressDialog.show(this, null, "Loading forum list...", true, true);
		handler = new MainTLHandler(progressDialog, this);
		Thread thread = new Thread(this);
		thread.start();
	}
	
	private class MainTLHandler extends TLHandler {
		public MainTLHandler(ProgressDialog progressDialog, Context context) {
			super(progressDialog, context);
		}

		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 0 && this.progressStatus == TLHandler.PROGRESS_OKAY){
				progressDialog.dismiss();
				setListAdapter(new SimpleCursorAdapter(context, android.R.layout.simple_list_item_1, forumsCursor, new String [] {"name"}, new int [] {android.R.id.text1}));	
			}

			else {
				super.handleMessage(msg);
			}
		}
	};
}
