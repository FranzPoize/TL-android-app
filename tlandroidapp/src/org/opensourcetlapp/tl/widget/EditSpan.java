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

package org.opensourcetlapp.tl.widget;

import org.opensourcetlapp.tl.EditPost;

import android.content.Context;
import android.content.Intent;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;

public class EditSpan extends ClickableSpan {
	private Context context;
	private int postId;
	private int topicId;
	private int currentPage;
	
	public EditSpan(Context context, int postId, int topicId, int currentPage){
		Log.d("EditSpan", String.format("%d, %d, %d", postId, topicId, currentPage));
		this.context = context;
		this.postId = postId;
		this.topicId = topicId;
		this.currentPage = currentPage;
	}
	@Override
	public void onClick(View widget) {
		Intent intent = new Intent().setClass(context, EditPost.class);
		intent.putExtra("postId", postId);
		intent.putExtra("topicId", topicId);
		intent.putExtra("currentPage", currentPage);
		context.startActivity(intent);
	}

}
