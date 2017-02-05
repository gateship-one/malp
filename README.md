![Logo](https://raw.githubusercontent.com/gateship-one/malp/master/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png)
# M.A.L.P. - Android MPD Client #

[<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="60">](https://f-droid.org/app/org.gateshipone.malp)<a href="https://play.google.com/store/apps/details?id=org.gateshipone.malp"><img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" height="60"></a>

This whole project is licensed under the  **GPLv3 or later** license (see LICENSE)

[Screenshots](https://github.com/gateship-one/malp/wiki/Screenshots)

**Mopidy (2.1, 2.0) is currently only supported with a workaround until the [Issue](https://github.com/mopidy/mopidy/issues/1315) is resolved. Problems might occur. Consider using the real MPD server ([Homepage](https://www.musicpd.org)).**

## Requirements: ##
 - Android 5.0
 - MPD >= 0.14
 
## Features: ##
 - Artist/Album/Files library browsing
 - AlbumArtist filtering
 - MBID filtering (multiple albums with same AlbumArtist, Title can be accessed separately) 
 - Server-based search
 - Local fast search
 - Launcher widget
 - Notification with background service (**optional, can be disabled in the settings**)
 - Volume control if application is in background (**only if notification is enabled & visible**)
 - Mostly optimized for tablets
 - Basic playlist management (add songs to saved lists, remove songs from saved lists, remove lists, save lists)
 - Multiple server profiles
 - Server statistics **(here is also the update database button)**
 - Artwork support with MusicBrainz, Last.fm, Fanart.tv as artwork provider (Album, artist images and fullscreen fanart)
 - Fanart view for tablets or stuff like that (click on the cover in the now playing view, or select fullscreen mode from menu)
 - Bulk downloader to download all artwork to local memory

## Description ##

This MPD client is tested with MPD 0.14, 0.19 and 0.20.

As MPD 0.19 has some features that really improve the user experience I would recommend using at least 0.19, but consider to always use the most recent version of MPD.

**This MPD client works best with accurately taged music libraries. I recommend tagging using [MusicBrainz Picard](https://picard.musicbrainz.org/)** 

Starting with MPD 0.19 it is possible to filter list requests which allows this client to 
use the AlbumArtist and Artist tag for retrieval of albums. So if you have a lot of "Greatest Hits" albums you will get a list entry for each one of it. 

With earlier versions you will get only one entry, that merges all "Greatest Hits" albums to one (if you are not browsing over the artist list first).

**If you think an album is missing tracks, try the "Show all Tracks" in the menu of the right upper corner**

This client also parses the MusicBrainz tags that MPD provides to accelerate artwork downloading.

If the version of MPD is new enough, it can use ranged playlist retrieval of the current playlist, so that only the viewed
parts of the playlist are fetched from the server (MPD v. 0.15 and greater).
