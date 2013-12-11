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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.format.Time;
import android.util.Log;
import android.util.TimeFormatException;

public class ServiceCalendar {
	private static final String TAG = "ServiceCalendar";
	private static final String mDBQuery = "select * from calendar where service_id = ?";
	private static final String mDBQueryDate = "select * from calendar_dates where date = ? and service_id = ?";

	// Cache some results, to save db lookups
	private final HashMap<String, String> truemap;
	private final HashMap<String, String> falsemap;
	private final HashMap<String, String> trip2servicemap;

	// Match day number to a string and an abbreviation
	private static final String[] mWeekDays = { "sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday" };
	private static final String[] mWeekDaysAbbrev = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };

	private SQLiteDatabase mDB = null;
	private String mDBName;
	private DatabaseHelper mDatabaseHelper;
	private boolean ampm;
	
	public ServiceCalendar(String aDBName, SQLiteDatabase aDB, boolean ampmflag) {
		// Log.v(TAG, "ServiceCalendar()");
		mDB = aDB;
		mDBName = aDBName;
		
		truemap = new HashMap<String, String>(32);
		falsemap = new HashMap<String, String>(32);
		trip2servicemap = new HashMap<String, String>(64);
		
		//mDatabaseHelper = aDatabaseHelper;
		ampm = ampmflag;

	}

	public void setDB(DatabaseHelper aDatabaseHelper) {
		mDatabaseHelper = aDatabaseHelper;
	}
	// Return string showing days this bus runs.
	// Cursor points to a row in the calendar table for this service_id.
	private static String getDays(Cursor csr) {
		String days = new String();

		for (int i = 0; i < 7; i++) {
			if (csr.getInt(csr.getColumnIndex(mWeekDays[i])) == 1) {
				days += mWeekDaysAbbrev[i] + " ";
			}
		}

		return days;
	}

	// Do the actual work of the getDays() call, but it makes
	// sure we close the cursor on exit.
	private String process_db(String service_id, String date, boolean limit, Cursor csr) {

		if (!csr.moveToFirst()) {
			return null;
		}

		// Make sure it's in a current schedule period
		final String start = csr.getString(csr.getColumnIndex("start_date"));
		final String end = csr.getString(csr.getColumnIndex("end_date"));
		if (date.compareTo(start) < 0 || date.compareTo(end) > 0) {
			// return "Schedule data has expired!";
			return null;
		}

		// If we're not limiting the display, return what we have
		if (!limit) {
			return getDays(csr);
		}

		// Check for exceptions
		final String[] selectargs = { date, service_id };
		final Cursor exp = mDB.rawQuery(mDBQueryDate, selectargs);
		if (exp.moveToFirst()) {
			final int exception = exp.getInt(exp.getColumnIndex("exception_type"));
			exp.close();
			if (exception == 2) {
				return null;
			}
			if (exception == 1) {
				return getDays(csr); // service added for this day
			}
			Log.e(TAG, "bogus exception type " + exception + " for service " + service_id + "!");
			return null;
		} else {
			exp.close();
		}

		// Check if the bus runs on the given day of the week.
		final Time t = new Time();
		try {
			t.parse(date);
			t.normalize(false);
		} catch (final TimeFormatException e) {
			Log.e(TAG, "got bogus date \"" + date + "\"");
			return null;
		}
		final int weekday = t.weekDay; // 0--6
		if (csr.getInt(csr.getColumnIndex(mWeekDays[weekday])) == 1) {
			return getDays(csr);
		}

		return null; // doesn't run on given date.
	}

	// Return a string showing the days a bus runs, or null if it doesn't
	// run on the given date. Limit to correct days of week, or not.
	public String getTripDaysofWeek(String trip_id, String date, boolean limit) {

		String retstr = null;

		// Get and translate the service id
		String service_id = null;
		if (trip2servicemap.containsKey(trip_id)) {
			service_id = trip2servicemap.get(trip_id);
		} else {
			final String svsq = "select service_id from trips where trip_id = ?";
			final String[] svsargs = { trip_id };
			final Cursor svs = mDB.rawQuery(svsq, svsargs);
			svs.moveToFirst();
			service_id = svs.getString(0);
			svs.close();
			if (service_id != null && !service_id.equals("")) {
				trip2servicemap.put(trip_id, service_id);
			}
		}
		if (service_id == null) {
			return null;
		}

		// First check the cache
		if (limit) {
			if (truemap.containsKey(service_id + date)) {
				retstr = truemap.get(service_id + date);
				// Log.v(TAG, "Retrieved " + service_id+":"+date + " -> " + retstr + " from truecache");
				return retstr;
			}
		} else {
			if (falsemap.containsKey(service_id + date)) {
				retstr = falsemap.get(service_id + date);
				// Log.v(TAG, "Retrieved " + service_id+":"+date + " -> " + retstr + " from falsecache");
				return retstr;
			}
		}

		final String[] selectargs = { service_id };
		final Cursor csr = mDB.rawQuery(mDBQuery, selectargs);
		retstr = process_db(service_id, date, limit, csr);
		csr.close();

		// Save in cache
		if (limit) {
			truemap.put(service_id + date, retstr);
		} else {
			falsemap.put(service_id + date, retstr);
		}

		return retstr;
	}



	/* Return the time and route details of the next bus for any route, or null if there isn't one today. */
	public ArrayList<String[]> getNextDepartureTimes(String stopid, int maxResults) {
		final Time t = new Time();
		t.setToNow();
		final String timenow = String.format("%02d%02d%02d", t.hour, t.minute+1, t.second);
		//add one minute to prevent negative-one minute errors
		final String timelimit = String.format("%02d%02d%02d", t.hour+1,t.minute,t.second);
		final String date = String.format("%04d%02d%02d", t.year, t.month, t.monthDay);
		
//		final ArrayList<String[]> listdetails = getRouteDepartureTimes(stopid, date, true, null);
//we will need to significantly optimize this - first of all, we don't want to get ALL route departure times!
//this can be further optimized by only collecting the next few buses for a given route!

		final String q = "select distinct trip_id,departure_time from stop_times where stop_id = ? and departure_time >= ? and departure_time <= ?";
					//maybe we don't need * ... just return what we need
		//we use the ordered results so that only time/route details are returned for the next hour.
		//distinct so that we don't get duplicates here!
		
		//final String q = "select distinct departure_time as _id, trip_id from stop_times where stop_id = ? and trip_id in "
		//		+ "(select trip_id from trips where route_id = ? and trip_headsign = ?) order by departure_time";
		final String[] selectargs = new String[] { stopid, timenow, timelimit };
		mDB = mDatabaseHelper.ReadableDB(mDBName, mDB);
		if( mDB == null )
		{
			Log.e(TAG,"Couldn't access database!");
			return null;
		}
		final Cursor csr = mDB.rawQuery(q, selectargs);

		// Load the array for the list
		final int maxcount = csr.getCount();
		final ArrayList<String[]> listdetails = new ArrayList<String[]>(0);
		final ArrayList<String[]> results = new ArrayList<String[]>(0);

		//need to find route (shortname) and trip headsign in order to return a full string
		//do another query!
		boolean more = csr.moveToFirst();
		while (more) {

			final String trip_id = csr.getString(0);

			final String daysstr = this.getTripDaysofWeek(trip_id, date, true);

			// Only add if the bus runs on this day.
			if (daysstr != null) {
				listdetails.add(new String[] { csr.getString(1), daysstr, trip_id });
			}


			more = csr.moveToNext();
		}
		csr.close();
		
		//sort the results by departure time

		if ( listdetails.size() > 0)
		{
			
			Collections.sort(listdetails, new Comparator <String[]>() {
				public int compare(String[] a, String[] b) {
					return (a[0].compareTo(b[0]));
				}
			});
			for (int i = 0; i < Math.min(maxResults,listdetails.size()); i++ ) {

				final String q2 = "select route_long_name, route_short_name, trip_headsign from routes " +
						"join trips on trip_id = ?";
				final String[] selectargs2 = new String[] { listdetails.get(i)[2] };
				final Cursor csr2 = mDB.rawQuery(q2, selectargs2);
				csr2.moveToFirst();
				//this should have only one element (trip_id is unique!)
				//the format of this:
				// departuretime	runstoday	trip_id		route_short_name	trip_headsign
				//	140300				1		34867		13					Route 13 Laurelwood
				
				//Some routes use only long_name, some use short_name. Also trip_headsign doesn't always exist.
				if (csr2.getString(2).equals(""))
				{
					results.add(new String[] { listdetails.get(i)[0], listdetails.get(i)[1], listdetails.get(i)[2], csr2.getString(0), csr2.getString(2) });
				} else {
					results.add(new String[] { listdetails.get(i)[0], listdetails.get(i)[1], listdetails.get(i)[2], csr2.getString(1), csr2.getString(2) });
				}
				csr2.close();
				
			}
			return results;
		}
		else
		{
			// No buses in the next "timelimit" (hour)!
			return null;
		}
		//final String timetodeparture = Integer.toString(Integer.parseInt(listdetails.get(i)[0]) - t.hour*10000 - t.minute*100 - t.second);
	}

	/* Return the time of the next bus for a given route, or null if there isn't one today. */
	public String getNextDepartureTime(String stopid, String routeid, String headsign, 
				int maxResults) {

		//final ArrayList<String[]> listdetails = getRouteDepartureTimes(stopid, routeid, headsign, date, true, null);
		final Time t = new Time();
		t.setToNow();
		final String timenow = String.format("%02d%02d%02d", t.hour, t.minute, t.second);
		final String timelimit = String.format("%02d%02d%02d", t.hour+1,t.minute,t.second);
		final String date = String.format("%04d%02d%02d", t.year, t.month + 1, t.monthDay);
		
		final String q = "select trip_id,departure_time from stop_times where stop_id = ? and departure_time >= ? and departure_time <= ?"
					+ "join trips on trip_id = ? where trip_headsign = ?";
					//maybe we don't need * ... just return what we need

		//final String q = "select distinct departure_time as _id, trip_id from stop_times where stop_id = ? and trip_id in "
		//		+ "(select trip_id from trips where route_id = ? and trip_headsign = ?) order by departure_time";
		final String[] selectargs = new String[] { stopid, timenow, timelimit, routeid, headsign };
		final Cursor csr = mDatabaseHelper.ReadableDB(mDBName, mDB).rawQuery(q, selectargs);
		
		// Load the array for the list
		final int maxcount = csr.getCount();
		final ArrayList<String[]> listdetails = new ArrayList<String[]>(maxcount);
		final ArrayList<String[]> results = new ArrayList<String[]>(maxResults);
		
		boolean more = csr.moveToFirst();
		while (more) {

			final String trip_id = csr.getString(0);
			final String daysstr = this.getTripDaysofWeek(trip_id, date, true);

			// Only add if the bus runs on this day.
			if (daysstr != null) {
				listdetails.add(new String[] { csr.getString(1), daysstr, csr.getString(0) });
			}


			more = csr.moveToNext();
		}
		csr.close();
		
		// Find when the next bus leaves
		/*for (int i = 0; i < listdetails.size(); i++) {
			final String departure_time = listdetails.get(i)[1];
			if (departure_time.compareTo(timenow) >= 0) {
				return departure_time;
			}
		}*/
		if ( listdetails.size() > 0)
		{
			return listdetails.get(0)[1];
		}
		else
		{
			// No more busses today.
			return null;
		}
	}
	/* Return a list of times that all busses for all routes depart a given stop, sorted by time. List is departure_time,
	 * route_id, trip_headsign. */
	public ArrayList<String[]> getRouteDepartureTimes(String stopid, String date, 
				boolean dontlimittotoday, SQLiteDatabase aDB) {

		final String q = "select distinct departure_time as _id, trips.trip_id, routes.route_short_name, trip_headsign from stop_times "
				+ "join trips on stop_times.trip_id = trips.trip_id " + "join routes on routes.route_id = trips.route_id  "
				+ "where stop_id = ? order by departure_time";

		final String[] selectargs = new String[] { stopid };
		final Cursor csr = aDB.rawQuery(q, selectargs);

		// Load the array for the list
		final int maxcount = csr.getCount();
		final ArrayList<String[]> listdetails = new ArrayList<String[]>(maxcount);

		boolean more = csr.moveToFirst();
		while (more) {

			final String trip_id = csr.getString(1);
			final String daysstr = getTripDaysofWeek(trip_id, date, !dontlimittotoday);

			// Only add if the bus runs on this day.
			if (daysstr != null) {
				listdetails.add(new String[] { csr.getString(0), daysstr, csr.getString(2), csr.getString(3) });
			}

			more = csr.moveToNext();
		}
		csr.close();

		return listdetails;
	}
	public ArrayList<String[]> getRouteDepartureTimes(String stopid, String routeid, String headsign, String date,
			boolean limittotoday, SQLiteDatabase aDB) {

		final String q = "select distinct departure_time as _id, trip_id from stop_times where stop_id = ? and trip_id in "
				+ "(select trip_id from trips where route_id = ? and trip_headsign = ?) order by departure_time";
		final String[] selectargs = new String[] { stopid, routeid, headsign };
		final Cursor csr = aDB.rawQuery(q, selectargs);

		// Load the array for the list
		final int maxcount = csr.getCount();
		final ArrayList<String[]> listdetails = new ArrayList<String[]>(maxcount);

		boolean more = csr.moveToFirst();
		while (more) {

			final String trip_id = csr.getString(1);
			final String daysstr = getTripDaysofWeek(trip_id, date, limittotoday);

			// Only add if the bus runs on this day.
			if (daysstr != null) {
				listdetails.add(new String[] { csr.getString(0), daysstr, csr.getString(1) });
			}

			more = csr.moveToNext();
		}
		csr.close();

		return listdetails;
	}

	
	/* Return a properly formatted time. Assumes nn:nn[:nn] input somewhere in the string, may return just that, or convert and
	 * add annoying American `am/pm' suffix. */
	/* **** Using hhmmss input - no colons, always seconds! */
	public static String formattedTime(String time, boolean inampm) {
		//final int i = time.indexOf(':'); // Take note of where first colon is
		//final int j = time.lastIndexOf(':'); // and the last.

		String hours;
		String seconds;
		String newtime;
		
		final String minutes = time.substring(2,4);

		if (time.length() < 5)
		{
			seconds = "";
		}
		else if (time.substring(4,6).equals("00"))
		{
			seconds = "";
		}
		else
		{
			seconds = time.substring(4,6) + "s";
		}
	
		newtime = time.substring(0,2) + ":" + minutes + seconds;
		
		int inthours;
		try {
			inthours = Integer.parseInt(time.substring(0,2));
		} catch (final NumberFormatException e) {
			Log.d(TAG, "NumberFormatException: " + e.getMessage() + ", for time `" + newtime + "'");
			return newtime;
		}

		while(inthours >= 24)
		{
			inthours = inthours - 24;
		}

		try {
			hours = Integer.toString(inthours) + ":";
		} catch (final Error e) {
			Log.d(TAG, "Error converting integer to string!" + e.getMessage());
			return newtime;
		}

		if (!inampm) {
			newtime = hours + minutes + seconds;
			//return newtime.replaceFirst(":", "h");
			return newtime;
		}

		final String AM = " am", PM = " pm";

		if (inthours > 12)
		{
			inthours = inthours - 12;
			try {
				newtime =  Integer.toString(inthours) + ":" + minutes + seconds + PM;
			} catch (final Error e) {
				Log.d(TAG, "Error converting integer to string!" + e.getMessage());
				return newtime;
			}
			return newtime;
		}
		else if (inthours == 12)
		{

			try {
				newtime =  Integer.toString(inthours) + ":" + minutes + seconds + PM;
			} catch (final Error e) {
				Log.d(TAG, "Error converting integer to string!" + e.getMessage());
				return newtime;
			}
			return newtime;

		}
		else
		{
			try {
				newtime =  Integer.toString(inthours) + ":" + minutes + seconds + AM;
			} catch (final Error e) {
				Log.d(TAG, "Error converting integer to string!" + e.getMessage());
				return newtime;
			}
			return newtime;
		}

		// Hopefully we actually have a time
		/*
		if (i > 0) {
			int inthours;
			try {
				inthours = Integer.parseInt(hours);
			} catch (final NumberFormatException e) {
				Log.d(TAG, "NumberFormatException: " + e.getMessage() + ", for time `" + newtime + "'");
				return newtime;
			}
			String prefix = AM;

			if (hours >= 12 && hours < 24) {
				prefix = PM;
				if (hours > 12) {
					hours -= 12;
				}
			}
			if (hours >= 24) {
				if (hours == 24) {
					hours = 12;
				} else {
					hours -= 24;
				}
			}

			// Reformat to drop leading zero, add prefix
			final int where = newtime.indexOf(" ", i); // to put the suffix
			if (where > 0) {
				newtime = String.format("%s%d%s%s%s", newtime.subSequence(0, i - 2), hours, newtime.substring(i, where),
						prefix, newtime.substring(where));
			} else { // stick it on the end
				newtime = String.format("%s%d%s%s", newtime.subSequence(0, i - 2), hours, newtime.substring(i), prefix);
			}
		}
		*/

	}
}
