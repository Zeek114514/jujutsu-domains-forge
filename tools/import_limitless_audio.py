"""Convert the documented user-requested third-party clips; never fabricates dialogue.
python tools/import_limitless_audio.py --ffmpeg /path/to/ffmpeg
"""
from pathlib import Path
import argparse,hashlib,json,subprocess
p=argparse.ArgumentParser();p.add_argument('--ffmpeg',required=True);args=p.parse_args()
root=Path(__file__).resolve().parents[1];source=root/'requested_limitless';dest=root/'src/main/resources/assets/unlimitedvoid/sounds'
sources={
 'blue':('gojo-lapse-blue','https://www.myinstants.com/en/instant/gojo-lapse-blue-19384/'),
 'red':('gojo-reversal-red','https://www.myinstants.com/en/instant/gojo-reversal-red-90179/'),
 'purple':('hollow-purple','https://www.myinstants.com/en/instant/hollow-purple-6717/')}
clips=[('blue_voice','blue',9.55,11.0),('red_voice','red',2.0,3.1),('purple_voice','purple',0,2.15),
 ('blue_charge','blue',4.2,5.8),('red_charge','red',3.6,4.6),('purple_charge','purple',2.15,4.0),
 ('blue_cast','blue',11.0,12.28),('red_cast','red',3.4,5.3),('purple_cast','purple',4.0,7.0),
 ('blue_impact','blue',11.55,12.28),('red_impact','red',5.3,8.85),('purple_impact','purple',7.0,11.35)]
manifest={'sources':{},'clips':[],'manual_listening':False,'alignment':'Local whisper.cpp base/small Japanese ASR for short Ao, Aka and Kyoshiki Murasaki callouts; approximate phonetic alignment, not human listening.','license':'Third-party character recordings requested by user. No general redistribution licence established; NOT MIT or CC0.'}
for key,(name,page) in sources.items():
 data=(source/(name+'.mp3')).read_bytes();manifest['sources'][key]={'file':name+'.mp3','page':page,'download':'https://www.myinstants.com/media/sounds/'+name+'.mp3','sha256':hashlib.sha256(data).hexdigest()}
for name,key,start,end in clips:
 duration=end-start;voice=name.endswith('_voice');level=-18 if voice or name.endswith('_impact') else -23
 filters=f'atrim=start={start}:end={end},asetpts=PTS-STARTPTS,highpass=f=75,loudnorm=I={level}:TP=-3:LRA=7,alimiter=limit=0.88:level=false,afade=t=in:d=0.018,afade=t=out:st={duration-.06:.4f}:d=0.06'
 if name=='red_voice':filters+=',volume=0.8'
 output=dest/(name+'.ogg')
 subprocess.run([args.ffmpeg,'-y','-loglevel','error','-i',str(source/(sources[key][0]+'.mp3')),'-af',filters,'-ar','44100','-ac','1','-c:a','libvorbis','-q:a','5',str(output)],check=True)
 manifest['clips'].append({'event':name,'source':key,'trim':[start,end],'duration':round(duration,3),'filter':filters,'sha256':hashlib.sha256(output.read_bytes()).hexdigest()})
(source/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n','utf-8')
print('IMPORTED',len(clips),'cues')
