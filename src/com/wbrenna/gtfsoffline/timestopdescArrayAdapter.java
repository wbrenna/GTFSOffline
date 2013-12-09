/*
 * Copyright 2011 Giles Malet.
 * Modified 2013 Wilson Brenna.
 *
 * This file is part of GTFSOffline.
 * 
 * GTFSOffline is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * GTFSOffline is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with GTFSOffline.  If not, see <http://www.gnu.org/licenses/>.
 */


/**
 * An adapter that is used when drawing the main window list of details.
 */
package com.wbrenna.gtfsoffline;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class timestopdescArrayAdapter extends ArrayAdapter <String[]> /*<ArrayList<String[]>>*/ {
	private static final String TAG = "timestopdescAdapter";

	private final ArrayList<String[]> mDetails;
	private final LayoutInflater mInflater;
	private final int mLayout;

	public timestopdescArrayAdapter(FragmentActivity context, int layout, ArrayList<String[]> details) {
		super(context, layout, details);
		// Log.v(TAG, "timestopdescArrayAdapter()");

		mDetails = details;
		mInflater = LayoutInflater.from(context);
		mLayout = layout;
	}

	static class ViewHolder {
		TextView stoptime;
		TextView label;
		TextView value;
		TextView tripheader;
		TextView departsin;
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		// Log.v(TAG, "getview(): position " + position);
		ViewHolder holder;

		// Reuse the convertView if we already have one.... Android will create
		// only enough to fill the screen.
		if (view == null) {
			view = mInflater.inflate(mLayout, parent, false);
			// Log.d(TAG, "new view " + view);

			// Save the view when we look them up.
			holder = new ViewHolder();
			holder.stoptime = (TextView) view.findViewById(R.id.stoptime);
			holder.label = (TextView) view.findViewById(R.id.label);
			holder.value = (TextView) view.findViewById(R.id.value);
			holder.tripheader = (TextView) view.findViewById(R.id.tripheader);
			holder.departsin = (TextView) view.findViewById(R.id.departsin);
			//
			view.setTag(holder);
		} else {
			// Log.d(TAG, "reusing view " + view);
			holder = (ViewHolder) view.getTag();
		}

		if (mDetails.size() > 0) {
			if (mDetails.get(0).length > 4)
			{
				holder.stoptime.setText(mDetails.get(position)[0]);
				holder.label.setText(mDetails.get(position)[1]);
				holder.value.setText(mDetails.get(position)[2]);
				holder.tripheader.setText(mDetails.get(position)[3]);
				holder.departsin.setText(mDetails.get(position)[4]);
			}
		}
		
		return view;
	}
}
