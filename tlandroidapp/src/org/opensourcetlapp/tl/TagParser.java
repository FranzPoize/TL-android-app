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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Debug;
import android.util.Log;

public class TagParser {
	private static int end = 0;
	private static char [] charBuffer = new char[4096];
	private static final String TAG = "TagParser";
	private static boolean DEBUG = false;
	
	private static final String SCRIPT_TAG_NAME = "script";
	
		private static void printTimeStamp(){
			Date date = new Date();
			Log.d(TAG, date.toGMTString());
		}
	
	   public static boolean extractTagToFile(String fullTag, InputStream is, BufferedWriter bw) throws IOException{
		 //  printTimeStamp();
		   InputStreamReader as = new InputStreamReader(is, "UTF-8");
		   BufferedReader br = new BufferedReader(as);
		   boolean found = false;
		   String tagName = parseTagName(fullTag);
			while (findNextTag(br, null)){
				printBuffer();
				if (bufferCompare(fullTag)){
					found = true;
					writeBuffer1(bw);
					break;
				}
				else if (bufferTagNameCompare(SCRIPT_TAG_NAME)){
					passThroughUntilClosingTag(br, null, SCRIPT_TAG_NAME);
				}
				else {
				}
			}
			
			int count = 1;
			while (findNextTag(br, bw)){
				writeBuffer1(bw);
				if (bufferTagNameCompare(tagName)){
					if (parseTagType() == OPEN_TAG){
						count ++;
					}
					else {
						count--;
					}
				}
				else if (bufferTagNameCompare(SCRIPT_TAG_NAME)){
					passThroughUntilClosingTag(br, bw, SCRIPT_TAG_NAME);
				}
				
				if (count == 0){
					break;
				}
			}
			return found;
		//	printTimeStamp();
	    }
	    
	    private static void passThroughUntilClosingTag(BufferedReader br, BufferedWriter bw,
			String tagName) throws IOException {
	    	int pos = 0;
	    	char [] closeTag = ("</"+tagName+">").toCharArray();
	    	while (true){
	    		char c = (char) br.read();
	    		if (bw != null){
	    			bw.write(c);
	    		}
	    		if (c == closeTag[pos]){
	    			if (pos == 0){
	    				br.mark(4096);
	    			}
	    			else if (pos == closeTag.length-1){
	    				break;
	    			}
	    			pos++;
	    		}
	    		else if (pos > 0){
	    			pos = 0;
	    			br.reset();
	    		}
	    	}
	    }

		public static boolean findNextTag(BufferedReader br, BufferedWriter bw) throws IOException{
			int startChar;
			int endChar;
			end = 0;
			int bufferChar = 0;
			
			boolean first= false;
			boolean jump= false;
			
			while (true){
				if (!jump)
					startChar = br.read();
				else {
					startChar = bufferChar;
					jump = false;
				}
				if (startChar == '<'){
					bufferChar = br.read();
					if(bufferChar != ' ') {
						charBuffer[end] = (char) startChar;
						end++;
						first = true;
						break;
					} else {
						jump = true;
						if (bw != null) {
							bw.write(startChar);
							bw.write(bufferChar);
						}
					}
				} 
				else if (startChar == -1) return false;
				else if (bw != null){
					bw.write(startChar);
				}
			}
			int count = 1;
			while (true){
				if (!first) {
					endChar = br.read();
				} else {
					endChar = bufferChar;
					first = false;
				}
				charBuffer[end] = (char)endChar;
				end++;
				if (endChar == '>'){
					if(charBuffer[end-1] != ' ') {
						count --;
					}
				}
				else if (endChar == '<'){
					bufferChar = br.read();
					if(bufferChar != ' ') {
						count++;
					}
					first = true;
				}
				else if (endChar == -1) return false;
				if (count == 0){
					break;
				}
			}
			return true;
		}
	 
		private static Pattern p = Pattern.compile("^</?([a-zA-Z0-9]*)");
		
	    public static String parseTagName(CharSequence tag){
	    	Matcher m = p.matcher(tag);
	    	m.find();
	    	return m.group(1);
	    }
	    
	    public static int OPEN_TAG = 0;
	    public static int CLOSE_TAG = 1;
	    
	    public static int parseTagType(){
	    	if (charBuffer[1] == '/'){
	    		return CLOSE_TAG;
	    	}
	    	else {
	    		return OPEN_TAG;
	    	}
	    }
	    
	    public static boolean bufferTagNameCompare(String tagName){
	    	int charBufferPos = 0;
	    	int c = charBuffer[charBufferPos];
	    	if (c != '<'){
	    		return false;
	    	}
	    	charBufferPos++;
	    	c = charBuffer[charBufferPos];

	    	if (c == '/'){
	    		charBufferPos++;
	    	}
	    	
	    	charBufferPos--;
	    	int tagNamePos = -1;
	    	while (true) {
	    		tagNamePos++;
	    		charBufferPos++;
	    		
	    		if (tagNamePos >= tagName.length() || charBufferPos >= charBuffer.length){
	    			break;
	    		}
	    		while (Character.isWhitespace(tagName.charAt(tagNamePos)) || tagName.charAt(tagNamePos) == '\'' || tagName.charAt(tagNamePos) == '"'){
	    			tagNamePos++;
	    			if (tagNamePos >= tagName.length()){
	    				return false;
	    			}
	    		}
	    		
	    		while (Character.isWhitespace(charBuffer[charBufferPos]) || charBuffer[charBufferPos] == '\'' || charBuffer[charBufferPos] == '"'){
	    			charBufferPos++;
	    			if (charBufferPos >= charBuffer.length){
	    				return false;
	    			}
	    		}

	    		char upperCase = Character.toUpperCase(charBuffer[charBufferPos]);
	    		char lowerCase = Character.toLowerCase(charBuffer[charBufferPos]);
	
	    		if (lowerCase != tagName.charAt(tagNamePos) && upperCase != tagName.charAt(tagNamePos)){
	    			return false;
	    		}
	    	}

	    	return true;
	    }
	    
	    private static boolean bufferCompare(String target){
	    	if (end != target.length()){
	    		return false;
	    	}
	    	for (int i = 0; i < end; i++){
	    		char c = Character.toUpperCase(charBuffer[i]);
	    		char d = Character.toLowerCase(charBuffer[i]);
	
	    		if (c != target.charAt(i) && d != target.charAt(i)){
	    			return false;
	    		}
	    	}
	    	return true;
	    }
	    
	    private static void writeBuffer1(BufferedWriter bw) throws IOException{
	    	for (int i = 0 ; i < end; i++){
	    		bw.write(charBuffer[i]);
	    	}
	    }
	    
	    private static void writeBuffer2(BufferedWriter bw) throws IOException{
	    	bw.write(charBuffer, 0, end);
	    }
	    
	    private static void printBuffer(){
	    	if (!DEBUG){
	    		return;
	    	}
	    	Log.d(TAG, String.copyValueOf(charBuffer, 0, end));
	    }
	    
	    
}
