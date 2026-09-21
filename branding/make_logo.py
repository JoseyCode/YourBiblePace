#!/usr/bin/env python3
"""Generates the YourBiblePace logo candidates as SVG (108 x 108 adaptive-icon canvas) and a preview sheet PNG.

Run with a Python that has fonttools and Pillow:  python make_logo.py
The visible part of an Android adaptive icon is the central 72 x 72 of the 108 x 108 canvas; the "safe zone" for artwork is the central 66.
"""
import subprocess
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from PIL import Image, ImageDraw, ImageFont

FONT = "/usr/share/fonts/noto/NotoSerif-Bold.ttf"

BROWN_TOP, BROWN_BOTTOM = "#5A3A22", "#3A2415"
GOLD_LIGHT, GOLD_DARK = "#DDB65C", "#A57628"
PARCHMENT, PARCHMENT_SHADE = "#F6EEDF", "#E3D3B4"
INK = "#3E2716"


def text_path(text, size, cx, cy, tracking=0.0):
    """Outline of `text` as one SVG path, horizontally centred on cx and cap-height centred on cy."""
    font = TTFont(FONT)
    glyphs, cmap, hmtx = font.getGlyphSet(), font.getBestCmap(), font["hmtx"]
    scale = size / font["head"].unitsPerEm
    cap = font["OS/2"].sCapHeight * scale
    total = sum(hmtx[cmap[ord(c)]][0] for c in text) * scale + tracking * (len(text) - 1)
    x, d = cx - total / 2, []
    for c in text:
        name = cmap[ord(c)]
        pen = SVGPathPen(glyphs)
        glyphs[name].draw(TransformPen(pen, (scale, 0, 0, -scale, x, cy + cap / 2)))
        d.append(pen.getCommands())
        x += hmtx[name][0] * scale + tracking
    return " ".join(d)


DEFS = f"""
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="{BROWN_TOP}"/><stop offset="1" stop-color="{BROWN_BOTTOM}"/>
    </linearGradient>
    <linearGradient id="gold" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="{GOLD_LIGHT}"/><stop offset="1" stop-color="{GOLD_DARK}"/>
    </linearGradient>
    <linearGradient id="pageL" x1="1" y1="0" x2="0" y2="0">
      <stop offset="0" stop-color="{PARCHMENT_SHADE}"/><stop offset="0.35" stop-color="{PARCHMENT}"/><stop offset="1" stop-color="{PARCHMENT}"/>
    </linearGradient>
    <linearGradient id="pageR" x1="0" y1="0" x2="1" y2="0">
      <stop offset="0" stop-color="{PARCHMENT_SHADE}"/><stop offset="0.35" stop-color="{PARCHMENT}"/><stop offset="1" stop-color="{PARCHMENT}"/>
    </linearGradient>
  </defs>"""


def open_book():
    lines = ""
    for i, y in enumerate((45.5, 50.5, 55.5)):
        w = 15 if i < 2 else 10
        lines += (f'<path d="M32.5,{y} q{w/2},-1.2 {w},0" stroke="#CDB88F" stroke-width="1.3" fill="none" stroke-linecap="round"/>'
                  f'<path d="M{75.5-w},{y} q{w/2},1.2 {w},0" stroke="#CDB88F" stroke-width="1.3" fill="none" stroke-linecap="round"/>')
    return f"""
  <!-- cover -->
  <path d="M23.5,37 C34,33 46,34.5 54,40 C62,34.5 74,33 84.5,37 L84.5,69.5 C74,67 62,68.5 54,75.5 C46,68.5 34,67 23.5,69.5 Z"
        fill="url(#gold)" stroke-linejoin="round"/>
  <!-- pages -->
  <path d="M53.2,42.5 C46,38.5 36.5,37.3 28,39 L28,66.5 C36.5,64.8 46,66 53.2,70.5 Z" fill="url(#pageL)"/>
  <path d="M54.8,42.5 C62,38.5 71.5,37.3 80,39 L80,66.5 C71.5,64.8 62,66 54.8,70.5 Z" fill="url(#pageR)"/>
  {lines}"""


# Geometry of the closed book on the 108 x 108 canvas, shared with export_android.py so the SVG previews and the Android icon can't drift.
X0, Y0, W, H, SPINE, RADIUS = 33, 25, 42, 56, 6, 4.5
PAGE_OFFSET = 2.4  # how far the page block peeks out to the right and bottom
SCALE = 0.93  # the whole book is scaled about its centre to sit comfortably inside the launcher safe zone
CROSS_BAR, CROSS_HEIGHT, CROSS_ARM, CROSS_BAR_TOP = 5.0, 25.0, 16.0, 7.2


def closed_book(mark="ybp"):
    x0, y0, w, h, spine = X0, Y0, W, H, SPINE
    cx, cy = x0 + spine + (w - spine) / 2, y0 + h / 2
    if mark == "ybp":
        emblem = f'<path d="{text_path("YBP", 14.5, cx, cy, tracking=0.6)}" fill="{INK}"/>'
    else:
        # Latin cross: crossbar about a third of the way down, centred as a whole on the cover. A pale offset copy gives a pressed-in look.
        bar, height, arm = CROSS_BAR, CROSS_HEIGHT, CROSS_ARM
        top = cy - height / 2
        def cross(dx, dy, fill, opacity=1):
            return (f'<g fill="{fill}" opacity="{opacity}" transform="translate({dx} {dy})">'
                    f'<rect x="{cx-bar/2}" y="{top}" width="{bar}" height="{height}" rx="0.9"/>'
                    f'<rect x="{cx-arm/2}" y="{top+CROSS_BAR_TOP}" width="{arm}" height="{bar}" rx="0.9"/></g>')
        emblem = cross(0.55, 0.55, GOLD_LIGHT, 0.75) + cross(0, 0, INK)
    return f"""<g transform="translate(54 54) scale({SCALE}) translate(-{x0+(w+PAGE_OFFSET)/2} -{y0+(h+PAGE_OFFSET)/2})">
  <!-- page block peeking out to the right and bottom -->
  <rect x="{x0+PAGE_OFFSET}" y="{y0+PAGE_OFFSET}" width="{w}" height="{h}" rx="{RADIUS}" fill="{PARCHMENT}"/>
  <!-- cover -->
  <rect x="{x0}" y="{y0}" width="{w}" height="{h}" rx="4.5" fill="url(#gold)"/>
  <!-- spine -->
  <path d="M{x0+4.5},{y0} h{spine-4.5} v{h} h-{spine-4.5} a4.5,4.5 0 0 1 -4.5,-4.5 v-{h-9} a4.5,4.5 0 0 1 4.5,-4.5 Z" fill="{INK}" opacity="0.35"/>
  <path d="M{x0+spine},{y0} v{h}" stroke="{INK}" stroke-width="0.6" opacity="0.45"/>
  <!-- inset border -->
  <rect x="{x0+spine+2.6}" y="{y0+3}" width="{w-spine-5.6}" height="{h-6}" rx="1.6" fill="none" stroke="{INK}" stroke-width="0.7" opacity="0.55"/>
  {emblem}
</g>"""


def svg(body, background=True):
    bg = '<rect width="108" height="108" fill="url(#bg)"/>' if background else ""
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="108" height="108">{DEFS}\n  {bg}{body}\n</svg>\n'


def render(svg_path, png_path, px):
    subprocess.run(["rsvg-convert", "-w", str(px), "-h", str(px), "-o", png_path, svg_path], check=True)


def mask_tile(png_path, size, shape):
    """Crop to the 72 x 72 visible area of the 108 canvas, resize, and apply a launcher-style mask."""
    img = Image.open(png_path).convert("RGBA")
    a, b = int(img.width * 18 / 108), int(img.width * 90 / 108)
    img = img.crop((a, a, b, b)).resize((size, size), Image.LANCZOS)
    mask = Image.new("L", (size * 4, size * 4), 0)
    d = ImageDraw.Draw(mask)
    if shape == "circle":
        d.ellipse((0, 0, size * 4 - 1, size * 4 - 1), fill=255)
    else:
        d.rounded_rectangle((0, 0, size * 4 - 1, size * 4 - 1), radius=int(size * 4 * 0.24), fill=255)
    img.putalpha(mask.resize((size, size), Image.LANCZOS))
    return img


def sheet(variants, out):
    label = ImageFont.truetype("/usr/share/fonts/TTF/DejaVuSans.ttf", 22)
    small = ImageFont.truetype("/usr/share/fonts/TTF/DejaVuSans.ttf", 16)
    pad, big = 40, 300
    w = pad + len(variants) * (2 * big + 3 * pad + 96 + 48)
    canvas = Image.new("RGB", (pad + 2 * big + pad + 96 + pad + 48 + pad + 60, pad + len(variants) * (big + 2 * pad + 20)), "#F4EBDD")
    d = ImageDraw.Draw(canvas)
    y = pad
    for name, png in variants:
        d.text((pad, y - 30), name, fill="#3E2716", font=label)
        x = pad
        canvas.paste(mask_tile(png, big, "square"), (x, y), mask_tile(png, big, "square"))
        x += big + pad
        canvas.paste(mask_tile(png, big, "circle"), (x, y), mask_tile(png, big, "circle"))
        x += big + pad
        for s in (96, 48):
            t = mask_tile(png, s, "square")
            canvas.paste(t, (x, y + (big - s) // 2), t)
            d.text((x, y + (big - s) // 2 + s + 6), f"{s}px", fill="#8A7358", font=small)
            x += s + pad
        y += big + 2 * pad + 20
    canvas.save(out)


if __name__ == "__main__":
    variants = {}
    for key, name, body in (
        ("a-open-book", "A: open book", open_book()),
        ("b-closed-book", "B: closed book with YBP", closed_book("ybp")),
        ("c-cross", "C: closed book with cross", closed_book("cross")),
    ):
        path = f"ybp-{key}.svg"
        open(path, "w").write(svg(body))
        render(path, f"ybp-{key}.png", 1080)
        variants[key] = (name, f"ybp-{key}.png")
    sheet(list(variants.values()), "preview.png")
    sheet([variants["b-closed-book"], variants["c-cross"]], "preview-b-vs-c.png")
    print("wrote previews")
