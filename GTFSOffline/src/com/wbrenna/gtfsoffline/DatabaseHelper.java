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

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

public class DatabaseHelper {
	private static final String TAG = "DatabaseHelper"; // getClass().getSimpleName();

	private String DB_PATH = null;
	private String DB_OLD_PATH = null;
	//modify this to open all databases
	//private static String DB_NAME = "GRT.db";
	//private ArrayList<String> DB_NAMES = new ArrayList<String>();
	private Set<String> DB_NAMES;
	private static Context mContext;
	//private static SQLiteDatabase DB = null;

	/**
	 * Constructor Takes and keeps a reference of the passed context in order to access the application assets and resources.
	 * 
	 * @param context
	 */
	public DatabaseHelper(Context context) {
		mContext = context;
		DB_NAMES = new HashSet<String>();
		
		DB_OLD_PATH = context.getApplicationInfo().dataDir + "/files/";


		// check external storage state first!
		//
		//NB This shouldn't require EXTERNAL_STORAGE permissions
		final File f = mContext.getExternalFilesDir(null);
		if (f != null) {
			//DB_PATH =  f.getParent();
			DB_PATH = f.getPath();
		} else {
			DB_PATH = DB_OLD_PATH;
			Log.e(TAG,"Cannot get path to ExternalFilesDir.");
		}

		final File f2 = new File(DB_PATH);
		if (!f2.exists() && !f2.mkdirs()) {
			Log.e(TAG, "can't create sdcard dirs, using phone storage :-(");
			DB_PATH = DB_OLD_PATH;
		}
		if (f2.exists() && !f2.canWrite()) {
			Log.e(TAG, "can't write to sdcard dirs, using phone storage :-(");
			DB_PATH = DB_OLD_PATH;
		}

	}
	
	public void gatherFiles() {
		//Set up the list of databases
		final File f2 = new File(DB_PATH);
		File dbfiles[] = f2.listFiles();
		if ( dbfiles == null ) {
			return;
		}
		
		for (int i=0; i<dbfiles.length; i++) {
			String DB_NAME = dbfiles[i].getName();
			//ensure it's a DB file
			String fnarray[] = DB_NAME.split("\\.");
			if( !fnarray[fnarray.length - 1].equals("db") ) {
				//this wasn't a database file
				break;
			}
			//check to see if it's a database that can be opened
			SQLiteDatabase test = ReadableDB(DB_NAME,null);
			if( test == null ) {
				break;
			}
			CloseDB(test);
			test = null;
			DB_NAMES.add(DB_NAME);
		
			// Delete the old database if it exists, and recreate on the sdcard.
			/**if (!DB_PATH.equals(DB_OLD_PATH)) {
				final File olddb = new File(DB_OLD_PATH + DB_NAME);
				if (olddb.exists() && !olddb.delete()) {
					Log.e(TAG, "failed to delete old db...!?");
				}

				// The database is on the sdcard.
				final String sdstate = Environment.getExternalStorageState();
				if (!sdstate.equals(Environment.MEDIA_MOUNTED)) {
					final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
					builder.setTitle("The external database is unavailable.")
					.setMessage("The sdcard is in state `" + sdstate
							+ "'.\nPlease retry when it is available.").create().show();
					return;
				}
			}
			**/
		
		
		}

		// Do this once, as we don't need them separate anymore.
		//DB_PATH += "/" + DB_NAME;
	}

	/* Return path to the database. */
	public String GetDBPath()
	{
		return DB_PATH;
	}

	/* Return list of databases */
	//public String[] GetListofDB() {
		//return DB_NAMES.toArray(new String[DB_NAMES.size()]);
	public Set<String> GetListofDB() {
		return DB_NAMES;
	}
	
	/* Force close the DB so we can recreate it */
	public void CloseDB(SQLiteDatabase DB)
	{
		if (DB != null) {
			DB.close();
			DB = null;
		}
	}
	/* Return a handle for reading the database. */
	public SQLiteDatabase ReadableDB(String DB_NAME, SQLiteDatabase DB) {

		if ( (DB == null) ) {
			try {
				DB = SQLiteDatabase.openDatabase(DB_PATH + "/" + DB_NAME, 
								null, SQLiteDatabase.OPEN_READONLY);
				//sqlite can attach other databases to this original one.
				//String[] dbList = getApplicationContext().databaseList();
				//can then be used to list databases associated with the application

				//TODO: see OsmAnd to see if they did something here

			} catch (final SQLiteException e) {
				// bah
				Log.e(TAG, "Could not read the database...");
//				Toast.makeText(mContext,  "Problem reading a database! Check *.db and restart.", 
//								Toast.LENGTH_LONG).show();
				return null;
			}
		}

		return DB;
	}

	/* Return version of current DB */
	public int GetDBVersion(String DB_NAME, SQLiteDatabase DB) {
		if (DB == null) {
			ReadableDB(DB_NAME, DB);
		}
		int dQuery = DB.rawQuery("PRAGMA user_version", null).getInt(0);

		CloseDB(DB);
		return dQuery;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
