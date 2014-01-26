/*
 * Copyright 2011 Giles Malet.
 * Modified 2013 Wilson Brenna.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class UpdateActivity extends Activity {
	private static final String TAG = "UpdateActivity";

	protected Activity mContext;
	//protected TextView mTitle;
	//protected ProgressBar mProgress;


//	public void runUpdater() {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//new LatestDB().execute();
		mContext = this;
		
		TextView text = new TextView(mContext);
		text.setText("Copying *.db.gz in background! You may close this window.");
		//setContentView(text);
		
		Button btn_close = new Button(mContext);
		btn_close.setText("OK");
		btn_close.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				mContext.finish();
			}
		});
		
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);
		//LinearLayout layout = new LinearLayout(mContext);
		LinearLayout mainlayout = new LinearLayout(mContext);
		mainlayout.setOrientation(LinearLayout.VERTICAL);
		mainlayout.addView(text, params);
		mainlayout.addView(btn_close, params);
		setContentView(mainlayout);
		
		new DBMover().execute();
	}

	
	@Override
	protected void onResume() {
		super.onResume();
	}

	private class DBMover extends AsyncTask<Void, Integer, Void> {

		private boolean alliswell = false;

		@Override
		protected void onPreExecute() {
			//mTitle.setText(R.string.db_copying);
		}

		@Override
		protected void onProgressUpdate(Integer... parms) {
			//mProgress.setProgress(parms[0]);
		}

		@Override
		protected Void doInBackground(Void... foo) {
			
			File myCacheDirectory = mContext.getExternalCacheDir();
			DatabaseHelper mDBHelper = new DatabaseHelper(mContext);
			String myFilesDirectory = mDBHelper.GetDBPath();
			File myFilesFile = new File(myFilesDirectory);
			
			File downloadFolder = Environment.getExternalStoragePublicDirectory
					(Environment.DIRECTORY_DOWNLOADS);
			
			FilenameFilter dbgzFilter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					Log.v(TAG,"File found: " + name);
					return name.endsWith(".db.gz");
				}
			};
			Log.v(TAG,"Download folder is: " + downloadFolder.getName());
			File[] downloadStrings = downloadFolder.listFiles(dbgzFilter);
			
			//int dwLength = downloadStrings.length;
			//int DBtotal = 0;
			//Toast.makeText(mContext, "Copying, please wait.", Toast.LENGTH_LONG).show();
			
			
			for(File theFile : downloadStrings ) {
				try {
					final int lastDot = theFile.getName().lastIndexOf('.');
					final String fileCachePath = myCacheDirectory.getAbsolutePath() + 
							theFile.getName().substring(0, lastDot) +  ".new";
					Log.v(TAG, theFile.getName());
					final FileOutputStream myOutput = new FileOutputStream(
							fileCachePath);
					final int chunkSize = 8*1024;
					final byte[] buffer = new byte[chunkSize];
					final FileInputStream myInput = new FileInputStream(theFile);
					
	
					// Remote file is zipped, but md5sum is of the uncompressed file.
					final GZIPInputStream zis = new GZIPInputStream(myInput);
	
					int count = 0;
					//final float tot = DBV.getSize() * 1024 * 1024 * 5.0f; // assume roughly 5 to 1 compression
					
					while ((count = zis.read(buffer, 0, chunkSize)) > 0) {
						//DBtotal += count;
						myOutput.write(buffer, 0, count);
						//publishProgress((int) ((DBtotal / dwLength) * 100.0f));
					}
					zis.close();
					
					myOutput.flush();
					myOutput.close();
	
	
					final File o = new File(myFilesFile + "/" + 
							theFile.getName().substring(0, lastDot));
					final File n = new File(fileCachePath);
					o.delete();
					n.renameTo(o);
	
					//DBtotal++;
	
				} catch (final FileNotFoundException e) {
					Log.e(TAG, "FileNotFoundException exception");
					e.printStackTrace();
				} catch (final IOException e) {
					Log.e(TAG, "IOException exception");
					e.printStackTrace();
				} catch (final Exception e) {
					Log.e(TAG, "unknown exception exception");
					e.printStackTrace();
				}
				
			}
			alliswell = true;
			return null;
		}

		@Override
		protected void onPostExecute(Void foo) {

			if (alliswell) {
				//startFavstops();
				Log.v(TAG,"Finished copying stops!");
				//Toast.makeText(mContext, "Finished!", Toast.LENGTH_LONG).show();
				return;
			}

			final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					switch (id) {
					case DialogInterface.BUTTON_NEGATIVE:
						mContext.finish();
						return;
					}
					dialog.cancel();
					//startFavstops();
					return;
				}
			};

			final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setTitle(R.string.db_is_corrupt)
			.setMessage(R.string.corrupt_exit)
			.setPositiveButton(R.string.cntinue, listener)
			.setNegativeButton(R.string.exit, listener)
			.create()
			.show();
		}
	}
}
