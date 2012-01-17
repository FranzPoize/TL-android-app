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

import org.opensourcetlapp.tl.ShowPost;

import android.content.Context;
import android.content.Intent;
import android.sax.StartElementListener;
import android.text.style.ClickableSpan;
import android.view.View;

public class DisplayableLinkSpan extends ClickableSpan{
	private String url;
	private Context context;
	
	public DisplayableLinkSpan(String url, Context context){
		this.url = url;
		this.context = context;
	}
	
	@Override
	public void onClick(View widget) {
		// Figure out what to do with the url
		assert(isDisplayable(url));
		if (url.contains("viewmessage.php")){
			Intent intent = new Intent().setClass(context, ShowPost.class);
			intent.putExtra("postURL", url);
			context.startActivity(intent);
		}
	}
	
	public static boolean isDisplayable(String url){
		boolean result = false;
		if (url.contains("viewmessage.php")){
			result = true;
		}
		return result;
	}

}
