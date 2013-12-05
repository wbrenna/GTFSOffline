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
import java.util.Comparator;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class LocationHelper {
	private static final String TAG = "LocationHelper";

	private static final int MIN_LOCN_UPDATE_TIME = 10000; // ms
	private static final int MIN_LOCN_UPDATE_DIST = 10; // m
	private static final int NUM_CLOSEST_STOPS = 8;
	//make this a preference at some point

	private LocationManager mLocationManager;
	private Location mLocation;
	//private timestopdescArrayAdapter mAdapter;
	private ArrayList<String[]> mListDetails;

	private Context mContext;
	
	private String myDBName;
	private SQLiteDatabase myDB;
	
	// Need to store some stuff in an array, so we can sort by distance
	class StopLocn {
		public float dist, bearing;
		public double lat, lon;
		public String stop_id, stop_name;
	}

	static StopLocn[] mStops = null;


	public LocationHelper(Context context, String aDBName, SQLiteDatabase aDB) {
		mContext = context;

		myDBName = aDBName;
		myDB = aDB;
		
		// Load animations used to show/hide progress bar
		//mTitle = (TextView) findViewById(R.id.listtitle);
		mListDetails = new ArrayList<String[]>(NUM_CLOSEST_STOPS);

		//mTitle.setText(R.string.loading_stops);


		// Acquire a reference to the system Location Manager
		mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

		//mAdapter = new timestopdescArrayAdapter(mContext, R.layout.timestopdesc, mListDetails);
		//mContext.setListAdapter(mAdapter);

		// Get a best guess of current location
		Location nwlocn = null, gpslocn = null;
		try {
			nwlocn = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		} catch (final IllegalArgumentException e) {
			Log.e(TAG, "Exception requesting last location from GPS_PROVIDER");
		}
		try {
			gpslocn = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		} catch (final IllegalArgumentException e) {
			Log.e(TAG, "Exception requesting last location from NETWORK_PROVIDER");
		}
		if (isBetterLocation(gpslocn, nwlocn)) {
			mLocation = gpslocn;
		} else {
			mLocation = nwlocn;
		}

		if (mLocation != null) {
			new ProcessBusStops().execute();
		} else {
			Toast.makeText(mContext, R.string.no_location_fix, Toast.LENGTH_LONG).show();
		}
	}

	// Define a listener that responds to location updates
	LocationListener locationListener = new LocationListener() {
		@Override
		public void onLocationChanged(Location location) {
			if (isBetterLocation(location, mLocation)) {
				mLocation = location;
				if (mLocation != null) {
					new ProcessBusStops().execute();
				} else {
					Toast.makeText(mContext, R.string.last_location_fix, Toast.LENGTH_LONG).show();
				}
			}
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onProviderDisabled(String provider) {
		}
	};

	public void refresh() {
		// Get location updates
		try {
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_LOCN_UPDATE_TIME, MIN_LOCN_UPDATE_DIST,
					locationListener);
		} catch (final IllegalArgumentException e) {
			Log.e(TAG, "Exception requesting location from GPS_PROVIDER");
		}

		try {
			mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_LOCN_UPDATE_TIME,
					MIN_LOCN_UPDATE_DIST, locationListener);
		} catch (final IllegalArgumentException e) {
			Log.e(TAG, "Exception requesting location from NETWORK_PROVIDER");
		}
	}


	/* Do the processing to load the ArrayAdapter for display. */
	public class ProcessBusStops extends AsyncTask<Void, Integer, Void> {
		// static final String TAG = "ProcessBusStops";

		
		@Override
		protected void onPreExecute() {
//			mListDetail.startAnimation(mSlideIn);
//			mProgress.setVisibility(View.VISIBLE);
		}

		// Update the progress bar.
		// 	- do nothing for now
		@Override
		protected void onProgressUpdate(Integer... parms) {
//			mProgress.setProgress(parms[0]);
		}

		@Override
		protected Void doInBackground(Void... foo) {
			// Log.v(TAG, "doInBackground()");

			//myDBName = foo[0];
			//myDB = foo[1];
			
			final String qry = "select stop_id as _id, stop_lat, stop_lon, stop_name from stops";
			int maxcount;

			// Load the stops from the database the first time through
			if (mStops == null) {
				final Cursor csr = DatabaseHelper.ReadableDB(myDBName, myDB).rawQuery(qry, null);
				maxcount = csr.getCount();
				mStops = new StopLocn[maxcount];
				boolean more = csr.moveToPosition(0);
				int locidx = 0;

				while (more) {
					// stash in array
					mStops[locidx] = new StopLocn();
					mStops[locidx].stop_id = csr.getString(0);
					mStops[locidx].lat = csr.getDouble(1);
					mStops[locidx].lon = csr.getDouble(2);
					mStops[locidx].stop_name = csr.getString(3);

					more = csr.moveToNext();
					publishProgress(((int) ((++locidx / (float) maxcount) * 100)));
				}
				csr.close();
			}

			// Calculate the distance to each point in the array
			final float[] results = new float[2];
			for (final StopLocn s : mStops) {
				Location.distanceBetween(mLocation.getLatitude(), mLocation.getLongitude(), s.lat, s.lon, results);
				s.dist = results[0];
				s.bearing = results[1];
			}

			// Sort by distance from our current location
			Arrays.sort(mStops, new Comparator<StopLocn>() {
				@Override
				public int compare(StopLocn entry1, StopLocn entry2) {
					return entry1.dist < entry2.dist ? -1 : entry1.dist == entry2.dist ? 0 : 1;
				}
			});

			// Transfer everything to an array list to load in display
			// Bearing is from -180 to +180, so use as index into here
			mListDetails.clear();
			final String[] DIRS = { "S", "SW", "W", "NW", "N", "NE", "E", "SE", };
			for (int i = 0; i < NUM_CLOSEST_STOPS; i++) {
				final StopLocn s = mStops[i];

				final String dir = DIRS[(int) (s.bearing + 180 + 22.5) % 360 / 45];
				String dist;
				if (s.dist < 1000) {
					dist = String.format("%3.0fm %s", s.dist, dir);
				} else {
					dist = String.format("%3.1fkm %s", s.dist / 1000.0, dir);
				}
				mListDetails.add(new String[] { dist, s.stop_id, s.stop_name });
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void foo) {
			// Log.v(TAG, "onPostExecute()");

			//mProgress.setVisibility(View.INVISIBLE);
			//mListDetail.startAnimation(mSlideOut);

			//mTitle.setText(R.string.title_activity_closest_stops);

			//mAdapter.notifyDataSetChanged();
		}
	}

	/* The following is copied straight from: http://developer.android.com/guide/topics/location/strategies.html */
	private static final int TWO_MINUTES = 1000 * 60 * 2;

	/**
	 * Determines whether one Location reading is better than the current Location fix
	 * 
	 * @param location
	 *            The new Location that you want to evaluate
	 * @param currentBestLocation
	 *            The current Location fix, to which you want to compare the new one
	 */
	protected boolean isBetterLocation(Location location, Location currentBestLocation) {
		if (location == null) {
			// An old location is always better than no location
			return false;
		}
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		final long timeDelta = location.getTime() - currentBestLocation.getTime();
		final boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		final boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		final boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		final int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
		final boolean isLessAccurate = accuracyDelta > 0;
		final boolean isMoreAccurate = accuracyDelta < 0;
		final boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		final boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
			return true;
		}
		return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}
}
