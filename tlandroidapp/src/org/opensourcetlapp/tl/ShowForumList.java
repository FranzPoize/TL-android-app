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
import org.opensourcetlapp.tl.R;
import org.opensourcetlapp.tl.Adapters.ForumsListCurosrAdapter;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.DhcpInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.CursorAdapter;
import android.text.Html;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ShowForumList extends ListFragment implements Runnable  {
	public static final String TAG = "main";
	private static TagNode node;
	private static final String FORUM_NAME_XPATH = "//a[@class='forummsginfo']";
	private DBHelper db;
	private Cursor forumsCursor;
	private static final int PROGRESS_DIALOG_KEY = 1;
	boolean seeHidden = false;
	private boolean rendered = false;
	
	private Context context;
	
	ProgressDialog progressDialog;
	
	private static final String [] HARD_CODED_FORUM_NAMES = {};
	private static final String [] HARD_CODED_FORUM_URLS = {};
	
	private TLHandler handler;
	private ShowForumList instance;
	private static final String ACTIVITY_SUBTITLE = "Forums";
	
    @Override 
    public void onConfigurationChanged(Configuration newConfig) { 
        super.onConfigurationChanged(newConfig); 
        //---code to redraw your activity here---
        //...
    }
	
	/** Called when the activity is first created. */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		View view = inflater.inflate(R.layout.show_forum_list,container,false);
		db = new DBHelper(getActivity());
		context = getActivity();
		instance = this;
		
		doThreadStuff();
		
		return view;
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		forumsCursor.close();
		db.close();
	}
	
	private void renderForumList(boolean hidden) throws IOException{
		forumsCursor = db.getForums(hidden);
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
			String parent = "";
			handler.sendEmptyMessage(TLHandler.PROGRESS_SEARCHING);
			Object[] forumNameNodes = node.evaluateXPath(FORUM_NAME_XPATH);
			for (Object forumNameObject : forumNameNodes){
				TagNode forumNameNode = (TagNode)forumNameObject;
				String fname = HtmlTools.unescapeHtml(forumNameNode.getChildren().iterator().next().toString().trim());
				String furl = HtmlTools.unescapeHtml(forumNameNode.getAttributeByName("href"));
				if (forumNameNode.getParent().getName().equals("h2")){
					db.insertForum(fname, furl, false,false);
				} else {
					db.insertForum(fname, furl, false, true);
				}
				Log.d("a", "b");
			}
			
			for (int i = 0; i < HARD_CODED_FORUM_NAMES.length; i++){
				db.insertForum(HARD_CODED_FORUM_NAMES[i], HARD_CODED_FORUM_URLS[i], false,false);
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
	
	public void onListItemClick(ListView l, View v, int position, long id) {
		String forumURLString = forumsCursor.getString(forumsCursor.getColumnIndex("url"));
		String forumName = forumsCursor.getString(forumsCursor.getColumnIndex("name"));
		Intent intent = new Intent().setClass(getActivity(), ShowForum.class);
		intent.putExtra("forumURL", forumURLString);
		intent.putExtra("forumName", forumName);
		startActivity(intent);
	}
	
	
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater menuInflater = getActivity().getMenuInflater();
		menuInflater.inflate(R.menu.hold_forum_menu, menu);
	}

	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.hide:
			AdapterView.AdapterContextMenuInfo info;
			try {
			    info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
			} catch (ClassCastException e) {
			    Log.e(TAG, "bad menuInfo", e);
			    return false;
			}
			db.hideForum(getListAdapter().getItemId(info.position));
			doThreadStuff();
			break;
		}
		return true;
	}
	
	
	@Override
	public void onCreateOptionsMenu(Menu menu,MenuInflater menuInflater) {
		menuInflater.inflate(R.menu.show_forums_menu, menu);
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh:
			db.clear();
			doThreadStuff();
			break;
		case R.id.unhide:
			db.unhide();
			doThreadStuff();
			break;
		}
		return true;
	}

	@Override
	public void run() {
		try {
			renderForumList(seeHidden);
		} catch (IOException e){
			handler.progressStatus = TLHandler.PROGRESS_NETWORK_DOWN;
			Log.d(TAG, "Cannot establish connection");	
		}
		handler.sendEmptyMessage(0);
	}
	
	private void doThreadStuff(){ // Find a better name
		if (!rendered) {
			progressDialog = ProgressDialog.show(getActivity(), null, "Loading forum list...", true, true);
			handler = new MainTLHandler(progressDialog, getActivity());
			Thread thread = new Thread(this);
			thread.start();
			rendered = true;
		}
	}
	
	private class MainTLHandler extends TLHandler {
		public MainTLHandler(ProgressDialog progressDialog, Context context) {
			super(progressDialog, context);
		}

		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 0 && this.progressStatus == TLHandler.PROGRESS_OKAY){
				progressDialog.dismiss();
				
				ForumsListCurosrAdapter adapter = new ForumsListCurosrAdapter(getActivity(), R.layout.show_forum_list_row,R.layout.show_sub_forum_list_row, forumsCursor, new String [] {"name"}, new int [] {android.R.id.text1},CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
				setListAdapter(adapter);	
			}

			else {
				super.handleMessage(msg);
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		registerForContextMenu(getListView());
	};
	
	
}
