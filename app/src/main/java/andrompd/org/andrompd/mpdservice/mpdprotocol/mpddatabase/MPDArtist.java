package andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase;


public class MPDArtist {
    /* Artist properties */
    private String pArtistName;

    /* Musicbrainz ID */
    private  String pMBID;

    public MPDArtist(String name, String mbid) {
        pArtistName = name;
        pMBID = mbid;
    }

    public String getArtistName() {
        return pArtistName;
    }

    public String getMBID() {
        return pMBID;
    }
}
