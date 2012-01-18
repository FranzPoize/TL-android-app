/*
 * Copyright 2010, 2011 Ali Piccioni, Francois Poizat, Bill Xu
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
import org.opensourcetlapp.tl.Adapters.ForumAdapter;
import org.opensourcetlapp.tl.Adapters.MyPostsAdapter;
import org.opensourcetlapp.tl.Structs.PostInfo;


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


public class MyPosts extends ListActivity implements Runnable {
	private static final String RESOURCE_PATH = "mytlnet/myposts.php";
	private static final String TAG = "MyPosts";
	private ProgressDialog progressDialog;
	private TLHandler handler;
	private Context context;
	private List<PostInfo> postInfoList = new ArrayList<PostInfo>(50);
	
    @Override 
    public void onConfigurationChanged(Configuration newConfig) { 
        super.onConfigurationChanged(newConfig); 
        //---code to redraw your activity here---
        //...
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.my_posts);
		context = this;
		setTitle(TLLib.makeActivityTitle("My Posts"));
		refresh();
	}
	
	private void refresh(){
		progressDialog = ProgressDialog.show(this, null, "Loading...", true, true);
		handler = new MyPostsHandler(progressDialog, this);
		new Thread(this).start();
	}
	
	private void render(){
		HtmlCleaner cleaner = TLLib.buildDefaultHtmlCleaner();
		String url = TLLib.getAbsoluteURL(RESOURCE_PATH);
		try {
			TagNode node = TLLib.TagNodeFromURLMyPosts(cleaner, new URL(url), handler, context);
			handler.sendEmptyMessage(TLHandler.PROGRESS_RENDERING);

			Object [] nodeList = node.evaluateXPath("//table[@width=\"748\"]/tbody/tr[position()>1]");
			
			TagNode n;
			for (Object o : nodeList){
				n = (TagNode)o;
				TagNode topicStarter =  (TagNode)(n.evaluateXPath("./td[3]"))[0];
				TagNode replies =  (TagNode)(n.evaluateXPath("./td[4]"))[0];
				TagNode lastMessage = (TagNode)(n.evaluateXPath("./td[6]"))[0];
				Object [] resourceList = (n.evaluateXPath("./td[2]/a"));
				TagNode topic = (TagNode)resourceList[0];
				TagNode lastPost = (TagNode)resourceList[resourceList.length-1];
				TagNode topicURL = (TagNode)resourceList[0];
				
				String topicURLString = topicURL.getAttributeByName("href");
				int postNumber = Integer.parseInt(lastPost.getChildren().iterator().next().toString());
				int pageNumber = (postNumber/20) + 1;
				
				topicURLString = String.format("%s&currentpage=%d#%d", topicURLString, pageNumber,postNumber);

				PostInfo postInfo = new PostInfo();
				if (topicStarter.getChildren().iterator().hasNext())
					postInfo.topicStarterString = HtmlTools.unescapeHtml(topicStarter.getChildren().iterator().next().toString());
				if (replies.getChildren().iterator().hasNext())
					postInfo.repliesString = HtmlTools.unescapeHtml(replies.getChildren().iterator().next().toString());
				postInfo.lastMessageString = HtmlTools.unescapeHtml(lastMessage.getChildren().get(0).toString());
				postInfo.lastMessageString += " " + HtmlTools.unescapeHtml(lastMessage.getChildren().get(2).toString());
				postInfo.topicURL = topicURLString;
				if (topic.getChildren().iterator().hasNext())
					postInfo.topicString = HtmlTools.unescapeHtml(topic.getChildren().iterator().next().toString());

				postInfoList.add(postInfo);
			}
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
	
	private class MyPostsHandler extends TLHandler {
		public MyPostsHandler(ProgressDialog progressDialog, Context context) {
			super(progressDialog, context);
		}
		
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 0 && this.progressStatus == TLHandler.PROGRESS_OKAY){
				progressDialog.setMessage("Rendering...");
				getListView().setAdapter(new MyPostsAdapter(postInfoList, context));
				progressDialog.dismiss();
			}
			else {
				super.handleMessage(msg);
			}
		}
	}
	
	protected void onListItemClick(ListView l, View v, int position, long id) {
		PostInfo postInfo = postInfoList.get((int) id);
		String postURL = postInfo.topicURL;
		String postTopic = postInfo.topicString;
		boolean postLocked = postInfo.locked;
		
		Intent intent = new Intent().setClass(this, ShowPost.class);
		intent.putExtra("postURL", postURL);
		intent.putExtra("postTopic", postTopic);
		intent.putExtra("postLocked", false);
		startActivity(intent);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.my_posts_menu, menu);
	    return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh:
			refresh();
			break;
		}
		return true;

	}
	
	public void run(){
		render();
		handler.sendEmptyMessage(0);
	}
}
