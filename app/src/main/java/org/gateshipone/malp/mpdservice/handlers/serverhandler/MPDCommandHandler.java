/*
 *  Copyright (C) 2018 Team Gateship-One
 *  (Hendrik Borghorst & Frederik Luetkes)
 *
 *  The AUTHORS.md file contains a detailed contributors list:
 *  <https://github.com/gateship-one/malp/blob/master/AUTHORS.md>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.gateshipone.malp.mpdservice.handlers.serverhandler;


import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import org.gateshipone.malp.mpdservice.mpdprotocol.MPDException;
import org.gateshipone.malp.mpdservice.mpdprotocol.MPDInterface;
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
     * HandlerThread that is used by the looper. This ensures that all requests to this handler
     * are done multi-threaded and do not block the UI.
     */
    private static HandlerThread mHandlerThread = null;
    private static MPDCommandHandler mHandlerSingleton = null;

    private boolean mSeekActive;
    private int mRequestedSeekVal;

    private boolean mSetVolumeActive;
    private int mRequestVolume;

    /**
     * Private constructor for use in singleton. Called by the static singleton retrieval method.
     *
     * @param looper Looper of a HandlerThread (that is NOT the UI thread)
     */
    protected MPDCommandHandler(Looper looper) {
        super(looper);
    }

    /**
     * Private method to ensure that the singleton runs in a separate thread.
     * Otherwise android will deny network access because of UI blocks.
     *
     * @return
     */
    public synchronized static MPDCommandHandler getHandler() {
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

        try {
            // Handle all the simple MPD actions here like play, pause, ....
            // None of the actions should result in a returned result like a track list.
            if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_NEXT_SONG) {
                MPDInterface.mInstance.nextSong();
            } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_PREVIOUS_SONG) {
                MPDInterface.mInstance.previousSong();
            } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_STOP) {
                MPDInterface.mInstance.stopPlayback();
            } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_PAUSE) {
                MPDInterface.mInstance.pause(true);
            } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_PLAY) {
                MPDCurrentStatus status = MPDInterface.mInstance.getCurrentServerStatus();
                MPDCurrentStatus.MPD_PLAYBACK_STATE state = status.getPlaybackState();
                if (state == MPDCurrentStatus.MPD_PLAYBACK_STATE.MPD_PAUSING) {
                    MPDInterface.mInstance.pause(false);
                } else {
                    MPDInterface.mInstance.playSongIndex(status.getCurrentSongIndex());
                }
            } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_TOGGLE_PAUSE) {
                MPDCurrentStatus status = MPDInterface.mInstance.getCurrentServerStatus();
                MPDCurrentStatus.MPD_PLAYBACK_STATE state = status.getPlaybackState();
                if (state == MPDCurrentStatus.MPD_PLAYBACK_STATE.MPD_PLAYING) {
                    MPDInterface.mInstance.pause(true);
                } else if (state == MPDCurrentStatus.MPD_PLAYBACK_STATE.MPD_PAUSING) {
                    MPDInterface.mInstance.pause(false);
                } else {
                    MPDInterface.mInstance.playSongIndex(status.getCurrentSongIndex());
                }
            } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_SET_RANDOM) {
                boolean random = mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_RANDOM) == 1;
                MPDInterface.mInstance.setRandom(random);
            } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_SET_REPEAT) {
                boolean repeat = mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_REPEAT) == 1;
                MPDInterface.mInstance.setRepeat(repeat);
            } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_SET_SINGLE) {
                boolean single = mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_SINGLE) == 1;
                MPDInterface.mInstance.setSingle(single);
            } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_SET_CONSUME) {
                boolean consume = mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_CONSUME) == 1;
                MPDInterface.mInstance.setConsume(consume);
            } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_JUMP_INDEX) {
                int index = mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_SONG_INDEX);
                MPDInterface.mInstance.playSongIndex(index);
            } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_SEEK_SECONDS) {
                int seekTo = -1;

                // Wait for mRequestedSeekVal to settle to the requested value.
                // This is necessary to prevent a flood of seek commands to MPD which will
                // queue up in this handler queue and block MPD for a long time with unnecessary
                // seek operations.
                while (seekTo != mRequestedSeekVal) {
                    seekTo = mRequestedSeekVal;
                    MPDInterface.mInstance.seekSeconds(seekTo);
                }

                mSeekActive = false;
            } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_SET_VOLUME) {
                int volume = -1;

                while (volume != mRequestVolume) {
                    volume = mRequestVolume;
                    MPDInterface.mInstance.setVolume(volume);
                }

                mSetVolumeActive = false;
            } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_TOGGLE_OUTPUT) {
                int outputID = mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_OUTPUT_ID);
                MPDInterface.mInstance.toggleOutput(outputID);
            } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_ENABLE_OUTPUT) {
                int outputID = mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_OUTPUT_ID);
                MPDInterface.mInstance.enableOutput(outputID);
            } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_DISABLE_OUTPUT) {
                int outputID = mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_OUTPUT_ID);
                MPDInterface.mInstance.disableOutput(outputID);
            } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_UP_VOLUME) {
                MPDCurrentStatus status = MPDInterface.mInstance.getCurrentServerStatus();

                // Get step size from message
                int stepSize = mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_VOLUME);

                // Limit the volume value to 100(%)
                int targetVolume = status.getVolume() + stepSize;
                if (targetVolume > 100) {
                    targetVolume = 100;
                }
                MPDInterface.mInstance.setVolume(targetVolume);
            } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_DOWN_VOLUME) {
                MPDCurrentStatus status = MPDInterface.mInstance.getCurrentServerStatus();

                // Get step size from message
                int stepSize = mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_VOLUME);

                // Limit the volume value to 0(%)
                int targetVolume = status.getVolume() - stepSize;
                if (targetVolume < 0) {
                    targetVolume = 0;
                }
                MPDInterface.mInstance.setVolume(targetVolume);
            }
        } catch (MPDException e) {
            handleMPDError(e);
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
        MPDCommandHandler handler = getHandler();
        if(!handler.mSeekActive) {
            handler.mSeekActive = true;
            MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_SEEK_SECONDS);
            Message msg = Message.obtain();
            if (msg == null) {
                return;
            }

            handler.mRequestedSeekVal = seconds;

            msg.obj = action;
            MPDCommandHandler.getHandler().sendMessage(msg);
        } else {
            // If a seek operation is already ongoing, set the new requested volume. The handleMessage
            // will seek to the last requested position until now further change is necessary.
            handler.mRequestedSeekVal = seconds;
        }
    }

    /**
     * Sets the server output volume to a percentage value of 0-100%.
     * @param volume Volume in percent (0-100)
     */
    public static void setVolume(int volume) {
        MPDCommandHandler handler = getHandler();
        if(!handler.mSetVolumeActive) {
            handler.mSetVolumeActive = true;
            MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_SET_VOLUME);
            Message msg = Message.obtain();
            if (msg == null) {
                return;
            }

            handler.mRequestVolume = volume;

            msg.obj = action;
            MPDCommandHandler.getHandler().sendMessage(msg);
        } else {
            handler.mRequestVolume = volume;
        }
    }

    /**
     * Increases the volume a notch
     * */
    public static void increaseVolume(int stepSize) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_UP_VOLUME);
        Message msg = Message.obtain();
        if (msg == null) {
            return;
        }

        action.setIntExtras(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_VOLUME, stepSize);

        msg.obj = action;
        MPDCommandHandler.getHandler().sendMessage(msg);
    }

    /**
     * Decreases the volume a notch
     * */
    public static void decreaseVolume(int stepSize) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_DOWN_VOLUME);
        Message msg = Message.obtain();
        if (msg == null) {
            return;
        }

        action.setIntExtras(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_VOLUME, stepSize);

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

    /**
     * Enable the output of the specified output id.
     * @param outputID ID of the output to enable
     */
    public static void enableOutput(int outputID) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_ENABLE_OUTPUT);
        Message msg = Message.obtain();
        if (msg == null) {
            return;
        }

        action.setIntExtras(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_OUTPUT_ID, outputID);

        msg.obj = action;
        MPDCommandHandler.getHandler().sendMessage(msg);
    }

    /**
     * Disable the output of the specified output id.
     * @param outputID ID of the output to disable
     */
    public static void disableOutput(int outputID) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_DISABLE_OUTPUT);
        Message msg = Message.obtain();
        if (msg == null) {
            return;
        }

        action.setIntExtras(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_OUTPUT_ID, outputID);

        msg.obj = action;
        MPDCommandHandler.getHandler().sendMessage(msg);
    }
}
