# Changelog

All notable changes to Pantograph. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and versions follow [Semantic Versioning](https://semver.org/). Full notes for each version are on the
[releases page](https://github.com/yighy/Pantograph/releases).

> Before 1.0, an update that changes the storage format clears your projects. Those versions are marked
> **Clears projects** below.

## [Unreleased]

### Changed
- The release APK is named `Pantograph-<version>-release.apk`.

## [0.18.0] - 2026-10-01

### Added
- **Anchor**: while the floating button is held, every line starts from the same point, which the finger
  on the button moves. Holding still first moves the button instead.
- **Timelapse** (experimental): record a drawing as you make it and export it as an MP4 of 15 s, 30 s or
  its own pace, saved to Movies/Pantograph. A REC chip shows while recording.
- **Experimental features** switch in Settings, off by default, which turns on the timelapse.
- **Check for updates**: off by default; once a day the app asks GitHub for a newer release and says so on
  the home screen. It downloads nothing itself.

### Changed
- The floating button is round, with its satellites as arcs around it; a satellite with no room at a
  screen edge moves to the next ring out.
- The bottom bar is split into two corner groups, with the panel a button opens floating above the bar.
  The colour button is filled with the current colour.
- Buttons along the top and bottom share one grouped style.
- A chip's drag readout sits half a screen away from the finger, on the other side.
- The settings screen is a plain list; the button's move distance is Short, Medium or Long. Wallpaper
  Colors is now Dynamic color.
- The brush preset list opens smoothly, keeping its previews between openings.
- The APK is about a quarter smaller (3.8 MB instead of 4.9 MB).

### Removed
- **Recoil**, replaced by Anchor.

### Fixed
- With the button against the left edge and low on the screen, the tool satellite could hang off the
  bottom.

## [0.17.2] - 2026-09-23

### Fixed
- The value dragged on a chip shows above the finger rather than beside it, where a thumb covered it.

## [0.17.1] - 2026-09-23

### Fixed
- A chip's values show again, larger, beside the fingertip while it is dragged.
- Raising a chip's value takes half the push it did.

## [0.17.0] - 2026-09-23

### Added
- Chips carry their tool's settings: drag up or down to change a value, sideways to pick which one. Lazy,
  Loupe, Fill, magic wand and colour select. Works in fullscreen too.

### Changed
- Hue on the colour satellite wraps round past 0° to 360°.
- Tool settings left the Settings panel, which keeps Draw Sensitivity.

### Fixed
- A path or selection in progress can be finished in fullscreen.
- The loupe keeps up with the canvas zoom.

## [0.16.2] - 2026-09-11

### Fixed
- The loupe's crosshair inverts what it lands on, so it stays visible over any colour.

## [0.16.1] - 2026-09-09

### Fixed
- The state chips and the back button no longer sit under the notch in fullscreen.

## [0.16.0] - 2026-09-09

### Added
- **Loupe**: a live magnified window onto the canvas under the brush, 2× to 12×, without interpolation.
  It moves out of the way when the brush reaches it, and stays in fullscreen.

## [0.15.0] - 2026-09-08

### Added
- **Fine**: Draw Sensitivity applies with the pen up as well, so aiming and drawing share one scale.

### Fixed
- Recoil sometimes did not return the cursor.
- The Brush Studio sliders lagged.
- Settings were written to disk on every slider frame; they are now written once the slider rests.
- The fill tool's Tolerance and Expand sliders move properly.

## [0.14.0] - 2026-09-06

**Clears projects.**

### Added
- Nine starter brushes in a Defaults folder.
- Tip shape: round or square, a ratio to squash it, and anti-aliasing that can be switched off.
- Hue, saturation and value jitter, and smudge.
- Fullscreen, leaving only the floating button, its satellites and the state chips.
- Recoil: the cursor springs back to where each stroke began.

### Fixed
- Filling inside an outline no longer leaves an unfilled hem along the line, thanks to an adjustable
  Expand.

## [0.13.0] - 2026-08-30

### Changed
- The whole brush follows you between projects; colour stays with each drawing. Brush settings reset
  once on update.

### Fixed
- Moving the floating button no longer takes back the last thing you did.

## [0.12.1] - 2026-08-30

### Fixed
- Draw Sensitivity applies while steering a path point.

## [0.12.0] - 2026-08-30

### Added
- **Path tool**: place points and bend a curve through them, with sharp or round points, closing, and
  commit as an ordinary stroke.

## [0.11.0] - 2026-08-27

**Clears projects.**

### Added
- Keep Undone Strokes: undone strokes stay faintly on a locked Traces layer, to redraw over.
- Layer locking.
- Chips naming each armed tool, tapped to switch it off.

### Fixed
- The selected layer is clearly marked in the dark theme.
- Deleting a layer or project reclaims its storage.
- Undo and redo no longer fire mid-stroke.

## [0.10.0] - 2026-08-26

### Added
- Undo walks the cursor back to where the undone stroke started, and redo returns it.

### Fixed
- Pinned tools that are on look on in the dark theme.
- Opening a project shows its thumbnail while the layers load, and layers load in parallel.
- Satellite readouts close with the same animation they open with.

## [0.9.1] - 2026-08-22

Initial release.

[Unreleased]: https://github.com/yighy/Pantograph/compare/v0.18.0...HEAD
[0.18.0]: https://github.com/yighy/Pantograph/compare/v0.17.2...v0.18.0
[0.17.2]: https://github.com/yighy/Pantograph/compare/v0.17.1...v0.17.2
[0.17.1]: https://github.com/yighy/Pantograph/compare/v0.17.0...v0.17.1
[0.17.0]: https://github.com/yighy/Pantograph/compare/v0.16.2...v0.17.0
[0.16.2]: https://github.com/yighy/Pantograph/compare/v0.16.1...v0.16.2
[0.16.1]: https://github.com/yighy/Pantograph/compare/v0.16.0...v0.16.1
[0.16.0]: https://github.com/yighy/Pantograph/compare/v0.15.0...v0.16.0
[0.15.0]: https://github.com/yighy/Pantograph/compare/v0.14.0...v0.15.0
[0.14.0]: https://github.com/yighy/Pantograph/compare/v0.13.0...v0.14.0
[0.13.0]: https://github.com/yighy/Pantograph/compare/v0.12.1...v0.13.0
[0.12.1]: https://github.com/yighy/Pantograph/compare/v0.12.0...v0.12.1
[0.12.0]: https://github.com/yighy/Pantograph/compare/v0.11.0...v0.12.0
[0.11.0]: https://github.com/yighy/Pantograph/compare/v0.10.0...v0.11.0
[0.10.0]: https://github.com/yighy/Pantograph/compare/v0.9.1...v0.10.0
[0.9.1]: https://github.com/yighy/Pantograph/releases/tag/v0.9.1
