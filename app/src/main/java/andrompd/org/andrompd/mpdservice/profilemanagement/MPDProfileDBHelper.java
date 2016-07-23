package andrompd.org.andrompd.mpdservice.profilemanagement;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MPDProfileDBHelper extends SQLiteOpenHelper{
    /**
     * Database name for the profiles database
     */
    public static final String DATABASE_NAME = "andrompd_database";

    /**
     * Database version, used for migrating to new versions.
     */
    public static final int DATABASE_VERSION = 1;

    /**
     * Constructor to create the database.
     * @param context Application context to create the database in.
     */
    public MPDProfileDBHelper(Context context) {
        super(context,DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Initializes the tables of this database.
     * Should call all table helpers.
     * @param database Database to use for tables.
     */
    @Override
    public void onCreate(SQLiteDatabase database) {
        MPDServerProfileTable.onCreate(database);
    }

    /**
     * Method to migrate the database to a new version. Nothing implemented for now.
     * @param database Database to migrate to a different version.
     * @param oldVersion Old version of the database to migrate from
     * @param newVersion New version of the database to migrate to
     */
    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        /**
         * No migrations necessary for now.
         */
    }
}
