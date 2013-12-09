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
import android.widget.ListView;
import android.widget.TextView;
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
	
	public static Location mLocation = null;
	public static LocationListener locationListener = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);


		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		//read in the preferences
		mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		
		
		Set<String> emptyString = new HashSet<String>();
		emptyString.clear();
		
		Set<String> initial_preferences = mPrefs.getStringSet(getString(R.string.pref_dbs), 
											emptyString);
		
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
		}
		if (workingDBList.size() == 0)
		{
			mDBActive = null;
		} else {
			mDBActive = workingDBList.toArray(new String[workingDBList.size()]);
		}
		
		
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
						Toast.makeText(getBaseContext(), R.string.last_location_fix, Toast.LENGTH_LONG).show();
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
		
		//eventually add automated downloading of databases...
		
		
		// Create the adapter that will return a fragment for each of the three
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
		
		//this is the list of currently checked databases
		mDBActive = null;
		//just to nullify the previous one.
		if (initial_preferences.size() == 0) {
			//mDBActive = null;
			//already null
		} else {
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
		
		//and create the appropriate tabs
		final ActionBar actionBar = getActionBar();
		if ( actionBar.getTabCount() < mSectionsPagerAdapter.getCount() ) {
			actionBar.removeAllTabs();
			for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
				actionBar.addTab(actionBar.newTab()
						.setText(mSectionsPagerAdapter.getPageTitle(i))
						.setTabListener(this));
			}
		}
		else if (actionBar.getTabCount() > mSectionsPagerAdapter.getCount()) {
			actionBar.removeAllTabs();
			for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
				actionBar.addTab(actionBar.newTab()
						.setText(mSectionsPagerAdapter.getPageTitle(i))
						.setTabListener(this));
			}
		}
		//restart location manager
		//mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		mLocationHelper.refresh(locationListener);
		
		
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
		//we want to turn off location management
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

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			// Return a DummySectionFragment (defined as a static inner class
			// below) with the page number as its lone argument.
			if (position == 0) {
				Fragment fragment = new DummySectionFragment();
				Bundle args = new Bundle();
				args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, position + 1);
				fragment.setArguments(args);
				return fragment;
			}
			else {
				Fragment fragment = new DBListFragment(mDBActive[position-1]);
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
			Log.e(TAG,"Updating positions");
			return POSITION_NONE;
		}
		
		@Override
		public int getCount() {
			// Sum of all the preference checkboxes = number of pages + 1
			if(mDBActive == null) {
				return 1;
			} else if (mDBActive[0].equals("")) {
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
	public static class DummySectionFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		public static final String ARG_SECTION_NUMBER = "section_number";

		public DummySectionFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main_dummy,
					container, false);
			
			TextView dummyTextView = (TextView) rootView
					.findViewById(R.id.section_label);
			dummyTextView.setText(Integer.toString(getArguments().getInt(
					ARG_SECTION_NUMBER)));
			
			//Intent intent = new Intent(this.getActivity(), FavstopsActivity.class);
			//this.getActivity().startActivity(intent);
			
			return rootView;
		}
	}

	public static class DBListFragment extends ListFragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		private static final String DATABASE = "DBPlaceholder";
		private LocationFragmentHelper mLocationFragHelper;
		private String myDatabaseName;
		private timestopdescArrayAdapter mListAdapter;

		private DBListFragment(String dbName) {
			myDatabaseName = dbName;
			//mLocationManager = aLocationManager;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {

			return inflater.inflate(R.layout.fragment_main_db, container, false);
		}
		
		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			
			mLocationFragHelper = new LocationFragmentHelper(this.getActivity(), myDatabaseName, null);
			//setup the list adapter
			mLocationFragHelper.runProcessOnLocation(mLocation);
			mListAdapter = new timestopdescArrayAdapter(this.getActivity(), R.layout.timestopdesc, 
							mLocationFragHelper.retrieveNextBusList());
			
			setListAdapter(mListAdapter);
			mLocationFragHelper.addTimeAdapter(mListAdapter);

		}
		
		public void onListItemClick(ListView lv, View v, int position, long id) {
			//do something
		}
		
		public void updatePositions() {
			mLocationFragHelper.runProcessOnLocation(mLocation);
		}
		
	}
	

}
