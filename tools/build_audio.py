"""Build the custom mono Ogg Vorbis assets. Requires FFmpeg, no Python packages."""
from pathlib import Path
import argparse, subprocess, json, array, sys

parser=argparse.ArgumentParser()
parser.add_argument('--ffmpeg',default='ffmpeg')
args=parser.parse_args()
ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'third_party_audio'
OUT=ROOT/'src/main/resources/assets/unlimitedvoid/sounds'
OUT.mkdir(parents=True,exist_ok=True)

def build(name,inputs,graph):
    command=[args.ffmpeg,'-hide_banner','-loglevel','error','-y']
    for source in inputs: command+=['-i',str(SOURCE/source)]
    command+=['-filter_complex',graph,'-map','[out]','-ac','1','-ar','44100','-c:a','libvorbis','-q:a','4',str(OUT/(name+'.ogg'))]
    result=subprocess.run(command,capture_output=True,text=True)
    if result.returncode: raise RuntimeError(name+': '+result.stderr)
    print(name,(OUT/(name+'.ogg')).stat().st_size,flush=True)

build('void_expand',['portal.mp3','energy_slash.mp3'],
    '[0:a]atrim=0:3.8,asetpts=PTS-STARTPTS,highpass=f=45,lowpass=f=7000,volume=0.8[p];'
    '[1:a]areverse,asetrate=35280,aresample=44100,adelay=700,volume=0.4[s];'
    '[p][s]amix=inputs=2:normalize=0,afade=t=in:d=0.04,afade=t=out:st=3:d=0.8,'
    'loudnorm=I=-19:TP=-3:LRA=7,alimiter=limit=0.85[out]')
build('shrine_expand',['portal.mp3','energy_slash.mp3','blade_impact.mp3'],
    '[0:a]atrim=0:3,asetpts=PTS-STARTPTS,asetrate=35280,aresample=44100,lowpass=f=4200,volume=0.9[p];'
    '[1:a]areverse,adelay=700,volume=0.55[s];'
    '[2:a]asetrate=33075,aresample=44100,adelay=1000,aecho=0.8:0.7:180:0.25,volume=0.65[h];'
    '[p][s][h]amix=inputs=3:normalize=0,atrim=0:3.7,afade=t=in:d=0.04,afade=t=out:st=2.8:d=0.9,'
    'loudnorm=I=-19:TP=-3:LRA=7,alimiter=limit=0.85[out]')
for i,rate in enumerate([39690,44100,48510],1):
    build('slash_wind_'+str(i),['energy_slash.mp3'],
        f'[0:a]asetrate={rate},aresample=44100,highpass=f=180,lowpass=f=10000,'
        'afade=t=in:d=0.003,afade=t=out:st=0.28:d=0.12,loudnorm=I=-23:TP=-4:LRA=5,alimiter=limit=0.8[out]')
for i,rate in enumerate([44100,35280],1):
    build('slash_hit_'+str(i),['blade_impact.mp3'],
        f'[0:a]atrim=0:0.58,asetrate={rate},aresample=44100,highpass=f=100,lowpass=f=8500,'
        'afade=t=out:st=0.39:d=0.18,loudnorm=I=-22:TP=-4:LRA=5,alimiter=limit=0.8[out]')
build('domain_collapse',['portal.mp3'],
    '[0:a]atrim=0:1.5,asetpts=PTS-STARTPTS,areverse,highpass=f=70,lowpass=f=4000,'
    'afade=t=in:d=0.03,afade=t=out:st=0.65:d=0.85,loudnorm=I=-23:TP=-5:LRA=7[out]')
build('shell_break',['blade_impact.mp3','portal.mp3'],
    '[0:a]asetrate=33075,aresample=44100,aecho=0.8:0.7:80|210:0.25|0.16[h];'
    '[1:a]atrim=0:1.3,asetpts=PTS-STARTPTS,areverse,lowpass=f=2400,volume=0.5[p];'
    '[h][p]amix=inputs=2:normalize=0,atrim=0:1.4,afade=t=out:st=0.7:d=0.7,loudnorm=I=-20:TP=-3:LRA=7[out]')
for name,start,lowpass in [('void_ambience',5,3000),('shrine_ambience',13,1300)]:
    raw=subprocess.run([args.ffmpeg,'-v','error','-ss',str(start),'-i',str(SOURCE/'portal.mp3'),'-t','8',
        '-af',f'highpass=f=45,lowpass=f={lowpass},loudnorm=I=-30:TP=-9:LRA=5',
        '-f','f32le','-ac','1','-ar','44100','-'],capture_output=True,check=True).stdout
    data=array.array('f');data.frombytes(raw)
    n=22050
    if len(data)<n*4:raise RuntimeError('Ambience source too short')
    loop=data[n:-n]
    loop.extend(data[-n+i]*(1-i/(n-1))+data[i]*i/(n-1) for i in range(n))
    subprocess.run([args.ffmpeg,'-v','error','-y','-f','f32le','-ar','44100','-ac','1','-i','-',
        '-c:a','libvorbis','-q:a','4',str(OUT/(name+'.ogg'))],input=loop.tobytes(),check=True)
    print(name,(OUT/(name+'.ogg')).stat().st_size,flush=True)

sounds={
    'void_expand':{'sounds':['unlimitedvoid:void_expand'],'subtitle':'subtitles.unlimitedvoid.void_expand'},
    'shrine_expand':{'sounds':['unlimitedvoid:shrine_expand'],'subtitle':'subtitles.unlimitedvoid.shrine_expand'},
    'slash_wind':{'sounds':[f'unlimitedvoid:slash_wind_{i}' for i in range(1,4)],'subtitle':'subtitles.unlimitedvoid.slash_wind'},
    'slash_hit':{'sounds':[f'unlimitedvoid:slash_hit_{i}' for i in range(1,3)],'subtitle':'subtitles.unlimitedvoid.slash_hit'},
    'domain_collapse':{'sounds':['unlimitedvoid:domain_collapse'],'subtitle':'subtitles.unlimitedvoid.domain_collapse'},
    'shell_break':{'sounds':['unlimitedvoid:shell_break'],'subtitle':'subtitles.unlimitedvoid.shell_break'},
    'void_ambience':{'sounds':[{'name':'unlimitedvoid:void_ambience','stream':True}]},
    'shrine_ambience':{'sounds':[{'name':'unlimitedvoid:shrine_ambience','stream':True}]},
}
(OUT.parent/'sounds.json').write_text(json.dumps(sounds,indent=2),encoding='utf-8')
if (ROOT/'requested_voice/gojo.mp3').exists():
    subprocess.run([sys.executable,str(ROOT/'tools/build_requested_voices.py'),'--ffmpeg',args.ffmpeg],check=True)
if (ROOT/'requested_slashes/launched.mp3').exists():
    subprocess.run([sys.executable,str(ROOT/'tools/build_sukuna_slashes.py'),'--ffmpeg',args.ffmpeg],check=True)
