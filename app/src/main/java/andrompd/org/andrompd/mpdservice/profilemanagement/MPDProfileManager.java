package andrompd.org.andrompd.mpdservice.profilemanagement;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class MPDProfileManager {

    /**
     * Database to use for all operations
     */
    private SQLiteDatabase mDatabase;

    /**
     * Instance of the helper class to initialize the database.
     */
    private MPDProfileDBHelper mDBHelper;

    public MPDProfileManager(Context context) {
        /* Create instance of the helper class to get the writable DB later. */
        mDBHelper = new MPDProfileDBHelper(context);

        /* Get a writable database here. */
        mDatabase = mDBHelper.getWritableDatabase();
    }

    /**
     * Creates a list of all available server profiles.
     * @return The list of currently saved server profiles.
     */
    public List<MPDServerProfile> getProfiles() {
        ArrayList<MPDServerProfile> profileList = new ArrayList<>();

        /* Query the database table for profiles */
        Cursor cursor  = mDatabase.query(MPDServerProfileTable.SQL_TABLE_NAME, MPDServerProfileTable.PROJECTION_SERVER_PROFILES, null, null, null, null, MPDServerProfileTable.COLUMN_PROFILE_NAME);


        /* Iterate over the cursor and create MPDServerProfile objects */
        if ( cursor.moveToFirst() ) {
            do {
                /* Profile parameters */
                String profileName = cursor.getString(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_PROFILE_NAME));
                boolean autoConnect = cursor.getInt(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_PROFILE_AUTO_CONNECT)) == 1;

                /* Server parameters */
                String serverHostname = cursor.getString(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_SERVER_HOSTNAME));
                String serverPassword = cursor.getString(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_SERVER_PASSWORD));
                int serverPort = cursor.getInt(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_SERVER_PORT));

                /* Create temporary object to append to list. */
                MPDServerProfile profile = new MPDServerProfile(profileName, autoConnect);
                profile.setHostname(serverHostname);
                profile.setPassword(serverPassword);
                profile.setPort(serverPort);

                /* Finish and add to list */
                profileList.add(profile);
            } while ( cursor.moveToNext() );
        }

        cursor.close();

        return profileList;
    }

    /**
     * Adds a new server profile. There is no way to change a profile directly in the table.
     * Just delete and readd the profile.
     * @param profile Profile to add to the database.
     */
    public void addProfile(MPDServerProfile profile) {
        /* Check if autoconnect is set, if it is, all other autoconnects need to be set to 0 */
        if ( profile.getAutoconnect() ) {
            ContentValues autoConValues = new ContentValues();
            autoConValues.put(MPDServerProfileTable.COLUMN_PROFILE_AUTO_CONNECT, 0);

            /* Update the table columns to 0. */
            mDatabase.update(MPDServerProfileTable.SQL_TABLE_NAME, autoConValues, MPDServerProfileTable.COLUMN_PROFILE_AUTO_CONNECT + "=?", new String[]{"1"});
        }


        /* Prepare the sql transaction */
        ContentValues values = new ContentValues();

        /* Profile parameters */
        values.put(MPDServerProfileTable.COLUMN_PROFILE_NAME, profile.getProfileName());
        values.put(MPDServerProfileTable.COLUMN_PROFILE_AUTO_CONNECT, profile.getAutoconnect());

        /* Server parameter */
        values.put(MPDServerProfileTable.COLUMN_SERVER_HOSTNAME, profile.getHostname());
        values.put(MPDServerProfileTable.COLUMN_SERVER_PASSWORD, profile.getPassword());
        values.put(MPDServerProfileTable.COLUMN_SERVER_PORT, profile.getPort());

        /* Insert the table in the database */
        mDatabase.insert(MPDServerProfileTable.SQL_TABLE_NAME, null, values);
    }


    /**
     * Removes a profile from the database. Make sure that you provide the correct profile.
     * @param profile Profile to remove.
     */
    public void deleteProfile(MPDServerProfile profile) {
        /* Create the where clauses */
        String whereClause = MPDServerProfileTable.COLUMN_PROFILE_NAME + "=? AND ";
        whereClause += MPDServerProfileTable.COLUMN_SERVER_HOSTNAME + "=? AND";
        whereClause += MPDServerProfileTable.COLUMN_SERVER_PASSWORD + "=? AND";
        whereClause += MPDServerProfileTable.COLUMN_SERVER_PORT + "=? AND";

        String[] whereValues = {profile.getProfileName(), profile.getHostname(), profile.getPassword(), String.valueOf(profile.getPort())};

        mDatabase.delete(MPDServerProfileTable.SQL_TABLE_NAME,whereClause, whereValues);
    }


}
