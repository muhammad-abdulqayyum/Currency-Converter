# Mobile icon refinement

Shared locally drawn SVG sources for Android and iOS. Category icons use stacked gold/silver bullion and a gold Bitcoin coin. Metal rows have distinct Au/Ag/Pt/Pd bars; BTC and BCH have distinct coin artwork. Existing ETH, LTC and XRP symbols remain. Assets are bundled; no remote images, permissions or runtime dependencies. SVGs are rendered at 192px to matching platform PNGs.

Version 4.0.4 uses bundled dimensional PNG atlases (`premium_assets`, 4 columns × 2 rows: Au, Ag, Pt, Pd, BTC, ETH, XRP, LTC; `premium_extras`, 2 columns × 1 row: Metals category, BCH). The prior SVG exploration remains as design history. SAT and DROP reuse their parent artwork and add the existing badge.

The 4.0.4 develop preview corrects BCH to a counterclockwise/left tilt using the built-in image editor. Edit specification: preserve the two-cell extras atlas, stacked bullion, green coin and silver rim; rotate only the readable white B counterclockwise about 15 degrees without mirroring it; retain transparency. The output is bundled in both platform atlas paths.
