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
import java.util.Hashtable;
import java.util.Iterator;

import org.htmlcleaner.ContentToken;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.SimpleXmlSerializer;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.opensourcetlapp.tl.R;
import org.opensourcetlapp.tl.widget.DisplayableLinkSpan;
import org.opensourcetlapp.tl.widget.EditSpan;
import org.opensourcetlapp.tl.widget.QuotePostSpan;
import org.opensourcetlapp.tl.widget.SpoilerSpan;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.AlignmentSpan;
import android.text.style.BulletSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.QuoteSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.BufferType;


public class ShowPost extends Activity implements Runnable {
	private String postURL;
	private int topicId;
	private int displayPostId;
	private int currentPage;
	
	private static final String TAG = "ShowPost";
	private HtmlCleaner cleaner;
	private Context context;

	private RenderUBB renderUBB;	// Do not make this static. It needs a reference to the context.

	private static final String PARSE_NODE =  "<table width=\"742\" cellspacing=\"0\" cellpadding=\"0\">";
	
	private static final String FORUM_XPATH = "//table[@width='742']";

	private static final String POST_NAME_XPATH = "./tbody/tr/td/a";
	private static final String POST_XPATH = "./tbody/tr/td/table/tbody/tr[2]";
	private static final String POST_HEADER_XPATH = "./tbody/tr/td/table[@class='solid']/tbody/tr[1]/td[1]";
	private static final String POST_CONTENT_XPATH = "./tbody/tr/td/table[@class='solid']/tbody";
	private static final String PREV_PAGE_XPATH = "./tbody/tr[last()]/td[2]/a";
	private static final String LOGGED_IN_PREV_PAGE_XPATH = "./tbody/tr/td[2]/a";
	private static final int POST_CONTENT_TYPE_NEWS = 0;
	private static final int POST_CONTENT_TYPE_DEFAULT = 1;


	private LinearLayout forumPostView;

	private View [] viewList;
	private ScrollView scrollView;
	private Hashtable<String, Integer> colorDictionary = new Hashtable<String, Integer>();

	private String nextURL;
	private String prevURL;
	private String lastURL;
	private String firstURL;
	private String postTopic;
	private int lastPage;
	private boolean postLocked;

	private ShowPostHandler handler;
	private ProgressDialog progressDialog;

    @Override 
    public void onConfigurationChanged(Configuration newConfig) { 
        super.onConfigurationChanged(newConfig); 
        //---code to redraw your activity here---
        //...
    }

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_post);
	
		colorDictionary.put(" red", new Integer(Color.RED)); // The white space before "red" is necessary															// is necessary
		colorDictionary.put(" blue", new Integer(Color.BLUE));
		colorDictionary.put(" green", new Integer(Color.parseColor("#008800")));

		forumPostView = (LinearLayout) findViewById(R.id.forumPost);
		scrollView = (ScrollView) findViewById(R.id.scroll);

		Bundle extras = getIntent().getExtras();
		postURL = extras.getString("postURL");
		postTopic = extras.getString("postTopic");	// TODO: Make the app pull the topic from the HTML
		postLocked = extras.getBoolean("postLocked"); // TODO: This only works properly when opening this activity is opened from the ShowForum. Add closed-thread detection. 
		
		parsePostURL();
		//setTitle(TLLib.makeActivityTitle(String.format("(%d) %s", currentPage, postTopic)));

		cleaner = TLLib.buildDefaultHtmlCleaner();
		context = this;

		renderUBB = new RenderUBB(this, null);
		refresh(true);
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
			Log.d(TAG, absolutePostURL);
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
			Object [] postContents = forumTagNode.evaluateXPath(POST_CONTENT_XPATH);
			int offset = ((TagNode)postContents[postContents.length-1]).evaluateXPath("//form[@name='theform']").length > 0 ? 1 : 1;
			TagNode [] posts = new TagNode[postContents.length - offset];
			TagNode [] postHeaders = new TagNode[postContents.length - offset];
			int [] postContentType = new int[postContents.length - offset];
			
			int postContentsPos = 0;
			for (Object o : postContents){
				TagNode p = (TagNode)o;
				if(postContentsPos < posts.length) {
					if (p.getChildren().size() >= 4){ // NEWS forum OP post
						postHeaders[postContentsPos] = (TagNode)p.evaluateXPath("./tr[1]")[0];
						posts[postContentsPos] = (TagNode)p.evaluateXPath("./tr[3]")[0];
						postContentType[postContentsPos] = POST_CONTENT_TYPE_NEWS;
					}
					else {
							postHeaders[postContentsPos] = (TagNode)p.evaluateXPath("./tr[1]/td[1]")[0];
							posts[postContentsPos] = (TagNode)p.evaluateXPath("./tr[2]")[0];
							postContentType[postContentsPos] = POST_CONTENT_TYPE_DEFAULT;
					}
				}
				postContentsPos++;
			}
			
		//	Object[] posts = forumTagNode.evaluateXPath(POST_XPATH);
			Object[] postNames = forumTagNode.evaluateXPath(POST_NAME_XPATH);
			//Object[] postHeaders = forumTagNode
					//.evaluateXPath(POST_HEADER_XPATH);
			Object[] prevPages;
			if (TLLib.loginStatus){
				//prevPages = forumTagNode.evaluateXPath(PREV_PAGE_XPATH);
				prevPages = forumTagNode.evaluateXPath(LOGGED_IN_PREV_PAGE_XPATH);
			}
			else {
				prevPages = forumTagNode.evaluateXPath(PREV_PAGE_XPATH);
			}
			Object[] nextPages = prevPages;			

			if (prevPages.length > 0) {
				TagNode prevPage = (TagNode) prevPages[0];
				if (prevPage.getChildren().iterator().next().toString().trim()
						.equals("Prev")) {
					prevURL = Html
							.fromHtml(prevPage.getAttributeByName("href"))
							.toString();
					TagNode firstPage = (TagNode) prevPages[1];
					firstURL = Html.fromHtml(
							firstPage.getAttributeByName("href")).toString();
				}
			}
			if (nextPages.length > 0) {
				int nextPageLength = nextPages.length;
				for (int i = nextPageLength - 1; i >= nextPageLength - 2; i--) {
					TagNode nextPage = (TagNode) nextPages[i];
					if (nextPage.getChildren().iterator().next().toString()
							.trim().equals("Next")) {
						nextURL = Html.fromHtml(
								nextPage.getAttributeByName("href")).toString();
						TagNode lastPage = (TagNode) nextPages[i - 1];
						lastURL = Html.fromHtml(
								lastPage.getAttributeByName("href")).toString();
					}
				}
			}
			parseLastPageFromPostURL();
			int numPosts = posts.length;
			handler.sendEmptyMessage(TLHandler.PROGRESS_RENDERING);
			viewList = new View[numPosts * 2];
			for (int i = 0; i < numPosts; i++) {
				TagNode post = (TagNode) posts[i];
				TagNode postName = (TagNode)postNames[i];
				int contentType = postContentType[i];
				if (postName.getAttributeByName("href") == null) {
					if (contentType == POST_CONTENT_TYPE_DEFAULT){
						renderUBB.renderHeader(postHeaders[i], Integer.parseInt(postName.getAttributeByName("name")), topicId, currentPage);
					}
					else if (contentType == POST_CONTENT_TYPE_NEWS) {
						renderUBB.renderNewsHeader(postHeaders[i], Integer.parseInt(postName.getAttributeByName("name")), topicId, currentPage);
					}
					else {
						assert(false);
					}
					viewList[2 * i] = renderUBB.curTextView;
	
					renderUBB.render(post);
					viewList[2 * i + 1] = renderUBB.curTextView;
				}

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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.show_post_menu, menu);
		return true;
	}
	
	private Dialog gotoPageDialog;
	private EditText gotoPageEditText;
	private Button gotoPageButton;
	private Button gotoPageCancelButton;
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
	
	private String buildPostURL(int pageNumber){
		String temp = postURL;
		int c = postURL.indexOf('?');
		if (c != -1){
			temp = postURL.substring(0, c);
		}
		temp += String.format("?topic_id=%d&currentpage=%d", topicId, pageNumber);
		return temp;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.gotoPage:
			showGotoPageDialog();
			break;
		case R.id.prev:
			if (prevURL != null) {
				loadPostURL(prevURL);
			}
			break;
		case R.id.next:
			if (nextURL != null) {
				loadPostURL(nextURL);
			}
			break;
		case R.id.first:
			if (firstURL != null) {
				loadPostURL(firstURL);
			}
			break;
		case R.id.last:
			if (lastURL != null) {
				loadPostURL(lastURL);
			}
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

	private void loadPostURL(String url) {
		postURL = url;
		parsePostURL();
		refresh(true);
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
				forumPostView.removeAllViews();
				for (View view : viewList) {
					if (view != null)
						forumPostView.addView(view);
				}
				if (resetScrollPosition){
					scrollView.scrollTo(0, 0);
				}
				setTitle(TLLib.makeActivityTitle(String.format("(%d/%d) %s", currentPage, lastPage, postTopic)));
				progressDialog.dismiss();
			} else {
				super.handleMessage(msg);
			}
		}
	};

}
