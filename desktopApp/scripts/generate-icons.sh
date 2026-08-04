#!/usr/bin/env bash
# Regenerate desktop icons to match androidApp adaptive launcher
# (background #2E7D6F + white ring/plus from ic_launcher_foreground.xml).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
python3 - "$ROOT" <<'PY'
import struct, zlib, sys
from pathlib import Path

ROOT = Path(sys.argv[1])
BG = (0x2E, 0x7D, 0x6F, 255)
WHITE = (255, 255, 255, 255)

def make(size, rounded=True):
    pixels = bytearray(size * size * 4)
    rcorner = size * 0.22 if rounded else 0
    def put(x, y, c):
        if 0 <= x < size and 0 <= y < size:
            i = (y * size + x) * 4
            pixels[i:i+4] = bytes(c)
    def inside_round(x, y):
        if not rounded:
            return True
        r = rcorner
        if x < r and y < r:
            return (x - r) ** 2 + (y - r) ** 2 <= r * r
        if x > size - 1 - r and y < r:
            return (x - (size - 1 - r)) ** 2 + (y - r) ** 2 <= r * r
        if x < r and y > size - 1 - r:
            return (x - r) ** 2 + (y - (size - 1 - r)) ** 2 <= r * r
        if x > size - 1 - r and y > size - 1 - r:
            return (x - (size - 1 - r)) ** 2 + (y - (size - 1 - r)) ** 2 <= r * r
        return True
    for y in range(size):
        for x in range(size):
            if inside_round(x, y):
                put(x, y, BG)
    s = size / 108.0
    cx = cy = size / 2.0
    ro, ri = 30 * s, 18 * s
    for y in range(size):
        for x in range(size):
            if pixels[(y*size+x)*4+3] == 0:
                continue
            d = ((x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2) ** 0.5
            if ri < d <= ro:
                put(x, y, WHITE)
    t = max(1.0, 3 * s)
    for y in range(int(42 * s), int(54 * s) + 1):
        for x in range(int(cx - t / 2), int(cx + t / 2) + 1):
            if 0 <= y < size and 0 <= x < size and pixels[(y*size+x)*4+3]:
                put(x, y, WHITE)
    for y in range(int(cy - t / 2), int(cy + t / 2) + 1):
        for x in range(int(48 * s), int(60 * s) + 1):
            if 0 <= y < size and 0 <= x < size and pixels[(y*size+x)*4+3]:
                put(x, y, WHITE)
    return pixels

def write_png(path, pixels, size):
    def chunk(tag, data):
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)
    raw = b''.join(b'\x00' + bytes(pixels[y*size*4:(y+1)*size*4]) for y in range(size))
    png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', size, size, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')
    Path(path).write_bytes(png)

icons = ROOT / 'icons'
res = ROOT / 'src/main/resources/icons'
icons.mkdir(parents=True, exist_ok=True)
res.mkdir(parents=True, exist_ok=True)
for s in (1024, 512, 256, 128, 64, 32, 16):
    write_png(icons / f'icon-{s}.png', make(s, True), s)
write_png(icons / 'icon.png', make(512, True), 512)
write_png(res / 'icon.png', make(256, True), 256)
write_png(res / 'tray.png', make(64, True), 64)

# ICO
entries = [(s, (icons / f'icon-{s}.png').read_bytes()) for s in (16, 32, 64, 128, 256)]
header = struct.pack('<HHH', 0, 1, len(entries))
dire = b''; blobs = b''; offset = 6 + 16 * len(entries)
for size, data in entries:
    w = 0 if size >= 256 else size
    h = 0 if size >= 256 else size
    dire += struct.pack('<BBBBHHII', w, h, 0, 0, 1, 32, len(data), offset)
    blobs += data
    offset += len(data)
(icons / 'icon.ico').write_bytes(header + dire + blobs)
print('wrote png/ico under', icons)
PY

if [[ "$(uname)" == Darwin ]]; then
  ICONSET="$(mktemp -d)/KeepStraight.iconset"
  mkdir -p "$ICONSET"
  SRC="$ROOT/icons/icon-1024.png"
  sips -z 16 16 "$SRC" --out "$ICONSET/icon_16x16.png" >/dev/null
  sips -z 32 32 "$SRC" --out "$ICONSET/icon_16x16@2x.png" >/dev/null
  sips -z 32 32 "$SRC" --out "$ICONSET/icon_32x32.png" >/dev/null
  sips -z 64 64 "$SRC" --out "$ICONSET/icon_32x32@2x.png" >/dev/null
  sips -z 128 128 "$SRC" --out "$ICONSET/icon_128x128.png" >/dev/null
  sips -z 256 256 "$SRC" --out "$ICONSET/icon_128x128@2x.png" >/dev/null
  sips -z 256 256 "$SRC" --out "$ICONSET/icon_256x256.png" >/dev/null
  sips -z 512 512 "$SRC" --out "$ICONSET/icon_256x256@2x.png" >/dev/null
  sips -z 512 512 "$SRC" --out "$ICONSET/icon_512x512.png" >/dev/null
  sips -z 1024 1024 "$SRC" --out "$ICONSET/icon_512x512@2x.png" >/dev/null
  iconutil -c icns "$ICONSET" -o "$ROOT/icons/icon.icns"
  echo "wrote $ROOT/icons/icon.icns"
fi
