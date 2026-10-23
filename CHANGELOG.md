# Changelog

All notable changes to Foldman. Versions follow [semantic versioning](https://semver.org); downloads are on the [releases page](https://github.com/deveshrai/Foldman/releases).

## [0.2.x] — 2026-10-23 (Aminafy edition)

### Added
- **Player skins.** Seven original designs, each with its own badge and finish: Classic Blue (enamel), Original '79 (brushed aluminium with a colour band), Sport (ribbed yellow rubber with a gasket), Chrome '85 (mirror chrome), Crystal (clear plastic showing the motor, flywheel and circuit board), Boombox Red (gloss with chrome trim) and Midnight (soft-touch black). Pick one in MODE.
- **Cassette types.** Type I normal, Type II chrome, Type IV metal and a clear shell that shows the whole tape path; or match the player, or colour by source.
- **Mixtapes.** Name a tape, choose a label colour and record songs from your folder in any order. Tap to play, hold to edit.
- **Tape sound.** MEGA BASS, tape hiss, wow & flutter and a five-band graphic EQ with presets, on local files.
- **Sleep timer** (15–90 minutes, fading local files out) and **auto-reverse**: at the end of the tape the deck flips to side B and the reels turn the other way.
- **Spotify** as a media-session source, alongside YT Music and Apple Music.
- **Home-screen widget** with the cover, title and play/pause/skip keys, coloured to match the skin.
- **Regular phones in landscape** get the deck and tape shelf side by side.
- MODE button on the cover screen and on the deck's end strip.

### Changed
- Titles and artists are read for every song in the folder, not just the first 600, and fill in as they're read.
- Builds are made and signed automatically on GitHub; the version counts up with each build.

## [0.1.2] — 2026-10-21

### Changed
- **Foldman's settings no longer go into Android backups or device transfers.** They include your Apple Music user token, which now stays on the phone. When you move to a new phone, sign in to Apple Music again there.

## [0.1.1] — 2026-10-21

### Fixed
- **Apple Music showed the next song instead of the one playing.** MusicKit lists only the songs *after* the current one in its queue, and Foldman read the playing song's position from that list. The title, artist, cassette label and cover now come from the song that's actually playing, and the queue and Up next start from it.
- **"Track 0 of N"** for Apple Music now shows the right position and count, including songs already played.

### Changed
- Apple Music playlists always play in order: Foldman explicitly turns shuffle off when it loads one, and turns it off again if Apple's player ever reports it on.

## [0.1.0] — 2026-10-21

The first release.

### Added
- **Three postures.** On the cover screen, an edge-to-edge upright portable player with the tape on end behind a window and the keys down a silver side strip. Unfolded, the same player laid down as a deck between a library panel and a now-playing panel. Half-open on a table (Flex mode), the deck on the top half and the library on the bottom.
- **The mechanism.** Latching PLAY, PAUSE, STOP·EJECT, FF and REW keys and AMS skip buttons. Reels turn at tape speed ÷ radius with spin-up and coast-down. A mechanical tape counter, VU needles that follow the real audio on local files, an OPERATE LED, synthesised key clicks and haptics, and CLICK / HAPTIC switches.
- **Local folders.** Media3 playback in a background service with media controls. Album art comes from embedded art or a cover image in the folder, and each sub-folder is listed as an album.
- **Apple Music via MusicKit** (optional AARs). Sign-in, library playlist browsing through the Apple Music API, and in-app streaming in a separate, crash-isolated process that resumes the song after a crash.
- **YT Music and Apple Music via media sessions.** Transport, artwork and queue, using notification access.
- Adaptive launcher icon with a themed (monochrome) layer.

[0.1.2]: https://github.com/deveshrai/Foldman/releases/tag/v0.1.2
[0.1.1]: https://github.com/deveshrai/Foldman/releases/tag/v0.1.1
[0.1.0]: https://github.com/deveshrai/Foldman/releases/tag/v0.1.0
