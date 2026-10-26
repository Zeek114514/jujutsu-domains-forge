# User-requested expansion voice clips

These are the two character clips explicitly selected by the user, not CC0 recordings and not covered by the Java MIT or the other sounds' CC0 declaration. No general redistribution license was established. Original rights remain with their respective owners.

Gojo requested page: https://soundboards.gg/sound/3498-gojo-domain-expansion
Actual public download from that page's app2.js download implementation: https://soundboardsgg.fra1.cdn.digitaloceanspaces.com/sounds/3498-gojo-domain-expansion.mp3

Sukuna requested page: https://soundinstants.com/sound/malevolent-shrine
Direct page download returned HTTP403 and its sound API HTTP401. Downloaded the same-title public candidate at https://www.myinstants.com/en/instant/malevolent-shrine-18466/ , actual MP3 https://www.myinstants.com/media/sounds/malevolent-shrine.mp3 . Its193959bytes is approximately189KB, matching the requested page's displayed size. The original site's file could not be obtained for a byte-for-byte comparison, so identity is not asserted.

Processing: normalize to mono first, -16LUFS target, -3dBTP target with a0.85 non-makeup limiter, filter sub70Hz, convert to mono44.1kHz Ogg Vorbis. No original spoken content was added, cloned or generated. manifest.json records hashes and exact durations.

## User-selected Fuga line

Selected page: https://soundboards.gg/sound/9151-sukuna-fi-re-arrow-audio (Isuk). Public download derived from that site's app2.js: https://soundboardsgg.fra1.cdn.digitaloceanspaces.com/sounds/9151-sukuna-fi-re-arrow-audio.mp3 .

The original is 21.696 seconds and includes other dialogue. Only source 5.00..7.75 seconds is used in `fuga_open.ogg`; the full source stays in the ignored build directory. Processing trims first and resets timestamps, then applies mono conversion, high-pass, loudness normalization, limiting and brief edge fades. No spoken content is cloned or generated. `fuga_manifest.json` records hashes and the exact filter.

Full draw at 1.50 seconds; release at 2.70 seconds after cast start. The source cue was located with local Japanese speech recognition/token alignment, rather than manual listening. Audio-channel startup, holding full draw, release and cancellation are checked in the isolated Forge test. This user-selected character recording is not MIT or CC0; no general redistribution license was established.

The same selected source now supplies two separate positional effects: source 7.75..8.35s for arrow launch, and 9.54..15.95s for the explosion and rumbling tail. Quiet waiting between them and the later dialogue are omitted. Waveform/spectral transitions guided the cuts; no manual listening is claimed. `fuga_manifest.json` includes exact filters, hashes, decoded duration and measured levels for both. Runtime release starts the launch cue; actual synchronized impact stops the launch cue and starts the explosion once. Neither cue relies on waiting for the other file to finish.
