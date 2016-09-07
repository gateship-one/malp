package org.gateshipone.malp.application.artworkdatabase;


import android.content.Context;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;


public class MALPRequestQueue extends RequestQueue {

    private Cache mCache;
    private Network mNetwork;

    private static MALPRequestQueue mInstance;

    private MALPRequestQueue(Cache cache, Network network) {
        super(cache, network);
        mCache = cache;
        mNetwork = network;
    }

    public synchronized static MALPRequestQueue getInstance(Context context) {
        if ( null == mInstance ) {
            Network network = new BasicNetwork(new HurlStack());
            // 10MB disk cache
            Cache cache = new DiskBasedCache(context.getCacheDir(), 1024 * 1024 * 10);

            mInstance = new MALPRequestQueue(cache,network);
            mInstance.start();
        }
        return mInstance;
    }
}
