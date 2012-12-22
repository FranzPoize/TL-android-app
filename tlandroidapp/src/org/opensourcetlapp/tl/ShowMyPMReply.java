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


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ShowMyPMReply extends Activity implements Runnable {
	private static final String NEW_PM_RESOURCE = "mytlnet/index.php?view=new";
	
	private Context context;
	private String replyURL = null;
	private ProgressDialog progressDialog;
	private TLHandler handler;
	private String replymsg;
	private EditText editText;
	private EditText toEditText;
	private EditText subjectEditText;
	private String to;
	private String subject;
	
	// Thread states
	private static final int FETCH_DATA = 0;
	private static final int SEND_MESSAGE = 1;
	private int state = FETCH_DATA;
	
	public static final String PARSE_NODE = "<table width='647' cellpadding='2' cellspacing='0' border='0' style='border: 1px solid #00005D;'>";
	
    @Override 
    public void onConfigurationChanged(Configuration newConfig) { 
        super.onConfigurationChanged(newConfig); 
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_my_pm_reply);
		context = this;
		
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			replyURL = extras.getString("replyURL");
			to = extras.getString("to");
		}
		
		editText = (EditText)findViewById(R.id.messageEditText);
		toEditText = (EditText)findViewById(R.id.toEditText);
		if (to != null)
			toEditText.setText(to);
		subjectEditText = (EditText)findViewById(R.id.subjectEditText);
		
		findViewById(R.id.replyButton).setOnClickListener(new OnClickListener() {		
			@Override
			public void onClick(View v) {
				sendMessage();
			}
		});
		
		setTitle(TLLib.makeActivityTitle("Send PM"));
		refresh();
	}
	
	private void refresh(){
		if (replyURL != null){
			progressDialog = ProgressDialog.show(this, null, "Loading...", true, true);
			handler = new ShowMyPMReplyFetchDataHandler(progressDialog, this);
			new Thread(this).start();
		}
	}
	
	private void fetchData(){
		HtmlCleaner cleaner = TLLib.buildDefaultHtmlCleaner();
		String absoluteURL = TLLib.getAbsoluteURL(replyURL);
		try {
			Object [] ret = TLLib.tagNodeWithEditText(cleaner, new URL(absoluteURL), handler, context, PARSE_NODE, true);
			TagNode node = (TagNode)ret[0];
			replymsg = (String)ret[1];
			
			Object [] inputTags = node.evaluateXPath("//input");
			to = ((TagNode)inputTags[1]).getAttributeByName("value");
			subject = ((TagNode)inputTags[2]).getAttributeByName("value");

			handler.sendEmptyMessage(TLHandler.PROGRESS_RENDERING);
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
	
	private void sendMessage(){
		progressDialog = ProgressDialog.show(context, null, "Sending message...", true, true);
		handler = new SendMessageHandler(progressDialog, context);
		findViewById(R.id.replyButton).setEnabled(false);
		state = SEND_MESSAGE;
		new Thread((Runnable)context).start();
	}
	
	private class ShowMyPMReplyFetchDataHandler extends TLHandler {
		public ShowMyPMReplyFetchDataHandler(ProgressDialog progressDialog, Context context) {
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
	
	private class SendMessageHandler extends TLHandler {
		public SendMessageHandler(ProgressDialog progressDialog, Context context) {
			super(progressDialog, context);
		}
		
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 0 && this.progressStatus == TLHandler.PROGRESS_OKAY){
				progressDialog.dismiss();
				finish();
			}
			else {
				super.handleMessage(msg);
			}
		}
	}
	
	public void run(){
		if (state == FETCH_DATA){
			fetchData();
			handler.sendEmptyMessage(0);
		}
		else if (state == SEND_MESSAGE){
			try {
				TLLib.sendPM(toEditText.getText().toString(), subjectEditText.getText().toString(), editText.getText().toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			handler.sendEmptyMessage(0);
		}
	}
	
	private void render(){
		toEditText.setText(to);
		subjectEditText.setText(subject);
		editText.setText(replymsg);
	}
}
