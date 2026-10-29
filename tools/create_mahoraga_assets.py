"""Deterministic UV swatches for cuboid geometry, not a painted character concept."""
from PIL import Image
from pathlib import Path
import random
rng=random.Random(1888)
image=Image.new('RGBA',(256,256))
for x in range(256):
 for y in range(256):
  base=(222,219,208) if x<128 and y<128 else (28,27,29) if y<128 else (38,37,42) if x<128 else (185,145,63)
  noise=rng.choice([-6,-3,0,0,2,4]);tile=((x//3*7+y//4*11)%9-4)*2
  rgb=tuple(max(0,min(255,c+noise+tile)) for c in base)
  if 64<=x<112 and 64<=y<112:rgb=tuple(max(0,c-26) for c in rgb)
  image.putpixel((x,y),(*rgb,255))
path=Path(__file__).resolve().parents[1]/'src/main/resources/assets/unlimitedvoid/textures/entity/mahoraga.png'
image.save(path)
print(path)

# A translucent native-rendered shadow pool. It is not baked into the body texture.
import math
pool=Image.new('RGBA',(64,64))
for x in range(64):
 for y in range(64):
  distance=math.hypot((x-31.5)/31.5,(y-31.5)/31.5)
  alpha=int(235*max(0,min(1,(1-distance)*5)))
  edge=1 if .77<distance<.87 else 0
  pool.putpixel((x,y),(8+edge*18,5+edge*10,13+edge*25,alpha))
pool.save(path.with_name('mahoraga_shadow.png'))

# Optional regeneration of the original wheel click; the runtime mod needs only the OGG.
import sys
if '--audio' in sys.argv:
 import numpy as np
 import soundfile as sf
 rate=44100;t=np.arange(int(rate*.24))/rate;rng=np.random.default_rng(108)
 samples=.45*rng.normal(0,1,len(t))*np.exp(-t/.004)+.24*np.sin(2*np.pi*923*t)*np.exp(-t/.05)+.16*np.sin(2*np.pi*1739*t)*np.exp(-t/.035)+.07*np.sin(2*np.pi*317*t)*np.exp(-t/.1)
 sf.write(path.parents[2]/'sounds/mahoraga_turn.ogg',np.clip(samples,-.9,.9),rate,format='OGG',subtype='VORBIS')
