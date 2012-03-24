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
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class PostMessage extends Activity implements Runnable {
	private String postURL;
	private static String TAG = "PostMessage";
	private String topicId;
	private Button postButton;
	private EditText messageEditText;
	private Context context;
	private ProgressDialog progressDialog;
	private TLHandler handler;
	private FetchQuoteUBB fetchUBBThread;
	private static final String quoteBaseURL = "forum/postmessage.php";
	
    @Override 
    public void onConfigurationChanged(Configuration newConfig) { 
        super.onConfigurationChanged(newConfig); 
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.post_message);
		setTitle(TLLib.makeActivityTitle("Post Reply"));
		
		postButton = (Button)findViewById(R.id.postButton);
		messageEditText = (EditText)findViewById(R.id.messageEditText);	
		context = this;
		
		postButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				progressDialog = ProgressDialog.show(context, null, "Posting message...", true, true);
				handler = new PostHandler(progressDialog, context);
				postButton.setEnabled(false);
				new Thread((Runnable)context).start();
			}
		});
		
		topicId = Integer.toString(getIntent().getExtras().getInt("topicId"));
		int postId = getIntent().getExtras().getInt("postId");
		if (postId != 0){
			String quoteURL = TLLib.getAbsoluteURL(String.format("%s?quote=%d&topic_id=%s",quoteBaseURL, postId, topicId));
			progressDialog = ProgressDialog.show(context, null, "Fetching quote UBB...", true, true);
			FetchUBBHandler ubbHandler= new FetchUBBHandler(progressDialog, context);
			fetchUBBThread = new FetchQuoteUBB(context, ubbHandler, quoteURL);
			fetchUBBThread.start();
		}
	}

	@Override
	public void run() {
		try {
			TLLib.postMessage(messageEditText.getText().toString(), "", topicId, context);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			handler.sendEmptyMessage(TLHandler.PROGRESS_NETWORK_DOWN);
		}
		handler.sendEmptyMessage(0);
	}
	
	private class PostHandler extends TLHandler {
		public PostHandler(ProgressDialog progressDialog, Context context) {
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
	
    private class FetchUBBHandler extends TLHandler{
		public FetchUBBHandler(ProgressDialog progressDialog, Context context) {
			super(progressDialog, context);
		}
		
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 0 && this.progressStatus == TLHandler.PROGRESS_OKAY){
				progressDialog.dismiss();
				messageEditText.append(fetchUBBThread.ubbCode);
			}
			else {
				super.handleMessage(msg);
			}
		}
    }
    
    private class FetchQuoteUBB extends Thread{
    	private TLHandler handler;
    	private String url;
    	private Context context;
    	public String ubbCode;
    	private TagNode node;
    	
    	public FetchQuoteUBB(Context context, TLHandler handler, String url){
    		this.handler = handler;
    		this.url = url;
    		this.context = context;
    	}
    	@Override
    	public void run() {
    		HtmlCleaner cleaner = TLLib.buildDefaultHtmlCleaner();
    		try {
    			ubbCode = TLLib.parseQuoteText(cleaner, new URL(url), handler, context);
    			handler.sendEmptyMessage(0);
    		} catch (MalformedURLException e) {
    			e.printStackTrace();
    		} catch (IOException e) {
    			handler.progressStatus = TLHandler.PROGRESS_NETWORK_DOWN;
    		}
    	}
    }
}
