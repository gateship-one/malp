// IBackgroundService.aidl
package org.gateshipone.malp.application.background;

// Declare any non-default types here with import statements

interface IBackgroundService {

    void stopStreamingPlayback();
    void startStreamingPlayback();

    boolean isPlayingStream();

}
