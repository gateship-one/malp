package andrompd.org.andrompd.mpdservice.service;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;

import andrompd.org.andrompd.mpdservice.mpdprotocol.MPDConnection;

/**
 * Created by hendrik on 16.07.16.
 */
public class MPDIntentService extends IntentService {
    private static final String pServiceName = "org.andrompd.mpd_connection_service";
    private static final String TAG = "MPDService";

    private MPDConnection pServerConnection;

    public MPDIntentService() {
        super(pServiceName);
        pServerConnection = null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v(TAG,"Received intent: " + intent.toString());
        /* Handle all MPD intents here */
        if ( intent.getAction().equals(MPDIntents.MPD_OPEN_CONNECTION_INTENT)) {
            Log.v(TAG,"MPD connection open requested");

            /* Check for the necessary extras */

            /* Check if a connection already exists and if not create one. */
            if ( pServerConnection == null ) {
                pServerConnection = new MPDConnection("daenerys","",6600);
            }

            try {
                pServerConnection.connectToServer();
            } catch (IOException e) {
                Log.e(TAG, "Error during connection attempt");
                e.printStackTrace();
            }
        }
    }
}
