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

package com.thoughtmetric.tl.Adapters;

import java.util.List;

import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

import com.thoughtmetric.tl.CustomImageGetter;
import com.thoughtmetric.tl.R;
import com.thoughtmetric.tl.Structs.PMInfo;
import com.thoughtmetric.tl.Structs.PostInfo;

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

public class ShowMyPMAdapter extends BaseAdapter{
	private class ViewHolder {
		public TextView from;
		public TextView subject;
		public TextView date;
	}
	
	private LayoutInflater mInflater;

	private List <PMInfo>pmInfoList;
	private Context context;
	
	private static final String TAG = "ForumAdapter";
	
	public ShowMyPMAdapter(List <PMInfo>pmInfoList, Context context){
		this.pmInfoList = pmInfoList;
		this.context = context;
		mInflater = LayoutInflater.from(context);
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return pmInfoList.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return pmInfoList.get(position);
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
			convertView = mInflater.inflate(R.layout.my_pm_row, null);
			
			holder = new ViewHolder();
			holder.from = (TextView)convertView.findViewById(R.id.from);
			holder.subject = (TextView)convertView.findViewById(R.id.subject);
			holder.date = (TextView)convertView.findViewById(R.id.date);
			
			convertView.setTag(holder);
		}
		else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		PMInfo postInfo = pmInfoList.get(position);

		holder.from.setText(postInfo.from);
		holder.subject.setText(postInfo.subject);
		holder.date.setText(postInfo.date);	

		return convertView;
	}

}
