/*
 * Copyright (C) 2016  Hendrik Borghorst
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package andrompd.org.andrompd.mpdservice.handlers.serverhandler;


import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import andrompd.org.andrompd.mpdservice.handlers.responsehandler.MPDResponseAlbumList;
import andrompd.org.andrompd.mpdservice.handlers.responsehandler.MPDResponseHandler;
import andrompd.org.andrompd.mpdservice.mpdprotocol.MPDConnection;

public class MPDCommandHandler extends MPDGenericHandler implements MPDConnection.MPDConnectionIdleChangeListener {

    private static final String TAG = "MPDCommandHandler";
    private static final String THREAD_NAME = "NetCommandHandler";


    private static HandlerThread mHandlerThread = null;
    private static MPDCommandHandler mHandlerSingleton = null;


    /**
     * Private constructor for use in singleton.
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
    private synchronized static MPDCommandHandler getHandler() {
        if (null == mHandlerSingleton) {
            mHandlerThread = new HandlerThread(THREAD_NAME);
            mHandlerThread.start();
            mHandlerSingleton = new MPDCommandHandler(mHandlerThread.getLooper());

            mHandlerSingleton.mMPDConnection.setpIdleListener(mHandlerSingleton);
        }
        return mHandlerSingleton;
    }


    /**
     * This is the main entry point of messages.
     * Here all possible messages types need to be handled with the MPDConnection.
     *
     * @param msg Message to process.
     */
    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);

        if (!(msg.obj instanceof MPDHandlerAction)) {
            /* Check if the message object is of correct type. Otherwise just abort here. */
            return;
        }

        MPDHandlerAction mpdAction = (MPDHandlerAction) msg.obj;
        /* Catch MPD exceptions here for now. */
        MPDResponseHandler responseHandler;
        MPDHandlerAction.NET_HANDLER_ACTION action = mpdAction.getAction();
        if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_NEXT_SONG) {
            mMPDConnection.nextSong();
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_PREVIOUS_SONG) {
            mMPDConnection.previousSong();
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_STOP) {
            mMPDConnection.stopPlayback();
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_PAUSE) {
            mMPDConnection.pause(true);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_COMMAND_PLAY) {
            mMPDConnection.pause(false);
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
        }
    }

    @Override
    public void onIdle() {

    }

    @Override
    public void onNonIdle() {
        // Go idle again
        mMPDConnection.startIdleing();
    }

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
     * Static command methods
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
     * Static command methods
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
     * Static command methods
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
     * Static command methods
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
     * Static command methods
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
}
