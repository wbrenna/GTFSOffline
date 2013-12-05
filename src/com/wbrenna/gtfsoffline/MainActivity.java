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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.prefs.Preferences;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements
		ActionBar.TabListener {

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
	
	//We store the active (checked) preferences in mDBActive
	public static String[] mDBActive = null;
	public static DatabaseHelper dbHelper = null;
	public static Set<String> mDBList;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//scan for databases
		//dbHelper = new DatabaseHelper(this);
		//mDBList = dbHelper.GetListofDB();
		
		
		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		//read in the preferences
		//PrefsFragment mPrefsFragment = new PrefsFragment();
		//mPrefs = this.getSharedPreferences("preferences",
		//										Context.MODE_PRIVATE);
		//mPrefs = this.getPreferences(Context.MODE_PRIVATE);
		
		mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		
		
		//listen to these preferences
		//mPrefs.registerOnSharedPreferenceChangeListener(this);
		
		
		//THIS WORKS!
		//String test = mPrefs.getString("@string/pref_db_GRT_key", "none");
		//SharedPreferences aSharedPref = this.getSharedPreferences(getString(R.string.pref_db_GRT_key), Context.MODE_PRIVATE);
		/**
		Boolean test = mPrefs.getBoolean(getString(R.string.pref_db_GRT_key), true);
		if (test) {
			Toast.makeText(this, "Preference is true", Toast.LENGTH_LONG).show();
		}
		else {
			Toast.makeText(this, "Preference is false", Toast.LENGTH_LONG).show();
		}
	**/
		
		Set<String> emptyString = new HashSet<String>();
		emptyString.clear();
		
		Set<String> initial_preferences = mPrefs.getStringSet(getString(R.string.pref_dbs), 
											emptyString);
		
		//this is the list of currently checked databases
		mDBActive = initial_preferences.toArray(new String[initial_preferences.size()]);
		
		
		
		//now we can unify the preferences with those scanned from the SD card
		//we'll only scan on startup to keep things running smoothly
		//mDBList.addAll(initial_preferences);
		
		//since sets are exclusive it won't replicate items
		//now we clear initial_preferences and replace it with mDBList.
		//Editor mPrefsEditor = mPrefs.edit();
		
		//mPrefsEditor.remove(getString(R.string.pref_dbs)).commit();
		//mPrefsEditor.putStringSet(getString(R.string.pref_dbs), mDBList).commit();
		
		
		/**
		Set<String> init_preferences = mPrefs.getStringSet(getString(R.string.pref_dbs), null);
		if (init_preferences == null) {
			//something went wrong
		} else {
			String[] selected = init_preferences.toArray(new String[] {"test"});
			Toast.makeText(this, selected[0], Toast.LENGTH_LONG).show();
		}
		
		if( init_preferences.isEmpty()) {
			Toast.makeText(this, "Init_preferences is empty...", Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(this, "Init_preferences contains this many elements: " + 
						Integer.toString(init_preferences.size()), Toast.LENGTH_LONG).show();
		}
		**/
		

		
		//eventually add automated downloading of databases...
		

		
		
		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		mViewPager
				.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						actionBar.setSelectedNavigationItem(position);
					}
				});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by
			// the adapter. Also specify this Activity object, which implements
			// the TabListener interface, as the callback (listener) for when
			// this tab is selected.
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
		mDBActive = initial_preferences.toArray(new String[initial_preferences.size()]);
		mSectionsPagerAdapter.notifyDataSetChanged();
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
				Fragment fragment = new DBSectionFragment(mDBActive[position-1]);
				Bundle args = new Bundle();
				args.putString(DBSectionFragment.DATABASE, mDBActive[position-1]);
				fragment.setArguments(args);
				return fragment;
			}
		}

		@Override
		public int getCount() {
			// Sum of all the preference checkboxes = number of pages + 1.
			//return mDBPreferences.length + 1;
			
			
			if(mDBActive == null) {
				return 1;
			}
			else {
				return 1 + mDBActive.length;
			}
			
		
		
			//return 1;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			/**
			switch (position) {
			case 0:
				return getString(R.string.favs).toUpperCase(l);
			case 1:
				return getString(R.string.pref_db_GRT_key).toUpperCase(l);
			case 2:
				return getString(R.string.pref_db_STT_key).toUpperCase(l);
			}
			return null;
			**/
			
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
			return rootView;
		}
	}

	public static class DBSectionFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		public static final String DATABASE = "DBPlaceholder";
		public static LocationHelper mLocationHelper;
		public static String myDatabaseName;

		public DBSectionFragment(String dbName) {
			myDatabaseName = dbName;
			//mLocationHelper = new LocationHelper(this.getActivity(), myDatabaseName, null);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main_db,
					container, false);
			TextView dummyTextView = (TextView) rootView
					.findViewById(R.id.section_label2);
			dummyTextView.setText(getArguments().getString(DATABASE));
			return rootView;
		}
	}


}
