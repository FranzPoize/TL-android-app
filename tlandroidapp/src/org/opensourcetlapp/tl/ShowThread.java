package org.opensourcetlapp.tl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.htmlcleaner.ContentNode;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.opensourcetlapp.tl.Structs.PostInfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Message;
import android.text.Html;
import android.text.InputFilter.LengthFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

public class ShowThread extends Activity implements Runnable {

	private String BASE_JS ="<script type='text/javascript'>function toggleShowQuote(b){var a=document.getElementById('showQuoteRest_'+b);a.style.display=a.style.display=='none'?'block':'none';}" +
			"function toggleShowSpoiler2(b,c){var a=document.getElementById('spoiler_'+c);a.style.display=a.style.display=='none'?'block':'none';}"+
			"function toggleShowSpoiler(c,a,b){toggleShowSpoiler2(c,b);}</script>";
	
	private static final String PREV_PAGE_XPATH = "./tbody/tr[last()]/td[2]/a";
	private static final String LOGGED_IN_PREV_PAGE_XPATH = "./tbody/tr/td[2]/a";
	
	private String postURL;
	private int topicId;
	private String postTopic;
	private int lastPage;
	private String lastURL;
	private boolean postLocked;
	
	private static CustomImageGetter imageGetter;
	
	private int displayPostId;
	private int currentPage;
	
	private HtmlCleaner cleaner;
	private Context context;
	
	private static PostData[] postList;
	
	private ShowPostHandler handler;
	private ProgressDialog progressDialog;
	
	private LayoutInflater mInflater;
	
	private LinearLayout container;
	
	private Dialog gotoPageDialog;
	private EditText gotoPageEditText;
	private Button gotoPageButton;
	private Button gotoPageCancelButton;
	
	/**
	 *  pattern of different html elements we want to parse 
	 */
	private static final String PARSE_NODE =  "<table width=\"742\" cellspacing=\"0\" cellpadding=\"0\">";
	private static final String FORUM_XPATH = "//table[@width='742']";
	
	@Override 
    public void onConfigurationChanged(Configuration newConfig) { 
        super.onConfigurationChanged(newConfig); 
    }
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_thread);
		
		mInflater = LayoutInflater.from(this);
		
		container = (LinearLayout)findViewById(R.id.listThread);

		Bundle extras = getIntent().getExtras();
		postURL = extras.getString("postURL");
		postTopic = extras.getString("postTopic");	// TODO: Make the app pull the topic from the HTML
		postLocked = extras.getBoolean("postLocked"); // TODO: This only works properly when opening this activity is opened from the ShowForum. Add closed-thread detection.
		imageGetter = new CustomImageGetter(container,this);
		
		findViewById(R.id.next).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {loadPostURL(buildPostURL(currentPage+1));}
		});
		
		findViewById(R.id.prev).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {loadPostURL(buildPostURL(currentPage-1));}
		});
		
		findViewById(R.id.last).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {loadPostURL(buildPostURL(lastPage));}
		});
		
		findViewById(R.id.first).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {loadPostURL(buildPostURL(1));}
		});
		
		

		cleaner = TLLib.buildDefaultHtmlCleaner();
		context = this;
		
		refresh(true);		
	}
	
	private void loadPostURL(String url) {
		postURL = url;
		parsePostURL();
		refresh(true);
	}
	
	private String buildPostURL(int pageNumber){
		String temp = postURL;
		int c = postURL.indexOf('?');
		if (c != -1){
			temp = postURL.substring(0, c);
		}
		temp += String.format("?topic_id=%d&currentpage=%d", topicId, pageNumber);
		return temp;
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
	
	private void parseLastPageFromPostURL(){
		if (lastURL != null){
			String getAttributes = lastURL.split("\\?")[1];
			String [] temp = getAttributes.split("#");
			String [] attributes = temp[0].split("&");
			for (String attribute : attributes){
				String [] nameValue = attribute.split("=");

				if (nameValue[0].equals("currentpage")){
					lastPage = Integer.parseInt(nameValue[1]);
				}
			}
		}
		else {
			lastPage = currentPage;
		}
	}
	
	private void refresh(boolean resetScrollPosition){
		progressDialog = ProgressDialog.show(this, null, "Loading...", true,
				true);
		handler = new ShowPostHandler(resetScrollPosition, progressDialog, this);
		container.removeAllViews();
		new Thread(this).start();
	}
	
	private void refreshDisplay() {
		String absolutePostURL = postURL;
		
		parsePostURL();

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
			int offset = ((TagNode)posts[posts.length-1]).evaluateXPath("//form[@name='theform']").length > 0 ? 2 : 2;
			
			/** get number of page */
			Object[] nextPages;
			

			nextPages = forumTagNode.evaluateXPath(LOGGED_IN_PREV_PAGE_XPATH);
			
			if (nextPages.length > 0) {
				int nextPageLength = nextPages.length;
				for (int i = nextPageLength - 1; i >= nextPageLength - 2; i--) {
					TagNode nextPage = (TagNode) nextPages[i];
					if (nextPage.getChildren().iterator().next().toString()
							.trim().equals("Next")) {
						TagNode lastPage = (TagNode) nextPages[i - 1];
						lastURL = Html.fromHtml(
								lastPage.getAttributeByName("href")).toString();
					}
				}
			}
			parseLastPageFromPostURL();
			
			postList = new PostData[posts.length - offset];
			
			for (int i = 0;i<posts.length - offset;i++) {
				TagNode post = (TagNode)posts[i];
				Object[] postTr = post.evaluateXPath("//table[@width='752']/tbody/tr");
				TagNode header = (TagNode)postTr[0];
				TagNode content = parsePostContent(postTr);
				
				PostData postData = new PostData();
				
				TagNode firstTd = (TagNode)((TagNode)postTr[1]).getChildren().get(0);
				boolean type = (firstTd.getAttributeByName("class").equals("forumPost"));
				
				postData.setContent(BASE_JS + cleaner.getInnerHtml(((TagNode)content.evaluateXPath("//td[@class='forumPost']")[0])));
				
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
				
				buildViews();
				
				progressDialog.dismiss();
			} else {
				super.handleMessage(msg);
			}
		}
	};
	/** build the view from the list of postData **/
	private void buildViews() {
		
		for (int i = 0; i<postList.length;i++) {
			
			View postView = mInflater.inflate(R.layout.show_thread_row, null);
			
			WebView content = (WebView)postView.findViewById(R.id.postContent);
			content.getSettings().setJavaScriptEnabled(true);
			TextView poster = (TextView)postView.findViewById(R.id.posterName);
			TextView countryDate = (TextView)postView.findViewById(R.id.postDate);
			ImageView icon = (ImageView)postView.findViewById(R.id.postIcon);
			
			LinearLayout options = (LinearLayout)postView.findViewById(R.id.posterInfos);

			content.loadDataWithBaseURL("http://www.teamliquid.net/",postList[i].getContent(), "text/html", null,null);
			icon.setImageDrawable(imageGetter.getDrawable(postList[i].getIcon()));
			
			/** load values depending on the type of the post (normal,news)*/
			if (postList[i].getType().equals("news")) {
				poster.setText(postList[i].getTitle());
			} else {
				poster.setText(postList[i].getPoster());
				postView.findViewById(R.id.postHeaderThread).setOnClickListener(new ThreadOnClickListener(options));
				
				if (postList[i].postId !=null) {
					options.findViewById(R.id.quoteButton).setOnClickListener(new QuoteOnClickListener(Integer.parseInt(postList[i].postId)));
				} else
					options.findViewById(R.id.quoteButton).setVisibility(TextView.GONE);
				
				if (postList[i].getPoster() !=null && TLLib.loginStatus) {
					options.findViewById(R.id.pmButton).setOnClickListener(new PMOnClickListener(postList[i].getPoster()));
				} else
					options.findViewById(R.id.pmButton).setVisibility(TextView.GONE);
				
				countryDate.setText(postList[i].getCountryDate());
			}
			
			container.addView(postView,i);
		}
		
		if (currentPage == 1) {
			findViewById(R.id.prev).setEnabled(false);
			findViewById(R.id.first).setEnabled(false);
		} else {
			findViewById(R.id.prev).setEnabled(true);
			findViewById(R.id.first).setEnabled(true);
		}
		
		if (currentPage == lastPage) {
			findViewById(R.id.next).setEnabled(false);
			findViewById(R.id.last).setEnabled(false);
		} else {
			findViewById(R.id.next).setEnabled(true);
			findViewById(R.id.last).setEnabled(true);
		}
		
		((TextView)findViewById(R.id.threadPage)).setText(currentPage+"/"+lastPage);
	}
	
	public class ThreadOnClickListener implements OnClickListener {

		private LinearLayout options;
		
		public ThreadOnClickListener(LinearLayout options) {
			this.options = options;
		}
		@Override
		public void onClick(View v) {
			options.setVisibility(options.getVisibility() == LinearLayout.GONE ? LinearLayout.VISIBLE : LinearLayout.GONE);			
		}
		
	}
	
	public class QuoteOnClickListener implements OnClickListener {

		private int postId;
		
		public QuoteOnClickListener(int postId) {
			this.postId = postId;
		}
		@Override
		public void onClick(View v) {
			Intent intent = new Intent().setClass(context, PostMessage.class);
			intent.putExtra("postId", postId);
			intent.putExtra("topicId", topicId);
			
			context.startActivity(intent);
		}
		
	}
	
	public class PMOnClickListener implements OnClickListener {

		private String login;
		
		public PMOnClickListener(String login) {
			this.login = login;
		}
		@Override
		public void onClick(View v) {
			Intent intent = new Intent().setClass(context, ShowMyPMReply.class);
			intent.putExtra("to", login);
			context.startActivity(intent);
		}
		
	}
	
	
	/** Data class containing all the information concerning a post*/
	public class PostData {
		private String content;
		
		private String poster;
		
		private String countryDate;
		
		private String icon;
		
		private String title;
		
		private String textBy;
		
		private String type;
		
		private String postId;

		public String getContent() {
			return content;
		}

		public void buildHeader(boolean type2, TagNode post) {
			try {
				this.setIcon(((TagNode)post.evaluateXPath("//img")[0]).getAttributeByName("src"));
				if (!type2) {
					TagNode node = null;
					
					Object[] nodesArray = post.evaluateXPath("//span[@class='forummsginfo']");
					if (nodesArray.length > 0)
						node = (TagNode)nodesArray[0];
					else {
						node = (TagNode)post.evaluateXPath("//span[@class='forummsginfoa']")[0];
					}
					
					String[] infos = null;
					
					if (node.getChildren().size() > 2 && ((TagNode)node.getChildren().get(1)).getName().equals("img")) {
						/** Depending on the size on the header get the poster and the country/date string */
						if (node.getChildren().size() > 6) {
						
							Object[] nodeInfos = node.getChildren().toArray();
							
							if (!((TagNode)nodeInfos[3]).getName().equals("img"))
								this.setPoster(((TagNode)nodeInfos[3]).getChildren().get(0).toString());
							else
								this.setPoster(((ContentNode)nodeInfos[2]).toString().replaceAll("&nbsp;", ""));
							
							this.setCountryDate(((ContentNode)nodeInfos[6]).toString().replaceAll("&nbsp;", ""));
						} else if (node.getChildren().size() > 5) {
							Object[] nodeInfos = node.getChildren().toArray();
							
							this.setPoster(((TagNode)nodeInfos[3]).getChildren().get(0).toString());
							this.setCountryDate(((TagNode)nodeInfos[5]).toString().replaceAll("&nbsp;", ""));
							
						} else if (node.getChildren().size() > 4) {
							Object[] nodeInfos = node.getChildren().toArray();
							if(TLLib.loginStatus) {
								if (((TagNode)nodeInfos[3]).getName().equals("img"))
									this.setPoster(((ContentNode)nodeInfos[2]).toString().replaceAll("&nbsp;", ""));
								else
									this.setPoster(((TagNode)nodeInfos[3]).getChildren().get(0).toString());
								this.setCountryDate(((ContentNode)nodeInfos[4]).toString().replaceAll("&nbsp;", ""));
							} else {
								this.setPoster(((ContentNode)nodeInfos[2]).toString().replaceAll("&nbsp;", ""));
								this.setCountryDate(((ContentNode)nodeInfos[4]).toString().replaceAll("&nbsp;", ""));
							}
						} else {
							
							infos = node.getChildren().get(2).toString().split("&nbsp;");
							this.setPoster(infos[1]);
							this.setCountryDate(infos[2]);
						}
						
					} else {
						/** special for PoP */
						this.setPoster("Pop!");
						this.setCountryDate(node.getChildren().get(0).toString().replaceAll("&nbsp;", ""));
					}
					
					Object[] links = ((TagNode)post.getChildren().get(1)).evaluateXPath("//a");
					
					for(int i = 0; i< links.length;i++) {
						if (((TagNode)links[i]).getChildren().get(0).toString().equals("Quote")) {
							postId = ((TagNode)links[i]).getAttributeByName("href").split("\\?")[1].split("&")[0].split("=")[1];
						}
					}
				} else {
					this.setTitle(((TagNode)((TagNode)((TagNode)post.evaluateXPath("//td")[0]).getChildren().get(2)).getChildren().get(1)).getChildren().get(0).toString());
				}
				
				
				this.setType(type2 ? "news" : "normal");
			
			} catch (XPatherException e) {
				Log.d("show thread", "Problem parsing header's stuff");
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

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
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
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.show_post_menu, menu);
		return true;
	}
	
	private void showGotoPageDialog(){
		if (gotoPageDialog == null){
			gotoPageDialog = new Dialog(this);
			gotoPageDialog.setContentView(R.layout.goto_page_popup);
			gotoPageDialog.setCancelable(true);
			gotoPageDialog.setTitle(String.format("Enter page number (%d %s)", lastPage, lastPage==1?"page":"pages"));
			
			gotoPageButton = (Button) gotoPageDialog.findViewById(R.id.gotoPageButton);
			gotoPageCancelButton = (Button) gotoPageDialog.findViewById(R.id.gotoPageCancelButton);
			gotoPageEditText = (EditText) gotoPageDialog.findViewById(R.id.gotoPageEditText);
			
			gotoPageButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					try {
						int pageNumber = Integer.parseInt(gotoPageEditText.getText().toString());
						if (pageNumber < 1){
							AlertDialog.Builder builder = new AlertDialog.Builder(context);
							builder.setMessage("Page number must be greater than 0.")
							       .setCancelable(false)
							       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
							           public void onClick(DialogInterface dialog, int id) {
							           
							           }
							       });
							AlertDialog alert = builder.create();
							alert.show();
						} else if (pageNumber > lastPage){
							AlertDialog.Builder builder = new AlertDialog.Builder(context);
							builder.setMessage("Page number must be no greater than " +lastPage)
							       .setCancelable(false)
							       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
							           public void onClick(DialogInterface dialog, int id) {
							           
							           }
							       });
							AlertDialog alert = builder.create();
							alert.show();
						}
						
						else {
							loadPostURL(buildPostURL(pageNumber));
							gotoPageDialog.dismiss();
						}
					} catch (NumberFormatException e){
						
					}
				}
			});
		}
		
		gotoPageCancelButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				gotoPageDialog.dismiss();
			}
		});
		gotoPageEditText.setText("");
		gotoPageDialog.show();
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.gotoPage:
			showGotoPageDialog();
			break;
		case R.id.refresh:
			refresh(false);
			break;
		case R.id.reply:
			if (!TLLib.loginStatus){
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
				alertDialogBuilder.setTitle("Login Error");
				alertDialogBuilder.setMessage("Please Login before posting.\n");
				alertDialogBuilder.setPositiveButton("Okay", null);
				alertDialogBuilder.show();
			}
			else if (postLocked){
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
				alertDialogBuilder.setTitle("Thread Closed");
				alertDialogBuilder.setMessage("This thread has been locked by a Moderator.");
				alertDialogBuilder.setPositiveButton("Okay", null);
				alertDialogBuilder.show();
			}
			else {
				Intent intent = new Intent().setClass(this, PostMessage.class);
				intent.putExtra("topicId", topicId);
				startActivity(intent);
			}
		break;
		}

		return true;

	}
}
