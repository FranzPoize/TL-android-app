package org.opensourcetlapp.tl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.opensourcetlapp.tl.Adapters.MyPostsAdapter;
import org.opensourcetlapp.tl.Structs.PostInfo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class SearchFragment extends ListFragment implements Runnable {
	
	EditText search;
	Fragment instance;
	ProgressBar progressBar;
	SearchHandler handler;
	ArrayList<PostInfo> postInfoList = new ArrayList<PostInfo>();
	int page = 1;
	
	private boolean mInstanceAlreadySaved;
    private Bundle mSavedOutState;
    private DBHelper db;
    private String searchType = "t"; // default; t => title, c => content, ct => title and content
    private String username;
	
    @Override 
    public void onConfigurationChanged(Configuration newConfig) { 
        super.onConfigurationChanged(newConfig);
    }

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelableArrayList("search", postInfoList);
		mInstanceAlreadySaved = true;
	}

	public void run() {
		try {
			TagNode response = TLLib.TagNodeFromURLSearch(new HtmlCleaner(),search.getText().toString()+"&t="+searchType+(username != null ? "&u="+username : 0), handler,getActivity());
			Object[] tableResults = null;
			Object[] nodeList = null;
			try {
				tableResults = response.evaluateXPath("//table[@width=748]/tbody");
				
				// Add 3 cases here; title, content, title & content //				
				nodeList = ((TagNode)tableResults[tableResults.length - 2]).evaluateXPath("//tr[position()>1]");
				
				TagNode n;
				for (Object o : nodeList){
					n = (TagNode)o;
					if (n.evaluateXPath("./td[3]").length > 0) {
						TagNode topicStarter =  (TagNode)(n.evaluateXPath("./td[3]"))[0];
						TagNode replies =  (TagNode)(n.evaluateXPath("./td[4]"))[0];
						TagNode lastMessage = (TagNode)(n.evaluateXPath("./td[6]"))[0];
						Object [] resourceList = (n.evaluateXPath("./td[2]/a"));
						TagNode topic = (TagNode)resourceList[0];
						TagNode lastPost = (TagNode)resourceList[resourceList.length-1];
						TagNode topicURL = (TagNode)resourceList[0];
						
						String topicURLString = topicURL.getAttributeByName("href");
	
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
				}
			} catch (XPatherException e) {
				Log.d("SearchFragment", "couldn't retrieve results tables");
				e.printStackTrace();
			}
			
			handler.sendEmptyMessage(0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (!postInfoList.isEmpty())
			getListView().setAdapter(new MyPostsAdapter(postInfoList, getActivity()));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		db = new DBHelper(getActivity());
		
		if (null == savedInstanceState && null != mSavedOutState) {
            savedInstanceState = mSavedOutState;
        }

        mInstanceAlreadySaved = false;
		
		if (savedInstanceState != null) {
			postInfoList = savedInstanceState.getParcelableArrayList("search");
		}
		
		instance = this;
		
		View view = inflater.inflate(R.layout.search, container,false);
		
		search = (EditText)view.findViewById(R.id.search);
		progressBar = (ProgressBar)view.findViewById(R.id.progressBar);

		search.setOnKeyListener(new OnKeyListener() {
			
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
					postInfoList = new ArrayList<PostInfo>();
					progressBar.setVisibility(View.VISIBLE);
					handler = new SearchHandler(progressBar);
					page = 1;
					new Thread((Runnable)instance).start();
					return true;
				}
				return false;
			}
		});
		
		return view;
	}
	
	public class SearchHandler extends Handler {
		private ProgressBar bar;
		
		public SearchHandler(ProgressBar bar) {
			this.bar = bar;
		}
		
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 0 ) {
				getListView().setAdapter(new MyPostsAdapter(postInfoList, getActivity()));
				bar.setVisibility(View.INVISIBLE);
			} else {
				super.handleMessage(msg);
			}
		}
	}
	
	@Override
    public void onStop() 
    {
        if (!mInstanceAlreadySaved)
        {
            mSavedOutState = new Bundle();
            onSaveInstanceState( mSavedOutState );
        }

        super.onStop();
    }
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		db.close();
	}
	
	public void onListItemClick(ListView l, View v, int position, long id) {
		PostInfo postInfo = postInfoList.get((int) id);
		String postURL = "/forum/"+postInfo.topicURL;
		String postTopic = postInfo.topicString;
		boolean postLocked = postInfo.locked;
		
		Intent intent = new Intent().setClass(getActivity(), ShowThread.class);
		intent.putExtra("postURL", postURL);
		intent.putExtra("postTopic", postTopic);
		intent.putExtra("postLocked", false);
		startActivity(intent);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		menuInflater.inflate(R.menu.search_menu, menu);
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		Context context = getActivity();
		switch (item.getItemId()) {
		case R.id.searchBy:
			final CharSequence[] items = {"Title", "Content", "Title and Content"};
			
			AlertDialog.Builder builderRadio = new AlertDialog.Builder(context);
			builderRadio.setTitle("Search Option");
			builderRadio.setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int id) {
			    	switch (id) {
			    	case 0:
			    		searchType = "t";
			    		break;
			    	case 1:
			    		searchType ="c";
			    		break;
			    	case 2:
			    		searchType = "ct";
			    		break;
			    	}
			    	dialog.dismiss();
			    }
			});
			AlertDialog alertRadio = builderRadio.create();
			alertRadio.show();
			break;
		case R.id.searchUser:
			LayoutInflater li = LayoutInflater.from(context);
			View promptsView = li.inflate(R.layout.search_user_prompt, null);
			final EditText userInput = (EditText) promptsView.findViewById(R.id.searchUsernameText);
			
			AlertDialog.Builder builderInput = new AlertDialog.Builder(context);
			builderInput.setView(promptsView);	
			builderInput.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int id) {
			    	username = userInput.getText().toString();
			    }
			  });
			AlertDialog AlertInput = builderInput.create();
			AlertInput.show();
			break;
		}
		return true;
	}	
}
