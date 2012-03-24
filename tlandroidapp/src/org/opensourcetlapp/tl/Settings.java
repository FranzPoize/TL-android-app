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

import org.opensourcetlapp.tl.R;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class Settings extends Activity {
	public static final String SETTINGS_FILE_NAME = "Settings";
	public static final String VIEW_ALL = "viewAll";
	public static final String DISABLE_SMART_PARSING = "disableSmartParsing";
	
	private CheckBox disableSmartParsingCheckBox;
	private CheckBox viewAllCheckBox;
	
	@Override 
    public void onConfigurationChanged(Configuration newConfig) { 
        super.onConfigurationChanged(newConfig); 
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		setTitle(TLLib.makeActivityTitle(SETTINGS_FILE_NAME));
		
		disableSmartParsingCheckBox = (CheckBox) findViewById(R.id.disableSmartParsing);
		viewAllCheckBox = (CheckBox) findViewById(R.id.viewAll);
		loadStoredValues();
		
		disableSmartParsingCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				saveBoolean(DISABLE_SMART_PARSING, isChecked);
			}
		});
		
		viewAllCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				saveBoolean(VIEW_ALL, isChecked);
			}
		});
		
	}

	private void loadStoredValues() {
		SharedPreferences settings = getSharedPreferences(SETTINGS_FILE_NAME, 0);
		boolean disableSmartParsing = settings.getBoolean(DISABLE_SMART_PARSING, false);
		boolean viewAll = settings.getBoolean(VIEW_ALL, false);

		disableSmartParsingCheckBox.setChecked(disableSmartParsing);
		viewAllCheckBox.setChecked(viewAll);
	}
	
	private void saveBoolean(String name, boolean value){
		SharedPreferences settings = getSharedPreferences(SETTINGS_FILE_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(name, value);
		editor.commit();
	}
}
