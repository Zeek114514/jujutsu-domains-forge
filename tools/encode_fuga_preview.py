"""Encode isolated-client screenshots; audio is a timed mix of the mod's existing cues."""
import argparse
from pathlib import Path
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument('--ffmpeg', required=True)
parser.add_argument('--frames', type=Path, required=True)
parser.add_argument('--output', type=Path, required=True)
args = parser.parse_args()
sounds = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/unlimitedvoid/sounds'
frames = sorted(args.frames.glob('frame-*.png'))
assert len(frames) == 216, f'Incomplete capture: {len(frames)} / 216 frames'
assert all(p.name == f'frame-{i:04d}.png' for i, p in enumerate(frames)), 'Capture sequence has gaps'
args.output.parent.mkdir(parents=True, exist_ok=True)
command = [args.ffmpeg, '-y', '-hide_banner', '-framerate', '20', '-i', str(args.frames/'frame-%04d.png')]
for name in ('fuga_open', 'fuga_launch', 'fuga_impact'):
    command += ['-i', str(sounds/(name+'.ogg'))]
command += ['-filter_complex',
            '[1:a]volume=0.9,apad[v];[2:a]volume=0.65,adelay=2700:all=1[l];'
            '[3:a]volume=0.9,adelay=3200:all=1[b];'
            '[v][l][b]amix=inputs=3:duration=longest:normalize=0,alimiter=limit=0.95[a]',
            '-map', '0:v', '-map', '[a]', '-t', '10.8', '-c:v', 'libx264', '-crf', '18',
            '-preset', 'medium', '-pix_fmt', 'yuv420p', '-c:a', 'aac', '-b:a', '192k',
            '-movflags', '+faststart', '-metadata',
            'comment=Actual Forge gameplay frames; existing mod voice/launch/impact cues mixed to cast timeline; no shaders.',
            str(args.output)]
subprocess.run(command, check=True)
subprocess.run([args.ffmpeg, '-v', 'error', '-i', str(args.output), '-f', 'null', '-'], check=True)
print(f'VERIFIED_VIDEO {args.output.resolve()} frames=216 seconds=10.8')
