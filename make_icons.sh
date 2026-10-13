#!/bin/bash
# Neatly icon generator.
# Reads /root/n-icon-raw.jpg (1024x1024 copper N on dark background),
# flattens background to solid #0A0A0A, centers the subject, and
# generates square + round launcher icons at five mipmap densities.
#
# Run after replacing the source image.

set -e

SRC="/root/n-icon-raw.jpg"
RES="/root/neatly-apk/app/src/main/res"
WORK="/tmp/neatly-icons"
BG="#0A0A0A"

if [ ! -f "$SRC" ]; then
    echo "ERROR: source image not found at $SRC"
    exit 1
fi

if command -v magick >/dev/null 2>&1; then
    IM="magick"
elif command -v convert >/dev/null 2>&1; then
    IM="convert"
else
    echo "ERROR: ImageMagick not found."
    exit 1
fi

rm -rf "$WORK"
mkdir -p "$WORK"

# Step 1: flatten the background to a single flat colour.
# The AI image has a very slight vignette. Solidify it.
$IM "$SRC" \
    -resize 1024x1024 \
    -background "$BG" \
    -flatten \
    "$WORK/flat.png"

# Step 2: auto-crop whitespace around the subject, then re-pad with
# a 12% margin on all sides, so the N sits centered with breathing room.
$IM "$WORK/flat.png" \
    -fuzz 8% \
    -trim \
    +repage \
    "$WORK/trimmed.png"

TRIMMED_W=$($IM identify -format "%w" "$WORK/trimmed.png")
TRIMMED_H=$($IM identify -format "%h" "$WORK/trimmed.png")

# Use the larger of the two dimensions so the subject fits either way.
MAX_DIM=$TRIMMED_W
if [ "$TRIMMED_H" -gt "$MAX_DIM" ]; then
    MAX_DIM=$TRIMMED_H
fi

# Add 24% padding (12% on each side).
PADDED=$(( MAX_DIM * 124 / 100 ))

# Step 3: place the trimmed subject on a square canvas of the padded
# size, filled with the background colour, centred.
$IM -size "${PADDED}x${PADDED}" "xc:$BG" \
    "$WORK/trimmed.png" -gravity center -composite \
    -resize 512x512 \
    "$WORK/base_square.png"

# Step 4: build a round version by masking the square with a circle.
$IM -size 512x512 xc:none \
    -fill white -draw "circle 256,256 256,0" \
    "$WORK/circle_mask.png"

$IM "$WORK/base_square.png" \
    "$WORK/circle_mask.png" \
    -alpha off -compose CopyOpacity -composite \
    "$WORK/base_round.png"

# Step 5: emit at every density.
generate() {
    local density="$1" size="$2"
    local dir="$RES/mipmap-$density"
    mkdir -p "$dir"
    $IM "$WORK/base_square.png" -resize "${size}x${size}" -depth 8 "$dir/ic_launcher.png"
    $IM "$WORK/base_round.png"  -resize "${size}x${size}" -depth 8 "$dir/ic_launcher_round.png"
}

generate mdpi     48
generate hdpi     72
generate xhdpi    96
generate xxhdpi   144
generate xxxhdpi  192

rm -rf "$WORK"

echo "Generated icons:"
find "$RES" -name 'ic_launcher*.png' | sort
