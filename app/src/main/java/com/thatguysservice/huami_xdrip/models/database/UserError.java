package com.thatguysservice.huami_xdrip.models.database;

import static com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry.isLoggingEnabled;

import android.os.AsyncTask;
import android.provider.BaseColumns;

import com.reactiveandroid.ReActiveAndroid;
import com.reactiveandroid.annotation.Column;
import com.reactiveandroid.annotation.Table;
import com.google.gson.annotations.Expose;
import com.reactiveandroid.Model;
import com.reactiveandroid.annotation.PrimaryKey;
import com.reactiveandroid.query.Delete;
import com.reactiveandroid.query.Select;
import com.thatguysservice.huami_xdrip.models.Constants;
import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.models.Pref;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;

//import com.bugfender.sdk.Bugfender;

/**
 * Created by Emma Black on 8/3/15.
 */

@Table(name = "UserErrors", database = AppDatabase.class)
public class UserError extends Model {

    @PrimaryKey
    private Long id;

    private final static String TAG = UserError.class.getSimpleName();

    @Expose
    @Column(name = "shortError")
    public String shortError; // Short error message to be displayed on table

    @Expose
    @Column(name = "message")
    public String message; // Additional text when error is expanded

    @Expose
    @Column(name = "severity")
    public int severity; // int between 1 and 3, 3 being most severe

    // 5 = internal lower level user events
    // 6 = higher granularity user events

    @Expose
    @Column(name = "timestamp")
    public long timestamp; // Time the error was raised

    //todo: rather than include multiples of the same error, should we have a "Count" and just increase that on duplicates?
    //or rather, perhaps we should group up the errors

    public String toString() {
        return severity + " ^ " + Helper.dateTimeText((long) timestamp) + " ^ " + shortError + " ^ " + message;
    }

    public UserError() {
    }

    public UserError(int severity, String shortError, String message) {
        if (!isLoggingEnabled()) return;
        this.severity = severity;
        this.shortError = shortError;
        this.message = message;
        this.timestamp = new Date().getTime();
        this.save();
       /* if (xdrip.useBF) {
            switch (severity) {
                case 2:
                case 3:
                    Bugfender.e(shortError, message);
                    break;
                case 5:
                case 6:
                    Bugfender.w(shortError, message);
                    break;
                default:
                    Bugfender.d(shortError, message);
                    break;
            }
        }*/
    }

    public UserError(String shortError, String message) {
        this(2, shortError, message);
    }

    public static UserError UserErrorHigh(String shortError, String message) {
        return new UserError(3, shortError, message);
    }

    public static UserError UserErrorLow(String shortError, String message) {
        return new UserError(1, shortError, message);
    }

    public static UserError UserEventLow(String shortError, String message) {
        return new UserError(5, shortError, message);
    }

    public static UserError UserEventHigh(String shortError, String message) {
        return new UserError(6, shortError, message);
    }

    // TODO move time calc stuff to JOH, wrap it here with our timestamp
    public String bestTime() {
        final long since = Helper.msSince(timestamp);
        if (since < Constants.DAY_IN_MS) {
            return Helper.hourMinuteString(timestamp);
        } else {
            return Helper.dateTimeText(timestamp);
        }
    }


    public static void cleanup() {
        new Cleanup().execute(deletable());
    }

    public static void cleanupByTimeAndClause(final long timestamp, final String clause) {
        Delete.from(UserError.class)
                .where("timestamp < ? AND ?%", timestamp, clause)
                .execute();
    }

    public synchronized static void cleanupRaw() {
        final long timestamp = Helper.tsl();
        cleanupByTimeAndClause(timestamp - Constants.DAY_IN_MS, "severity < 3");
        cleanupByTimeAndClause(timestamp - Constants.DAY_IN_MS * 2, "severity = 3");
        cleanupByTimeAndClause(timestamp - Constants.DAY_IN_MS * 2, "severity > 3");
        ReActiveAndroid.getModelAdapter(UserError.class).getModelCache().clear();
    }


    public static List<UserError> all() {
        return Select.from(UserError.class).orderBy("timestamp desc").fetch();
    }

    public static List<UserError> deletable() {
        List<UserError> userErrors = Select
                .from(UserError.class)
                .where("severity < ? AND timestamp < ? ", 3, (new Date().getTime() - 1000 * 60 * 60 * 24))
                .orderBy("timestamp desc")
                .fetch();
        List<UserError> highErrors = Select
                .from(UserError.class)
                .where("severity = ? AND timestamp < ?", 3, (new Date().getTime() - 1000 * 60 * 60 * 24 * 2))
                .orderBy("timestamp desc")
                .fetch();
        List<UserError> events = Select
                .from(UserError.class)
                .where("severity > ? AND timestamp < ?", 3, (new Date().getTime() - 1000 * 60 * 60 * 24 * 2))
                .orderBy("timestamp desc")
                .fetch();
        userErrors.addAll(highErrors);
        userErrors.addAll(events);
        return userErrors;
    }

    public static List<UserError> bySeverity(Integer[] levels) {
        String levelsString = " ";
        for (int level : levels) {
            levelsString += level + ",";
        }
        Log.d("UserError", "severity in (" + levelsString.substring(0, levelsString.length() - 1) + ")");
        return Select
                .from(UserError.class)
                .where("severity in (" + levelsString.substring(0, levelsString.length() - 1) + ")")
                .orderBy("timestamp desc")
                .limit(10000)//too many data can kill akp
                .fetch();
    }

    public static List<UserError> bySeverityNewerThanID(long id, Integer[] levels, int limit) {
        String levelsString = " ";
        for (int level : levels) {
            levelsString += level + ",";
        }
        Log.d("UserError", "severity in (" + levelsString.substring(0, levelsString.length() - 1) + ")");
        return Select
                .from(UserError.class)
                .where("id > ? AND severity in (" + levelsString.substring(0, levelsString.length() - 1) + ")", id)
                .orderBy("timestamp desc")
                .limit(limit)
                .fetch();
    }

    public static List<UserError> newerThanID(long id, int limit) {
        return Select
                .from(UserError.class)
                .where("id > ?", id)
                .orderBy("timestamp desc")
                .limit(limit)
                .fetch();
    }

    public static List<UserError> olderThanID(long id, int limit) {
        return Select
                .from(UserError.class)
                .where("id < ?", id)
                .orderBy("timestamp desc")
                .limit(limit)
                .fetch();
    }

    public static List<UserError> bySeverityOlderThanID(long id, Integer[] levels, int limit) {
        String levelsString = " ";
        for (int level : levels) {
            levelsString += level + ",";
        }
        Log.d("UserError", "severity in (" + levelsString.substring(0, levelsString.length() - 1) + ")");
        return Select
                .from(UserError.class)
                .where("id < ? AND severity in (" + levelsString.substring(0, levelsString.length() - 1) + ")", id)
                .orderBy("timestamp desc")
                .limit(limit)
                .fetch();
    }


    public static UserError getForTimestamp(UserError error) {
        try {
            return Select
                    .from(UserError.class)
                    .where("timestamp = ? AND shortError = ? AND message = ?", error.timestamp, error.shortError, error.message)
                    .fetchSingle();
        } catch (Exception e) {
            Log.e(TAG, "getForTimestamp() Got exception on Select : " + e.toString());
            return null;
        }
    }

    private static class Cleanup extends AsyncTask<List<UserError>, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(List<UserError>... errors) {
            try {
                for (UserError userError : errors[0]) {
                    userError.delete();
                    userError.save();
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    public static List<UserError> bySeverity(int level) {
        return bySeverity(new Integer[]{level});
    }

    public static List<UserError> bySeverity(int level, int level2) {
        return bySeverity(new Integer[]{level, level2});
    }

    public static List<UserError> bySeverity(int level, int level2, int level3) {
        return bySeverity(new Integer[]{level, level2, level3});
    }


    public static class Log {
        public static void e(String a, String b) {
            android.util.Log.e(a, b);
            new UserError(a, b);
        }

        public static void e(String tag, String b, Exception e) {
            android.util.Log.e(tag, b, e);
            new UserError(tag, b + "\n" + e.toString());
        }

        public static void w(String tag, String b) {
            android.util.Log.w(tag, b);
            UserError.UserErrorLow(tag, b);
        }

        public static void w(String tag, String b, Exception e) {
            android.util.Log.w(tag, b, e);
            UserError.UserErrorLow(tag, b + "\n" + e.toString());
        }

        public static void wtf(String tag, String b) {
            android.util.Log.wtf(tag, b);
            UserError.UserErrorHigh(tag, b);
        }

        public static void wtf(String tag, String b, Exception e) {
            android.util.Log.wtf(tag, b, e);
            UserError.UserErrorHigh(tag, b + "\n" + e.toString());
        }

        public static void wtf(String tag, Exception e) {
            android.util.Log.wtf(tag, e);
            UserError.UserErrorHigh(tag, e.toString());
        }

        public static void uel(String tag, String b) {
            android.util.Log.i(tag, b);
            UserError.UserEventLow(tag, b);
        }

        public static void ueh(String tag, String b) {
            android.util.Log.i(tag, b);
            UserError.UserEventHigh(tag, b);
        }

        public static void d(String tag, String b) {
            android.util.Log.d(tag, b);
            if (ExtraLogTags.shouldLogTag(tag, android.util.Log.DEBUG)) {
                UserErrorLow(tag, b);
            }
        }

        public static void v(String tag, String b) {
            android.util.Log.v(tag, b);
            if (ExtraLogTags.shouldLogTag(tag, android.util.Log.VERBOSE)) {
                UserErrorLow(tag, b);
            }
        }

        public static void i(String tag, String b) {
            android.util.Log.i(tag, b);
            if (ExtraLogTags.shouldLogTag(tag, android.util.Log.INFO)) {
                UserErrorLow(tag, b);
            }
        }

        static ExtraLogTags extraLogTags = new ExtraLogTags();
    }

    public static class ExtraLogTags {

        static Hashtable<String, Integer> extraTags;

        ExtraLogTags() {
            extraTags = new Hashtable<String, Integer>();
            String extraLogs = Pref.getStringDefaultBlank("extra_tags_for_logging");
            readPreference(extraLogs);
        }

        /*
         * This function reads a string representing tags that the user wants to log
         * Format of string is tag1:level1,tag2,level2
         * Example of string is Alerts:i,BG:W
         *
         */
        public static void readPreference(String extraLogs) {
            extraLogs = extraLogs.trim();
            if (extraLogs.length() > 0) UserErrorLow(TAG, "called with string " + extraLogs);
            extraTags.clear();

            // allow splitting to work with a single entry and no delimiter zzz
            if ((extraLogs.length() > 1) && (!extraLogs.contains(","))) {
                extraLogs += ",";
            }
            String[] tags = extraLogs.split(",");
            if (tags.length == 0) {
                return;
            }

            // go over all tags and parse them
            for (String tag : tags) {
                if (tag.length() > 0) parseTag(tag);
            }
        }

        static void parseTag(String tag) {
            // Format is tag:level for example  Alerts:i
            String[] tagAndLevel = tag.trim().split(":");
            if (tagAndLevel.length != 2) {
                Log.e(TAG, "Failed to parse " + tag);
                return;
            }
            String level = tagAndLevel[1];
            String tagName = tagAndLevel[0].toLowerCase();
            if (level.compareTo("d") == 0) {
                extraTags.put(tagName, android.util.Log.DEBUG);
                UserErrorLow(TAG, "Adding tag with DEBUG " + tagAndLevel[0]);
                return;
            }
            if (level.compareTo("v") == 0) {
                extraTags.put(tagName, android.util.Log.VERBOSE);
                UserErrorLow(TAG, "Adding tag with VERBOSE " + tagAndLevel[0]);
                return;
            }
            if (level.compareTo("i") == 0) {
                extraTags.put(tagName, android.util.Log.INFO);
                UserErrorLow(TAG, "Adding tag with info " + tagAndLevel[0]);
                return;
            }
            Log.e(TAG, "Unknown level for tag " + tag + " please use d v or i");
        }

        public static boolean shouldLogTag(final String tag, final int level) {
            //final Integer levelForTag = extraTags.get(tag != null ? tag.toLowerCase() : "");
            //return levelForTag != null && level >= levelForTag;
            return true;
        }

    }
}
