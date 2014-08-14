#!/bin/bash
# Automatically create sqlite3 db from GTFS files
# Creates DB of given name in current dir from all files
# rm the sqlite3/GRT.db, then cd to the data/GRT-?? dir, and run
# ../../bin/mksql3db.sh ../sqlite3/GRT.db *

MYNAME=$(basename $0)
#SQ3=/usr/local/android-sdk-linux/tools/sqlite3
SQ3=/opt/adt-bundle-linux-x86_64-20131030/sdk/tools/sqlite3

function usage {
    echo "Usage: $MYNAME dbname file [file ...]"
    exit 1
}

function warn {
    echo "$MYNAME: $@" 1>&2
}

function error {
    warn "$@"
    exit 1
}

function getdbversion {
    db=$1

    test -s "$db" || { warn "no existing db; using version 1"; return 1; }

    local version=$(echo 'PRAGMA user_version;' | sqlite3 "$db")
    test -z "$version" -o "$version" -lt 0 -o "$version" -gt 999 && {
	warn "strange version \"$version\"; using version 1"
	return 1
    }

    return $version
}

test $# -lt 2 && usage

DB=$1
shift

getdbversion $DB
version=$(expr $? + 1)
echo "using database version $version"

tmpfile=$(mktemp)
trap "rm -f $tmpfile $DB.new" 0

# Create the necessary metadata table
$SQ3 $DB.new <<-EOT
  CREATE TABLE "android_metadata" ("locale" TEXT DEFAULT 'en_US');
  INSERT INTO "android_metadata" VALUES ('en_US');
EOT

# Pipe everything to sqlite3
####Don't pipe everything! Don't need shapes.txt.
####Also think about this. If the service only requires
####stop names, GPS coords, route names, route<->stop link, and holidays,
####we can parse like this:
####	Grab current date. Is it a holiday? Filter on the appropriate set of service_ids for that day.
####	Find the current GPS location. Vector-sort the list of stops so that we can quickly find which one works.
####		In practise, just find the nearest point on one axis and move < 5km away upwards and downwards. 
####	From those stops, pick all within 350m. To find the next 5 buses at each of these stops...	
####	Do this by: sorting stop_times.txt by stop, returning the first X (5) trip_ids
####		for the set of valid times within 30 mins of after the current time, then using the sorted trips.txt (sorted by trip_id)
####		to filter out invalid service_ids, getting a list of: route_ids, direction, time from now, stop number.
####	**use multi-column indices for this: create index idx3 on fruitsforsale(fruit,state);
####		-> then select price from fruitsforsale where fruit='orange' and state='ca'
####			**will have to assume we can move through the stop times minute by minute - fortunately this is very fast
####	Finally, use routes.txt to pull out the name of the route and stops.txt to pull out the name of the stop. Display nicely (:
####
####	Issues: Should not differentiate between 13 Laurelwood and 13 Boardwalk, for example (in text yes, but not as different routes)
####		Should give option for "nearest stops" to be homepage and "favourite stops" to be homepage
####			(do this with a dropdown in the application list, so that we don't back out through a million things, and save this as a preference)
####		Compile with ART support
####		Create a new tab for each transit service DB that is activated. Swipe between them :)
####		Ensure that parents in stops.txt are taken care of!



#sort the file stops.txt and create a column of numbers
#$sortvar = "stop_sorted,"
calendartest=false
calendartest2=false

while [ $# -gt 0 ]
do
    file=$1
    echo $file
    case "`basename $file`" in
    stops.txt)
	    table=$(echo `basename $file` | sed -e 's/\..*//')
	    columns="stop_sorted,$(cat $file | tr -d '\015' | head -n 1 | sed -e '1 s/^\xef\xbb\xbf//')"
	    #tail -n +2 "$file" | sort -k6 -g -t, | cat -n -s | awk '{ $1 = $1","; print}' > $tmpfile
	    tail -n +2 $file | sort -k6 -g -t, | grep -n '^' - | sed -e '/^$/d' | sed -e 's/,\ */,/g' | sed -e 's/\([0-9]\):/\1,/g' > $tmpfile
	    (
		echo "create table $table($columns);"
		echo ".separator ,"
		echo ".import $tmpfile $table"
	    ) | $SQ3 $DB.new
	    ;;
    stop_times.txt)
	    #We need to strip the colons out of the time
	    table=$(echo `basename $1` | sed -e 's/\..*//')
	    #columns=$(head -1 $file)
	    columns=$(cat $file | tr -d '\015' | head -n 1 | sed -e '1 s/^\xef\xbb\xbf//')
	    tail -n +2 $file | sed -e '/^$/d' | sed -e 's/,\ */,/g' | sed -e '/^\s*$/d' | sed -e 's/\,\ \([0-9]\):\([0-9][0-9]\):\([0-9][0-9]\)/\,0\1\2\3/g' | sed -e 's/\,\([0-9]\):\([0-9][0-9]\):\([0-9][0-9]\)/\,0\1\2\3/g' | sed -e 's/://g' > $tmpfile

	    #This nifty command will strip times between 24:00:00 and 48:00:00 down to 000000-235959. Unfortunately it's not exactly needed!
	    #tail -n +2 "stop_times.txt" | sed -e 's/?\ \([0-9]\):\([0-9][0-9]\):\([0-9][0-9]\)/0\1\2\3/g' | sed -e 's/\(^.*\)\(2[4-9]\|[3-4][0-9]\):\([0-9][0-9]\):\([0-9][0-9]\)/echo \1`echo  "$(echo \2 - 24|bc)"`:\3:\4/ge' | sed -e 's/\(^.*\)\(2[4-9]\|[3-4][0-9]\):\([0-9][0-9]\):\([0-9][0-9]\)/echo \1`echo  "$(echo \2 - 24|bc)"`:\3:\4/ge' | sed -e 's/\,\([0-9]\):\([0-9][0-9]\):\([0-9][0-9]\)/\,0\1\2\3/g' | sed -e 's/://g' > $tmpfile
	    (
		echo "create table $table($columns);"
		echo ".separator ,"
		echo ".import $tmpfile $table"
	    ) | $SQ3 $DB.new
	    ;;
    calendar.txt)
	    calendartest2=true
	    table=$(echo `basename $1` | sed -e 's/\..*//')
	    columns=$(cat $file | tr -d '\015' | head -n 1 | sed -e '1 s/^\xef\xbb\xbf//')
	    tail -n +2 "$file" > $tmpfile
	    (
		echo "create table $table($columns);"
		echo ".separator ,"
		echo ".import $tmpfile $table"
	    ) | $SQ3 $DB.new
	    ;;
    routes.txt|trips.txt)
	    table=$(echo `basename $1` | sed -e 's/\..*//')
	    #columns=$(cat $file | tr -d '\015' | sed -e '1 s/^\xef\xbb\xbf//' | head -n 1)
	    columns=$(cat $file | head -n 1 | tr -d '\015' | sed -e '1 s/^\xef\xbb\xbf//')
	    #tail -n +2 "$file" | tr '\r' '\n' > $tmpfile
	    tail -n +2 "$file" | tr -d '\015' | sed -e '/^$/d' | sed -e 's/,\ */,/g' > $tmpfile
	    (
		echo "create table $table($columns);"
		echo ".separator ,"
		echo ".import $tmpfile $table"
	    ) | $SQ3 $DB.new
	    ;;
    calendar_dates.txt)
	    calendartest=true
	    table=$(echo `basename $1` | sed -e 's/\..*//')
	    #columns=$(head -1 $file)
	    columns=$(cat $file | tr -d '\015' | head -n 1 | sed -e '1 s/^\xef\xbb\xbf//')
	    #cat $file | sed -e1d > $tmpfile
	    tail -n +2 "$file" > $tmpfile
	    (
		echo "create table $table($columns);"
		echo ".separator ,"
		echo ".import $tmpfile $table"
	    ) | $SQ3 $DB.new
	    ;;
    *)
	    ;;
    esac
    shift
done

echo "Checking for missing calendar_dates.txt or calendar.txt..."
#This is not robust if some of these files don't exist. In particular, calendar_dates can be excluded. Handle that here:
if ! $calendartest
then
	echo "WARNING: Could not find calendar_dates.txt. Creating an empty table for holiday data..."
	table="calendar_dates"
	columns="service_id,date,exception_type"
	(
		echo "create table $table($columns);"
		echo ".separator ,"
	) | $SQ3 $DB.new
fi

#This is not robust if some of these files don't exist. In particular, calendar can be excluded. Handle that here:
if ! $calendartest2
then
	echo "WARNING: Could not find calendar.txt. Creating an empty table for regular data..."
	table="calendar"
	columns="service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date"
	(
		echo "create table $table($columns);"
		echo ".separator ,"
	) | $SQ3 $DB.new
fi

echo "Creating indices..."
$SQ3 $DB.new <<-EOT
  create index stops_stop_id on stops ( stop_id );
  create index routes_route_id on routes ( route_id );
  create index trips_trip_id on trips ( trip_id );
  create index trips_route_id on trips ( route_id );
  create index stop_times_stop_id_and_time on stop_times ( stop_id, departure_time);
  create index stops_stop_sorted on stops ( stop_sorted );
  create index calendar_dates_date on calendar_dates ( date );
  create index calendar_service_id on calendar ( service_id );
  PRAGMA user_version = $version;
  vacuum;
EOT
###	Indexes used:
###		service_ids on calendar (for each service_id check whether our condition matches it)
###		date on calendar_dates (look for calendar_dates[date] exists?) - append these to our date if exception_type(1), else remove

###		stop_sorted on stops.txt (sorting by longitude: large negative at small number)
###		(stop_id,departure_time) on stop_times.txt
###		trip_id on trips.txt
###		service_ids on routes.txt
###				***no service id on routes.txt - need route_id
###		stop_id on stops.txt (to get stop_name)

###	****departure_time is now hhmmss, with NO COLONS - this enables sorting


echo "Indices created. Renaming and cleaning up..."
mv $DB $DB.old
mv $DB.new $DB
mv $DB.version $DB.version.old

gzip -9v -c $DB > $DB.gz

md5=$(md5sum $DB | cut -f1 -d' ')
size=$(stat -c "%s" $DB.gz)
sizem=$(echo "1k $size 512+ 1024/1024/p" | dc)
echo "$version $sizem $md5" > $DB.version

ls -l $DB $DB.gz $DB.version
cat $DB.version

exit 0
