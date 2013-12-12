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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

public class LocationHelper {
	
	private static final String TAG = "LocationHelper";

	private static final int MIN_LOCN_UPDATE_TIME = 10000; // ms
	private static final int MIN_LOCN_UPDATE_DIST = 10; // m
	//TODO: make this a preference at some point

	private LocationManager mLocationManager;
	private Location mLocation;

	private Context mContext;

	


	public LocationHelper(Context context) {
		mContext = context;
	}
	
	public Location startLocationManager() {

		mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
		

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
			//we need to push an update to all open listeners
			return mLocation;
		} else {
			Toast.makeText(mContext, R.string.no_location_fix, Toast.LENGTH_LONG).show();
			//Log.e(TAG, "No location fix...");
			return null;
		}
	}


	public void refresh(LocationListener locationListener) {
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
	
	public void unlinkLocation(LocationListener locationListener) {
		//This will be called when we are exiting the program, etc, so we should shut down our database.
		//Log.e(TAG, "Unlinking LocationListener updates.");
		mLocationManager.removeUpdates(locationListener);
		//mLocationManager = null;
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
