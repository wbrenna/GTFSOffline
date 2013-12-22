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
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class timestopdescArrayAdapter extends ArrayAdapter <String[]> /*<ArrayList<String[]>>*/ {
	//private static final String TAG = "timestopdescAdapter";

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
		TextView stoptime; //this is the distance to the stop (not labeled well!)
		TextView label;	//this is stop_id
		TextView value; // this is stop_name
		TextView tripheader; //this is the trip header
		TextView departsin; //the time until it departs
		TextView tripid; //a hidden trip id
		TextView dbname; //a hidden database name so Fav knows where to search
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
			holder.tripid = (TextView) view.findViewById(R.id.trip_id);
			holder.dbname = (TextView) view.findViewById(R.id.dbname);
			//
			view.setTag(holder);
		} else {
			// Log.d(TAG, "reusing view " + view);
			holder = (ViewHolder) view.getTag();
		}

		if (mDetails.size() >= position) {
			if (mDetails.get(position).length > 6)
			{
				holder.stoptime.setText(mDetails.get(position)[0]);
				holder.label.setText(mDetails.get(position)[1]);
				holder.value.setText(mDetails.get(position)[2]);
				holder.tripheader.setText(mDetails.get(position)[3]);
				holder.departsin.setText(mDetails.get(position)[4]);
				holder.tripid.setText(mDetails.get(position)[5]);
				holder.dbname.setText(mDetails.get(position)[6]);
			}
		}
		
		return view;
	}
}
