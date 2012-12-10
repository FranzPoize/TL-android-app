package org.opensourcetlapp.tl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.opensourcetlapp.tl.R;
import org.opensourcetlapp.tl.Adapters.MyPostsAdapter;
import org.opensourcetlapp.tl.Structs.SubInfo;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;


public class MySubs extends ListActivity implements Runnable {
	private static final String RESOURCE_PATH = "mytlnet/mythreads.php";
	private static final String FORUM_ENTRIES_XPATH = "//table[@class='solid']/tbody/tr[position()>1]";
	private static final String TAG = "MySubs";
	private ProgressDialog progressDialog;
	private TLHandler handler;
	private Context context;
	private List<SubInfo> subInfoList = new ArrayList<SubInfo>(50);
	
    @Override 
    public void onConfigurationChanged(Configuration newConfig) { 
        super.onConfigurationChanged(newConfig); 
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.my_posts);
		context = this;
		setTitle(TLLib.makeActivityTitle("My Subscribed Threads"));
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

			Object[] forumTable = node.evaluateXPath(FORUM_ENTRIES_XPATH);
			for (Object o : forumTable){
				TagNode entry = (TagNode) o;
				subInfoList.add(SubInfo.buildPostInfoFromForumEntry(entry));
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
	
	private class MyPostsHandler extends TLHandler {
		public MyPostsHandler(ProgressDialog progressDialog, Context context) {
			super(progressDialog, context);
		}
		
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 0 && this.progressStatus == TLHandler.PROGRESS_OKAY){
				progressDialog.setMessage("Rendering...");
				//getListView().setAdapter(new SubThreadAdapter(subInfoList, context));
				progressDialog.dismiss();
			}
			else {
				super.handleMessage(msg);
			}
		}
	}
	
	protected void onListItemClick(ListView l, View v, int position, long id) {
		SubInfo subInfo = subInfoList.get((int) id);
		String postURL = subInfo.topicURL;
		String postTopic = subInfo.topicString;
		boolean postLocked = subInfo.locked;
		
		Intent intent = new Intent().setClass(this, ShowThread.class);
		intent.putExtra("postURL", postURL);
		intent.putExtra("postTopic", postTopic);
		intent.putExtra("postLocked", postLocked);
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
