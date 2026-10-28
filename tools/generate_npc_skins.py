"""Original deterministic 64x64 skins; no downloaded character assets."""
from pathlib import Path
from PIL import Image,ImageDraw
OUT=Path(__file__).resolve().parents[1]/'src/main/resources/assets/unlimitedvoid/textures/entity'
OUT.mkdir(parents=True,exist_ok=True)
for gojo,name in [(True,'student_gojo'),(False,'sukuna_16')]:
 im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
 skin=(229,190,165,255);skinlight=(244,210,183,255);hair=(234,238,250,255) if gojo else (206,135,133,255)
 navy=(28,32,49,255);shade=(21,24,37,255);light=(39,43,62,255);red=(135,36,42,255);black=(16,15,22,255)
 def rect(x,y,w,h,c):d.rectangle((x,y,x+w-1,y+h-1),fill=c)
 def cube(u,v,w,h,depth,c):
  rect(u+depth,v,w,depth,c);rect(u+depth+w,v,w,depth,c)
  rect(u,v+depth,depth,h,c);rect(u+depth,v+depth,w,h,c);rect(u+depth+w,v+depth,depth,h,c);rect(u+depth*2+w,v+depth,w,h,c)
 cube(0,0,8,8,8,skin);rect(8,0,16,8,hair)
 # Hair along sides/back; front forehead remains readable below the fringe.
 for x in (0,8,16,24):rect(x,8,8,2 if x==8 else 3,hair)
 rect(24,8,8,8,hair);rect(8,14,8,2,skin)
 # Eye pixels, brows and subtle mouth.
 rect(9,11,2,1,black);rect(13,11,2,1,black)
 if not gojo:
  rect(10,11,1,1,(143,35,44,255));rect(13,11,1,1,(143,35,44,255))
  for x,y in [(11,8),(12,8),(11,9),(12,9),(8,12),(9,13),(14,13),(15,12),(11,12),(12,12),(11,15),(12,15)]:rect(x,y,1,1,black)
  rect(9,12,1,1,(100,48,47,255));rect(14,12,1,1,(100,48,47,255))
 else:rect(9,11,6,1,black)
 rect(11,14,2,1,(158,107,93,255))
 cube(16,16,8,12,4,navy)
 rect(20,20,8,2,shade if gojo else red);rect(23,22,1,9,shade)
 rect(26,22,1,1,(203,169,86,255));rect(20,30,8,1,shade)
 rect(21,24,1,5,light);rect(26,25,1,4,light)
 if not gojo:
  rect(32,20,8,4,red);rect(21,20,6,3,red)
  rect(24,21,1,1,(191,76,65,255));rect(25,23,1,1,(196,158,76,255))
 for u,v in [(40,16),(32,48)]:
  cube(u,v,4,12,4,navy)
  # Four arm faces: lower sleeves, skin hands and tattoo wrist bands.
  for x in [u,u+4,u+8,u+12]:
   rect(x,v+4+8,4,1,shade if gojo else red);rect(x,v+4+9,4,3,skin)
   if not gojo:rect(x,v+4+10,4,1,black)
 for u,v in [(0,16),(16,48)]:
  cube(u,v,4,12,4,shade)
  for x in [u,u+4,u+8,u+12]:
   rect(x,v+4,1,9,navy);rect(x,v+4+9,4,3,black)
   if not gojo:rect(x,v+4+9,4,1,red);rect(x+1,v+4+10,2,1,red)
 # Unused skin-overlay space supplies flat cuboid hair and accessory colors.
 rect(0,32,36,15,hair);rect(36,32,28,15,shade if gojo else red)
 im.save(OUT/(name+'.png'))
 print(name,'64x64 RGBA')
