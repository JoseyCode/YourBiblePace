#!/usr/bin/env python3
"""Exports the chosen logo (closed book with a cross) into the Android app's launcher icon resources.

Writes, under app/src/main/res: the adaptive icon layers (background, foreground, monochrome as vector drawables), and the legacy
mipmap WebP icons for phones older than Android 8. Also writes branding/ybp-play-store-512.png for the Play Console listing.

Run from anywhere with a Python that has Pillow and fonttools:  python export_android.py
"""
import pathlib
import subprocess

from PIL import Image

import make_logo as g

HERE = pathlib.Path(__file__).parent
RES = HERE.parent / "app" / "src" / "main" / "res"


def rrect(x, y, w, h, r):
    """Rounded rectangle path data using arcs."""
    return (f"M{x+r},{y} H{x+w-r} A{r},{r} 0 0 1 {x+w},{y+r} V{y+h-r} A{r},{r} 0 0 1 {x+w-r},{y+h} H{x+r} "
            f"A{r},{r} 0 0 1 {x},{y+h-r} V{y+r} A{r},{r} 0 0 1 {x+r},{y} Z")


def cross_polygon(cx, cy, inset=0.0):
    """Latin cross outline; `inset` shrinks it so a round-joined stroke of width 2*inset restores the size with softened corners."""
    b, arm = g.CROSS_BAR / 2 - inset, g.CROSS_ARM / 2 - inset
    top, bottom = cy - g.CROSS_HEIGHT / 2 + inset, cy + g.CROSS_HEIGHT / 2 - inset
    bar_top = cy - g.CROSS_HEIGHT / 2 + g.CROSS_BAR_TOP + inset
    bar_bottom = cy - g.CROSS_HEIGHT / 2 + g.CROSS_BAR_TOP + g.CROSS_BAR - inset
    pts = [(cx - b, top), (cx + b, top), (cx + b, bar_top), (cx + arm, bar_top), (cx + arm, bar_bottom), (cx + b, bar_bottom),
           (cx + b, bottom), (cx - b, bottom), (cx - b, bar_bottom), (cx - arm, bar_bottom), (cx - arm, bar_top), (cx - b, bar_top)]
    return "M" + " L".join(f"{x:.2f},{y:.2f}" for x, y in pts) + " Z"


def argb(hex_color, alpha):
    return f"#{round(alpha * 255):02X}{hex_color.lstrip('#')}"


X0, Y0, W, H, S = g.X0, g.Y0, g.W, g.H, g.SPINE
CX, CY = X0 + S + (W - S) / 2, Y0 + H / 2
# Android vector groups scale about a pivot and then translate. Pivot = the book's centre, translate = move that centre to (54, 54).
BOOK_CX, BOOK_CY = X0 + (W + g.PAGE_OFFSET) / 2, Y0 + (H + g.PAGE_OFFSET) / 2
GROUP = (f'android:pivotX="{BOOK_CX}" android:pivotY="{BOOK_CY}" android:scaleX="{g.SCALE}" android:scaleY="{g.SCALE}" '
         f'android:translateX="{54 - BOOK_CX:.2f}" android:translateY="{54 - BOOK_CY:.2f}"')
HEAD = ('<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    xmlns:aapt="http://schemas.android.com/aapt"\n    android:width="108dp"\n    android:height="108dp"\n'
        '    android:viewportWidth="108"\n    android:viewportHeight="108">\n')


def gradient(attr, x1, y1, x2, y2, c1, c2):
    return (f'        <aapt:attr name="android:{attr}">\n            <gradient android:type="linear" android:startX="{x1}" android:startY="{y1}" '
            f'android:endX="{x2}" android:endY="{y2}">\n                <item android:offset="0" android:color="{c1}" />\n'
            f'                <item android:offset="1" android:color="{c2}" />\n            </gradient>\n        </aapt:attr>\n')


def background():
    return (HEAD + '    <path android:pathData="M0,0h108v108h-108z">\n' + gradient("fillColor", 54, 0, 54, 108, g.BROWN_TOP, g.BROWN_BOTTOM)
            + '    </path>\n</vector>\n')


def foreground():
    spine = (f"M{X0+g.RADIUS},{Y0} H{X0+S} V{Y0+H} H{X0+g.RADIUS} A{g.RADIUS},{g.RADIUS} 0 0 1 {X0},{Y0+H-g.RADIUS} "
             f"V{Y0+g.RADIUS} A{g.RADIUS},{g.RADIUS} 0 0 1 {X0+g.RADIUS},{Y0} Z")
    cross = cross_polygon(CX, CY, 0.45)
    return (HEAD + f'    <group {GROUP}>\n'
            f'        <path android:fillColor="{g.PARCHMENT}" android:pathData="{rrect(X0+g.PAGE_OFFSET, Y0+g.PAGE_OFFSET, W, H, g.RADIUS)}" />\n'
            f'        <path android:pathData="{rrect(X0, Y0, W, H, g.RADIUS)}">\n' + gradient("fillColor", X0, Y0, X0 + W, Y0 + H, g.GOLD_LIGHT, g.GOLD_DARK)
            + '        </path>\n'
            f'        <path android:fillColor="{argb(g.INK, 0.35)}" android:pathData="{spine}" />\n'
            f'        <path android:pathData="M{X0+S},{Y0} V{Y0+H}" android:strokeColor="{argb(g.INK, 0.45)}" android:strokeWidth="0.6" />\n'
            f'        <path android:pathData="{rrect(X0+S+2.6, Y0+3, W-S-5.6, H-6, 1.6)}" android:strokeColor="{argb(g.INK, 0.55)}" android:strokeWidth="0.7" />\n'
            f'        <path android:fillColor="{argb(g.GOLD_LIGHT, 0.75)}" android:strokeColor="{argb(g.GOLD_LIGHT, 0.75)}" android:strokeWidth="0.9" '
            f'android:strokeLineJoin="round" android:pathData="{cross_polygon(CX + 0.55, CY + 0.55, 0.45)}" />\n'
            f'        <path android:fillColor="{g.INK}" android:strokeColor="{g.INK}" android:strokeWidth="0.9" android:strokeLineJoin="round" '
            f'android:pathData="{cross}" />\n'
            '    </group>\n</vector>\n')


def monochrome():
    """Single-colour silhouette for Android 13 themed icons: the cover, with the cross and the spine seam cut out."""
    slit = f"M{X0+S-0.35},{Y0} H{X0+S+0.35} V{Y0+H} H{X0+S-0.35} Z"
    return (HEAD + f'    <group {GROUP}>\n        <path android:fillColor="#FF000000" android:fillType="evenOdd" '
            f'android:pathData="{rrect(X0, Y0, W, H, g.RADIUS)} {cross_polygon(CX, CY)} {slit}" />\n    </group>\n</vector>\n')


def legacy_icons():
    """Pre-Android-8 phones ignore adaptive icons and need a finished picture, in a square-ish and a round version."""
    svg_path = HERE / "ybp-c-cross.svg"
    big = HERE / "ybp-c-cross.png"
    subprocess.run(["rsvg-convert", "-w", "1080", "-h", "1080", "-o", str(big), str(svg_path)], check=True)
    for folder, size in (("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)):
        for name, shape in (("ic_launcher", "square"), ("ic_launcher_round", "circle")):
            g.mask_tile(str(big), size, shape).save(RES / f"mipmap-{folder}" / f"{name}.webp", "WEBP", lossless=True)
    # Play Console wants a 512 x 512 PNG with no transparency; Google applies its own rounding.
    play = Image.open(big).convert("RGB")
    a, b = int(play.width * 18 / 108), int(play.width * 90 / 108)
    play.crop((a, a, b, b)).resize((512, 512), Image.LANCZOS).save(HERE / "ybp-play-store-512.png")


if __name__ == "__main__":
    g_out = {"ic_launcher_background.xml": background(), "ic_launcher_foreground.xml": foreground(), "ic_launcher_monochrome.xml": monochrome()}
    for name, xml in g_out.items():
        (RES / "drawable" / name).write_text(xml)
    (RES / "mipmap-anydpi-v26" / "ic_launcher.xml").write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
        '    <background android:drawable="@drawable/ic_launcher_background" />\n'
        '    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n'
        '    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />\n</adaptive-icon>\n')
    (RES / "mipmap-anydpi-v26" / "ic_launcher_round.xml").write_text((RES / "mipmap-anydpi-v26" / "ic_launcher.xml").read_text())
    legacy_icons()
    print("exported")
