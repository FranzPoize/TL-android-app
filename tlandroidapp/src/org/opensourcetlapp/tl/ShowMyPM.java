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
import java.util.Iterator;
import java.util.List;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.opensourcetlapp.tl.R;
import org.opensourcetlapp.tl.Adapters.ShowMyPMAdapter;
import org.opensourcetlapp.tl.Structs.PMInfo;


import android.app.Activity;
import android.app.ListActivity;
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
import android.view.View;
import android.widget.ListView;


public class ShowMyPM extends ListActivity implements Runnable {
	private static final String RESOURCE_PATH = "mytlnet";
	private static final String PARSE_NODE = "<table width='100%' cellspacing='0'>";	// TODO: Figure out why the TagParser won't parse the page when there are new PMs
	private static final String TAG = "Private Messages";
	private ProgressDialog progressDialog;
	private TLHandler handler;
	private Context context;
	private List<PMInfo> pmInfoList = new ArrayList<PMInfo>(50);
	
    @Override 
    public void onConfigurationChanged(Configuration newConfig) { 
        super.onConfigurationChanged(newConfig); 
        //---code to redraw your activity here---
        //...
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_my_pm);
		context = this;
		setTitle(TLLib.makeActivityTitle("Private Messages"));
		refresh();
	}
	
	private void refresh(){
		pmInfoList.clear();
		progressDialog = ProgressDialog.show(this, null, "Loading...", true, true);
		handler = new ShowMyPMHandler(progressDialog, this);
		new Thread(this).start();
	}
	
	private void render(){
		HtmlCleaner cleaner = TLLib.buildDefaultHtmlCleaner();
		String url = TLLib.getAbsoluteURL(RESOURCE_PATH);
		try {
			TagNode node = TLLib.TagNodeFromURLEx2(cleaner, new URL(url), handler, context, PARSE_NODE, true);
			handler.sendEmptyMessage(TLHandler.PROGRESS_RENDERING);

			Object [] nodeList = node.evaluateXPath("//tbody/tr[position()>2]");
			Log.d(TAG, "NodeList Length: "+ nodeList.length);
			
			TagNode n;
			for (Object o : nodeList){
				n = (TagNode)o;
				try {
					TagNode from;
					TagNode subject;
					TagNode subjectURL;
					TagNode date;
					try {
						from =  (TagNode)(n.evaluateXPath("./td[1]/b"))[0];
						subjectURL =  (TagNode)(n.evaluateXPath("./td[2]/a"))[0];
						subject =  (TagNode)(n.evaluateXPath("./td[2]/a/b"))[0];
						date = (TagNode)(n.evaluateXPath("./td[3]/b"))[0];
					} catch (ArrayIndexOutOfBoundsException e){
						from =  (TagNode)(n.evaluateXPath("./td[1]"))[0];
						subject =  (TagNode)(n.evaluateXPath("./td[2]/a"))[0];
						subjectURL = subject;
						date = (TagNode)(n.evaluateXPath("./td[3]"))[0];
					}
					PMInfo pmInfo = new PMInfo();
					pmInfo.from = HtmlTools.unescapeHtml(from.getChildren().iterator().next().toString());
					pmInfo.subject = HtmlTools.unescapeHtml(subject.getChildren().iterator().next().toString());
					pmInfo.url = HtmlTools.unescapeHtml(subjectURL.getAttributeByName("href"));
					pmInfo.date = HtmlTools.unescapeHtml(date.getChildren().iterator().next().toString());

					pmInfoList.add(pmInfo);
				}
				catch (ArrayIndexOutOfBoundsException e){
					Log.d(TAG, "Index out of bounds");
					break;
				}
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			Log.d(TAG, "Malformed URL");
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.d(TAG, "IOEXCEPTION");
			e.printStackTrace();
		} catch (XPatherException e) {
			// TODO Auto-generated catch block
			Log.d(TAG, "XPAtherException");
			e.printStackTrace();
		}
	}
	
	private class ShowMyPMHandler extends TLHandler {
		public ShowMyPMHandler(ProgressDialog progressDialog, Context context) {
			super(progressDialog, context);
		}
		
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 0 && this.progressStatus == TLHandler.PROGRESS_OKAY){
				progressDialog.setMessage("Rendering...");
				getListView().setAdapter(new ShowMyPMAdapter(pmInfoList, context));
				progressDialog.dismiss();
			}
			else {
				super.handleMessage(msg);
			}
		}
	}
	
	protected void onListItemClick(ListView l, View v, int position, long id) {
		PMInfo pmInfo = pmInfoList.get((int) id);
		String url = pmInfo.url;
		String from = pmInfo.from;
		String subject = pmInfo.subject;
		String date = pmInfo.date;
		
		Intent intent = new Intent().setClass(this, ShowMyPMPost.class);
		intent.putExtra("url", url);
		intent.putExtra("from", from);
		intent.putExtra("subject", subject);
		intent.putExtra("date", date);

		startActivity(intent);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.show_my_pm_menu, menu);
	    return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh:
			refresh();
			break;
		case R.id.sendMessage:
			Intent intent = new Intent().setClass(this, ShowMyPMReply.class);
			startActivity(intent);
			break;
		}
		return true;

	}
	
	public void run(){
		render();
		handler.sendEmptyMessage(0);
	}
}
