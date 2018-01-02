![Logo](https://raw.githubusercontent.com/gateship-one/malp/master/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png)
# M.A.L.P. - Android MPD Client #

[<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/app/org.gateshipone.malp)

This whole project is licensed under the  **GPLv3 or later** license (see LICENSE)

**[Screenshots](https://github.com/gateship-one/malp/wiki/Screenshots)**

**Please check out the [Frequently Asked Questions](https://github.com/gateship-one/malp/wiki/FAQ)**

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
 - HTTP audio streaming from MPD server
 - HTTP cover download

## Description ##

This is M.A.L.P. a fast and easy to use MPD client. The main goal is to create a MPD client that is both simple and fast to use and also follow the material design guidelines. 

This MPD client is tested with MPD 0.14, 0.19 and 0.20 but you really should not use outdated versions of MPD.

**This MPD client works best with accurately taged music libraries. I recommend tagging using [MusicBrainz Picard](https://picard.musicbrainz.org/)** 

Starting with MPD 0.19 it is possible to filter list requests which allows this client to 
use the AlbumArtist and Artist tag for retrieval of albums. So if you have a lot of "Greatest Hits" albums you will get a list entry for each one of it. 


