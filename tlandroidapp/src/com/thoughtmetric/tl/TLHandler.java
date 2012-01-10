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

package com.thoughtmetric.tl;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.SimpleCursorAdapter;

public class TLHandler extends Handler {
		public static final int PROGRESS_OKAY = 200;
		public static final int PROGRESS_RENDERING = 201;
		public static final int PROGRESS_SEARCHING = 202;
		public static final int PROGRESS_NETWORK_DOWN = 203;
		public static final int PROGRESS_LOGIN = 204;
		
		private ProgressDialog progressDialog;
		private Context context;
		public int progressStatus = PROGRESS_OKAY;
		
		public TLHandler (ProgressDialog progressDialog, Context context){
			this.progressDialog = progressDialog;
			this.context = context;
		}
		
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				if (progressStatus == PROGRESS_NETWORK_DOWN){
					progressDialog.dismiss();
					AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
					alertDialogBuilder.setTitle("Connection Error");
					alertDialogBuilder.setMessage("Please check your phone's internet connection.");
					alertDialogBuilder.setPositiveButton("Okay", null);
					alertDialogBuilder.show();
				}
				break;
			case TLLib.PROGRESS_CONNECTING:
				progressDialog.setMessage("Connecting to TeamLiquid.net...");
				break;
			case TLLib.PROGRESS_PARSING:
				progressDialog.setMessage("Preparing for display on mobile device...");
				break;
			case TLLib.PROGRESS_DOWNLOADING:
				progressDialog.setMessage("Downloading data...");
				break;
			case PROGRESS_RENDERING:
				progressDialog.setMessage("Rendering...");
				break;
			case PROGRESS_SEARCHING:
				progressDialog.setMessage("Rendering...");
				break;
			case PROGRESS_LOGIN:
				progressDialog.setMessage("Logging in...");
				break;
			default:
				break;
			}
		}
}
