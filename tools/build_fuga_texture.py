"""Original nearest-filtered 8-frame pixel fire atlas; Python standard library only."""
from pathlib import Path
import math, struct, zlib

width, height = 32, 256
pixels = bytearray()
for y in range(height):
    pixels.append(0)
    frame, row = divmod(y, 32)
    for x in range(width):
        t = 1 - row / 31
        drift = math.sin(t * 10 - frame * math.pi / 4) * 0.09 * t
        center = (x + .5) / 32 - .5 - drift
        tongues = .05 * math.sin(x * 1.7 + frame * .9) + .035 * math.sin(x * 3.1 - frame)
        half = (.39 * (1 - t) ** .48 + tongues) * (.95 + .05 * math.cos(frame))
        edge = half - abs(center)
        alpha = 0 if edge < 0 or t > .96 else min(235, max(0, int(edge * 1800)))
        heat = max(0, min(1, (1 - abs(center) / max(.02, half)) * .8 + (1 - t) * .2))
        color = (255, int(48 + 195 * heat), int(6 + 150 * heat ** 4), alpha)
        pixels.extend(color)

def chunk(name, data):
    return struct.pack('>I', len(data)) + name + data + struct.pack('>I', zlib.crc32(name + data) & 0xffffffff)

out = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/unlimitedvoid/textures/effect/fuga_flame.png'
out.parent.mkdir(parents=True, exist_ok=True)
out.write_bytes(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(pixels, 9)) + chunk(b'IEND', b''))
out.with_suffix('.png.mcmeta').write_text('{"texture":{"blur":false,"clamp":false}}\n', encoding='utf-8')
print(out)
