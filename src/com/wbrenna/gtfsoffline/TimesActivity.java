/*
 * Copyright 2011 Giles Malet.
 *
 * This file is part of GRTransit.
 * 
 * GRTransit is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * GRTransit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with GRTransit.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.wbrenna.gtfsoffline;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class TimesActivity extends ListActivity {
	private static final String TAG = "TimesActivity";

	private String mTrip_id = null, mHeadsign, mStop_id;
	private String mRoute_id = null;
	private ArrayList<String[]> mListDetails = null;
	private boolean showAllTrips = true; // force redraw
	private ListActivity mContext;
	protected TextView mTitle;
	protected View mListDetail;

	private static boolean mCalendarChecked = false, mCalendarOK;
	private String mDBName;
	private SQLiteDatabase mDB;
	private SharedPreferences mPrefs;
	private boolean ampmflag;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		mContext = this;
		setContentView(R.layout.timeslayout);
		super.onCreate(savedInstanceState);

		final String pkgstr = mContext.getApplicationContext().getPackageName();
		final Intent intent = getIntent();
		mTrip_id = intent.getStringExtra(pkgstr + ".trip_id");
		mHeadsign = intent.getStringExtra(pkgstr + ".headsign");
		mStop_id = intent.getStringExtra(pkgstr + ".stop_id");
		mDBName = intent.getStringExtra(pkgstr + ".db_name");
		
		Log.e(TAG,"List of data: " + mTrip_id + mHeadsign + mStop_id + mDBName);
		//Set up shared preferences and get the database key.
		
		mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		showAllTrips = mPrefs.getBoolean(mContext.getString(R.string.pref_showallbusses_key), false);
		ampmflag = mPrefs.getBoolean(mContext.getString(R.string.pref_ampmtimes_key), false);

		DatabaseHelper DBHelper = new DatabaseHelper(this);
		mDB = DBHelper.ReadableDB(mDBName, null);

	}

	@Override
	protected void onResume() {
		super.onResume();

		// See if we need to recalculate and redraw the screen.
		// This happens if the user brings up the preferences screen.
		//if (PrefChanged) {
		//	new ProcessBusTimes().execute();
		//	PrefChanged = false;
		//}
		
		//assuming that this only happens when we _need_ to redraw
		//so, reload shared preferences and execute
		showAllTrips = mPrefs.getBoolean(mContext.getString(R.string.pref_showallbusses_key), false);
		ampmflag = mPrefs.getBoolean(mContext.getString(R.string.pref_ampmtimes_key), false);
		new ProcessBusTimes().execute();
		
		
	}

	/* Do the processing to load the ArrayAdapter for display. */
	//private class ProcessBusTimes extends AsyncTask<Void, Integer, Integer> implements NotificationCallback {
	private class ProcessBusTimes extends AsyncTask<Void, Integer, Integer> {
		static final String TAG = "ProcessBusTimesAsync";

		// TODO -- should set a listener that will call this callback.

		// A callback from CalendarService, for updating our progress bar
		//@Override
		//public void notificationCallback(Integer progress) {
		//	publishProgress(progress);
		//}

		@Override
		protected void onPreExecute() {
			// Log.v(TAG, "onPreExecute()");
			//mListDetail.startAnimation(mSlideIn);
			//mProgress.setVisibility(View.VISIBLE);
		}

		// Update the progress bar.
		@Override
		protected void onProgressUpdate(Integer... parms) {
			//mProgress.setProgress(parms[0]);
		}

		@Override
		protected Integer doInBackground(Void... foo) {
			// Log.v(TAG, "doInBackground()");

			// Will find where to position the list of bus departure times
			final Time t = new Time();
			t.setToNow();
			final String timenow = String.format("%02d%02d%02d", t.hour, t.minute, t.second);
			final String datenow = String.format("%04d%02d%02d", t.year, t.month + 1, t.monthDay);

			// Make sure we actually have some valid data, since schedules change often.
			if (!mCalendarChecked) {
				mCalendarOK = CheckCalendar(datenow);
			}
			if (!mCalendarOK) {
				return null;
			}

			if (mTrip_id == null) {
				// showing all routes
				ServiceCalendar aSC = new ServiceCalendar(mDBName, mDB, ampmflag);
				mListDetails = aSC.getRouteDepartureTimes(mStop_id, datenow,
						showAllTrips, mDB);
			} else {

				// TODO Setting a listener means not passing `this'

				//convert trip id to route id
				final String q = "select distinct route_id from trips where trip_id = ? ";
				final String[] selectargs = new String[] { mTrip_id };
				final Cursor csr = mDB.rawQuery(q, selectargs);	
				csr.moveToFirst();
				if (csr.getString(0).equals(""))
				{
					// showing all routes
					csr.close();
					ServiceCalendar aSC = new ServiceCalendar(mDBName, mDB, ampmflag);
					mListDetails = aSC.getRouteDepartureTimes(mStop_id, datenow,
							showAllTrips, mDB);
					
				} else {
					Log.e(TAG,"Entered specific search.");
					mRoute_id = csr.getString(0);
					csr.close();
					// showing just one route
					ServiceCalendar aSC = new ServiceCalendar(mDBName, mDB, ampmflag);
					mListDetails = aSC.getRouteDepartureTimes(mStop_id, mRoute_id, mHeadsign, datenow,
							showAllTrips, mDB);
				}

			}

			// Find when the next bus leaves
			int savedpos = -1;
			for (int i = 0; i < mListDetails.size(); i++) {
				final String departure_time = mListDetails.get(i)[0];
				if (departure_time.compareTo(timenow) >= 0) {
					savedpos = i;
					break;
				}
			}

			return savedpos;
		}

		@Override
		protected void onPostExecute(Integer savedpos) {
			// Log.v(TAG, "onPostExecute()");

			if (!mCalendarOK) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
				//builder.setIcon(R.drawable.grticon);
				builder.setTitle(R.string.app_name);
				builder.setMessage(R.string.calendar_expired);
				builder.create();
				builder.show();
				return;
			}

			//mProgress.setVisibility(View.INVISIBLE);
			//mListDetail.startAnimation(mSlideOut);

			TextView tv = null;
			final ListView lv = getListView();
			if (lv.getFooterViewsCount() == 0) {
				tv = new TextView(mContext);
				lv.addFooterView(tv);
			}
			lv.setOnTouchListener(mGestureListener);

			if (mRoute_id == null) { // showing all routes
				//mTitle.setText("Stop " + mStop_id + " - all routes");
				if (tv != null) {
					tv.setText(R.string.tap_time_for_route);
				}
				final TimesArrayAdapter adapter = new TimesArrayAdapter(mContext, 
								R.layout.row2layout, mListDetails);
				mContext.setListAdapter(adapter);
			} else {
				// TODO should be route_short_name?
				//mTitle.setText(mRoute_id + " - " + mHeadsign);
				if (tv != null) {
					tv.setText(R.string.tap_time_for_trip);
				}
				//final ListArrayAdapter adapter = new ListArrayAdapter(mContext, R.layout.rowlayout, mListDetails);
				//lv.setListAdapter(adapter);
			}

			// Calculate the time difference
			Toast msg;
			if (savedpos >= 0) {
				final Time t = new Time();
				t.setToNow();

				final String nextdeparture = mListDetails.get(savedpos)[0];
				int hourdiff = Integer.parseInt(nextdeparture.substring(0, 2));
				hourdiff -= t.hour;
				hourdiff *= 60;
				int mindiff = Integer.parseInt(nextdeparture.substring(2, 4));
				mindiff -= t.minute;
				hourdiff += mindiff;

				if (hourdiff >= 60) {
					//msg = Toast.makeText(mContext, "Next bus leaves at " + ServiceCalendar.formattedTime(nextdeparture),
					//		Toast.LENGTH_LONG);
					msg = Toast.makeText(mContext, "Next bus leaves at " + nextdeparture,
							Toast.LENGTH_LONG);
				} else {
					final String plural = hourdiff > 1 ? "s" : "";
					msg = Toast.makeText(mContext, "Next bus leaves in " + hourdiff + " minute" + plural, Toast.LENGTH_LONG);
				}

				getListView().setSelectionFromTop(savedpos, 50); // position next bus just below top

			} else {
				setSelection(mListDetails.size()); // position the list at the last bus
				msg = Toast.makeText(mContext, R.string.no_more_busses, Toast.LENGTH_LONG);
			}

			msg.setGravity(Gravity.TOP, 0, 0);
			msg.show();
		}
	}

	/* Make sure the calendar is current. Updates mCalendarChecked if we get a result of some sort. */
	private boolean CheckCalendar(String datenow) {
		boolean retval = true; // report OK even if failure, so we just continue
		final String[] selectargs = { datenow, datenow };
		Cursor csr = null;

		try {
			csr = mDB.rawQuery(
					"select count(*) from calendar where " + "start_date <= ? and end_date >= ?", selectargs);
		} catch (final SQLException e) {
			Log.e(TAG, "DB query failed checking calendar expiry: " + e.getMessage());
		}

		if (csr != null) {
			if (csr.getCount() == 0 || !csr.moveToFirst() || csr.getInt(0) <= 0) {
				retval = false;
			}

			mCalendarChecked = true;
			csr.close();
		}

		return retval;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Log.v(TAG, "clicked position " + position);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return true;
	}

	private final View.OnTouchListener mGestureListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			return mGestureDetector.onTouchEvent(event);
		}
	};

	// Catch flings, to show all busses coming to this stop.
	// This must be called on the GIU thread.
	private final GestureDetector mGestureDetector = new GestureDetector(mContext,
			new GestureDetector.SimpleOnGestureListener() {
				@Override
				public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
					Log.d(TAG, "fling X " + velocityX + ", Y " + velocityY);
					// Catch a fling sort of from left to right
					if (velocityX > 100 && Math.abs(velocityX) > Math.abs(velocityY)) {
						Log.d(TAG, "fling detected");
						finish();
						return true;
					}
					return false;
				}
			});
}
