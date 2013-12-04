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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.prefs.Preferences;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
	public final String[] mDBPreferences = {"Grand River Transit", "Saskatoon Transit"};
	public static DatabaseHelper dbHelper = null;
	public static String[] mDBList = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//scan for databases
		dbHelper = new DatabaseHelper(this);
		mDBList = dbHelper.GetListofDB();
		
		
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
		SharedPreferences aSharedPref = this.getSharedPreferences(getString(R.string.pref_db_GRT_key), Context.MODE_PRIVATE);
		Boolean test = mPrefs.getBoolean(getString(R.string.pref_db_GRT_key), true);
		if (test) {
			Toast.makeText(this, "Preference is true", Toast.LENGTH_LONG).show();
		}
		else {
			Toast.makeText(this, "Preference is false", Toast.LENGTH_LONG).show();
		}
	
		
		Set<String> init_preferences = mPrefs.getStringSet(getString(R.string.pref_dbs), null);
		if (init_preferences == null) {
			//something went wrong
		} else {
			String[] selected = init_preferences.toArray(new String[] {});
			Toast.makeText(this, selected[0], Toast.LENGTH_LONG).show();
		}
		

		//for (String str: init_preferences) {
		//	Log.d("Preference is: ", str);
		//}
		
		
		//eventually add automated downloading of databases...
		
		
		//Set<String> selectionSet = new HashSet<String>();
		//selectionSet.addAll(Arrays.asList(mDBPreferences));
		/**
		MultiSelectListPreference multiSelectPref = new MultiSelectListPreference(this);
		multiSelectPref.setKey("db_multipref");
		multiSelectPref.setTitle("Multiple List Pref");
		multiSelectPref.setEntries(mDBPreferences);
		multiSelectPref.setEntryValues(mDBPreferences);
		multiSelectPref.setDefaultValue(selectionSet);
		mPrefs.addPreference(multiSelectPref);
		**/
		
		
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
			Fragment fragment = new DummySectionFragment();
			Bundle args = new Bundle();
			args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, position + 1);
			fragment.setArguments(args);
			return fragment;
		}

		@Override
		public int getCount() {
			// Sum of all the preference checkboxes = number of pages + 1.
			//return mDBPreferences.length + 1;
			
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.favs).toUpperCase(l);
			case 1:
				return getString(R.string.pref_db_GRT_key).toUpperCase(l);
			case 2:
				return getString(R.string.pref_db_STT_key).toUpperCase(l);
			}
			return null;
			
			//we just use the array of preferences
			/**if (position == 0) {
				return getString(R.string.favs);
			}
			else {
				return mDBPreferences[position-1];
			}**/
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

}
