# M.A.L.P. #

Development sources of a new MPD client for Android.

This whole project is licensed under the  **GPLv3 or later** license (see LICENSE)

## Requirements: ##
 - Android 5.0
 - MPD >= 0.14
 
## Features: ##
 - Artist/Album/Files library browsing
 - AlbumArtist filtering
 - MBID filtering (multiple albums with same AlbumArtist, Title can be accessed separately) 
 - Server-based search
 - Local fast search
 - Basic playlist management (add songs to saved lists, remove songs from saved lists, remove lists, save lists)
 - Multiple server profiles
 - Server statistics **(here is also the update database button)**
 
## Planned Features: ##
 - Artwork support with MusicBrainz as artwork provider

## Description ##

This MPD client is tested with MPD 0.14 and MPD 0.19.

As MPD 0.19 has some features that really improve the user experience I would recommend using at least 0.19.

** This MPD client works best with accurately taged music libraries. I recommend tagging using [MusicBrainz Picard](https://picard.musicbrainz.org/) ** 

Starting with MPD 0.19 it is possible to filter list requests which allows this client to 
use the AlbumArtist and Artist tag for retrieval of albums. So if you have a lot of "Greatest Hits" albums you will get a list entry for each one of it. 

With earlier versions you will get only one entry, that merges all "Greatest Hits" albums to one (if you are not browsing over the artist list first).

**If you think a album is missing tracks, try the "Show all Tracks" in the menu of the right upper corner**

This client also parses the MusicBrainz tags that MPD provides so in the future this will be used to fetch artwork.

If the version of MPD is new enough, it can use ranged playlist retrieval of the current playlist, so that only the viewed
parts of the playlist are fetched from the server (MPD v. 0.15 and greater).
 
