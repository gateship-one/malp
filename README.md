Development sources of a new MPD client for Android.

This whole project is licensed on GPLv3 license (see LICENSE)

This MPD client is tested with MPD 0.14 and MPD 0.19.

As MPD 0.19 has some features that really improve the user experience I would recommend using at least 0.19.

If the version of MPD is new enough, it can use ranged playlist retrieval of the current playlist, so that only the viewed
parts of the playlist are fetched from the server.

Starting with MPD 0.19 it is possible to filter list requests which allows this client to 
use the AlbumArtist and Artist tag for retrieval of albums. So if you have a lot of "Greatest Hits" albums you will get a list entry for each one of it. 

With earlier versions you will get only one entry, that merges all "Greatest Hits" albums to one (if you are not browsing over the artist list first).

This client also parses the MusicBrainz tags that MPD provides so in the future this will be used to fetch artwork. 
