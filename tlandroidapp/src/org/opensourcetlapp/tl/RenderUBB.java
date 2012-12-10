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
import java.util.Hashtable;
import java.util.Iterator;

import org.htmlcleaner.ContentToken;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.htmlcleaner.XmlSerializer;
import org.opensourcetlapp.tl.widget.DisplayableLinkSpan;
import org.opensourcetlapp.tl.widget.EditSpan;
import org.opensourcetlapp.tl.widget.QuotePostSpan;
import org.opensourcetlapp.tl.widget.SpoilerSpan;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
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
import android.widget.TextView;
import android.widget.TextView.BufferType;



public class RenderUBB {
	private static final String TAG = "RenderUBB";
	public TextView curTextView;
	private Editable curEditable;
	private SpoilerSpan curSpoilerSpan;
	private SpoilerSpan parentSpoilerSpan;
	private Context context;	// THIS VARIABLE SHOULD NOT BE MADE STATIC FOR ANY REASON
	private XmlSerializer serializer;
	private CustomImageGetter imageGetter;
	private Hashtable<String, Integer> colorDictionary = new Hashtable<String, Integer>();
	private TagNode node;
	private TextView globalTextView;

	public RenderUBB(Context context, TagNode node){
		this.context = context;
		serializer = TLLib.getXmlSerializer();
		//imageGetter = new CustomImageGetter(context);
		this.node = node;
		
		colorDictionary.put(" red", new Integer(Color.RED)); // The white space																// is necessary
		colorDictionary.put(" blue", new Integer(Color.BLUE));
		colorDictionary.put(" green", new Integer(Color.parseColor("#008800")));
	}

	private void makeHeaderTextView() {
		curTextView = new TextView(context);
		curTextView.setBackgroundResource(R.drawable.header_background);
		curTextView.setTextColor(Color.BLACK);
		curTextView.setText("", BufferType.EDITABLE);
		curTextView.setPadding(5, 5, 5, 5);
		
		curEditable = (Editable) curTextView.getText();

		MovementMethod m = curTextView.getMovementMethod();
		if ((m == null) || !(m instanceof LinkMovementMethod)) {
			curTextView.setMovementMethod(LinkMovementMethod.getInstance());
		}
	}
	
	private void makeTextView() {
		curTextView = new TextView(context);
		curTextView.setBackgroundColor(Color.parseColor("#d9dde0"));
		curTextView.setTextColor(Color.BLACK);
		curTextView.setText("", BufferType.EDITABLE);
		curTextView.setPadding(5, 5, 5, 5);

		curEditable = (Editable) curTextView.getText();

		MovementMethod m = curTextView.getMovementMethod();
		if ((m == null) || !(m instanceof LinkMovementMethod)) {
			curTextView.setMovementMethod(LinkMovementMethod.getInstance());
		}
	}
	
	public void renderNewsHeader(Object newsHeaderNode, int postId, int topicId, int currentPage) throws IOException{
		makeHeaderTextView();
		TagNode header = (TagNode)newsHeaderNode;
		curTextView.append(Html.fromHtml(serializer.getXmlAsString(header), imageGetter, null));
	}

	public void renderHeader(Object postHeaderNode, int postId, int topicId, int currentPage) throws IOException {
		makeHeaderTextView();
		TagNode header = (TagNode) postHeaderNode;
		TagNode scriptNode = header.findElementByName("script", false);
		
		String script = "";
		if (scriptNode != null){ // Needed to handle header images of Liquid`Nazgul and Evil Teletuby
			Iterator i = scriptNode.getChildren().iterator();
			while (i.hasNext()){
				script = i.next().toString();
				if (script.contains("img")) {
					script = script.replace("\\", "");
					
					int start = script.indexOf('\'');
					int end = script.indexOf('\'', start+1);
					
					script = script.substring(start+1, end);
					script = String.format("<img src=\"%s\" />", script);
					break;
				}
			}
			scriptNode.removeFromTree();
		}
		
		try {
			TagNode spanNode = (TagNode)(header.evaluateXPath("./span")[0]);
			if (!spanNode.hasAttribute("class")
					|| spanNode.getAttributeByName("class").equals("forummsginfoa")) {
				curTextView.setBackgroundColor(Color.parseColor("#3f5e8d"));
			}	
		} catch (XPatherException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
		}

		
		int start;
		int end;
		
		String headerString = script + serializer.getXmlAsString(header); 
		curTextView.append(Html.fromHtml(script));
		renderTagNode(header);
		//curTextView.append(Html.fromHtml(headerString, // Does not recursively apply HTML formatting =(
				//imageGetter, null));
		
		Editable editable = (Editable)curTextView.getText();
		if (TLLib.loginStatus){
			editable.append("  ");
			start = editable.length();
			editable.append("Quote");
			end = editable.length();
			editable.setSpan(new QuotePostSpan(context, postId, topicId), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		
		if (TLLib.loginStatus && headerString.matches(String.format("(?idsmu:.*%s.*)", TLLib.loginName))){
				editable.append("  ");
				start = editable.length();
				editable.append("Edit");
				end = editable.length();
				editable.setSpan(new EditSpan(context, postId, topicId, currentPage), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
	}
	
	public void render() throws IOException{
		render(node);
	}
	
	public void render(TagNode node) throws IOException {
		makeTextView();
		globalTextView = curTextView;
		renderRec(node);
	}
	
	private void renderRec(Object node) throws IOException {
		if (node.getClass() == TagNode.class) {
			renderTagNode((TagNode) node);
		} else if (node.getClass() == ContentToken.class) {
			renderContentToken((ContentToken) node);
		}
	}

	private void renderChildren(TagNode node) throws IOException {
		for (Object nextNode : node.getChildren()) {
			renderRec(nextNode);
		}
	}

	private void renderTagNode(TagNode node) throws IOException {
		String nodeString = node.toString();

		if (nodeString.equals("br")) {
			curEditable.append("\n");
			renderChildren(node);
		} else if (nodeString.equals("b") || nodeString.equals("strong")) {
			int start = curEditable.length();
			renderChildren(node);
			int end = curEditable.length();
			((Spannable) curEditable).setSpan(new StyleSpan(
					android.graphics.Typeface.BOLD), start, end,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			curEditable.append(" ");
		} else if (nodeString.equals("i")) {
			int start = curEditable.length();
			renderChildren(node);
			int end = curEditable.length();
			((Spannable) curEditable).setSpan(new StyleSpan(
					android.graphics.Typeface.ITALIC), start, end,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			curEditable.append(" ");
		} else if (nodeString.equals("font")) {
			int start = curEditable.length();
			renderChildren(node);
			int end = curEditable.length();

			int size;
			if (node.hasAttribute("size")) {
				size = Integer.parseInt(node.getAttributeByName("size"));
				if (size == 4) {
					((Spannable) curEditable).setSpan(new TextAppearanceSpan(
							null, 0, 20, null, null), start, end,
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}

			}
			if (node.hasAttribute("style")) {
				String style = node.getAttributeByName("style");
				renderStyleAttribute(style, start, end);
			}

		} else if (nodeString.equals("div") && node.hasAttribute("id") && (node.getAttributeByName("id").contains("spoiler") || node.getAttributeByName("id").contains("Quote"))) {
			TextView temp = curTextView;
			Editable tempEditable = curEditable;
			makeTextView();
			curEditable.append("\n");
			SpoilerSpan tempSpoilerSpan = curSpoilerSpan;
			SpoilerSpan tempParentSpoilerSpan = parentSpoilerSpan;	// In retrospect perhaps I should have just gone with an actually recursive implementation
			parentSpoilerSpan = curSpoilerSpan;
			renderChildren(node);
			curSpoilerSpan = tempSpoilerSpan;
			parentSpoilerSpan = tempParentSpoilerSpan; 
			curSpoilerSpan.setTagNode(node);
			curSpoilerSpan.setCharSequence((CharSequence) curTextView.getText());
			curTextView = temp;
			curEditable = tempEditable;
		} else if (nodeString.equals("div") && node.hasAttribute("class") && node.getAttributeByName("class").equals("quote")){
			int start = curEditable.length();
			renderChildren(node);
			int end = curEditable.length();
			((Spannable) curEditable).setSpan(new QuoteSpan(R.color.TLBlueBlack), start,
					end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			((Spannable) curEditable).setSpan(new LeadingMarginSpan.Standard(10), start,
					end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			((Spannable) curEditable).setSpan(new TextAppearanceSpan(null,
					0, 12, null, null), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		
		else if (nodeString.equals("div")) {
			curEditable.append("\n");
			int start = curEditable.length();
			renderChildren(node);
			int end = curEditable.length();

			if (node.hasAttribute("style")) {
				String style = node.getAttributeByName("style");
				renderStyleAttribute(style, start, end);
			}
		} 
		else if (nodeString.equals("a")) {
			if (node.hasAttribute("onclick") ) { // spoiler tag
				String txt ="";
				//the spoiler 'a' tag contains two span like this <span>+ Show </span> Spoiler<span> + </span>
				if (node.getAttributeByName("onclick").contains("ShowSpoiler")) {
					Iterator itTagNode = node.getChildren().iterator();
					TagNode firstSpan = (TagNode)itTagNode.next();
					String middleString = HtmlTools.unescapeHtml(itTagNode.next().toString().trim());
					TagNode thirdSpan = (TagNode)itTagNode.next();
					txt=  HtmlTools.unescapeHtml(firstSpan.getChildren().iterator().next().toString().trim())
							+ " " +middleString + " "
							+HtmlTools.unescapeHtml(thirdSpan.getChildren().iterator().next().toString().trim());
				} else { //this is for the nested quotes
					txt = HtmlTools.unescapeHtml(node.getChildren().iterator().next().toString().trim());
				}
				
				int start = curEditable.length();
				curTextView.append(txt);
				int end = curEditable.length();

				curSpoilerSpan = new SpoilerSpan(end, globalTextView);
				if (parentSpoilerSpan != null){
					parentSpoilerSpan.spoilerSpanList.add(curSpoilerSpan);
				}
				((Spannable) curEditable).setSpan(curSpoilerSpan, start, end,
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			} else if (node.hasAttribute("href")) {
				int start = curEditable.length();
				renderChildren(node);
				int end = curEditable.length();
				String link = node.getAttributeByName("href");

				if (DisplayableLinkSpan.isDisplayable(link)) {
					((Spannable) curEditable).setSpan(new DisplayableLinkSpan(
							link, context), start, end,
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				} else {
					if (link.charAt(0) == '/') {
						link = TLLib.getAbsoluteURL(link);
					}
					((Spannable) curEditable).setSpan(new URLSpan(link), start,
							end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				curTextView.append(" ");
			}
		} else if (nodeString.equals("img")) {
			if (Config.showImages) {
				if (node.getAttributeByName("src").charAt(0) == '/' || node.getAttributeByName("src").startsWith("http://www.teamliquid.net")) {
					curTextView.append(Html.fromHtml(serializer
							.getXmlAsString(node), imageGetter,
							null));
				} else {
					String imageLink = "<a href=\""
							+ node.getAttributeByName("src")
							+ "\">View Image</a><br/>";
					curTextView.append(Html.fromHtml(imageLink));
				}
			}
		} else if (nodeString.equals("center")) {
			int start = curEditable.length();
			renderChildren(node);
			int end = curEditable.length();
			Spannable span = (Spannable) curEditable;
			span.setSpan(new AlignmentSpan.Standard(
					Layout.Alignment.ALIGN_CENTER), start, end,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		} else if (nodeString.equals("hr")) {
			curTextView.append("\n----------------\n");
		} else if (nodeString.equals("p")) {
			curTextView.append("\n");
			renderChildren(node);
			curTextView.append("\n");
		} else if (nodeString.equals("object")) {
			String youtubeURL = node.getAttributeByName("data");
			String imageLink = String.format("<a href=\"%s\">Watch Video</a>",
					youtubeURL);
			curTextView.append(Html.fromHtml(imageLink));
		} else if (nodeString.equals("table") || nodeString.equals("tbody")
				|| nodeString.equals("tr") || nodeString.equals("td")) {
			renderChildren(node);
		} else if (nodeString.equals("ul")) {
			curTextView.append("\n");
			renderChildren(node);
			curTextView.append("\n");
		} else if (nodeString.equals("li")) {
			int start = curEditable.length();
			renderChildren(node);
			int end = curEditable.length();
			Spannable span = (Spannable) curEditable;
			span.setSpan(new BulletSpan(), start, end,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		} else if (nodeString.equals("s")) {
			int start = curEditable.length();
			renderChildren(node);
			int end = curEditable.length();
			((Spannable) curEditable).setSpan(new StrikethroughSpan(), start,
					end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			curTextView.append(" ");
		} 
		else if (nodeString.equals("u")){
			int start = curEditable.length();
			renderChildren(node);
			int end = curEditable.length();
			((Spannable) curEditable).setSpan(new UnderlineSpan(), start,
					end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			curTextView.append(" ");
		}
		else if (nodeString.equals("span")){
			int start = curEditable.length();
			renderChildren(node);
			int end = curEditable.length();
			if (node.hasAttribute("style")){
				renderStyleAttribute(node.getAttributeByName("style"), start, end);
			}
		}
		else {
			Spanned span = Html.fromHtml(node.toString(), null, null);
			curTextView.append(span);
			renderChildren(node);
		}

	}

	private void renderStyleAttribute(String style, int start, int end) {
		String[] styleList = style.split(";");
		for (String s : styleList) {
			String[] styleInfo = s.split(":");
			if (styleInfo[0].trim().equals("color")) {
				if (colorDictionary.containsKey(styleInfo[1])) {
					((Spannable) curEditable).setSpan(new ForegroundColorSpan(
							colorDictionary.get(styleInfo[1]).intValue()),
							start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				else if (styleInfo[1].contains("#")){	
					styleInfo[1] = styleInfo[1].trim();
					if (styleInfo[1].length() == 4){
						String r, g, b;
						r = styleInfo[1].charAt(1)+"";
						g = styleInfo[1].charAt(2)+"";
						b = styleInfo[1].charAt(3)+"";
						String colorString = "#" + r + r + g + g + b + b;
						((Spannable) curEditable).setSpan(new ForegroundColorSpan(
								Color.parseColor(colorString)),
								start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						
					}else {
						((Spannable) curEditable).setSpan(new ForegroundColorSpan(
								Color.parseColor(styleInfo[1].trim())),
								start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					}

					
				}

			} else if (styleInfo[0].trim().equals("text-align")) {
				Spannable span = (Spannable) curEditable;
				span.setSpan(new AlignmentSpan.Standard(
						Layout.Alignment.ALIGN_CENTER), start, end,
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			} else if (styleInfo[0].trim().equals("float")) {
				if (styleInfo[1].trim().equals("right")) {
					Spannable span = (Spannable) curEditable;
					span.setSpan(new AlignmentSpan.Standard(
							Layout.Alignment.ALIGN_OPPOSITE), start, end,
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
			} else if (styleInfo[0].trim().equals("font-size")) {
				try {
					int size = Integer.parseInt(((styleInfo[1].split("p"))[0])
						.trim());
					Spannable span = (Spannable) curEditable;
					((Spannable) curEditable).setSpan(new TextAppearanceSpan(null,
						0, size, null, null), start, end,
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				} catch (NumberFormatException e){
					
				}
			}

		}
	}

	private void renderContentToken(ContentToken token) {
		ContentToken childToken = (ContentToken) token;
		Spanned span = Html.fromHtml(token.toString(), null, null);

		curTextView.append(span);
	}
}
