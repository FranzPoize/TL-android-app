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

package org.opensourcetlapp.tl.Structs;

import java.util.List;

import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.opensourcetlapp.tl.HtmlTools;


import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.os.ParcelableCompat;

public class PostInfo implements Parcelable {
	public String topicURL;
	public String topicString;
	public String topicStarterString;
	public String repliesString;
	public String viewsString;
	public String lastMessageString; 
	public String imageString;
	public boolean locked;
	
	private static StringBuilder	sb;
	
	public PostInfo(Parcel source) {
		this.topicURL = source.readString();
		this.topicString = source.readString();
		this.topicStarterString = source.readString();
		this.repliesString = source.readString();
		this.viewsString = source.readString();
		this.lastMessageString = source.readString(); 
		this.imageString = source.readString();
	}
	
	public PostInfo() {
	}
	
	public static PostInfo buildPostInfoFromForumEntry(TagNode entry) throws XPatherException{
		PostInfo postInfo = new PostInfo();
		
		if(sb == null) { sb = new StringBuilder(); }
		List <TagNode> children = entry.getChildren();
		
		TagNode imageTagNode = (TagNode) children.get(0).getChildren().get(0);
		TagNode topicTagNode = null;
		if (children.get(1).evaluateXPath("//span").length > 0)
			topicTagNode = (TagNode) children.get(1).getChildren().get(2);
		else
			topicTagNode = (TagNode) children.get(1).getChildren().get(0);
		TagNode topicStarterTagNode = (TagNode) children.get(2);
		TagNode repliesTagNode = (TagNode) children.get(3);
		TagNode viewsTagNode = (TagNode) children.get(4);
		TagNode lastMessageTagNode = (TagNode) children.get(5);
		sb.setLength(0);
		sb.append(imageTagNode.getAttributeByName("src"));
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
	@Override
	public int describeContents() {
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(topicURL);
		dest.writeString(topicString);
		dest.writeString(topicStarterString);
		dest.writeString(repliesString);
		dest.writeString(viewsString);
		dest.writeString(lastMessageString); 
		dest.writeString(imageString);
	}
	
	public static final Parcelable.Creator<PostInfo> CREATOR = new Parcelable.Creator<PostInfo>()
	{
	    @Override
	    public PostInfo createFromParcel(Parcel source)
	    {
	        return new PostInfo(source);
	    }
	 
	    @Override
	    public PostInfo[] newArray(int size)
	    {
	    return new PostInfo[size];
	    }
	};
}
