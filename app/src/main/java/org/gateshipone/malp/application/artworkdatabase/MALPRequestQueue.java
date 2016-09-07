package org.gateshipone.malp.application.artworkdatabase;


import android.content.Context;
import android.util.Log;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;

import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;


public class MALPRequestQueue extends RequestQueue implements RequestQueue.RequestFinishedListener {
    private static final String TAG = MALPRequestQueue.class.getSimpleName();
    private Cache mCache;
    private Network mNetwork;

    private Timer mLimiterTimer;

    private static MALPRequestQueue mInstance;

    private final Queue<Request<?>> mLimitingRequestQueue;

    /**
     * Wait 1000ms between every request
     */
    private static final int REQUEST_RATE = 1000;

    private MALPRequestQueue(Cache cache, Network network) {
        super(cache, network, 1);
        mCache = cache;
        mNetwork = network;
        mLimitingRequestQueue = new LinkedBlockingQueue<>();
        mLimiterTimer = null;
        super.addRequestFinishedListener(this);
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

    @Override
    public void onRequestFinished(Request request) {
        Log.v(TAG,"Request finished");
    }


    @Override
    public <T> Request<T> add(Request<T> request) {
        if ( null == request ) {
            return null;
        }
        Log.v(TAG,"RATE LIMITING REQUEST ADDED");
        synchronized (mLimitingRequestQueue ) {
            mLimitingRequestQueue.add(request);
            if (null == mLimiterTimer) {
                // Timer currently not running
                mLimiterTimer = new Timer();
                mLimiterTimer.schedule(new LimiterTask(), 0, REQUEST_RATE);
            }
        }
        return request;
    }


    private <T> void realAddRequest(Request<T> request) {
        super.add(request);
    }

    private class LimiterTask extends TimerTask {
        @Override
        public void run() {
            synchronized (mLimitingRequestQueue ) {
                Request request = mLimitingRequestQueue.poll();
                if (null != request) {
                    realAddRequest(request);
                    Log.v(TAG,"RATE LIMITING FORWARED");
                } else {
                    // Stop the timer, no requests left
                    mLimiterTimer.cancel();
                    mLimiterTimer.purge();
                    mLimiterTimer = null;
                    Log.v(TAG,"RATE LIMITING EMPTY, STOPPING");
                }
            }

        }
    }

    /**
     * Cancels all requests in this queue for which the given filter applies.
     * @param filter The filtering function to use
     */
    public void cancelAll(RequestFilter filter) {
        super.cancelAll(filter);
        synchronized (mLimitingRequestQueue) {
            for (Request<?> request : mLimitingRequestQueue) {
                if (filter.apply(request)) {
                    Log.v(TAG,"Canceling request: " + request);
                    request.cancel();
                    mLimitingRequestQueue.remove(request);
                }
            }
        }
    }

}
