/*
 * Copyright 2010, 2011 Ali Piccioni
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

package com.thoughtmetric.tl.Structs;

import java.io.IOException;
import java.util.List;

import org.htmlcleaner.SimpleXmlSerializer;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.htmlcleaner.XmlSerializer;

import com.thoughtmetric.tl.HtmlTools;

import android.text.Html;
import android.util.Log;

public class PostInfo {
	public String topicURL;
	public String topicString;
	public String topicStarterString;
	public String repliesString;
	public String viewsString;
	public String lastMessageString; 
	public String imageString;
	public boolean locked;
	
	private static StringBuilder	sb;
	public static PostInfo buildPostInfoFromForumEntry(TagNode entry) throws XPatherException{
		PostInfo postInfo = new PostInfo();
		
		if(sb == null) { sb = new StringBuilder(); }
		List <TagNode> children = entry.getChildren();
		
		TagNode imageTagNode = (TagNode) children.get(0).getChildren().get(0);
		TagNode topicTagNode = (TagNode) children.get(1).getChildren().get(2);
		TagNode topicStarterTagNode = (TagNode) children.get(2);
		TagNode repliesTagNode = (TagNode) children.get(3);
		TagNode viewsTagNode = (TagNode) children.get(4);
		TagNode lastMessageTagNode = (TagNode) children.get(5);
		sb.setLength(0);
		sb.append("<img src='");
		sb.append(imageTagNode.getAttributeByName("src"));
		sb.append("' />");
		//postInfo.imageString = String.format("<img src='%s' />", (imageTagNode.getAttributeByName("src")));
		postInfo.imageString = sb.toString();
		postInfo.locked = imageTagNode.getAttributeByName("alt").equals("Locked");
		postInfo.topicURL = ((TagNode)topicTagNode).getAttributeByName("href");
		postInfo.topicString = HtmlTools.unescapeHtml(((TagNode)topicTagNode).getChildren().get(0).toString());
		
		postInfo.topicStarterString = ((TagNode)topicStarterTagNode).getChildren().get(0).toString();
		postInfo.repliesString = ((TagNode)repliesTagNode).getChildren().get(0).toString();
		postInfo.viewsString = ((TagNode)viewsTagNode).getChildren().get(0).toString();
		postInfo.lastMessageString = ((TagNode)lastMessageTagNode).getChildren().get(0).toString();
		return postInfo;
	}
}
