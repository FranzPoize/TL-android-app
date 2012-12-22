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
import org.opensourcetlapp.tl.Adapters.ForumAdapter;
import org.opensourcetlapp.tl.Structs.PostInfo;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;


public class MySubs extends ListActivity implements Runnable {
	private static final String RESOURCE_PATH = "mytlnet/mythreads.php";
	private static final String FORUM_ENTRIES_XPATH = "//table[@class='solid']/tbody/tr[position()>1]";
	private static final String TAG = "MySubs";
	private ProgressDialog progressDialog;
	private TLHandler handler;
	private Context context;
	private List<PostInfo> subInfoList = new ArrayList<PostInfo>(50);
	
    @Override 
    public void onConfigurationChanged(Configuration newConfig) { 
        super.onConfigurationChanged(newConfig); 
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_forum);
		context = this;
		setTitle(TLLib.makeActivityTitle("My Subscribed Threads"));
		refresh();
		registerForContextMenu(getListView());
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
			TagNode node = TLLib.TagNodeFromURLMySubs(cleaner, new URL(url), handler, context);
			handler.sendEmptyMessage(TLHandler.PROGRESS_RENDERING);
			Object[] forumTable = node.evaluateXPath(FORUM_ENTRIES_XPATH);
			for (Object o : forumTable){
				TagNode entry = (TagNode) o;
				subInfoList.add(PostInfo.buildPostInfoFromForumEntry(entry));
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
	
	private int parseTopicId(String threadUrl) {
		String getAttributes = threadUrl.split("\\?")[1];
		String [] temp = getAttributes.split("#");
		String [] attributes = temp[0].split("&");
		for (String attribute : attributes){
			String [] nameValue = attribute.split("=");
			if (nameValue[0].equals("topic_id")){
				return Integer.parseInt(nameValue[1]);
			}
		}
		return -1; // Unsupported url
	}
	
	private class MyPostsHandler extends TLHandler {
		public MyPostsHandler(ProgressDialog progressDialog, Context context) {
			super(progressDialog, context);
		}
		
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 0 && this.progressStatus == TLHandler.PROGRESS_OKAY){
				progressDialog.setMessage("Rendering...");
				getListView().setAdapter(new ForumAdapter(subInfoList, context, getListView()));
				progressDialog.dismiss();
			}
			else {
				super.handleMessage(msg);
			}
		}
	}
	
	protected void onListItemClick(ListView l, View v, int position, long id) {
		PostInfo subInfo = subInfoList.get((int) id);
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
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.hold_sub_menu, menu);
	}

	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.unsubscribe:
			AdapterView.AdapterContextMenuInfo info;
			try {
			    info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
			} catch (ClassCastException e) {
			    Log.e(TAG, "bad menuInfo", e);
			    return false;
			}
			int topicId = parseTopicId(subInfoList.get(info.position).topicURL);
			subInfoList.remove(info.position);
			UnsubscribeThreadTask unsubscribeThreadTask = new UnsubscribeThreadTask(topicId);
			unsubscribeThreadTask.execute();
			break;
		}
		return true;
	}
	
	public void run(){
		render();
		handler.sendEmptyMessage(0);
	}
	
	private class UnsubscribeThreadTask extends AsyncTask<Void, Void, Boolean> {
		String topicId;
		
		public UnsubscribeThreadTask(int topicId) {
			this.topicId = Integer.toString(topicId);
		}
		
		@Override
		protected Boolean doInBackground(Void... arg0) {
			try {
				TLLib.subscribeThread(topicId);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			if (result)
				Toast.makeText(context, "Thread unsubscribed!", Toast.LENGTH_SHORT).show();
			getListView().setAdapter(new ForumAdapter(subInfoList, context, getListView()));
		}
	}
}
