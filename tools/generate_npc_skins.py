"""Historical 1.6.x concept atlases. Writes to ignored Artifacts, never overwrites supplied skins."""
from pathlib import Path
from PIL import Image,ImageDraw
OUT=Path(__file__).resolve().parents[1]/'Artifacts/legacy-npc-atlases'
OUT.mkdir(parents=True,exist_ok=True)
for gojo,name in [(True,'student_gojo'),(False,'sukuna_16')]:
 im=Image.new('RGBA',(256,256));d=ImageDraw.Draw(im)
 navy=(31,35,53);shadow=(21,24,38);lit=(44,48,67);skin=(235,202,181);skinshadow=(205,162,144)
 hair=(235,238,248) if gojo else (210,139,138);red=(147,39,47);ink=(26,20,29)
 def poly(points,c):d.polygon([(round(x*4),round(y*4)) for x,y in points],fill=c)
 def rect(x,y,w,h,c):d.rectangle((round(x*4),round(y*4),round((x+w)*4)-1,round((y+h)*4)-1),fill=c)
 def line(points,c,width=.25):d.line([(round(x*4),round(y*4)) for x,y in points],fill=c,width=max(1,round(width*4)))
 def cube(u,v,w,h,z,c):
  rect(u+z,v,w,z,c);rect(u+z+w,v,w,z,c)
  for x,ww in [(u,z),(u+z,w),(u+z+w,z),(u+2*z+w,w)]:rect(x,v+z,ww,h,c)
 def fabric(u,v,w,h):
  rect(u,v,w,h,navy)
  for t in range(1,int(w*4),3):line([(u+t/4,v),(u+t/4,v+h)],(32,36,54),.25)
  poly([(u+.2,v+.4),(u+w*.30,v+h*.35),(u+w*.21,v+h*.8),(u+.1,v+h)],shadow)
  poly([(u+w*.7,v+.2),(u+w*.95,v+h*.3),(u+w*.8,v+h*.8),(u+w*.62,v+h*.65)],lit)
  line([(u+.2,v+h-.5),(u+w-.2,v+h-.5)],(16,20,33),.25)
 cube(0,0,8,8,8,skin)
 rect(8,0,16,8,hair)
 for x in [0,16]:
  rect(x,8,8,2.8,hair);rect(x,10.8,1.5,3.2,hair)
  rect(x+2.5,12,1,1.8,skinshadow)
 rect(24,8,8,8,hair)
 # Face shading and jaw, at 32x32 actual pixels.
 rect(8,8,8,8,skin);rect(8,8,8,.6,hair)
 poly([(8,9),(8.6,10.5),(8.6,14.8),(10,16),(8,16)],skinshadow)
 poly([(16,9),(15.4,10.5),(15.4,14.8),(14,16),(16,16)],skinshadow)
 line([(11.7,12),(11.5,13.1),(12.15,13.25)],(187,143,129),.25)
 if gojo:
  for x in [9,13]:rect(x,11,1.8,.65,(232,241,249));rect(x+.6,11.05,.6,.6,(75,155,209))
  line([(11.25,14),(12.75,14)],(165,112,107),.25)
 else:
  poly([(8.9,11.35),(9.4,10.95),(11.1,11.2),(10.8,11.95),(9.35,11.9)],(248,235,227))
  poly([(12.9,11.2),(14.6,10.95),(15.1,11.35),(14.65,11.9),(13.2,11.95)],(248,235,227))
  for x in [10,13.5]:rect(x,11.2,.5,.75,(143,32,45));rect(x+.20,11.25,.22,.65,ink)
  line([(8.9,10.8),(9.8,10.5),(11.1,10.8)],ink,.3);line([(12.9,10.8),(14.2,10.5),(15.1,10.8)],ink,.3)
  # Forehead diamond/trident; nasal mark; angled cheek bands and lower-eye slits.
  line([(12,8.9),(11.4,9.5),(12,10.1),(12.6,9.5),(12,8.9)],ink,.32)
  line([(12,8.7),(12,10.3)],ink,.28)
  line([(11.55,12.0),(12.45,12.0)],ink,.3)
  for points in [[(8.45,12.1),(9.0,12.75),(10,13.0)],[(8.4,12.8),(8.9,13.5),(9.65,13.65)],[(15.55,12.1),(15,12.75),(14,13)],[(15.6,12.8),(15.1,13.5),(14.35,13.65)]]:line(points,ink,.28)
  for x in [9.1,13.65]:line([(x,12.25),(x+.65,12.4)],(97,39,46),.28)
  line([(10.8,14.2),(11.8,14.5),(13,14.25)],(136,71,75),.25)
  rect(11.25,15.1,.35,.75,ink);rect(12.4,15.1,.35,.75,ink)
 cube(16,16,8,12,4,navy)
 for x,w in [(16,4),(20,8),(28,4),(32,8)]:fabric(x,20,w,12)
 line([(23.9,22),(23.9,31)],(13,17,28),.25)
 if not gojo:rect(20,20,8,2.5,red);rect(32,20,8,3.5,red)
 for u,v in [(40,16),(32,48)]:
  cube(u,v,4,12,4,navy)
  for x in [u,u+4,u+8,u+12]:
   fabric(x,v+4,4,9);rect(x,v+12,4,1,shadow if gojo else red);rect(x,v+13,4,3,skin)
   line([(x+.35,v+14.75),(x+3.4,v+14.75)],skinshadow,.25)
   if not gojo:rect(x,v+13.4,4,.4,ink);rect(x,v+14.25,4,.4,ink)
 for u,v in [(0,16),(16,48)]:
  cube(u,v,4,12,4,shadow)
  for x in [u,u+4,u+8,u+12]:
   fabric(x,v+4,4,9);rect(x,v+13,4,3,(18,18,25));line([(x+.2,v+8),(x+1.2,v+9),(x+3.4,v+8.4)],(42,45,61),.25)
 # Original overlay regions are repurposed as model material swatches.
 rect(0,32,36,16,hair)
 for start in [0,8,16,24]:
  for j in range(32):
   factor=(j%7-3)*2
   c=tuple(max(0,min(255,k+factor)) for k in hair)
   rect(start+j/4,32,.25,16,c)
  line([(start+1,33),(start+1.6,37),(start+.6,41)],tuple(max(0,k-18) for k in hair),.25)
 rect(36,32,28,16,navy)
 for x in range(144,256):
  for y in range(128,192):
   t=((x//5+y//9)%3-1)*2;im.putpixel((x,y),tuple(k+t for k in navy)+(255,))
 rect(48,32,5,15,shadow)
 rect(0,48,16,16,red)
 for x in range(64):
  c=(156 if x%11<3 else 136,42 if x%11<3 else 32,49 if x%11<3 else 40)
  rect(x/4,48,.25,16,c)
 rect(32,0,24,16,(14,17,25));poly([(32,0),(38,0),(32,5)],(41,46,61));line([(33,1),(36,1.2)],(87,93,108),.25)
 rect(56,0,8,8,(195,151,68));rect(56,0,8,.5,(240,206,125));rect(56,0,.5,8,(224,183,94))
 rect(56,8,8,8,skin);rect(56,8,1,8,skinshadow)
 im.save(OUT/(name+'.png'));print(name,'256x256 RGBA')
