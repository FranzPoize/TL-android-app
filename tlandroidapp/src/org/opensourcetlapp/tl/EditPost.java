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
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class EditPost extends Activity  {
	private int postId;
	private int topicId;
	private int currentPage;
	private String token;
	private EditText messageEditText;
	private FetchEditUBB fetchUBBThread;
	private TLHandler fetchUBBHandler;
	private ProgressDialog progressDialog;
	private Button postButton;
	private Context context;
	private static final String baseURL = "forum/edit.php?action=edit";
    @Override 
    public void onConfigurationChanged(Configuration newConfig) { 
        super.onConfigurationChanged(newConfig); 
        //---code to redraw your activity here---
        //...
    }
    
    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.post_message);
		setTitle(TLLib.makeActivityTitle("Edit Post"));

		Bundle extras = getIntent().getExtras();
		context = this;
		
		postId = extras.getInt("postId");
		topicId = extras.getInt("topicId");
		currentPage = extras.getInt("currentPage");
		
		messageEditText = (EditText)findViewById(R.id.messageEditText);	
		
		String url = TLLib.getAbsoluteURL(buildEditURL());
		
		progressDialog = ProgressDialog.show(this, null, "Loading forum list...", true, true);
		fetchUBBHandler = new FetchUBBHandler(progressDialog, this); 
		fetchUBBThread = new FetchEditUBB(this, fetchUBBHandler, url);
		fetchUBBThread.start();
		
		postButton = (Button)findViewById(R.id.postButton);
		postButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				progressDialog = ProgressDialog.show(context, null, "Editing message...", true, true);
				Handler handler = new PostHandler(progressDialog, context);
				postButton.setEnabled(false);
				new PostThread(handler, context).start();
			}
		});
    }
    
    private String buildEditURL(){
    	return baseURL + String.format("&post_id=%d&topic_id=%d&currentpage=%d", postId, topicId, currentPage);
    }
    
    
    private class FetchUBBHandler extends TLHandler{

		public FetchUBBHandler(ProgressDialog progressDialog, Context context) {
			super(progressDialog, context);
		}
		
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 0 && this.progressStatus == TLHandler.PROGRESS_OKAY){
				progressDialog.dismiss();
				messageEditText.setText(fetchUBBThread.ubbCode);
			}
			else {
				super.handleMessage(msg);
			}
		}
    }
    
    private class FetchEditUBB extends Thread{
    	private TLHandler handler;
    	private String url;
    	private Context context;
    	public String ubbCode;
    	private TagNode node;
    	
    	public FetchEditUBB(Context context, TLHandler handler, String url){
    		this.handler = handler;
    		this.url = url;
    		this.context = context;
    	}
    	@Override
    	public void run() {
    		HtmlCleaner cleaner = TLLib.buildDefaultHtmlCleaner();
    		try {
    			Object [] ret =  TLLib.parseEditText(cleaner, new URL(url), handler, context);
    			ubbCode = (String)ret[0];
    			node = (TagNode)ret[1];
    			TagNode tokenNode = (TagNode)node.evaluateXPath("//input[@name='token']")[0];
    			token = tokenNode.getAttributeByName("value");
    			
    			handler.sendEmptyMessage(0);
    		} catch (MalformedURLException e) {
    			e.printStackTrace();
    		} catch (IOException e) {
    			handler.progressStatus = TLHandler.PROGRESS_NETWORK_DOWN;
			} catch (XPatherException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		
    	}
    }
    
    private class PostThread extends Thread {
    	private Handler handler;
    	private Context context;
    	
    	public PostThread(Handler handler, Context context){
    		this.handler = handler;
    		this.context = context;
    	}
    	
    	public void run(){
			try {
				TLLib.editMessage(messageEditText.getText().toString(), topicId, postId, currentPage, token, context);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				handler.sendEmptyMessage(TLHandler.PROGRESS_NETWORK_DOWN);
			}
			handler.sendEmptyMessage(0);
    	}
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


}
