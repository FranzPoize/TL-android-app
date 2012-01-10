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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

public class CreateThread extends Activity implements Runnable{
	private Context context;
	private EditText editText;
	private EditText subjectEditText;
	private ProgressDialog progressDialog;
	private TLHandler handler;
	private int forumCode;
	private static final String TAG = "CreateThread";
	
    @Override 
    public void onConfigurationChanged(Configuration newConfig) { 
        super.onConfigurationChanged(newConfig); 
        //---code to redraw your activity here---
        //...
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.create_thread);
		context = this;
		
		Bundle extras = getIntent().getExtras();
		forumCode = extras.getInt("forumCode");
		editText = (EditText)findViewById(R.id.messageEditText);
		subjectEditText = (EditText)findViewById(R.id.subjectEditText);
		
		findViewById(R.id.replyButton).setOnClickListener(new OnClickListener() {		
			@Override
			public void onClick(View v) {
				sendMessage();
			}
		});
		
		setTitle(TLLib.makeActivityTitle("Create New Topic"));
		refresh();
	}
	
	private void sendMessage(){
		progressDialog = ProgressDialog.show(context, null, "Posting Message...", true, true);
		handler = new PostMessageHandler(progressDialog, context);
		findViewById(R.id.replyButton).setEnabled(false);
		new Thread((Runnable)context).start();
	}
	
	private void refresh(){
	}
	
	public void run(){
		Log.d(TAG, "Posting Message");
		Log.d(TAG, subjectEditText.getText().toString());
		Log.d(TAG, editText.getText().toString());
		try {
			TLLib.postNewThread(forumCode, subjectEditText.getText().toString(), editText.getText().toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		handler.sendEmptyMessage(0);
	}
	
	private void render(){
	}
	
	private class PostMessageHandler extends TLHandler {
		public PostMessageHandler(ProgressDialog progressDialog, Context context) {
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
