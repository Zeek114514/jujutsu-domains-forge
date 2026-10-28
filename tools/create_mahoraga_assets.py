"""Deterministic UV swatches for cuboid geometry, not a painted character concept."""
from PIL import Image
from pathlib import Path
import random
rng=random.Random(1888)
image=Image.new('RGBA',(256,256))
for x in range(256):
 for y in range(256):
  base=(222,219,208) if x<128 and y<128 else (28,27,29) if y<128 else (133,119,94) if x<128 else (143,111,59)
  noise=rng.choice([-6,-3,0,0,2,4]);tile=((x//3*7+y//4*11)%9-4)*2
  rgb=tuple(max(0,min(255,c+noise+tile)) for c in base)
  if 64<=x<112 and 64<=y<112:rgb=tuple(max(0,c-26) for c in rgb)
  image.putpixel((x,y),(*rgb,255))
path=Path(__file__).resolve().parents[1]/'src/main/resources/assets/unlimitedvoid/textures/entity/mahoraga.png'
image.save(path)
print(path)
