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
import java.util.ArrayList;
import java.util.List;

import org.opensourcetlapp.tl.R;

import com.viewpagerindicator.TitlePageIndicator;
import com.viewpagerindicator.TitleProvider;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

public class main extends FragmentActivity{
	private ViewPager mViewPager;  
    private TLPagerAdapter mTLPagerAdapter;
    
    @Override 
    public void onConfigurationChanged(Configuration newConfig) { 
        super.onConfigurationChanged(newConfig); 
    }
    
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		TitlePageIndicator indicator =
		        (TitlePageIndicator)findViewById( R.id.indicator );
		mViewPager = (ViewPager) findViewById(R.id.viewpager);
		mTLPagerAdapter = new TLPagerAdapter(getSupportFragmentManager());
		mViewPager.setAdapter(mTLPagerAdapter);
		indicator.setViewPager(mViewPager);
		mViewPager.setCurrentItem(1, false);
	}
	
	private static class TLPagerAdapter extends FragmentPagerAdapter implements TitleProvider {  
		private List<Fragment> pages;
		
		private static String MYTLNET_SUBTITLE = "MyTLnet";
		private static String FORUM_SUBTITLE = "Forums";
		private static String SEARCH_SUBTITLE = "Search";
		
		private static String[] titles = new String[]
	    {
			SEARCH_SUBTITLE,
			MYTLNET_SUBTITLE,
			FORUM_SUBTITLE,
	    };
		
        public TLPagerAdapter(FragmentManager fm) {  
             super(fm);
             pages = new ArrayList<Fragment>();
             Fragment searchTL = new SearchFragment();
             searchTL.setHasOptionsMenu(true);
             pages.add(searchTL);
             Fragment mytlnet = new MytlnetFragment();
             mytlnet.setHasOptionsMenu(true);
             pages.add(mytlnet);
             Fragment showForumList = new ShowForumList();
             showForumList.setHasOptionsMenu(true);
             pages.add(showForumList);
        }  

        @Override  
        public Fragment getItem(int index) {  

             return pages.get(index);
        }  

        @Override  
        public int getCount() {  

             return titles.length;  
        }

		@Override
		public String getTitle(int position) {
			return titles[ position ];
		}  
   }  
}
