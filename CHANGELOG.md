### Version: 1.1.13 Tag: release-22 (2018-xx-xx) ###
 * MPD and connection errors are shown to the user
 * Configurable volume button step size
 * Default action for library tracks selectable (play song, add song (default), add as next song, details)
 * Option to keep the display on when application is active
 * Lots of code cleanup and refactoring (especially the MPD connection handling)
 * Crash fixes in artist handling
 * Updated korean translation
 * Hidden feature to open MusicBrainz pages from the song details dialog

### Version: 1.1.12 Tag: release-21 (2017-12-31) ###
 * Experimental feature: local HTTP cover download (see [FAQ](https://github.com/gateship-one/malp/wiki/FAQ))
 * Minor bug fixes
 * Restructure cover art storage to support larger images (previous downloaded cover art will be lost)
 * Improve image processing (scaling and caching)
 * Happy new year!

### Version: 1.1.11 Tag: release-20 (2017-11-13) ###
 * Hotfix for: https://issuetracker.google.com/issues/64434571

### Version: 1.1.10 Tag: release-19 (2017-11-10) ###
 * Fix bug: #83
 * Follow album sort method in play/add action
 * New output selection from NowPlayingView on hold volume button (thanks to sejerpz)
 * Statusbar image fix
 * Adaptable icon fix
 * Widget fix on Android 8

### Version: 1.1.9 Tag: release-18 (2017-10-17) ###
 * Fix bugs: #72, #68
 * New sdk version (26)
 * Adaptive icon
 * Notification channels
 * Foreground notification during playback
 * Statusbar is currently colored in artist/album view (no fix known at the time)
 * Output dialog on longpress on the volume button in NowPlayingView
 * Fix for notification image sometimes not showing

### Version: 1.1.8 Tag: release-17 (2017-08-05) ###
 * Stability fixes in UI
 * Fix bug #59
 * Implement feature request #46
 * General stability fixes in current playlist adapter
 * Crash fix when entering empty port number in profiles view

### Version: 1.1.7 Tag: release-16 (2017-07-22) ###
 * Stability fixes in UI
 * Format MPD dates more nicely
 * UI fixes when creating new profiles (new profiles not shown in list)
 * Use internal cache instead of external cache for FanartActivity

### Version: 1.1.6 Tag: release-15 (2017-07-10) ###
 * Deidleing with actual timeout to prevent deadlocking/ANR on certain disconnect situations
 * UI refinements
 * Horizontal resizeable launcher widget

### Version: 1.1.5 Tag: release-14 (2017-06-15) ###
 * Experimental support to play remote streams in M.A.L.P. (http output plugin of MPD for example)
 * Small stability fixes

### Version: 1.1.4 Tag: release-13 (2017-03-02) ###
 * French translation (Thanks to Guy de Mölkky)
 * UI flickering fixes in ListViews 

### Version: 1.1.3 Tag: release-12 (2017-02-05) ###
 * Crash fix on number conversion in MPD connection
 * Show artist image in now playing
 * Fix multiple profiles created on device rotation
 * Reset artwork for one album/artist in their fragment
 * Option to reload album / artist image in the corresponing fragments
 * Remove visible album sections from current playlist

### Version: 1.1.2 Tag: release-11 (2017-01-15) ###
 * Crash fix in SearchFragment if "Add" button is clicked but no server connection was established
 * Crash fix in FanartCache 
 * Filtering in SavedPlaylists working again
 * Workaround for Mopidy's MPD protocol insufficiencies
 * Notification enabled by default from now (can be disabled in the settings)
 * Artwork is fetched also from the background service
 * Small fixes

### Version: 1.1.1 Tag: release-10 (2017-01-10) ###
 * Profile name shown in navigation drawer
 * Bugfixes for notification not shown after rotation
 * Delayed disconnect on Activity changes
 * Save search string on device rotation
 * Various crash fixes (reported over play services)
 * Single & consume playback option in menu of NowPlayingView
 * Japanese translation (thanks to naofum)

### Version: 1.1.0 Tag: release-9 (2016-12-24) ###
 * Launcher widget
 * Optional notification if main activity is hidden
 * Volume control from outside the application (**only if notification is enabled!**)
 * Tablet optimized nowplaying screen
 * Album images in playlist view as sections
 * Listviews for artists / albums now with images
 * Option to change volume slider to volume buttons or disable visible volume controls
 * Option to use AlbumArtist tag instead of artist tag for artists list
 * Save last used search type in SearchFragment
 * Hardware buttons for volume control repeating
 * Stabilization of the Artwork downloading
 * Fixes for upcoming MPD version 0.20
 * Fix of NowPlayingView not shown on Android >=7
