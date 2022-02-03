# Changelog

### 1.2.2
- Fixed a bug with non-square tooltip border textures.
- Added blacklist configuration option.

### 1.2.1
- Fixed a crash bug that could occur when then "borders match rarity" option was disabled.
- Fixed a graphical glitch that could occur when custom borders are specified with a non-default texture size.

### 1.2.0
#### Configuration Improvements

- Added documentation block to top of config file.
- Reordered config file to follow a more logical order.
- Tooltip background colors can now be configured.
- All color values can now be optionally specified as strings (color names or hex codes).

#### New Features

- Added tooltip title centering option, which is on by default.
- Added minimum width enforcement option, which is off by default.
- Added new custom frame definition functionality for mod-defined borders.
  - Mod authors can use this code-based method to add any number of entirely custom borders to items.
- Added new custom frame definition functionality that can be included in resource packs.
  - Mod authors or resource pack authors can use this data-based method to add any number of entirely custom borders to items.

#### Bug Fixes

- Fixed a bug where tooltip shadows would not match the height of Equipment Compare comparison tooltips.
- Fixed a crash bug caused by certain tooltip border colors.
- Fixed a bug where creative inventory tab tooltips would render separators outside of the tooltip.
- Fixed a bug where separators would sometimes draw in the wrong place when comparing items with Equipment Compare.

### 1.1.5
- Bumped required Iceberg version to support NBT tag selectors.

### 1.1.4
- Decoupled selector logic to Iceberg.

### 1.1.3
- Fixed a bug that sometimes caused frame levels to repeat.

### 1.1.2
- Now supports 6 or 8 digit color codes for border colors.
- Bumped required Iceberg version to fix custom border alignment issues.

### 1.1.1
- Added border priority configuration option.
- First Fabric 1.17 release.

### 1.1.0
- Increased number of possible custom frames from 4 to 16.

### 1.0.2
- Fixed a bug that prevented frame levels 1 - 3 from rendering properly.
- First Forge 1.17 release.

### 1.0.1
- Fixed a bug with name separator not appearing in the proper location for items with very long names.
- Fixed a bug where some items were displaying a custom border when they didn't match configured operators.
- Added new frame matching operators:
-   % matches any text in item's display name.
-   ^ matches any text in item's tooltip text (besides display name).

### 1.0.0
- Initial Forge 1.16 release.