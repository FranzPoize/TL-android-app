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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.provider.OpenableColumns;
import android.text.Html;
/*
 * @Author H3R3T1C 
 * (With improvements by apiccion)
 * Email: th3h3r3t1c@gmail.com
 */
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.VideoView;

public class CustomImageGetter implements Html.ImageGetter{
	// NOTE: The directories must already exist
	private static final String TAG = "CustomImageGetter";
	private Context context;
	private AssetManager assetManager;
	
	public CustomImageGetter(Context context) {
		this.context = context;
		assetManager = context.getAssets();
		Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
	}
	
	@Override
	public Drawable getDrawable(String url) {
		try{
			// get name of image
			String name = url.substring(url.lastIndexOf("/")+1);
			InputStream is;
			try {
				try {
					is = assetManager.open("images/"+name);
				}
				catch (IOException e){
					is = context.openFileInput(name);
				}
			}
			catch (FileNotFoundException e){
				is = getImageInputStream(url);
				if (!name.contains("draw.php?poll_id")){
					FileOutputStream out = context.openFileOutput(name, Context.MODE_PRIVATE);
			        byte[] buffer = new byte[1024];
			        int totalLength = 0;
			        int length;
			        while ((length = is.read(buffer)) >= 0 ) {
			            totalLength += length;
			        	out.write(buffer, 0, length);
			        }	
			        is.close();
			        out.close();
					is = context.openFileInput(name);
				}
		     }
			
			// cache dir + name of image + the image save format

			Drawable d = Drawable.createFromStream(is, name);
			//Drawable d = Drawable.createFromPath(f.getAbsolutePath());
			d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());// make it the size of the image
			return d;
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	private void downloadImage(String url,File f) throws IOException
    {
		URL myFileUrl;
		try {
			myFileUrl =new URL(url);
		}
		catch (MalformedURLException e){
			myFileUrl = new URL(TLLib.getAbsoluteURL(url));
		}
		Log.d(TAG, "Downloading image: " + myFileUrl.toString());
    	HttpURLConnection conn= (HttpURLConnection)myFileUrl.openConnection();
        conn.setDoInput(true);
        conn.connect();
        InputStream is = conn.getInputStream();

        BufferedInputStream buff = new BufferedInputStream(is);
//        Bitmap bm = BitmapFactory.decodeStream(buff);
       
        FileOutputStream out = new FileOutputStream(f);
        
        byte[] buffer = new byte[1024];
        int totalLength = 0;
        int length;
        while ((length = buff.read(buffer)) >= 0 ) {
            totalLength += length;
        	out.write(buffer, 0, length);
        }
        out.flush();
        //bm.compress(Bitmap.CompressFormat.PNG, 90, out);    
    }
	
	private InputStream getImageInputStream(String url) throws IOException{
		URL myFileUrl;
		try {
			myFileUrl =new URL(url);
		}
		catch (MalformedURLException e){
			myFileUrl = new URL(TLLib.getAbsoluteURL(url));
		}
    	HttpURLConnection conn= (HttpURLConnection)myFileUrl.openConnection();
        conn.setDoInput(true);
        conn.connect();
        return conn.getInputStream();
        
	}
}
