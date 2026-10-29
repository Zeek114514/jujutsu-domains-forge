"""Preserve supplied 64x64 UVs; edit only the agreed human Sukuna clothing mask."""
from pathlib import Path
from PIL import Image,ImageDraw
import hashlib,json,shutil
import argparse
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--source',type=Path,required=True,help='Folder containing the supplied source skin PNGs')
SOURCE=parser.parse_args().source
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'src/main/resources/assets/unlimitedvoid/textures/entity'
files={'student_gojo':'skin_Suguru_Nobara_e459248157541bc5de80071429f3ed7f.png','shinjuku_gojo':'skin_Boy_Sweatpants_7e3d91008aeb1c4ca6635f00c9fa9088.png','sukuna_16':'b0059d77b9487888.png','true_sukuna':'720b8352ce7efeed.png','awakened_gojo':'skin_Anime_Jujutsu Kaisen_e4d8a245b9e687df4b4b286cfbf5c7f1.png'}
records={}
for name,filename in files.items():
 src=SOURCE/filename;im=Image.open(src).convert('RGBA');assert im.size==(64,64)
 if name!='sukuna_16':shutil.copy2(src,OUT/(name+'.png'))
 else:
  before=im.copy();d=ImageDraw.Draw(im);mask=Image.new('1',(64,64));m=ImageDraw.Draw(mask)
  regions=[(16,16,40,32),(16,32,40,48),(40,16,56,28),(40,32,56,44),(32,48,48,60),(48,48,64,60),(0,16,16,32),(0,32,16,48),(16,48,32,64),(0,48,16,64)]
  for x0,y0,x1,y1 in regions:
   m.rectangle((x0,y0,x1-1,y1-1),fill=1)
   for y in range(y0,y1):
    for x in range(x0,x1):
     shade=((x//2+y//3)%3)*3;im.putpixel((x,y),(25+shade,28+shade,44+shade,255))
  # Body front/back: Yuji's red hood, dark jacket, small school button.
  for u,v in [(16,16),(16,32)]:
   d.rectangle((u+4,v+4,u+11,v+5),fill=(129,35,43,255));d.rectangle((u+16,v+4,u+23,v+6),fill=(129,35,43,255))
   d.line((u+8,v+6,u+8,v+15),fill=(17,20,33,255));d.point((u+10,v+7),fill=(208,169,80,255))
  for u,v in [(40,16),(40,32),(32,48),(48,48)]:
   d.rectangle((u,v+10,u+15,v+11),fill=(145,40,47,255))
  for u,v in [(0,16),(0,32),(16,48),(0,48)]:
   d.rectangle((u,v+13,u+15,v+15),fill=(24,19,25,255));d.line((u,v+13,u+15,v+13),fill=(151,41,47,255))
  changed=0
  for y in range(64):
   for x in range(64):
    if before.getpixel((x,y))!=im.getpixel((x,y)):
     assert mask.getpixel((x,y)),(x,y);changed+=1
  assert im.crop((0,0,64,16)).tobytes()==before.crop((0,0,64,16)).tobytes()
  for box in [(40,28,56,32),(32,60,48,64)]:assert im.crop(box).tobytes()==before.crop(box).tobytes()
  im.save(OUT/(name+'.png'));print('CLOTHING_ONLY_PASS',changed,'changed pixels; head/hands/UV untouched')
 records[name]={'source_filename':filename,'source_sha256':hashlib.sha256(src.read_bytes()).hexdigest(),'output_sha256':hashlib.sha256((OUT/(name+'.png')).read_bytes()).hexdigest(),'model':'slim' if name in ['student_gojo','true_sukuna','awakened_gojo'] else 'classic'}
(ROOT/'docs/supplied-skins.json').write_text(json.dumps(records,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
# Small original pixel icons for the functional items, not resampled AI concept images.
items=OUT.parent/'item';items.mkdir(exist_ok=True)
im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
d.polygon([(3,13),(22,8),(26,11),(21,20),(8,23),(3,20)],fill='#11131e');d.line([(4,14),(21,10),(24,12)],fill='#5a6076',width=2);d.line([(6,19),(19,17),(23,12)],fill='#34384b',width=2)
d.polygon([(22,12),(30,7),(29,11),(24,15)],fill='#202436');d.polygon([(23,17),(29,23),(28,26),(21,20)],fill='#35394c');im.save(items/'blindfold.png')
im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
d.polygon([(7,27),(3,23),(7,18),(15,9),(22,3),(26,5),(27,10),(19,19),(11,27)],fill='#48252b');d.line([(6,23),(12,17),(18,10),(24,6)],fill='#a56c53',width=4)
for x,y in [(8,21),(12,16),(17,11)]:d.line([(x-2,y-1),(x+2,y+2)],fill='#452027',width=2)
d.polygon([(21,8),(23,3),(27,2),(27,7),(24,11)],fill='#20151d');im.save(items/'sukuna_finger.png')
