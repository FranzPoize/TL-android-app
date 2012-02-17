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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.SimpleXmlSerializer;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.htmlcleaner.XmlSerializer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.text.Html;
import android.util.Log;

public class TLLib {
	public static final int PROGRESS_CONNECTING = 100;
	public static final int PROGRESS_DOWNLOADING = 101;
	public static final int PROGRESS_PARSING = 102;
	private static final String TAG = "TLLib";
	private static final String TEMP_FILE_NAME = "TLLIBTEMP";
	private static final String LOGIN_URL = "http://www.teamliquid.net/mytlnet/login.php";
	private static final String LOGOUT_URL = "http://www.teamliquid.net/mytlnet/logout.php";	
	private static final String CREATE_NEW_THREAD_URL = "http://www.teamliquid.net/forum/addtopic.php";
	private static final String CREATE_NEW_BLOG_POST_URL = "http://www.teamliquid.net/blogs/addblogentry.php";
	private static final String POST_URL = "http://www.teamliquid.net/forum/postmessage.php";
	private static final String PM_URL = "http://www.teamliquid.net/mytlnet/index.php";
	private static final String EDIT_URL = "http://www.teamliquid.net/forum/edit.php";
	private static final String USER_FIELD = "loginname";
	private static final String PASS_FIELD = "loginpasswd";
	private static final String REMEMBERME = "makeAcookie";
	private static CookieStore cookieStore;
	
	public static boolean loginStatus = false;
	public static String loginName;
	private static String tokenField;
	
	private static HtmlCleaner cleaner = null; 
	private static XmlSerializer serializer;
	
	public static HtmlCleaner buildDefaultHtmlCleaner(){
		if (cleaner == null){
			cleaner = new HtmlCleaner();
			CleanerProperties props = cleaner.getProperties();
			props.setAllowHtmlInsideAttributes(true);
			props.setAllowMultiWordAttributes(true);
			props.setRecognizeUnicodeChars(true);
			props.setOmitComments(true);
		}
        //props.setPruneTags("script");
        return cleaner;
	}
	
	public static XmlSerializer getXmlSerializer(){
		if (serializer == null){
			buildDefaultHtmlCleaner();
			serializer = new SimpleXmlSerializer(cleaner.getProperties());
		}
		return serializer;
	}

	public static String getAbsoluteURL(String relativeURL){
		if (relativeURL.charAt(0) == '/'){
			return Config.DOMAIN_NAME + relativeURL;
		}
		else {
			return Config.DOMAIN_NAME + "/" + relativeURL;
		}
	}
	
	public static boolean login(String login, String pw, Handler handler, Context context) throws IOException{
		handler.sendEmptyMessage(TLHandler.PROGRESS_LOGIN);
		logout();
		
		// Fetch the token
		HtmlCleaner cleaner = TLLib.buildDefaultHtmlCleaner();
		URL url = new URL(LOGIN_URL);
		TagNode node = TagNodeFromURLEx2(cleaner, url, handler, context, "<html>", false);
		//TagNode node = TLLib.TagNodeFromURLLoginToken(cleaner, url, handler, context);
		
		String token = null;
		try {
			TagNode result = (TagNode) (node.evaluateXPath("body/table/tbody/tr/td/table[2]/tbody/tr/td[2]/table/tbody/tr[2]/td/table/tbody/tr/td/table/tbody/tr[2]/td/form/input")[0]);
			token = result.getAttributeByName("value");
		} catch (XPatherException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		if (token == null){
			return false;
		}
		// 
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpPost httpost = new HttpPost(LOGIN_URL);

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair(USER_FIELD, login));
        nvps.add(new BasicNameValuePair(PASS_FIELD, pw));
        nvps.add(new BasicNameValuePair(REMEMBERME, "1"));
        nvps.add(new BasicNameValuePair("stage", "1"));
        nvps.add(new BasicNameValuePair("back_url", "/"));
        nvps.add(new BasicNameValuePair("token", token));
		Log.d("token:", token);
		tokenField = token;
		
		if (cookieStore != null) {
			httpclient.setCookieStore(cookieStore);
		}

        try {
			httpost.setEntity(new UrlEncodedFormEntity(nvps));
			HttpResponse response = httpclient.execute(httpost);
			HttpEntity entity = response.getEntity();
			
			Header [] headers = response.getHeaders("Set-Cookie");
			if ( cookieStore.getCookies().size() < 2 ){
				loginName = null;
				loginStatus = false;
			}
			else {
				loginName = login;
				loginStatus = true;
				cookieStore = httpclient.getCookieStore();
			}
			
			if (entity != null) {
				entity.consumeContent();
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return loginStatus;
	}
	
	public static void logout(){
		DefaultHttpClient httpclient = new DefaultHttpClient();
		httpclient.setCookieStore(cookieStore);
		
		HttpGet httpGet = new HttpGet(LOGOUT_URL+ "?t=" +tokenField);
		
		try {
			httpclient.execute(httpGet);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		loginStatus = false;
		cookieStore = null;
	}
	
	public static void sendPM(String to, String subject, String message) throws IOException{
        DefaultHttpClient httpclient = new DefaultHttpClient();
		httpclient.setCookieStore(cookieStore);
        HttpPost httpost = new HttpPost(PM_URL);

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("to", to));
        nvps.add(new BasicNameValuePair("subject", subject));
        nvps.add(new BasicNameValuePair("body", message));
        nvps.add(new BasicNameValuePair("view", "Send"));
        nvps.add(new BasicNameValuePair("token", tokenField));
        Log.d(TAG, "Sending message");
        Log.d(TAG, to);
        Log.d(TAG, subject);
        Log.d(TAG, message);
       
        try {
			httpost.setEntity(new UrlEncodedFormEntity(nvps));
			HttpResponse response = httpclient.execute(httpost);
			HttpEntity entity = response.getEntity();
			
			if (entity != null) {
				entity.consumeContent();
			}
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		}
	}
	
	public static void postMessage(String message, String backurl, String topicId, Context context) throws IOException{
        DefaultHttpClient httpclient = new DefaultHttpClient();
		httpclient.setCookieStore(cookieStore);
        HttpPost httpost = new HttpPost(POST_URL);

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("bericht", message));
        nvps.add(new BasicNameValuePair("stage", "1"));
        nvps.add(new BasicNameValuePair("backurl", backurl));
        nvps.add(new BasicNameValuePair("token", tokenField));
        nvps.add(new BasicNameValuePair("topic_id", topicId));
        nvps.add(new BasicNameValuePair("submit_button", "Post"));
        
        try {
			httpost.setEntity(new UrlEncodedFormEntity(nvps));
			HttpResponse response = httpclient.execute(httpost);
			HttpEntity entity = response.getEntity();
			
			if (entity != null) {
				entity.consumeContent();
			}
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		}
	}
	
	public static void editMessage(String message, int topicId, int postId, int currentPage, String token, Context context) throws IOException{
        DefaultHttpClient httpclient = new DefaultHttpClient();
		httpclient.setCookieStore(cookieStore);
		
        HttpPost httpost = new HttpPost(EDIT_URL);

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("action", "edit"));
        nvps.add(new BasicNameValuePair("token", token));
        nvps.add(new BasicNameValuePair("stage", "1"));
        nvps.add(new BasicNameValuePair("topic_id", Integer.toString(topicId)));
        nvps.add(new BasicNameValuePair("post_id", Integer.toString(postId)));
        nvps.add(new BasicNameValuePair("currentPage", Integer.toString(currentPage)));
        nvps.add(new BasicNameValuePair("content", message));
        nvps.add(new BasicNameValuePair("submit_button", "Update"));

        
        try {
			httpost.setEntity(new UrlEncodedFormEntity(nvps));
			HttpResponse response = httpclient.execute(httpost);
			HttpEntity entity = response.getEntity();
			
			if (entity != null) {
				entity.consumeContent();
			}
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		}
	}
	
    private static void writeEntitytoFile(HttpEntity entity, Context context) throws IllegalStateException, IOException{
		InputStream is = entity.getContent();
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(context.openFileOutput("TEMP.html", Context.MODE_PRIVATE)));
		String line;
		while ((line = br.readLine()) != null){
			bw.write(line);
		}
    }
    
    private static TagNode TagNodeFromURLHelper(InputStream is, String fullTag, Handler handler, Context context, HtmlCleaner cleaner) throws IOException{
		SharedPreferences settings = context.getSharedPreferences(Settings.SETTINGS_FILE_NAME, 0);
    	boolean disableSmartParsing = settings.getBoolean(Settings.DISABLE_SMART_PARSING, false);
		if (fullTag != null && !disableSmartParsing){
			FileOutputStream fos = context.openFileOutput(TEMP_FILE_NAME, Context.MODE_WORLD_WRITEABLE);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
			TagParser.extractTagToFile(fullTag, is, bw);
			bw.flush();
			bw.close();
			if (handler != null)
				handler.sendEmptyMessage(PROGRESS_PARSING);
			
			return cleaner.clean(context.openFileInput(TEMP_FILE_NAME));
		} else{
			if (handler != null)
				handler.sendEmptyMessage(PROGRESS_PARSING);
			return cleaner.clean(is);
		}
    }
	
	public static TagNode TagNodeFromURLEx2(HtmlCleaner cleaner, URL url,
			Handler handler, Context context, String fullTag, boolean login) throws IOException {

			handler.sendEmptyMessage(PROGRESS_CONNECTING);
			
			DefaultHttpClient httpclient = new DefaultHttpClient();
			if (cookieStore != null){
				httpclient.setCookieStore(cookieStore);
			}
			HttpGet httpGet = new HttpGet(url.toExternalForm());
			HttpResponse response = httpclient.execute(httpGet);
			if (cookieStore == null){
				cookieStore = httpclient.getCookieStore();
			}
			
			handler.sendEmptyMessage(PROGRESS_DOWNLOADING);
			HttpEntity httpEntity = response.getEntity();
			InputStream is = httpEntity.getContent();		
			return TagNodeFromURLHelper(is, fullTag, handler, context, cleaner);
	}
	
	// TODO: Get rid of all these TagNode functions. Their respective Activity classes should call TagNodeFromURLEx2 directly
	public static TagNode TagNodeFromURLShowForumList(HtmlCleaner cleaner, URL url,
			Handler handler, Context context) throws IOException{
			return TagNodeFromURLEx2(cleaner, url, handler, context, "<TABLE cellpadding=0 cellspacing=0 width=\"764\" >", true);
	}

	public static TagNode TagNodeFromURLLoginToken(HtmlCleaner cleaner, URL url, Handler handler, Context context) throws IOException {
		return TagNodeFromURLEx2(cleaner, url, handler, context, "<table cellpadding=\"0\" cellspacing=\"0\" style=\"width: 100%; position: relative; margin-bottom: 4px\">", false);
	}
	
	public static TagNode TagNodeFromURLShowForum(HtmlCleaner cleaner, URL url,
			Handler handler, Context context) throws IOException{
			return TagNodeFromURLEx2(cleaner, url, handler, context, "<table width=\"747\" class=\"solid\" cellspacing=0>", true);
	}	
	
	public static TagNode TagNodeFromURLMyPosts(HtmlCleaner cleaner, URL url,
			Handler handler, Context context) throws IOException{
			return TagNodeFromURLEx2(cleaner, url, handler, context, "<table width='748' cellpadding='3' cellspacing='0' border='0' style='border:1px solid #00005D;'>", true);
	}	
	
	private static final String APPLICATION_TITLE = "Team Liquid";
	
	public static String makeActivityTitle(String subtitle){
		if (subtitle == null || subtitle.length() == 0){
			return APPLICATION_TITLE;
		}
		else {
			return String.format("%s - %s", APPLICATION_TITLE, subtitle);
		}
	}

	public static Object[] parseEditText(HtmlCleaner cleaner, URL url, TLHandler handler, Context context) throws IOException {
		// Although probably not THE worst hack I've written, this function ranks near the top.
		// TODO: rework this routine get rid of code duplication.
		
		DefaultHttpClient httpclient = new DefaultHttpClient();
		httpclient.setCookieStore(cookieStore);
		
		HttpGet httpGet = new HttpGet(url.toExternalForm());
		HttpResponse response = httpclient.execute(httpGet);
		
		
		handler.sendEmptyMessage(PROGRESS_DOWNLOADING);
		InputStream is = response.getEntity().getContent();
		
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		
		FileOutputStream fos = context.openFileOutput(TEMP_FILE_NAME, Context.MODE_PRIVATE);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
				
		String line;
		String formStart = "<form action=\"/forum/edit.php";
		while((line = br.readLine()) != null){
			if (line.startsWith(formStart)){
				Log.d(TAG, line);
				bw.write(line);
				break;
			}
		}
		
		String start = "\t\t<textarea";
		String end = "\t\t<p>";
		StringBuffer sb = new StringBuffer();
		while((line = br.readLine()) != null){
			if (line.startsWith(start)){
				bw.write("</form>");
				int i = line.lastIndexOf('>');
				sb.append(Html.fromHtml(line.substring(i+1)).toString());
				sb.append("\n");
				break;
			}
			else {
				bw.write(line);
			}
		}
		
		while ((line = br.readLine()) != null){
			if (line.startsWith(end)){
				break;
			}		
			sb.append(Html.fromHtml(line).toString());
			sb.append("\n");
		}
		
		bw.flush();
		bw.close();
		
		if (handler != null)
			handler.sendEmptyMessage(PROGRESS_PARSING);
		
		Object []ret = new Object[2];
		
		ret[0] = sb.toString();
		ret[1] = cleaner.clean(context.openFileInput(TEMP_FILE_NAME));
		return ret;
	}
	
	public static String parseQuoteText(HtmlCleaner cleaner, URL url, TLHandler handler, Context context) throws IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		httpclient.setCookieStore(cookieStore);
		
		HttpGet httpGet = new HttpGet(url.toExternalForm());
		HttpResponse response = httpclient.execute(httpGet);
		
		
		handler.sendEmptyMessage(PROGRESS_DOWNLOADING);
		InputStream is = response.getEntity().getContent();
		
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		
		return parseTextArea(br);
	}
	
	public static Object[] tagNodeWithEditText(HtmlCleaner cleaner, URL url,
			Handler handler, Context context, String fullTag, boolean login) throws IOException {
		Object [] ret = new Object[2];

		handler.sendEmptyMessage(PROGRESS_CONNECTING);
		
		DefaultHttpClient httpclient = new DefaultHttpClient();
		if (login){
			httpclient.setCookieStore(cookieStore);
		}
		HttpGet httpGet = new HttpGet(url.toExternalForm());
		HttpResponse response = httpclient.execute(httpGet);
		
		handler.sendEmptyMessage(PROGRESS_DOWNLOADING);
		InputStream is = response.getEntity().getContent();
		BufferedReader br = new BufferedReader(new InputStreamReader(context.openFileInput(TEMP_FILE_NAME)));
		ret[0] =  TagNodeFromURLHelper(is, fullTag, handler, context, cleaner);
		ret[1] = parseTextArea(br);
		
		return ret;
	}
	
	public static String parseTextArea (BufferedReader br) throws IOException{
		String start = "<textarea";
		String end = "</textarea>";
		String line;
		
		StringBuffer sb = new StringBuffer();

		
		while((line = br.readLine()) != null){
			if (line.contains(start)){
				int i = line.indexOf(start);
				int j = line.indexOf('>', i);
				int k = line.length();
				try {
					if (line.contains(end)){
						k = line.indexOf(end); // The case where the tag is closed on the same line
						Log.d(TAG, String.format("%d, %d", j+1, k));
						sb.append(Html.fromHtml(line.substring(j+1, k)).toString());
						return sb.toString();
					}
					else {
						sb.append(Html.fromHtml(line.substring(j+1, k)).toString());
						sb.append("\n");
					}
				}
				catch (StringIndexOutOfBoundsException e) {
					// Do nothing
				}
				break;
			}
		}
		
		while ((line = br.readLine()) != null){
			if (line.contains(end)){
				int i = line.indexOf(end);
				sb.append(Html.fromHtml(line.substring(0, i)).toString());
				break;
			}		
			sb.append(Html.fromHtml(line).toString());
			sb.append("\n");
		}
		
		
		return sb.toString();
		
	}

	public static void postNewThread(int forumCode, String subject,
			String message) throws IOException {
        DefaultHttpClient httpclient = new DefaultHttpClient();
		httpclient.setCookieStore(cookieStore);
		HttpPost httpost;
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();

		if (forumCode == 18){
			httpost = new HttpPost(CREATE_NEW_BLOG_POST_URL);
	        nvps.add(new BasicNameValuePair("topic_name", subject));
	        nvps.add(new BasicNameValuePair("bericht", message));
	        nvps.add(new BasicNameValuePair("stage", "1"));
	        nvps.add(new BasicNameValuePair("submit", "Post Message"));
	        nvps.add(new BasicNameValuePair("type", ""));
		}else {
			httpost = new HttpPost(CREATE_NEW_THREAD_URL);
	        nvps.add(new BasicNameValuePair("topic_name", subject));
	        nvps.add(new BasicNameValuePair("content", message));
	        nvps.add(new BasicNameValuePair("submit", "Post Message"));
	        nvps.add(new BasicNameValuePair("type", String.format("%d", forumCode)));
		}
   
        try {
			httpost.setEntity(new UrlEncodedFormEntity(nvps));
			HttpResponse response = httpclient.execute(httpost);
			HttpEntity entity = response.getEntity();
			
			if (entity != null) {
				entity.consumeContent();
			}
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		}		
	}
	
}
