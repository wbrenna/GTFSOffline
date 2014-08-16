/*
 * Written 2013 Wilson Brenna.
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
//import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements
		ActionBar.TabListener {
	private static final String TAG = "MainActivity";

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	SharedPreferences mPrefs;
	//public final String[] mDBPreferences = {"Grand River Transit", "Saskatoon Transit"};
	//LocationManager mLocationManager;
	LocationHelper mLocationHelper;
	
	//We store the active (checked) preferences in mDBActive
	public static String[] mDBActive = null;
	public static DatabaseHelper dbHelper = null;
	public static Set<String> mDBList;
	public static Set<String> mDBListPrefsOld;
	
	public static Location mLocation = null;
	public static LocationListener locationListener = null;
	public static ProgressBar mProgress = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);


		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
        if(actionBar != null) {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        }


		mProgress = (ProgressBar) findViewById(R.id.progress);
		
		//read in the preferences
		mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		
		
		Set<String> emptyString = new HashSet<String>();
		emptyString.clear();
		
		Set<String> initial_preferences = mPrefs.getStringSet(getString(R.string.pref_dbs), 
											emptyString);
		mDBListPrefsOld = initial_preferences;
		
		//this is the list of currently checked databases
		String[] tmpDBActive = initial_preferences.toArray(new String[initial_preferences.size()]);

		//we have to be careful to exclude databases that aren't in our directory
		dbHelper = new DatabaseHelper(this);
		dbHelper.gatherFiles();
		mDBList = dbHelper.GetListofDB();
		List<String> workingDBList = new ArrayList<String>();
		
		for (int i=0; i < tmpDBActive.length; i++) {
			if ( mDBList.contains(tmpDBActive[i]) )
			{
				workingDBList.add(tmpDBActive[i]);
			}
			else
			{
				initial_preferences.remove(tmpDBActive[i]);
			}
		}
		if (workingDBList.size() == 0)
		{
			mDBActive = null;
		} else {
			mDBActive = workingDBList.toArray(new String[workingDBList.size()]);
		}
		
		Editor prefsDBEditor = mPrefs.edit();
		prefsDBEditor.putStringSet(getString(R.string.pref_dbs), initial_preferences);
		prefsDBEditor.commit();
		
		//Set up the location management
		mLocationHelper = new LocationHelper(this);
		mLocation = mLocationHelper.startLocationManager();
		
		// Define a listener that responds to location updates
		locationListener = new LocationListener() {
			@Override
			public void onLocationChanged(Location location) {
				if (mLocationHelper.isBetterLocation(location, mLocation)) {
					mLocation = location;
					if (mLocation != null) {
						//new ProcessBusStops().execute();
						mSectionsPagerAdapter.notifyDataSetChanged();
						
					} else {
						Toast.makeText(getBaseContext(), R.string.last_location_fix, 
								Toast.LENGTH_LONG).show();
						//Log.e(TAG, "No more location fixes ");
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
		
		//TODO: eventually add automated downloading of databases...
		
		
		// Create the adapter that will return a fragment for each of the
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		mViewPager
				.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						actionBar.setSelectedNavigationItem(position);
					}
				});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			actionBar.addTab(actionBar.newTab()
					.setText(mSectionsPagerAdapter.getPageTitle(i))
					.setTabListener(this));
		}
		
		
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		//we want to refresh the databases list
		Set<String> emptyString = new HashSet<String>();
		emptyString.clear();
		
		Set<String> initial_preferences = mPrefs.getStringSet(getString(R.string.pref_dbs), 
											emptyString);
		
		//in case we're on a low-ram system and things aren't cached
		try {
			if( !initial_preferences.equals(mDBListPrefsOld)) {
			
				mDBListPrefsOld = initial_preferences;
				//this is the list of currently checked databases
				mDBActive = null;
				//just to nullify the previous one.
				if (initial_preferences.size() != 0) {
					String[] tmpDBActive = initial_preferences.toArray(new String[initial_preferences.size()]);
					dbHelper.gatherFiles();
					mDBList = dbHelper.GetListofDB();
					List<String> workingDBList = new ArrayList<String>();
					
					for (int i=0; i < tmpDBActive.length; i++) {
						if ( mDBList.contains(tmpDBActive[i]) )
						{
							workingDBList.add(tmpDBActive[i]);
						}
					}
					if (workingDBList.size() == 0)
					{
						mDBActive = null;
					} else {
						mDBActive = workingDBList.toArray(new String[workingDBList.size()]);
					}
				}
				
				mSectionsPagerAdapter.notifyDataSetChanged();
				
				//totally kill the viewPager and all, and recreate!
				//mSectionsPagerAdapter = null;
				//getFragmentManager().beginTransaction().replace(containerViewId, fragment);
				//and create the appropriate tabs
				final ActionBar actionBar = getActionBar();
		
				/**mSectionsPagerAdapter = new SectionsPagerAdapter(
						getSupportFragmentManager());

				// Set up the ViewPager with the sections adapter.
				mViewPager = (ViewPager) findViewById(R.id.pager);
				mViewPager.setAdapter(mSectionsPagerAdapter);

				mViewPager
						.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
							@Override
							public void onPageSelected(int position) {
								actionBar.setSelectedNavigationItem(position);
							}
						});**/
				if(actionBar != null) {
                    actionBar.removeAllTabs();
                }
				for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
					actionBar.addTab(actionBar.newTab()
							.setText(mSectionsPagerAdapter.getPageTitle(i))
							.setTabListener(this));
					
					//update the fragment contents
					/**if (i > 0 ) {
						Fragment newFragment = new DBListFragment();
						Bundle args = new Bundle();
						args.putString(DBListFragment.DATABASE, mDBActive[i-1]);
						newFragment.setArguments(args);
						
						getFragmentManager().beginTransaction().replace(i, newFragment);
					}**/
					if (i>0) {
						DBListFragment aFragment = (DBListFragment) getSupportFragmentManager().
								findFragmentByTag("android:switcher:"+R.id.pager+":"+Integer.toString(i));
						if (aFragment != null) {
							getSupportFragmentManager().beginTransaction().remove(aFragment).commit();
							mSectionsPagerAdapter.notifyDataSetChanged();
							//if (aFragment.getView() != null) {
								//aFragment.updateDisplay();
							//}
							
						}
					}
					else if (i==0) {
						FavSectionFragment aFavFragment = (FavSectionFragment) getSupportFragmentManager().
								findFragmentByTag("android:switcher:"+R.id.pager+":"+Integer.toString(i));
						if (aFavFragment != null) {
							//getSupportFragmentManager().beginTransaction().remove(aFavFragment).commit();
							//mSectionsPagerAdapter.notifyDataSetChanged();
							//if (aFavFragment.getView() != null) {
								//aFavFragment.updateDisplay();

							//}
							aFavFragment.updatePositions();
							
						}
					}
		
				}
			}
		} finally {
		
			//restart location manager
			//mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
			mLocationHelper.refresh(locationListener);
		}
		
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		//kill the location manager
		mLocationHelper.unlinkLocation(locationListener);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			//launch prefs
			Intent i = new Intent(this, PrefsActivity.class);
			startActivity(i);
			/**
			 * This code replaces just the active fragment. Not what I want! Useful later...
			android.app.FragmentManager mFragmentManager = getFragmentManager();
			FragmentTransaction mFragmentTransaction = mFragmentManager.beginTransaction();
			PrefsFragment mPrefsFragment = new PrefsFragment();
			mFragmentTransaction.replace(android.R.id.content, mPrefsFragment);
			mFragmentTransaction.commit();
			**/
			break;
		case R.id.action_movedb:
			//launch UpdateActivity
			Intent j = new Intent(this, UpdateActivity.class);
			startActivity(j);
			break;
		}
		return true;
	}
	
	
	@Override
	public void onTabSelected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		//we want to turn off location management?
		//mSectionsPagerAdapter.notifyDataSetChanged();
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {
		//private final FragmentManager mFragmentManager;

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
			//mFragmentManager = fm;
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			// Return a DummySectionFragment (defined as a static inner class
			// below) with the page number as its lone argument.
			if (position == 0) {
				Fragment fragment = new FavSectionFragment();
				Bundle args = new Bundle();
				args.putInt(FavSectionFragment.ARG_SECTION_NUMBER, position + 1);
				fragment.setArguments(args);
				return fragment;
			}
			else {
				//Fragment fragment = new DBListFragment(mDBActive[position-1]);
				Fragment fragment = new DBListFragment();
				Bundle args = new Bundle();
				args.putString(DBListFragment.DATABASE, mDBActive[position-1]);
				fragment.setArguments(args);
				return fragment;
			}
		}

		@Override
		public int getItemPosition(Object object) {
			//this is called on notifyDataSetChanged, which we use on tab swipe
			//Fragment f = (Fragment) object;
		//	if (f instanceof DBListFragment) {
				//((DBListFragment) f).updatePositions();
				//Log.e(TAG,"Updating positions");
		//	}
			/**
			Fragment f = (Fragment) object;
			if (f instanceof DBListFragment) {
				if (super.getItemPosition(f) == mViewPager.getCurrentItem()) {
					((DBListFragment) f).relinkPosition();
					Log.e(TAG, "Relinking from Main");
				}
				else {
					((DBListFragment) f).unlinkPosition();
					Log.e(TAG, "Unlinking from Main");
				}
					
			}
			**/
			
			//Instead let's just force a view refresh
			//return super.getItemPosition(object);
			//Log.e(TAG,"Updating positions");
			return POSITION_NONE;
		}
		
		@Override
		public int getCount() {
			// Sum of all the preference checkboxes = number of pages + 1
			if(mDBActive == null) {
				return 1;
			} else if (mDBActive[0].equals("")) {
				//this shouldn't happen
				mDBActive = null;
				return 1;
			}
			else {
				return 1 + mDBActive.length;
			}
		}

		@Override
		public CharSequence getPageTitle(int position) {
			//Locale l = Locale.getDefault();
			//we just use the array of preferences
			if (position == 0) {
				return getString(R.string.favs);
			}
			else {
				return mDBActive[position-1];
			}
		}
	}

	/**
	 * A dummy fragment representing a section of the app, but that simply
	 * displays dummy text.
	 */
	public static class FavSectionFragment extends ListFragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		public static final String ARG_SECTION_NUMBER = "section_number";
		private FavFragmentHelper mFavFragHelper;
		//private String myDatabaseName;
		private timestopdescArrayAdapter mListAdapter;
		
		
//		public FavSectionFragment() {
//		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main_db,
					container, false);
			
			return rootView;
		}
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
		}
		
		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			
			mFavFragHelper = new FavFragmentHelper(this.getActivity(), 
					mDBActive, mProgress);
			//setup the list adapter
			mFavFragHelper.runProcess();
			mListAdapter = new timestopdescArrayAdapter(this.getActivity(), R.layout.favstopdesc, 
							mFavFragHelper.retrieveNextBusList());
			
			setListAdapter(mListAdapter);
			mFavFragHelper.addTimeAdapter(mListAdapter);
			
			//set up the long click favourites
			getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
					mFavFragHelper.onListItemLongClick(parent, view, position, id);
					return true; // we consumed the click
				}
			});

		}
		
		public void onListItemClick(ListView lv, View v, int position, long id) {
			Log.v(TAG, "clicked position " + position);

			final String[] strs = (String[]) lv.getItemAtPosition(position);
			if (strs == null) {
				return;
			}
			final String stop_id = strs[1];
			final String stop_name = strs[2];
			final String headsign = strs[3];
			final String trip_id = strs[5];
			final String myDatabaseName = strs[6];
			Log.v(TAG, "Found stop with ID,name,etc:" + stop_id + stop_name + trip_id);

			final Intent routes = new Intent(this.getActivity(), TimesActivity.class);
			final String pkgstr = this.getActivity().getApplicationContext().getPackageName();
			routes.putExtra(pkgstr + ".stop_id", stop_id);
			routes.putExtra(pkgstr + ".stop_name", stop_name);
			routes.putExtra(pkgstr + ".trip_id", trip_id);
			routes.putExtra(pkgstr + ".headsign", headsign);
			routes.putExtra(pkgstr + ".db_name", myDatabaseName);
			this.getActivity().startActivity(routes);
		}
		
		public void updatePositions() {
			mFavFragHelper.reloadPreferences();
			mFavFragHelper.runProcess();
		}
		
	}

	public static class DBListFragment extends ListFragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		private static final String DATABASE = "DBPlaceholder";
		public String myDatabase;
		private LocationFragmentHelper mLocationFragHelper;
		private timestopdescArrayAdapter mListAdapter;

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {

			return inflater.inflate(R.layout.fragment_main_db, container, false);
			

		}
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);	
			//set our database
			myDatabase = getArguments().getString(DATABASE);
			
		}
		
		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			
			mLocationFragHelper = new LocationFragmentHelper(this.getActivity(), 
					myDatabase, null, mProgress);
			//setup the list adapter
			mLocationFragHelper.runProcessOnLocation(mLocation);
			mListAdapter = new timestopdescArrayAdapter(this.getActivity(), R.layout.timestopdesc, 
							mLocationFragHelper.retrieveNextBusList());
			
			setListAdapter(mListAdapter);
			mLocationFragHelper.addTimeAdapter(mListAdapter);
			
			//set up the long click favourites
			getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
					mLocationFragHelper.onListItemLongClick(parent, view, position, id);
					return true; // we consumed the click
				}
			});

		}
		
		public void onListItemClick(ListView lv, View v, int position, long id) {
			Log.v(TAG, "clicked position " + position);

			final String[] strs = (String[]) lv.getItemAtPosition(position);
			if (strs == null) {
				return;
			}
			final String stop_id = strs[1];
			final String stop_name = strs[2];
			final String headsign = strs[3];
			final String trip_id = strs[5];
			Log.v(TAG, "Found stop with ID,name,etc:" + stop_id + stop_name + trip_id);

			final Intent routes = new Intent(this.getActivity(), TimesActivity.class);
			final String pkgstr = this.getActivity().getApplicationContext().getPackageName();
			routes.putExtra(pkgstr + ".stop_id", stop_id);
			routes.putExtra(pkgstr + ".stop_name", stop_name);
			routes.putExtra(pkgstr + ".trip_id", trip_id);
			routes.putExtra(pkgstr + ".headsign", headsign);
			routes.putExtra(pkgstr + ".db_name", myDatabase);
			this.getActivity().startActivity(routes);
		}
		
		public void updatePositions() {
			mLocationFragHelper.runProcessOnLocation(mLocation);
		}
		
	}
	

}
