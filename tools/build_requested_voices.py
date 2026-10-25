from pathlib import Path
import argparse,subprocess,json,array,math,hashlib
p=argparse.ArgumentParser();p.add_argument('--ffmpeg',default='ffmpeg');args=p.parse_args()
root=Path(__file__).resolve().parents[1];sources=root/'requested_voice';info=[]
for name,target in [('gojo','void_expand'),('sukuna','shrine_expand')]:
    source=sources/(name+'.mp3');out=root/'src/main/resources/assets/unlimitedvoid/sounds'/(target+'.ogg')
    subprocess.run([args.ffmpeg,'-v','error','-y','-i',str(source),'-af',
        'aformat=channel_layouts=mono,highpass=f=70,loudnorm=I=-16:TP=-3:LRA=7,alimiter=limit=0.85:level=0,afade=t=in:d=0.01',
        '-ac','1','-ar','44100','-c:a','libvorbis','-q:a','5',str(out)],check=True)
    data=subprocess.run([args.ffmpeg,'-v','error','-i',str(out),'-f','f32le','-ac','1','-ar','22050','-'],capture_output=True,check=True).stdout
    samples=array.array('f');samples.frombytes(data)
    peak=max(abs(x) for x in samples)
    if peak>=1:raise RuntimeError('Voice peak clips: '+name)
    info.append({'name':name,'event':target,'seconds':len(samples)/22050,'peak':peak,
        'rms':math.sqrt(sum(x*x for x in samples)/len(samples)),
        'source_bytes':source.stat().st_size,'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest()})
(sources/'manifest.json').write_text(json.dumps(info,indent=2),encoding='utf-8')
print(json.dumps(info,indent=2))
