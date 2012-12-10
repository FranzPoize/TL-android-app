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

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.opensourcetlapp.tl.R;
import org.opensourcetlapp.tl.Adapters.ShowMyPMAdapter;
import org.opensourcetlapp.tl.Structs.PMInfo;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ShowMyPMPost extends Activity implements Runnable{
	private Context context;
	private ProgressDialog progressDialog;
	private TLHandler handler;
	private static final String PARSE_NODE = "<table width='100%' cellpadding='2' cellspacing='0' border='0' style='border: 1px solid #00005D;'>";
	
	private String BASE_JS ="<script type='text/javascript'>function toggleShowQuote(b){var a=document.getElementById('showQuoteRest_'+b);a.style.display=a.style.display=='none'?'block':'none';}" +
			"function toggleShowSpoiler2(b,c){var a=document.getElementById('spoiler_'+c);a.style.display=a.style.display=='none'?'block':'none';}"+
			"function toggleShowSpoiler(c,a,b){toggleShowSpoiler2(c,b);}</script>";
	
	private LinearLayout forumPostView;
	
	private TextView fromTextView;
	private TextView subjectTextView;
	private TextView dateTextView;
	
	private String url;
	private String msg;
	private String replyURL;
	private RenderUBB renderUBB;
	TextView msgTextView;
	
	private String content;
	
    @Override 
    public void onConfigurationChanged(Configuration newConfig) { 
        super.onConfigurationChanged(newConfig); 
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_my_pm_post);
		context = this;
		
		Bundle extras = getIntent().getExtras();
		url = extras.getString("url");
		forumPostView = (LinearLayout) findViewById(R.id.forumPost);
		
		fromTextView = (TextView)findViewById(R.id.from);
		subjectTextView = (TextView)findViewById(R.id.subject);
		dateTextView = (TextView)findViewById(R.id.date);

		fromTextView.setText(extras.getString("from"));
		subjectTextView.setText(extras.getString("subject"));
		dateTextView.setText(extras.getString("date"));

		
		setTitle(TLLib.makeActivityTitle(extras.getString("subject")));
		refresh();
	}
	
	private void render(){
		WebView contentView = new WebView(context);
		forumPostView.addView(contentView);
		contentView.loadDataWithBaseURL("http://www.teamliquid.net/", BASE_JS + content, "text/html", "UTF-8", null);
	}
	
	private void fetchData(){
		HtmlCleaner cleaner = TLLib.buildDefaultHtmlCleaner();
		String absoluteURL = TLLib.getAbsoluteURL(url);
		try {
			TagNode node = TLLib.TagNodeFromURLEx2(cleaner, new URL(absoluteURL), handler, context, PARSE_NODE, true);
			handler.sendEmptyMessage(TLHandler.PROGRESS_RENDERING);

			TagNode msgNode = (TagNode)(node.evaluateXPath("//table[@width='100%']/tbody/tr/td"))[3];
			TagNode replyURLNode = (TagNode)(msgNode.getParent().getParent().getParent().getParent().evaluateXPath("./a")[0]);
			replyURL = replyURLNode.getAttributeByName("href");
			
			content = cleaner.getInnerHtml(msgNode);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XPatherException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void refresh(){
		progressDialog = ProgressDialog.show(this, null, "Loading...", true, true);
		handler = new ShowMyPMPostHandler(progressDialog, this);
		new Thread(this).start();
	}
	
	private class ShowMyPMPostHandler extends TLHandler {
		public ShowMyPMPostHandler(ProgressDialog progressDialog, Context context) {
			super(progressDialog, context);
		}
		
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 0 && this.progressStatus == TLHandler.PROGRESS_OKAY){
				progressDialog.setMessage("Rendering...");
				render();
				progressDialog.dismiss();
			}
			else {
				super.handleMessage(msg);
			}
		}
	}
	
	public void run(){
		fetchData();
		handler.sendEmptyMessage(0);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.show_my_pm_post_menu, menu);
	    return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.reply:
			Intent intent = new Intent();
			intent.setClass(this, ShowMyPMReply.class);
			intent.putExtra("replyURL", replyURL);
			startActivity(intent);
			break;
		}
		return true;
	}
}
