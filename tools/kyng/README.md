# Kyng Keyboard Assets & Importer Tools

This directory contains tooling and converted assets extracted from **Kyng Keyboard** (`com.aionyxe.kyng`).

## Components

- **`convert_kyng.py`**: Automated conversion script that reads `kyng.apks` (or `base.apk`) and exports:
  - 47 sound packs as native `.wmsoundpack` files.
  - 73 visual themes as native `.wmtheme.json` files (including vector key skins matching `KeySkinSpec`).
- **`output/soundpacks/`**: 47 converted sound packs ready for direct import or distribution.
- **`output/themes/`**: 73 converted themes ready for direct import.

## Bundled Seed Sound Packs

The top 6 mechanical and tactile sound packs have been seeded into `app/src/main/assets/soundpacks/`:
1. `brass-mech.wmsoundpack` (Brass Mech)
2. `deep-thock.wmsoundpack` (Deep Thock)
3. `butter-linear.wmsoundpack` (Butter Linear)
4. `bubbles.wmsoundpack` (Bubbles)
5. `typewriter.wmsoundpack` (Typewriter)
6. `retro.wmsoundpack` (Retro)

These automatically appear in the sound pack picker for users out-of-the-box.
