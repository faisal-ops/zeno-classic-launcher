#!/usr/bin/env python3
"""
Resize wallpapers to the connected device's logical resolution (720x720 for Q25).
Center-crop with "cover" semantics so the square screen is filled.

Usage (from repo root):
  python3 scripts/export_wallpapers_for_phone.py

Reads:  wallpapers/*.{jpg,jpeg,png,webp} (skips zero-byte files and phone_720/)
Writes: wallpapers/phone_720/*.jpg
"""

from __future__ import annotations

import sys
from pathlib import Path

try:
    from PIL import Image, ImageOps
except ImportError as e:
    print("Install Pillow: pip install pillow", file=sys.stderr)
    raise SystemExit(1) from e

# Match `adb shell wm size` Physical size (width x height)
TARGET_W = 720
TARGET_H = 720

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "wallpapers"
OUT = SRC / "phone_720"

EXTS = {".jpg", ".jpeg", ".png", ".webp", ".JPG", ".JPEG", ".PNG", ".WEBP"}


def fit_cover_crop(im: Image.Image, tw: int, th: int) -> Image.Image:
    """Scale up so image covers tw×th, then center-crop."""
    w, h = im.size
    scale = max(tw / w, th / h)
    nw = max(int(round(w * scale)), tw)
    nh = max(int(round(h * scale)), th)
    im = im.resize((nw, nh), Image.Resampling.LANCZOS)
    left = (nw - tw) // 2
    top = (nh - th) // 2
    return im.crop((left, top, left + tw, top + th))


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    count = 0
    for path in sorted(SRC.iterdir()):
        if not path.is_file():
            continue
        if path.suffix not in EXTS:
            continue
        if path.stat().st_size == 0:
            print(f"skip (empty): {path.name}")
            continue
        im = Image.open(path)
        im = ImageOps.exif_transpose(im)
        if im.mode not in ("RGB", "RGBA"):
            im = im.convert("RGBA")
        if im.mode == "RGBA":
            bg = Image.new("RGB", im.size, (255, 255, 255))
            bg.paste(im, mask=im.split()[3])
            im = bg
        else:
            im = im.convert("RGB")

        out_im = fit_cover_crop(im, TARGET_W, TARGET_H)
        stem = path.stem
        out_path = OUT / f"{stem}_720.jpg"
        out_im.save(out_path, "JPEG", quality=92, optimize=True)
        print(f"ok: {path.name} -> {out_path.name} ({TARGET_W}x{TARGET_H})")
        count += 1

    if count == 0:
        print("No images processed.", file=sys.stderr)
        raise SystemExit(1)
    print(f"Done: {count} file(s) in {OUT}")


if __name__ == "__main__":
    main()
