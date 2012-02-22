package org.opensourcetlapp.tl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.htmlcleaner.DefaultTagProvider;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagInfo;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.opensourcetlapp.tl.Structs.PostInfo;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;

public class ShowThread extends ListActivity implements Runnable {

	private String BASE_JS = "function toggleHidden(b,c,a,d){span=document.getElementById(b);link=document.getElementById(c);if(span.style.display!='none'){span.style.display='none';link.innerHTML=a}else{span.style.display='';link.innerHTML=d}}" +
			"function toggleShowQuote(a){toggleHidden('showQuoteRest_'+a,'showQuoteLink_'+a,'Show nested quote +','Hide nested quote -')}" +
			"function toggleShowSpoiler2(b,c){var a=$(b).children('span');if($(a[0]).text()=='+ Show'){$('#spoiler_'+c).show();$(a[0]).text('- Hide');$(a[1]).text(' -')}else{$('#spoiler_'+c).hide();$(a[0]).text('+ Show');$(a[1]).text(' +')}return false}" +
			"function toggleShowSpoiler(c,a,b){spoilerDiv=document.getElementById('spoiler_'+b);if(c.innerHTML.match('^\\+')){c.innerHTML=getSpoilerHeader(a,false);spoilerDiv.style.display='';spoilerDiv.style.visibility='visible'}else{c.innerHTML=getSpoilerHeader(a,true);spoilerDiv.style.display='none';spoilerDiv.style.visibility='hidden'}}";
	
	private String postURL;
	private int topicId;
	private String postTopic;
	private int lastPage;
	private boolean postLocked;
	
	private CustomImageGetter imageGetter;
	
	private int displayPostId;
	private int currentPage;
	
	private HtmlCleaner cleaner;
	private Context context;
	
	private PostData[] postList;
	
	private ShowPostHandler handler;
	private ProgressDialog progressDialog;
	
	/**
	 *  pattern of different html elements we want to parse 
	 */
	private static final String PARSE_NODE =  "<table width=\"742\" cellspacing=\"0\" cellpadding=\"0\">";
	private static final String FORUM_XPATH = "//table[@width='742']";
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_thread);

		Bundle extras = getIntent().getExtras();
		postURL = extras.getString("postURL");
		postTopic = extras.getString("postTopic");	// TODO: Make the app pull the topic from the HTML
		postLocked = extras.getBoolean("postLocked"); // TODO: This only works properly when opening this activity is opened from the ShowForum. Add closed-thread detection.
		imageGetter = new CustomImageGetter(this);
		
		parsePostURL();
		//setTitle(TLLib.makeActivityTitle(String.format("(%d) %s", currentPage, postTopic)));

		cleaner = TLLib.buildDefaultHtmlCleaner();
		context = this;
		
		progressDialog = ProgressDialog.show(this, null, "Loading...", true,
				true);
		handler = new ShowPostHandler(true, progressDialog, this);
		
		refreshDisplay();
		handler.sendEmptyMessage(0);
		
		setListAdapter(new ArrayAdapter<View>(this, R.layout.show_thread_row,(View[]) null) {
			@Override
			public View getView(int position, View convertView,ViewGroup parent) {
				return null;
			}
		});
	}
	
	private void parsePostURL() {
		currentPage = 1;
		String getAttributes = postURL.split("\\?")[1];
		String [] temp = getAttributes.split("#");
		if (temp.length == 2){
			displayPostId = Integer.parseInt(temp[1]);
		}
		String [] attributes = temp[0].split("&");
		for (String attribute : attributes){
			String [] nameValue = attribute.split("=");
			if (nameValue[0].equals("topic_id")){
				topicId = Integer.parseInt(nameValue[1]);
			}
			else if (nameValue[0].equals("currentpage")){
				currentPage = Integer.parseInt(nameValue[1]);
			}
		}
	}
	
	private void refresh(boolean resetScrollPosition){
		progressDialog = ProgressDialog.show(this, null, "Loading...", true,
				true);
		handler = new ShowPostHandler(resetScrollPosition, progressDialog, this);
		new Thread(this).start();
	}
	
	private void refreshDisplay() {
		String absolutePostURL = postURL;
		
		if (postURL.charAt(0) == '/') {
			absolutePostURL = TLLib.getAbsoluteURL(postURL);
		}
		try {
			SharedPreferences settings = context.getSharedPreferences(Settings.SETTINGS_FILE_NAME, 0);
	    	boolean viewAll = settings.getBoolean(Settings.VIEW_ALL, false);
	    	if (viewAll && !absolutePostURL.contains("currentpage")){
	    		absolutePostURL += "&currentpage=All";
	    	}
			URL url = new URL(absolutePostURL);
			TagNode node = TLLib.TagNodeFromURLEx2(cleaner, url, handler, context, PARSE_NODE, false);

			Object[] forum = node.evaluateXPath(FORUM_XPATH);
			TagNode forumTagNode;
			// TODO: ASAP Figure out what's causing the TagParser to occasionally break
			// It may have something to do with the post "GSL S3 ...." It seems to be hitting EOF prematurely
			if (forum.length > 0){
				forumTagNode = (TagNode) forum[0];
			}
			else {
				 node = TLLib.TagNodeFromURLEx2(cleaner, url, handler, context, null, false);
				 forum = node.evaluateXPath(FORUM_XPATH);
				 forumTagNode = (TagNode) forum[0];
			}
			
			Object[] posts =  node.evaluateXPath("//table[@width='742']/tbody/tr");
			int offset = ((TagNode)posts[posts.length-1]).evaluateXPath("//form[@name='theform']").length > 0 ? 2 : 1;
			
			postList = new PostData[posts.length - offset];
			
			for (int i = 0;i<posts.length - offset;i++) {
				TagNode post = (TagNode)posts[i];
				Object[] postTr = post.evaluateXPath("//table[@width='752']/tbody/tr");
				TagNode header = (TagNode)postTr[0];
				TagNode content = parsePostContent(postTr);
				
				PostData postData = new PostData();
				
				TagNode firstTd = (TagNode)((TagNode)postTr[1]).getChildren().get(0);
				boolean type = (firstTd.getAttributeByName("class").equals("forumPost"));
				
				postData.setContent(cleaner.getInnerHtml(((TagNode)content.evaluateXPath("//td[@class='forumPost']")[0])).replaceAll("src=\"/", "src=\"http://www.teamliquid.net/"));
				
				postData.buildHeader(type,header);
				
				postList[i] = postData;
			}
			
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			handler.progressStatus = TLHandler.PROGRESS_NETWORK_DOWN;
		} catch (XPatherException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private TagNode parsePostContent(Object[] post) {
		TagNode firstTd = (TagNode)((TagNode)post[1]).getChildren().get(0);
		return (TagNode)((firstTd.getAttributeByName("class").equals("forumPost")) ? post[2] : post[1]);
	}

	@Override
	public void run() {
		refreshDisplay();
		handler.sendEmptyMessage(0);
	}
	
	private class ShowPostHandler extends TLHandler {
		private boolean resetScrollPosition;
		public ShowPostHandler(boolean resetScrollPosition, ProgressDialog progressDialog, Context context) {
			super(progressDialog, context);
			this.resetScrollPosition = resetScrollPosition;
		}

		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 0
					&& handler.progressStatus == TLHandler.PROGRESS_OKAY) {
				setTitle(TLLib.makeActivityTitle(String.format("(%d/%d) %s", currentPage, lastPage, postTopic)));
				progressDialog.dismiss();
			} else {
				super.handleMessage(msg);
			}
		}
	};
	
	private class PostData {
		private String content;
		
		private String poster;
		
		private String countryDate;
		
		private String icon;
		
		private String textBy;
		
		private String type;
		
		private String postId;

		public String getContent() {
			return content;
		}

		public void buildHeader(boolean type2, TagNode post) {
			try {
				this.setIcon(((TagNode)post.evaluateXPath("//img")[0]).getAttributeByName("src"));
				String[] infos = ((TagNode)post.evaluateXPath("//span[@class=forummsginfo]")[0]).getChildren().get(2).toString().split("\\s\\s\\s");
				this.setPoster(infos[0]);
				this.setCountryDate(infos[1]);
				
				this.setType(type2 ? "news" : "normal");
			
			} catch (XPatherException e) {
				Log.d("show thread", "Problem parsing header's stuff")
			}
			
		}

		public void setContent(String content) {
			this.content = content;
		}

		public String getPoster() {
			return poster;
		}

		public void setPoster(String poster) {
			this.poster = poster;
		}

		public String getIcon() {
			return icon;
		}

		public void setIcon(String icon) {
			this.icon = icon;
		}

		public String getTextBy() {
			return textBy;
		}

		public void setTextBy(String textBy) {
			this.textBy = textBy;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getPostId() {
			return postId;
		}

		public void setPostId(String postId) {
			this.postId = postId;
		}

		public String getCountryDate() {
			return countryDate;
		}

		public void setCountryDate(String countryDate) {
			this.countryDate = countryDate;
		}
		
		
	}
}
