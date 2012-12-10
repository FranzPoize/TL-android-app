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
package org.opensourcetlapp.tl.Adapters;

import java.util.List;

import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.opensourcetlapp.tl.CustomImageGetter;
import org.opensourcetlapp.tl.R;
import org.opensourcetlapp.tl.Structs.PostInfo;


import android.content.Context;
import android.graphics.Color;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MyPostsAdapter extends BaseAdapter{
	private class ViewHolder {
		public TextView topic;
		public TextView topicStarter;
		public TextView replies;
		public TextView lastMessage;
	}
	
	private LayoutInflater mInflater;

	private List <PostInfo>postInfoList;
	private Context context;
	
	private static final String TAG = "ForumAdapter";
	
	public MyPostsAdapter(List <PostInfo>postInfoList, Context context){
		this.postInfoList = postInfoList;
		this.context = context;
		mInflater = LayoutInflater.from(context);
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return postInfoList.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return postInfoList.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		
		if (convertView == null){
			convertView = mInflater.inflate(R.layout.my_posts_row, null);
			
			holder = new ViewHolder();
			holder.topic = (TextView)convertView.findViewById(R.id.topic);
			holder.topicStarter = (TextView)convertView.findViewById(R.id.topicStarter);
			holder.replies = (TextView)convertView.findViewById(R.id.replies);
			holder.lastMessage = (TextView)convertView.findViewById(R.id.lastMessage);
			
			convertView.setTag(holder);
		}
		else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		PostInfo postInfo = postInfoList.get(position);

		holder.topic.setText(postInfo.topicString);
		holder.topicStarter.setText(postInfo.topicStarterString);
		holder.replies.setText(postInfo.repliesString);	
		holder.lastMessage.setText(postInfo.lastMessageString);

		return convertView;
	}

}
