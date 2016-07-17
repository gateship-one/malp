package andrompd.org.andrompd.mpdservice.mpdprotocol;

/**
 * Created by hendrik on 16.07.16.
 */
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
}
