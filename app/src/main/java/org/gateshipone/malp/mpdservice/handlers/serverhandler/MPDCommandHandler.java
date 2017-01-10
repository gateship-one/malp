/*
 * Copyright (C) 2016 Team Gateship-One
 * (Hendrik Borghorst & Frederik Luetkes)
 *
 * The AUTHORS.md file contains a detailed contributors list:
 * <https://github.com/gateship-one/malp/blob/master/AUTHORS.md>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.gateshipone.malp.mpdservice.handlers.serverhandler;


import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import org.gateshipone.malp.mpdservice.handlers.MPDConnectionStateChangeHandler;
import org.gateshipone.malp.mpdservice.handlers.responsehandler.MPDResponseAlbumList;
import org.gateshipone.malp.mpdservice.handlers.responsehandler.MPDResponseHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.MPDConnection;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDCurrentStatus;

/**
 * This is a subclass of the generic handler that allows to execute simple commands to the connected MPD server.
 * Those commands are short in execution duration and should not return any query results.
 * This ensures that basic control functionality is still available even if another Handler is busy with
 * a long running query.
 */
public class MPDCommandHandler extends MPDGenericHandler {
    private static final String TAG = "MPDCommandHandler";

    /**
     * Name of the thread created for the Looper.
     */
    private static final String THREAD_NAME = "NetCommandHandler";

    /**
     * Value to step the volume. Used for increase/decreaseVolume method.
     */
    private static final int VOLUME_STEP_SIZE = 1;


    /**
     * HandlerThread that is used by the looper. This ensures that all requests to this handler
     * are done multi-threaded and do not block the UI.
     */
    private static HandlerThread mHandlerThread = null;
    private static MPDCommandHandler mHandlerSingleton = null;

    /**
     * Private constructor for use in singleton. Called by the static singleton retrieval method.
     *
     * @param looper Looper of a HandlerThread (that is NOT the UI thread)
     */
    protected MPDCommandHandler(Looper looper) {
        super(looper);
        mMPDConnection.setID("Command");
    }

    /**
     * Private method to ensure that the singleton runs in a separate thread.
     * Otherwise android will deny network access because of UI blocks.
     *
     * @return
     */
    private synchronized static MPDCommandHandler getHandler() {
        // Check if handler was accessed before. If not create the singleton object for the first
        // time.
        if (null == mHandlerSingleton) {
            // Create a new thread used as a looper for this handler.
            // This is the thread in which all messages sent to this handler are handled.
            mHandlerThread = new HandlerThread(THREAD_NAME);
            // It is important to start the thread before using it as a thread for the Handler.
            // Otherwise the handler will cause a crash.
            mHandlerThread.start();
            // Create the actual singleton instance.
            mHandlerSingleton = new MPDCommandHandler(mHandlerThread.getLooper());
        }
        return mHandlerSingleton;
    }


    /**
     * This is the main entry point of messages.
     * Here all possible messages types need to be handled with the MPDConnection.
     * Have a look into the baseclass MPDGenericHandler for more information about the handling.
     *
     * @param msg Message to process.
     */
    @Override
    public void handleMessage(Message msg) {
        // Call the baseclass handleMessage method here to ensure that the messages handled
        // by the baseclass are handled in subclasses as well.
        super.handleMessage(msg);

        // Type checking
        if (!(msg.obj instanceof MPDHandlerAction)) {
            /* Check if the message object is of correct type. Otherwise just abort here. */
            return;
        }

        MPDHandlerAction mpdAction = (MPDHandlerAction) msg.obj;
        /* Catch MPD exceptions here for now. */
        MPDHandlerAction.NET_HANDLER_ACTION action = mpdAction.getAction();

        // Handle all the simple MPD actions here like play, pause, ....
        // None of the actions should result in a returned result like a track list.
        if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_NEXT_SONG) {
            mMPDConnection.nextSong();
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_PREVIOUS_SONG) {
            mMPDConnection.previousSong();
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_STOP) {
            mMPDConnection.stopPlayback();
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_PAUSE) {
            mMPDConnection.pause(true);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_PLAY) {
            MPDCurrentStatus status = mMPDConnection.getCurrentServerStatus();
            MPDCurrentStatus.MPD_PLAYBACK_STATE state = status.getPlaybackState();
            if ( state == MPDCurrentStatus.MPD_PLAYBACK_STATE.MPD_PAUSING ) {
                mMPDConnection.pause(false);
            } else {
                mMPDConnection.playSongIndex(status.getCurrentSongIndex());
            }
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_TOGGLE_PAUSE) {
            MPDCurrentStatus status = mMPDConnection.getCurrentServerStatus();
            MPDCurrentStatus.MPD_PLAYBACK_STATE state = status.getPlaybackState();
            if ( state == MPDCurrentStatus.MPD_PLAYBACK_STATE.MPD_PLAYING ) {
                mMPDConnection.pause(true);
            } else if ( state == MPDCurrentStatus.MPD_PLAYBACK_STATE.MPD_PAUSING ) {
                mMPDConnection.pause(false);
            } else {
                mMPDConnection.playSongIndex(status.getCurrentSongIndex());
            }
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_SET_RANDOM) {
            boolean random = mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_RANDOM) == 1;
            mMPDConnection.setRandom(random);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_SET_REPEAT) {
            boolean repeat = mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_REPEAT) == 1;
            mMPDConnection.setRepeat(repeat);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_SET_SINGLE) {
            boolean single = mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_SINGLE) == 1;
            mMPDConnection.setSingle(single);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_SET_CONSUME) {
            boolean consume = mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_CONSUME) == 1;
            mMPDConnection.setConsume(consume);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_JUMP_INDEX) {
            int index = mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_SONG_INDEX);
            mMPDConnection.playSongIndex(index);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_SEEK_SECONDS) {
            int seconds = mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_SEEK_TIME);
            mMPDConnection.seekSeconds(seconds);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_SET_VOLUME) {
            int volume = mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_VOLUME);
            mMPDConnection.setVolume(volume);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_TOGGLE_OUTPUT) {
            int outputID = mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_OUTPUT_ID);
            mMPDConnection.toggleOutput(outputID);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_UP_VOLUME) {
            MPDCurrentStatus status = mMPDConnection.getCurrentServerStatus();

            // Limit the volume value to 100(%)
            int targetVolume = status.getVolume() + VOLUME_STEP_SIZE;
            if ( targetVolume > 100 ) {
                targetVolume = 100;
            }
            mMPDConnection.setVolume(targetVolume);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_DOWN_VOLUME) {
            MPDCurrentStatus status = mMPDConnection.getCurrentServerStatus();

            // Limit the volume value to 0(%)
            int targetVolume = status.getVolume() - VOLUME_STEP_SIZE;
            if ( targetVolume < 0 ) {
                targetVolume = 0;
            }
            mMPDConnection.setVolume(targetVolume);
        }
    }

    /**
     * These static methods provide the only interface to outside classes.
     * They should not be allowed to interact with the instance itself.
     *
     * All of these methods work with the same principle. They all create an handler message
     * that will contain a MPDHandlerAction as a payload that contains all the information
     * of the requested action with extras.
     */

    /**
     * Set the server parameters for the connection. MUST be called before trying to
     * initiate a connection because it will fail otherwise.
     *
     * @param hostname Hostname or ip address to connect to.
     * @param password Password that is used to authenticate with the server. Can be left empty.
     * @param port     Port to use for the connection. (Default: 6600)
     */
    public static void setServerParameters(String hostname, String password, int port) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_SET_SERVER_PARAMETERS);
        Message msg = Message.obtain();
        if (msg == null) {
            return;
        }
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_SERVER_HOSTNAME, hostname);
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_SERVER_PASSWORD, password);
        action.setIntExtras(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_SERVER_PORT, port);
        msg.obj = action;
        MPDCommandHandler.getHandler().sendMessage(msg);
    }

    /**
     * Connect to the previously configured MPD server.
     */
    public static void connectToMPDServer() {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_CONNECT_MPD_SERVER);
        Message msg = Message.obtain();
        if (msg == null) {
            return;
        }
        msg.obj = action;
        MPDCommandHandler.getHandler().sendMessage(msg);
    }

    /**
     * Disconnect to the previously connected MPD server.
     */
    public static void disconnectFromMPDServer() {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_DISCONNECT_MPD_SERVER);
        Message msg = Message.obtain();
        if ( msg == null ) {
            return;
        }
        msg.obj = action;
        MPDCommandHandler.getHandler().sendMessage(msg);
    }

    /**
     * Resume playback if in pause state.
     */
    public static void play() {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_PLAY);
        Message msg = Message.obtain();
        if (msg == null) {
            return;
        }
        msg.obj = action;
        MPDCommandHandler.getHandler().sendMessage(msg);
    }

    /**
     * Pause playbakc if in playing state.
     */
    public static void pause() {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_PAUSE);
        Message msg = Message.obtain();
        if (msg == null) {
            return;
        }
        msg.obj = action;
        MPDCommandHandler.getHandler().sendMessage(msg);
    }

    /**
     * Pause playbakc if in playing state.
     */
    public static void togglePause() {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_TOGGLE_PAUSE);
        Message msg = Message.obtain();
        if (msg == null) {
            return;
        }
        msg.obj = action;
        MPDCommandHandler.getHandler().sendMessage(msg);
    }

    /**
     * Stops playback. Does not reset current playing index to 0!
     */
    public static void stop() {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_STOP);
        Message msg = Message.obtain();
        if (msg == null) {
            return;
        }
        msg.obj = action;
        MPDCommandHandler.getHandler().sendMessage(msg);
    }


    /**
     * Jumps to next song
     */
    public static void nextSong() {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_NEXT_SONG);
        Message msg = Message.obtain();
        if (msg == null) {
            return;
        }
        msg.obj = action;
        MPDCommandHandler.getHandler().sendMessage(msg);
    }

    /**
     * Jumps to previous song
     */
    public static void previousSong() {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_PREVIOUS_SONG);
        Message msg = Message.obtain();
        if (msg == null) {
            return;
        }
        msg.obj = action;
        MPDCommandHandler.getHandler().sendMessage(msg);
    }

    /**
     * Sets the random value to random
     * @param random Enable/disable server side random
     */
    public static void setRandom(boolean random) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_SET_RANDOM);
        Message msg = Message.obtain();
        if (msg == null) {
            return;
        }

        action.setIntExtras(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_RANDOM, random ? 1 : 0);

        msg.obj = action;
        MPDCommandHandler.getHandler().sendMessage(msg);
    }

    /**
     * Sets the repeat value to repeat-
     * @param repeat Enable/disable server side repeat
     */
    public static void setRepeat(boolean repeat) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_SET_REPEAT);
        Message msg = Message.obtain();
        if (msg == null) {
            return;
        }

        action.setIntExtras(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_REPEAT, repeat ? 1 : 0);

        msg.obj = action;
        MPDCommandHandler.getHandler().sendMessage(msg);
    }

    /**
     * Set single song playback. (Stop after song)
     * @param single Enable/disable single playback
     */
    public static void setSingle(boolean single) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_SET_SINGLE);
        Message msg = Message.obtain();
        if (msg == null) {
            return;
        }

        action.setIntExtras(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_SINGLE, single ? 1 : 0);

        msg.obj = action;
        MPDCommandHandler.getHandler().sendMessage(msg);
    }

    /**
     * Sets consume song after playback on the server.
     * @param consume Enable/disable consume feature
     */
    public static void setConsume(boolean consume) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_SET_CONSUME);
        Message msg = Message.obtain();
        if (msg == null) {
            return;
        }

        action.setIntExtras(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_CONSUME, consume ? 1 : 0);

        msg.obj = action;
        MPDCommandHandler.getHandler().sendMessage(msg);
    }

    /**
     * Jumps to the index in the current playlist. No client side checking of boundaries (but
     * should also not be necessary)
     * @param index Index to play
     */
    public static void playSongIndex(int index) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_JUMP_INDEX);
        Message msg = Message.obtain();
        if (msg == null) {
            return;
        }

        action.setIntExtras(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_SONG_INDEX, index);

        msg.obj = action;
        MPDCommandHandler.getHandler().sendMessage(msg);
    }

    /**
     * Seeks in the currently playing song to the position in seconds defined via the seconds parameter.
     * @param seconds Position to seek to (in seconds)
     */
    public static void seekSeconds(int seconds) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_SEEK_SECONDS);
        Message msg = Message.obtain();
        if (msg == null) {
            return;
        }

        action.setIntExtras(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_SEEK_TIME, seconds);

        msg.obj = action;
        MPDCommandHandler.getHandler().sendMessage(msg);
    }

    /**
     * Sets the server output volume to a percentage value of 0-100%.
     * @param volume Volume in percent (0-100)
     */
    public static void setVolume(int volume) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_SET_VOLUME);
        Message msg = Message.obtain();
        if (msg == null) {
            return;
        }

        action.setIntExtras(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_VOLUME, volume);

        msg.obj = action;
        MPDCommandHandler.getHandler().sendMessage(msg);
    }

    /**
     * Increases the volume a notch
     * */
    public static void increaseVolume() {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_UP_VOLUME);
        Message msg = Message.obtain();
        if (msg == null) {
            return;
        }

        msg.obj = action;
        MPDCommandHandler.getHandler().sendMessage(msg);
    }

    /**
     * Decreases the volume a notch
     * */
    public static void decreaseVolume() {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_DOWN_VOLUME);
        Message msg = Message.obtain();
        if (msg == null) {
            return;
        }

        msg.obj = action;
        MPDCommandHandler.getHandler().sendMessage(msg);
    }

    /**
     * Toggles the output state of the specified output id.
     * @param outputID ID of the output to toggle
     */
    public static void toggleOutput(int outputID) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_TOGGLE_OUTPUT);
        Message msg = Message.obtain();
        if (msg == null) {
            return;
        }

        action.setIntExtras(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_OUTPUT_ID, outputID);

        msg.obj = action;
        MPDCommandHandler.getHandler().sendMessage(msg);
    }

    public static void registerConnectionStateListener(MPDConnectionStateChangeHandler stateHandler) {
        getHandler().internalRegisterConnectionStateListener(stateHandler);
    }

    public static void unregisterConnectionStateListener(MPDConnectionStateChangeHandler stateHandler) {
        getHandler().internalUnregisterConnectionStateListener(stateHandler);
    }
}
