"""Original deterministic procedural glow and synthesised cues; no sampled recordings.
Usage: python tools/generate_limitless_assets.py --ffmpeg /path/to/ffmpeg
"""
import argparse,math,random,struct,subprocess,tempfile,wave,zlib
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--ffmpeg',required=True);args=p.parse_args()
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/unlimitedvoid'
audio_output=Path(__file__).resolve().parents[1]/'build/synthesized-limitless-audio'
audio_output.mkdir(parents=True,exist_ok=True)
def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
rows=[]
for y in range(128):
 row=bytearray([0])
 for x in range(128):
  r=math.hypot((x-63.5)/63.5,(y-63.5)/63.5)
  a=max(0,1-r*r)**2
  row.extend((255,255,255,int(a*255)))
 rows.append(row)
png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',128,128,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(b''.join(rows),9))+chunk(b'IEND',b'')
(root/'textures/effect/limitless_glow.png').write_bytes(png)
for index,(name,duration) in enumerate([('limitless_charge',3.5),('blue_cast',1.5),('red_cast',1.0),('purple_cast',2.2)]):
 rng=random.Random(1500+index);rate=44100;samples=[];phase=0;low=0
 for n in range(int(rate*duration)):
  t=n/rate;q=t/duration;noise=rng.uniform(-1,1);low=.94*low+.06*noise
  if index==0:
   hz=65+q*q*460;env=min(1,t/.08)*(.4+.6*q)*min(1,(duration-t)/.12)
   phase+=2*math.pi*hz/rate;v=(math.sin(phase)*.32+math.sin(phase*1.501)*.15+low*.7)*env
  else:
   hz=(145 if index==1 else 60 if index==2 else 38)+(1-q)**3*(330 if index==1 else 180)
   phase+=2*math.pi*hz/rate;env=min(1,t/.012)*math.exp(-q*(3 if index==1 else 5))
   v=(math.sin(phase)*.44+low*(2 if index==1 else 3)+noise*(.10 if index==1 else .24))*env
  samples.append(int(max(-.94,min(.94,v))*32767))
 with tempfile.TemporaryDirectory() as tmp:
  wav=Path(tmp)/'cue.wav'
  with wave.open(str(wav),'wb') as f:f.setnchannels(1);f.setsampwidth(2);f.setframerate(rate);f.writeframes(struct.pack('<'+'h'*len(samples),*samples))
  subprocess.run([args.ffmpeg,'-y','-loglevel','error','-i',str(wav),'-c:a','libvorbis','-q:a','5',str(audio_output/f'{name}.ogg')],check=True)
 print(name,duration)
