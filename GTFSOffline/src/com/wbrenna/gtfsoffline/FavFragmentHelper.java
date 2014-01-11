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

package com.wbrenna.gtfsoffline;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class FavFragmentHelper {
	
	private static final String TAG = "FavFragmentHelper";
	private static String FAVSTOPS_KEY;

	private static int NUM_CLOSEST_STOPS;
	private static int NUM_BUSES;	//the number of next buses per stop to be shown.

	//private Location mLocation;
	private timestopdescArrayAdapter mAdapter;
	private ArrayList<String[]> mListDetails;

	private Context mContext;
	private DatabaseHelper mDatabaseHelper;

	// Need to store some stuff in an array, so we can sort by distance
	class StopLocn {
		public float dist, bearing;
		public double lat, lon;
		public String stop_id, stop_name;
	}
	private StopLocn[] mStops;
	private SharedPreferences mPrefs;
	private boolean ampmflag;
	private String[] mActiveDB;
	private int hoursLookAhead;
	private ProgressBar mProgress;
	


	public FavFragmentHelper(Context context, String[] activeDBs, ProgressBar aProgress) {
		mContext = context;
	
		mDatabaseHelper = new DatabaseHelper(mContext);
		mActiveDB = activeDBs;
		
		// Load animations used to show/hide progress bar
		//mTitle = (TextView) findViewById(R.id.listtitle);
		mProgress = aProgress;
		mListDetails = new ArrayList<String[]>(NUM_CLOSEST_STOPS*NUM_BUSES);

		//mTitle.setText(R.string.loading_stops);
		mStops = null;
		//mLocation = null;
		
		//set up prefs
		FAVSTOPS_KEY = new String(mContext.getString(R.string.pref_favstops_key));
		mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		reloadPreferences();
	}

	public void runProcess() {
		new ProcessBusStops().execute();
	}

	private void reloadPreferences() {
		ampmflag = mPrefs.getBoolean(mContext.getString(R.string.pref_ampmtimes_key), false);
		NUM_CLOSEST_STOPS = Integer.parseInt(mPrefs.getString(
											mContext.getString(R.string.pref_num_closest_stops), "8"));
		NUM_BUSES = Integer.parseInt(mPrefs.getString(
								mContext.getString(R.string.pref_num_buses_per_stop), "3"));
		hoursLookAhead = Integer.parseInt(mPrefs.getString(
					mContext.getString(R.string.pref_hours_look_ahead), "1"));
	}

	public ArrayList<String[]> retrieveNextBusList() {
		return mListDetails;
	}
	
	public void addTimeAdapter(timestopdescArrayAdapter anAdapter) {
		mAdapter = anAdapter;
	}

	/* Do the processing to load the ArrayAdapter for display. */
	public class ProcessBusStops extends AsyncTask<Void, Integer, Void> {
		// static final String TAG = "ProcessBusStops";

		
		@Override
		protected void onPreExecute() {
//			mListDetail.startAnimation(mSlideIn);
			mProgress.setVisibility(View.VISIBLE);
		}

		// Update the progress bar.
		// 	- do nothing for now
		@Override
		protected void onProgressUpdate(Integer... parms) {
			mProgress.setProgress(parms[0]);
		}

		@Override
		protected Void doInBackground(Void... foo) {
			// Log.v(TAG, "doInBackground()");

			//really inefficient...at the moment we just search all databases
			mListDetails.clear();
			// Load the stops from preferences
			
			ArrayList<String[]> tmpStops = GetBusstopFavourites();
			mStops = new StopLocn[tmpStops.size()];
			
			//for (final StopLocn s : mStops) {
			if (tmpStops.size() == 0) {
				Log.v(TAG, "Empty favourites");
				return null;
			}
			for (int i = 0; i<tmpStops.size(); i++) {
				//Log.e(TAG, "Stops are: " + Arrays.toString(tmpStops.get(i)));
				mStops[i] = new StopLocn();
				mStops[i].stop_id = tmpStops.get(i)[0];
				mStops[i].stop_name = tmpStops.get(i)[1];
			}
			
			for (String myDBName : mActiveDB) {
				//Log.e(TAG, "Running on database: " + myDBName);
				SQLiteDatabase myDB = mDatabaseHelper.ReadableDB(myDBName, null);
				String[] mStopIdArray = new String[mStops.length];
				for (int i = 0; i < mStops.length; i++) {
					mStopIdArray[i] = mStops[i].stop_id;
					//final StopLocn s = mStops[i];
					//Log.e(TAG, "Running on stop: " + s.stop_id);
				}
				
				//Now, we need to query to find the next NUM_BUSES.
				ServiceCalendar myBusService = new ServiceCalendar(myDBName, myDB, ampmflag);
				myBusService.setDB(mDatabaseHelper);
				final ArrayList<String[]> fullResults = myBusService.getNextDepartureTimesGen(
							//mStopIdArray, NUM_BUSES, hoursLookAhead);
							mStopIdArray, NUM_BUSES, hoursLookAhead);
				//the format of this:
				// departuretime	runstoday	trip_id		route_short_name	trip_headsign
				//	140300				1		34867		13					Route 13 Laurelwood
				
				
				final Time t = new Time();
				t.setToNow();
				if (fullResults == null)
				{
					continue;
				}
				final int favcounter = fullResults.size();
				int loopcounter = 0;
				
				for (String[] str: fullResults) {
					//process str[0] to get the right departure time
					final String hours = str[0].substring(0,2);
					final String minutes = str[0].substring(2,4);
					//String departsIn;
		
					//Log.e(TAG, "Adding to list: " + s.stop_name);
					mListDetails.add(new String[] { "", 
							str[5], 
							mStops[Arrays.asList(mStopIdArray).indexOf(str[5])].stop_name, str[4], 
							myBusService.formattedDepartureTime(t, hours, minutes), 
							str[2], myDBName });
					publishProgress(((int) ((++loopcounter / (float) favcounter) * 100)));
				}
				//TODO: this can be cleaned up a little! Maybe a better array type
				
				//close the database
				mDatabaseHelper.CloseDB(myDB);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void foo) {
			// Log.v(TAG, "onPostExecute()");

			mProgress.setVisibility(View.INVISIBLE);
			//mListDetail.startAnimation(mSlideOut);

			//mTitle.setText(R.string.title_activity_closest_stops);
			if(mAdapter == null) {
				// do nothing
			} else {
				mAdapter.notifyDataSetChanged();
			}
		}
	}
	
	// Called for a long click
	public void onListItemLongClick(AdapterView<?> parent, View v, int position, long id) {
		//Log.v(TAG, "long clicked position " + position);

		final String[] strs = (String[]) parent.getItemAtPosition(position);
		if (strs == null) {
			return;
		}
		final String stop_id = strs[1];
		final String stop_name = strs[2];

		final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				switch (id) {
				case DialogInterface.BUTTON_POSITIVE:
					RemoveBusstopFavourite(stop_id);
					break;
				}
				dialog.cancel();
			}
		};

		final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setTitle("Stop " + stop_id + ", " + stop_name);
		builder.setMessage(R.string.favs_remove_from_list).setPositiveButton(R.string.yes, listener)
		.setNegativeButton(R.string.no, listener).create().show();
	}
	
	public ArrayList<String[]> GetBusstopFavourites() {
		final String favs = mPrefs.getString(FAVSTOPS_KEY, "");

		// Load the array for the list
		final ArrayList<String[]> details = new ArrayList<String[]>();

		// favs is a semi-colon separated string of stops, with a trailing semi-colon.
		// Then each stop has a description stored as KEY-stop.
		if (!favs.equals("")) {
			final TextUtils.StringSplitter splitter = new TextUtils.SimpleStringSplitter(';');
			splitter.setString(favs);
			for (final String s : splitter) {
				final String[] strs = { s, mPrefs.getString(FAVSTOPS_KEY + "-" + s, "") };
				details.add(strs);
			}
		}
		return details;
	}

	public void RemoveBusstopFavourite(String busstop) {
		final String favs = mPrefs.getString(FAVSTOPS_KEY, "");
		String newfavs = "";

		final TextUtils.StringSplitter splitter = new TextUtils.SimpleStringSplitter(';');
		splitter.setString(favs);

		for (final String s : splitter) {
			if (!s.equals(busstop)) {
				newfavs += s + ";";
			}
		}
		mPrefs.edit().putString(FAVSTOPS_KEY, newfavs).remove(FAVSTOPS_KEY + "-" + busstop).commit();
		Toast.makeText(mContext, "Stop " + busstop + " was removed from your favourites.", Toast.LENGTH_LONG).show();

	}
	
}
