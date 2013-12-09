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

import java.util.Set;

import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

public class PrefsActivity extends PreferenceActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		

		getFragmentManager().beginTransaction().replace(android.R.id.content,
				new PrefsFragment()).commit();
		
		//dynamically populate this with the databases available
		//final String DB_PATH = APIReflectionWrapper.API8.getDBPath(mContext);
		
	}

	@Override
	protected void onResume() {
		super.onResume();
	}
	
    public static class PrefsFragment extends PreferenceFragment {
   	 
        @Override
        public void onCreate(Bundle savedInstanceState) {
                    super.onCreate(savedInstanceState);

                    // Load the preferences from an XML resource
                    addPreferencesFromResource(R.xml.preferences);
                    
            		//dynamically populate this with the databases available
            		final DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
            		dbHelper.gatherFiles();
            		final Set<String> mDBList = dbHelper.GetListofDB();
            		MultiSelectListPreference myMultPref = (MultiSelectListPreference) findPreference(getString(R.string.pref_dbs));
            		if (myMultPref != null) {
            			
            			CharSequence entries[] = new String[mDBList.size()];
            			CharSequence entryValues[] = new String[mDBList.size()];
            			int i = 0;
            			for (String str : mDBList) {
            				entries[i] = str;
            				entryValues[i] = str;
            				i++;
            			}
            			myMultPref.setEntries(entries);
            			myMultPref.setEntryValues(entryValues);
            		}
        }
        
    }
	
	//I don't think this is necessary
	//static preference loader
	/**
    public static class PrefsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
    	 
        @Override
        public void onCreate(Bundle savedInstanceState) {
                    super.onCreate(savedInstanceState);

                    // Load the preferences from an XML resource
                    addPreferencesFromResource(R.xml.preferences);
        }
        
        
        //and listener apps
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        	
        }
        
        @Override
        public void onResume() {
        	super.onResume();
        	getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }
        @Override
        public void onPause() {
        	super.onPause();
        	getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }
    }
    **/
    
    
    //dynamic preference loader take 2
    /**
    protected void initializeDatabaseList() {
    	ListPreference lpPref = (ListPreference)findPreference("dbList");
    	//read in the database lists
    	//initially set the default values to false
    	lpPref.setEntries(entries);
    	lpPref.setEntryValues(entryValues);
    }**/
	
	//dynamic preference loader
    /**
    public static class PrefsDynamicFragment extends PreferenceFragment {
    	 
        @Override
        public void onCreate(Bundle savedInstanceState) {
                    super.onCreate(savedInstanceState);

                    Intent intent = new Intent(getActivity(), DBLoader.class);
                    addPreferencesFromIntent(intent);
        }
    }**/
	
	
	
}
