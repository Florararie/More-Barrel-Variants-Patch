# Polymer Patch for More Barrel Variants

Makes [More Barrel Variants](https://github.com/pnk2u/More-Barrel-Variants) server-side only: a
vanilla client sees every barrel variant, its wood texture, name, facing and open lid, without
installing the mod.

## How it works

* Each barrel block is sent to clients as a **barrier**, which is invisible, solid and does not cull
  the faces of neighbouring blocks.
* The barrel art is drawn by an **item display entity** using the mod's own barrel model. That model
  is a plain cube with a wood side texture and a lid texture on top, so the entity is turned to match
  the barrel's facing, including barrels placed on their side or upside down.
* The server flips the barrel's open property while a player has it open, so the entity swaps to a
  generated open model (the mod ships the block model, the patch adds the item model for it).
* Barrel **items** are sent as vanilla barrels with the mod's item model, so inventory icons and
  names match while the stack still places the mod's block on the server.
* The mod's textures, models, language files and atlas are bundled from its own jar into the Polymer
  pack that the server generates; the patch adds only the open barrel item models.
* No block entity work is needed: every barrel uses the vanilla barrel block entity type, which a
  client without the mod already knows.

Because the client block is a barrier, mining a barrel draws no cracking overlay until it breaks.
Block breaking, placement, sounds, redstone output and container menus all stay normal and
server-side.

## Usage

Drop this jar and More Barrel Variants in `mods/`, plus Polymer. No client mod is required.

## Building

```sh
./gradlew build
```

The jar lands in `build/libs/`.
