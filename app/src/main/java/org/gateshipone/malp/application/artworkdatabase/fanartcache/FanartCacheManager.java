package org.gateshipone.malp.application.artworkdatabase.fanartcache;


import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FanartCacheManager {
    private static final String TAG = FanartCacheManager.class.getSimpleName();
    private static final int MAX_ARTISTS_CACHED = 10;
    private static final String FANART_CACHE_SUFFIX = "/fanart";

    private static final String IMAGE_FILENAME_PREFIX = "image_";

    private static final int SESSION_LRU_MAX = 10;

    /**
     * The maximum size of the applications cache. If it gets too big it will be trimmed.
     */
    private static final long MAX_CACHE_SIZE = 100 * 1024 * 1024;

    private final String mCacheBasePath;

    private List<String> mLastAccessedMBIDs;

    public FanartCacheManager(Context context) {
        String cachePath = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                !Environment.isExternalStorageRemovable() ? context.getExternalCacheDir().getPath() :
                context.getCacheDir().getPath();
        mCacheBasePath = cachePath + FANART_CACHE_SUFFIX;

        mLastAccessedMBIDs = new LinkedList<>();
    }

    /**
     * Checks how many entries for the given MBID are available
     *
     * @param mbid MBID to check for cached entries
     * @return number of cached entries for mbid
     */
    public synchronized int getFanartCount(String mbid) {
        File artistDir = new File(mCacheBasePath + "/" + mbid);
        if (!artistDir.exists()) {
            return 0;
        }


        return artistDir.list().length;
    }

    /**
     * Fetches an image from the cache.
     *
     * @param mbid  MBID to get the cached fanart for
     * @param index Index of the fanart that is requested
     * @return File object that contains the image
     */
    public synchronized File getFanart(String mbid, int index) {
        if (index >= getFanartCount(mbid)) {
            return null;
        }

        // Remove from LRU list if it was in there
        mLastAccessedMBIDs.remove(mbid);
        mLastAccessedMBIDs.add(0, mbid);
        File artistDir = new File(mCacheBasePath + "/" + mbid);
        File[] subFiles = artistDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                if ( file.isFile()) {
                    return true;
                }
                return false;
            }
        });
        File retFile = subFiles[index];
        return retFile;
    }

    /**
     * Adds a new fanart. This should check if old entries can be removed.
     * After the count reaches MAX_ARTISTS_CACHED it should remove the LRU entries
     *
     * @param mbid
     * @param image
     */
    public synchronized void addFanart(String mbid, String name,byte[] image) {
        int newIndex = getFanartCount(mbid);
        Log.v(TAG,"Add fanart: " + newIndex + "for mbid: " + mbid);

        File outputDir = new File(mCacheBasePath + "/" + mbid);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File outputFile = new File(mCacheBasePath + "/" + mbid + "/" + name);
        if(!outputFile.exists()) {
            FileOutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(outputFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            try {
                outputStream.write(image);
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        mLastAccessedMBIDs.add(mbid);
        long cacheSize = getCacheSize();
        Log.v(TAG, "Cache is now " + cacheSize / (1024 * 1024) + "MB in size");
        if ( cacheSize > MAX_CACHE_SIZE ) {
            trimCache();
        }
    }

    public synchronized boolean inCache(String mbid, String name) {
        Log.v(TAG,"Check if exists: " + mCacheBasePath + "/" + mbid + "/" + name );
        File checkFile = new File(mCacheBasePath + "/" + mbid + "/" + name);
        return checkFile.exists();
    }

    private void trimCache() {
        Log.v(TAG, "Trim of cache started");

        // Get a list of remaining MBIDs
        List<String> lruEntries = new ArrayList<>();
        for (int i = 0; i < SESSION_LRU_MAX && i < mLastAccessedMBIDs.size(); i++) {
            lruEntries.add(mLastAccessedMBIDs.get(i));
            Log.v(TAG,"Entry : " + mLastAccessedMBIDs.get(i) + "Should not be removed");
        }

        mLastAccessedMBIDs.clear();
        mLastAccessedMBIDs.addAll(lruEntries);

        File cacheDir = new File(mCacheBasePath);
        for (File subFile : cacheDir.listFiles()) {
            Log.v(TAG,"Check entry: " + subFile.getName());
            if (!lruEntries.contains(subFile.getName())) {
                Log.v(TAG, "Removing cache entry for: " + subFile.getName());
                deleteDirectory(subFile);
            }
        }
    }

    private void deleteDirectory(File url) {
        for (File subFile : url.listFiles()) {
            if (subFile.isFile()) {
                Log.v(TAG, "Removing file: " + subFile.getPath());
                subFile.delete();
            } else {
                deleteDirectory(subFile);
            }
        }
        url.delete();
    }

    private long getDirectorySize(File dir) {
        long retSize = 0;
        for (File subFile : dir.listFiles()) {
            if (subFile.isFile()) {
                retSize += subFile.length();
            } else {
                retSize += getDirectorySize(subFile);
            }
        }
        return retSize;
    }

    private long getCacheSize() {
        return getDirectorySize(new File(mCacheBasePath));
    }
}
