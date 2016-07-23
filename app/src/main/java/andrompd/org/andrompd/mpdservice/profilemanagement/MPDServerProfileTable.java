package andrompd.org.andrompd.mpdservice.profilemanagement;


import android.database.sqlite.SQLiteDatabase;

public class MPDServerProfileTable {
    /**
     * Table name of the SQL table inside a database
     */
    public static final String SQL_TABLE_NAME = "andrompd_mpd_server_profiles";

    /**
     * Column descriptions
     */
    public static final String COLUMN_PROFILE_NAME = "profile_name";
    public static final String COLUMN_SERVER_HOSTNAME = "server_hostname";
    public static final String COLUMN_SERVER_PASSWORD = "server_password";
    public static final String COLUMN_SERVER_PORT = "server_port";
    public static final String COLUMN_PROFILE_AUTO_CONNECT = "autoconnect";

    /**
     * Projection string array used for queries on this table
     */
    public static final String[] PROJECTION_SERVER_PROFILES = {COLUMN_PROFILE_NAME, COLUMN_PROFILE_AUTO_CONNECT,
        COLUMN_SERVER_HOSTNAME, COLUMN_SERVER_PASSWORD, COLUMN_SERVER_PORT
    };


    /**
     * String to initially create the table
     */
    public static final String DATABASE_CREATE = "create table if not exists " +  SQL_TABLE_NAME + " (" +
            COLUMN_PROFILE_NAME + " text," + COLUMN_PROFILE_AUTO_CONNECT + " integer," +
            COLUMN_SERVER_HOSTNAME + " text," + COLUMN_SERVER_PASSWORD + " integer," +
            COLUMN_SERVER_PORT + " integer );";

    /**
     * Creates the inital database table.
     * @param database Database to use for table creation.
     */
    public static void onCreate(SQLiteDatabase database) {
        /*
         * Create table in the given database here.
         */
        database.execSQL(DATABASE_CREATE);
    }
}
