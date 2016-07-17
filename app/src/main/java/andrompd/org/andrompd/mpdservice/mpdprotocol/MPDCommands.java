package andrompd.org.andrompd.mpdservice.mpdprotocol;

public class MPDCommands {

    public static final String MPD_COMMAND_CLOSE = "close";
    public static final String MPD_COMMAND_NODIDLE = "noidle";

    public static final String MPD_COMMAND_PASSWORD = "password ";

    /* Database request commands */
    public static final String MPD_COMMAND_REQUEST_ALBUMS = "list album";
    public static final String MPD_COMMAND_REQUEST_ALBUMS_WITH_MBID = "list album group MUSICBRAINZ_ALBUMID";


    public static String MPD_COMMAND_REQUEST_ARTIST_ALBUMS(String artistName) {
        return "list album \"" + artistName + "\"";
    }

    public static String MPD_COMMAND_REQUEST_ARTIST_ALBUMS_WITH_MBID(String artistName) {
        return "list album artist \"" + artistName + "\" group MUSICBRAINZ_ALBUMID";
    }

    public static final String MPD_COMMAND_REQUEST_ARTISTS = "list artist";

    public static final String MPD_COMMAND_REQUEST_ALL_FILES = "listallinfo";

    /* Control commands */
    public static String MPD_COMMAND_PAUSE(boolean pause) {
        return "pause " + (pause ? "1" : "0");
    }

    public static final String MPD_COMMAND_NEXT = "next";
    public static final String MPD_COMMAND_PREVIOUS = "previous";
    public static final String MPD_COMMAND_STOP = "stop";

    public static final String MPD_COMMAND_GET_CURRENT_STATUS = "currentstatus";
}
