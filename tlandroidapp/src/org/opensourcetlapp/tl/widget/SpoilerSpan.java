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

import java.util.ArrayList;

import org.htmlcleaner.TagNode;

import android.text.Editable;
import android.text.Spannable;
import android.text.style.ClickableSpan;
import android.text.style.QuoteSpan;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class SpoilerSpan extends ClickableSpan {
	private static final String TAG = "SpoilerSpan";
	private TagNode node;
	private boolean showing = false;
	private int pos;
	private TextView textView;
	private CharSequence text;
	private QuoteSpan quoteSpan;
	public ArrayList<SpoilerSpan> spoilerSpanList = new ArrayList<SpoilerSpan>();
	
	public SpoilerSpan(int pos, TextView textView) {	
		this.pos = pos;
		this.textView = textView;
		this.quoteSpan = new QuoteSpan();
	}

	@Override
	public void onClick(View widget) {
		if (!showing){
			open();
		}else {
			close();
		}
	}
	
	private void open(){
		if (showing) return;
		Editable ed = (Editable)textView.getText();
		pos = ((Spannable) ed).getSpanEnd(this);
		
		ed.insert(pos, text);
		((Spannable) ed).setSpan(quoteSpan, pos, pos + text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		showing = true;
	}
	
	private void close(){
		if (!showing) return;
		for (SpoilerSpan s :spoilerSpanList){
			s.close();
		}
		
		Editable ed = (Editable)textView.getText();
		pos = ((Spannable) ed).getSpanEnd(this);
		
		ed.removeSpan(quoteSpan);
		ed.replace(pos, pos + text.length(), "");
		showing = false;
	}
	
	public void setTagNode(TagNode node){
		this.node = node;
	}

	public void setCharSequence(CharSequence text) {
		this.text = text;
	}

}
