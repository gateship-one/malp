package andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase;

/**
 * Created by hendrik on 16.07.16.
 */
public class MPDAlbum {
    /* Album properties */
    private String pName;

    /* Musicbrainz ID */
    private String pMBID;

    /* Artists name (if any) */
    private String pArtistName;

    public MPDAlbum(String name, String mbid, String artist ) {
        pName = name;
        pMBID = mbid;
        pArtistName = artist;
    }

    /* Getters */

    public String getName() {
        return pName;
    }

    public String getMBID() {
        return pMBID;
    }

    public String getArtistName() {
        return pArtistName;
    }
}
