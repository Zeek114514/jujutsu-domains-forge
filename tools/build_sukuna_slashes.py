"""Build the short character-labelled slash cues and verify decoded peaks."""
from pathlib import Path
import argparse,subprocess,json,array,math
parser=argparse.ArgumentParser();parser.add_argument('--ffmpeg',default='ffmpeg');args=parser.parse_args()
root=Path(__file__).resolve().parents[1];source=root/'requested_slashes';out=root/'src/main/resources/assets/unlimitedvoid/sounds'
def build(name,inputs,graph):
 command=[args.ffmpeg,'-v','error','-y']
 for file in inputs:command+=['-i',str(source/file)]
 command+=['-filter_complex',graph,'-map','[out]','-ac','1','-ar','44100','-c:a','libvorbis','-q:a','5',str(out/(name+'.ogg'))]
 subprocess.run(command,check=True)
def finish(label,target):return f'[{label}]apad=whole_dur=0.45,atrim=0:0.45,highpass=f=55,loudnorm=I={target}:TP=-3:LRA=4,alimiter=limit=0.78:level=0,afade=t=out:st=0.27:d=0.18[out]'
for i,(src,rate) in enumerate([('launched.mp3',44100),('launched2.mp3',41454),('launched.mp3',47628)],1):
 graph=f'[0:a]aformat=channel_layouts=mono,asetrate={rate},aresample=44100,volume=0.68,apad=whole_dur=0.32[cut];'
 graph+='aevalsrc=0.12*sin(2*PI*(115*t-210*t*t))*exp(-32*t):s=44100:d=0.20[bass];'
 graph+='[1:a]aformat=channel_layouts=mono,highpass=f=850,volume=0.16,adelay=34[edge];'
 graph+='[cut][bass][edge]amix=inputs=3:normalize=0[mix];'+finish('mix',-19)
 build('slash_wind_'+str(i),[src,'landed.mp3'],graph)
for i,rate in enumerate([44100,38808],1):
 graph=f'[0:a]aformat=channel_layouts=mono,asetrate={rate},aresample=44100,volume=0.95[hit];'
 graph+='aevalsrc=0.16*sin(2*PI*(95*t-130*t*t))*exp(-25*t):s=44100:d=0.23[body];'
 graph+='[1:a]aformat=channel_layouts=mono,highpass=f=1000,volume=0.2[edge];'
 graph+='[hit][body][edge]amix=inputs=3:normalize=0[mix];'+finish('mix',-18)
 build('slash_hit_'+str(i),['landed.mp3','launched2.mp3'],graph)
manifest=[]
for name in ['slash_wind_1','slash_wind_2','slash_wind_3','slash_hit_1','slash_hit_2']:
 path=out/(name+'.ogg');raw=subprocess.run([args.ffmpeg,'-v','error','-i',str(path),'-f','f32le','-ac','1','-ar','44100','-'],capture_output=True,check=True).stdout
 samples=array.array('f');samples.frombytes(raw);peak=max(abs(x) for x in samples);rms=math.sqrt(sum(x*x for x in samples)/len(samples))
 assert peak<1 and rms>0.02,(name,peak,rms)
 manifest.append({'file':name+'.ogg','seconds':len(samples)/44100,'peak':peak,'rms':rms,'bytes':path.stat().st_size})
(source/'processed.json').write_text(json.dumps(manifest,indent=2),'utf-8');print(json.dumps(manifest))
