# Changelog

### 1.1.7
- Added "compatibility mode" config option to turn on an experimental workaround to improve compatibility with mods that alter tooltips.

### 1.1.6
- Fixed a bug with non-square tooltip border textures.
- Added blacklist configuration option.

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