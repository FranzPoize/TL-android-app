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

import org.opensourcetlapp.tl.R;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.TextView.BufferType;

public class ShowPostAdapter extends BaseAdapter{
	private class ViewHolder {
		public TextView header;
		public TextView post;
	}
	
	private SpannableStringBuilder [] headers;
	private SpannableStringBuilder [] posts;
	private LayoutInflater mInflater;
	private Context context;
	
	public ShowPostAdapter(SpannableStringBuilder [] headers, SpannableStringBuilder [] posts, Context context){
		this.headers = headers;
		this.posts = posts;
		this.context = context;
		mInflater = LayoutInflater.from(context);
	}
	
	@Override
	public int getCount() {
		return posts.length;
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		
		if (convertView == null){
			convertView = mInflater.inflate(R.layout.show_post_row, null);
			
			holder = new ViewHolder();
			holder.header = (TextView)convertView.findViewById(R.id.header);
			holder.post = (TextView)convertView.findViewById(R.id.post);
			
			holder.header.setFocusable(true);
			holder.post.setFocusable(true);
			
			holder.header.setClickable(true);
			holder.post.setClickable(true);
			
			convertView.setTag(holder);
		}
		else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		holder.header.setText(headers[position], BufferType.EDITABLE);
		holder.post.setText(posts[position], BufferType.EDITABLE);

		return convertView;
	}

}
