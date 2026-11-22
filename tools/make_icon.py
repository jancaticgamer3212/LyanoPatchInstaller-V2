#!/usr/bin/env python3
"""Renders the launcher icons (legacy squircle, round, adaptive foreground).

No image libraries needed: rasterizes with 4x supersampling and writes PNGs
with zlib. Run from the project root: python tools/make_icon.py
"""
import math
import os
import struct
import zlib

SS = 4

START = (0x12, 0xC4, 0x6B)
END = (0x00, 0x79, 0x4F)

R_OUT = 0.335
R_IN = 0.272
GAP_START = math.radians(-26)
GAP_END = math.radians(26)

TRI = ((0.415, 0.315), (0.415, 0.685), (0.705, 0.500))

DENSITIES = [("mdpi", 1.0), ("hdpi", 1.5), ("xhdpi", 2.0), ("xxhdpi", 3.0), ("xxxhdpi", 4.0)]


def lerp(a, b, t):
    return a + (b - a) * t


def in_rounded_rect(x, y, r=0.22):
    dx = max(abs(x - 0.5) - (0.5 - r), 0.0)
    dy = max(abs(y - 0.5) - (0.5 - r), 0.0)
    return math.hypot(dx, dy) <= r


def in_circle(x, y):
    return math.hypot(x - 0.5, y - 0.5) <= 0.5


def in_triangle(x, y):
    (ax, ay), (bx, by), (cx, cy) = TRI
    d1 = (x - bx) * (ay - by) - (ax - bx) * (y - by)
    d2 = (x - cx) * (by - cy) - (bx - cx) * (y - cy)
    d3 = (x - ax) * (cy - ay) - (cx - ax) * (y - ay)
    neg = d1 < 0 or d2 < 0 or d3 < 0
    pos = d1 > 0 or d2 > 0 or d3 > 0
    return not (neg and pos)


def in_ring(x, y):
    """Arc (math coords, y up) with a gap on the right and an arrowhead."""
    mx, my = x - 0.5, 0.5 - y
    r = math.hypot(mx, my)
    th = math.atan2(my, mx)
    if th < 0:
        th += 2 * math.pi
    gap_lo = GAP_START % (2 * math.pi)
    gap_hi = GAP_END % (2 * math.pi)
    in_arc = R_IN <= r <= R_OUT and not (gap_lo <= th <= gap_hi)
    if in_arc:
        return True
    # Arrowhead at the upper end of the arc, pointing counter-clockwise.
    t = GAP_END
    rm = (R_OUT + R_IN) * 0.5
    px, py = rm * math.cos(t), rm * math.sin(t)
    tx, ty = -math.sin(t), math.cos(t)
    nx, ny = math.cos(t), math.sin(t)
    tip = (px + tx * 0.095, py + ty * 0.095)
    b1 = (px + nx * 0.075, py + ny * 0.075)
    b2 = (px - nx * 0.075, py - ny * 0.075)
    (ax, ay), (bx, by), (cx, cy) = tip, b1, b2
    d1 = (mx - bx) * (ay - by) - (ax - bx) * (my - by)
    d2 = (mx - cx) * (by - cy) - (bx - cx) * (my - cy)
    d3 = (mx - ax) * (cy - ay) - (cx - ax) * (my - ay)
    neg = d1 < 0 or d2 < 0 or d3 < 0
    pos = d1 > 0 or d2 > 0 or d3 > 0
    return not (neg and pos)


def shade(x, y, mode):
    if mode == "fg":
        # Adaptive layers are 108dp with a 66dp safe zone; keep the mark inside it.
        x = 0.5 + (x - 0.5) / 0.82
        y = 0.5 + (y - 0.5) / 0.82
        if in_ring(x, y) or in_triangle(x, y):
            return (1.0, 1.0, 1.0, 1.0)
        return (0.0, 0.0, 0.0, 0.0)
    if mode == "square":
        if not in_rounded_rect(x, y):
            return (0.0, 0.0, 0.0, 0.0)
    else:
        if not in_circle(x, y):
            return (0.0, 0.0, 0.0, 0.0)
    if in_ring(x, y) or in_triangle(x, y):
        return (1.0, 1.0, 1.0, 1.0)
    d = (x + (1.0 - y)) * 0.5
    return (lerp(START[0], END[0], d) / 255.0,
            lerp(START[1], END[1], d) / 255.0,
            lerp(START[2], END[2], d) / 255.0, 1.0)


def render(size, mode):
    w = h = size
    hi = size * SS
    acc = [0.0] * (w * h * 4)
    step = 1.0 / hi
    for py in range(hi):
        y = (py + 0.5) * step
        for px in range(hi):
            x = (px + 0.5) * step
            r, g, b, a = shade(x, y, mode)
            i = ((py // SS) * w + (px // SS)) * 4
            acc[i] += r * a
            acc[i + 1] += g * a
            acc[i + 2] += b * a
            acc[i + 3] += a
    n = SS * SS
    out = bytearray(w * h * 4)
    for p in range(w * h):
        i = p * 4
        a = acc[i + 3] / n
        if a > 0:
            out[i] = min(255, int(round(acc[i] / n / a * 255)))
            out[i + 1] = min(255, int(round(acc[i + 1] / n / a * 255)))
            out[i + 2] = min(255, int(round(acc[i + 2] / n / a * 255)))
        out[i + 3] = min(255, int(round(a * 255)))
    return bytes(out)


def write_png(path, size, pixels):
    raw = bytearray()
    for y in range(size):
        raw.append(0)
        raw += pixels[y * size * 4:(y + 1) * size * 4]

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    png = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
           + chunk(b"IEND", b""))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png)


def main():
    for name, scale in DENSITIES:
        legacy = int(round(48 * scale))
        fg = int(round(108 * scale))
        write_png("res/mipmap-%s/ic_launcher.png" % name, legacy, render(legacy, "square"))
        write_png("res/mipmap-%s/ic_launcher_round.png" % name, legacy, render(legacy, "round"))
        write_png("res/drawable-%s/ic_launcher_foreground.png" % name, fg, render(fg, "fg"))
        print("%-8s legacy %3d  foreground %3d" % (name, legacy, fg))


if __name__ == "__main__":
    main()
