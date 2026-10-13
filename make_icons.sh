#!/bin/bash
# Neatly icon generator.
# Generates launcher icons at all five mipmap densities.
# Run once after the res/ tree exists.

set -e

RES="/root/neatly-apk/app/src/main/res"

if command -v magick >/dev/null 2>&1; then
    IM="magick"
elif command -v convert >/dev/null 2>&1; then
    IM="convert"
else
    echo "ERROR: ImageMagick not found. Install with: dnf install -y ImageMagick"
    exit 1
fi

FONT=""
for f in /usr/share/fonts/dejavu-serif-fonts/DejaVuSerif.ttf \
         /usr/share/fonts/dejavu/DejaVuSerif.ttf \
         /usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf; do
    if [ -f "$f" ]; then
        FONT="$f"
        break
    fi
done

if [ -z "$FONT" ]; then
    echo "ERROR: DejaVu Serif not found. Install with: dnf install -y dejavu-serif-fonts"
    exit 1
fi

# Base square icon: warm off-white background, dark serif N centered.
$IM -size 512x512 xc:'#F3EFE7' \
    -font "$FONT" -pointsize 380 -fill '#1A1A1A' \
    -gravity center -annotate +0+0 'N' \
    /tmp/neatly_base_square.png

# Base round icon: same design, circular mask.
$IM -size 512x512 xc:none \
    -fill '#F3EFE7' -draw "circle 256,256 256,0" \
    -font "$FONT" -pointsize 380 -fill '#1A1A1A' \
    -gravity center -annotate +0+0 'N' \
    /tmp/neatly_base_round.png

generate() {
    local density="$1" size="$2"
    local dir="$RES/mipmap-$density"
    mkdir -p "$dir"
    $IM /tmp/neatly_base_square.png -resize "${size}x${size}" "$dir/ic_launcher.png"
    $IM /tmp/neatly_base_round.png  -resize "${size}x${size}" "$dir/ic_launcher_round.png"
}

generate mdpi     48
generate hdpi     72
generate xhdpi    96
generate xxhdpi   144
generate xxxhdpi  192

rm -f /tmp/neatly_base_square.png /tmp/neatly_base_round.png

echo "Generated icons:"
find "$RES" -name 'ic_launcher*.png' | sort
