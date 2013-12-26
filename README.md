GTFSOffline
=========================

Description
-------------------------

GTFSOffline is a transit app for Android >= 4.4,
forked from <a href="http://github.com/gdmalet/grtransit">GRTransit</a>,
an excellent transit app for the Grand River Transit bus system.

Because of the standardization of the GTFS feed format, many transit
initiatives across the country are releasing open routes in GTFS format.
This app allows you to download a GTFS zip, process it, put it on your
phone, and get access to the stops nearest you, even with multiple bus systems.


Features
-------------------------

* Flexibility to load as many or as few databases as you want
* 100% completely offline operation
* Permissions required: only GPS coarse/fine, nothing else
* Open source GPLv3 Licensed


Guide (Process DB on Linux)
-------------------------

It is currently under development, so hang on
until I get this stable and working properly!


Install the apk found in GTFSOffline/bin/GTFSOffline.apk as per
http://www.maketecheasier.com/install-applications-without-the-market/.

You will need to find the GTFS zip files for the transit agenc[y,ies] you use.
Hopefully these are distributed online.
Unzip these files into their own folders in the data/ directory.
For each folder, do something like:
```
cd path-to-gtfsoffline/data/
./mksql3db.sh aDBName.db aGTFSFolder/*.txt
```

This command will take a while to run - it is sorting the text files, stripping
out unnecessary bits, and building them into nicely indexed SQLite databases.

When this is done, simply transfer the databases to your phone, into the folder
```
/sdcard/Android/data/com.wbrenna.gtfsoffline/files/
```

The app will detect the files (sometimes the phone needs to be restarted - an AOSP bug, not mine)
and you can select them from the Settings menu. The other settings should be fairly self explanatory!

If the app slows down considerably, you might try reducing the time it looks ahead for buses, the number
of buses at each stop, and/or the number of stops. You could also reduce the number of databases used.

You also must be sure to update your database regularly. Often a stale database will clog up the app,
as it will search through every entry trying to find one that is within the time range.

TODO
-------------------------

* The database handling could be much faster. 
* A vibrate on approaching bus preference.
* Automatically download *.db.gz files.
