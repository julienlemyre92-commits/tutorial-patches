# Build506 / patch503

Alex: replace GET_BUCKET scenery scans with bounded real ground-item scans; implement Trade -> shop-open proof -> Buy-1 -> inventory proof; two action attempts maximum; reset new fields at startup and RESET. Fix classifyPenByTargets radius/item-ID confusion with radius8 and explicit ID filters.

Compiled against installed Microbot 2.6.18; deprecation warnings only. Gameplay and final quest completion are not yet verified. Patch is cumulative from patch502. Added a loaded-scene tile-item cache scan and exact selected-item Take action; no guessed bucket coordinates. No loader or Supervisor changes.

Source SHA256: 293462689a9fb2fee9b2184037c884d0408637241287c9bcee6c4b509dbcf927
Patch SHA256: 7591336a3af52e1d13fc8cbb63d1c16771ade0cf9b72d06905f34a89f82898f7
