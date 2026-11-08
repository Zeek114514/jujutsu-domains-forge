"""Deterministic soft particle masks; original procedural assets, no external images."""
from pathlib import Path
from PIL import Image
import math
output=Path(__file__).resolve().parents[1]/'src/main/resources/assets/unlimitedvoid/textures/particle'
output.mkdir(parents=True,exist_ok=True)
size=128
for name,smoke in [('fuga_particle',False),('fuga_smoke',True)]:
    image=Image.new('RGBA',(size,size))
    for y in range(size):
        for x in range(size):
            u=(x+0.5-size/2)/(size/2);v=(y+0.5-size/2)/(size/2)
            angle=math.atan2(v,u);radius=math.hypot(u,v)
            edge=.88+.065*math.sin(angle*7+1.2)+.04*math.cos(angle*11)
            a=max(0,1-radius/edge)
            sx=x*48/size;sy=y*48/size
            noise=.82+.10*math.sin(sx*.49+sy*.26)+.08*math.sin(sx*.31-sy*.60)
            alpha=(a**(1.25 if smoke else 1.65))*noise
            value=int(255*(.8+.2*a)) if smoke else 255
            image.putpixel((x,y),(value,value,value,int(max(0,min(255,alpha*255)))))
    image.save(output/(name+'.png'))
    print(output/(name+'.png'))
