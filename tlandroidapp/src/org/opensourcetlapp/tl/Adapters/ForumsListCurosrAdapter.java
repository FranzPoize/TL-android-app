package org.opensourcetlapp.tl.Adapters;

import org.opensourcetlapp.tl.ShowForumList;

import com.viewpagerindicator.R;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.TransitionDrawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.format.Time;
import android.widget.TextView;

public class ForumsListCurosrAdapter extends SimpleCursorAdapter {
	
	private final Context mContext;
	private final int mLayout;
	private final int mLayoutSub;
	private final Cursor mCursor;
	private final int mNameIndex;
	private final int mSubForumIndex;
	private final LayoutInflater mLayoutInflater;

	private final class ViewHolder {
	    public TextView name;
	}
	
	

	public ForumsListCurosrAdapter(Context context, int layout,int layoutSub, Cursor c, String[] from, int[] to,int flags) {
	    super(context, layout, c, from, to,flags);

	    this.mContext = context;
	    this.mLayout = layout;
	    this.mLayoutSub = layoutSub;
	    this.mCursor = c;
	    this.mNameIndex = mCursor.getColumnIndex("name");
	    this.mSubForumIndex = mCursor.getColumnIndex("subforum");
	    this.mLayoutInflater = LayoutInflater.from(mContext);
	}
	
	@Override
    public void bindView(View v, Context context, Cursor c) {

		int nameCol = c.getColumnIndex("name");

        String name = c.getString(nameCol);
        
        nameCol = c.getColumnIndex("subforum");
        
        if (c.getInt(mSubForumIndex) == 1) {
    		v.setBackgroundResource(R.drawable.sub_forum_selector);
        } else {
        	v.setBackgroundResource(R.drawable.forum_selector);
        }
        
        

        /**
         * Next set the name of the entry.
         */     
        TextView name_text = (TextView) v.findViewById(R.id.text1);
        if (name_text != null) {
            name_text.setText(name);
            if (c.getInt(mSubForumIndex) == 1) {
            	name_text.setTextColor(Color.parseColor("#13173E"));
            	int padding_in_dp = 50;
                final float scale = context.getResources().getDisplayMetrics().density;
                int padding_in_px = (int) (padding_in_dp * scale + 0.5f);
            	name_text.setPadding(padding_in_px, 0, 0, 0);
            } else {
            	name_text.setTextColor(Color.parseColor("#13173E"));
            	int padding_in_dp = 15;
                final float scale = context.getResources().getDisplayMetrics().density;
                int padding_in_px = (int) (padding_in_dp * scale + 0.5f);
            	name_text.setPadding(padding_in_px, 0, 0, 0);
            }
        }
    }

}
