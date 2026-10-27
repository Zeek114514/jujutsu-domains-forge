"""Explicit opt-in Windows GUI QA; isolated directories and retained child-process handles only."""
from pathlib import Path
import json,subprocess,uuid,shutil,sys,re,time,argparse,os
p=argparse.ArgumentParser(description='Windows disposable Forge client/network/shader QA; never opens your saves')
p.add_argument('--minecraft-root',required=True)
p.add_argument('--version-dir',help='Forge launcher profile directory; defaults to versions/1.20.1-Forge')
p.add_argument('--java',default=shutil.which('java') or str(Path(os.environ.get('JAVA_HOME',''))/'bin/java.exe'))
p.add_argument('--mode',choices=['network','shader','plain'],default='network')
p.add_argument('--run-id',default=str(int(time.time())))
p.add_argument('--compat-dir',help='Directory containing only Oculus and Embeddium test jars and the chosen shader zip')
p.add_argument('--shader-pack',default='ComplementaryReimagined_r5.2.2.zip')
p.add_argument('--test-jar',help='Explicit visual-test jar; otherwise use newest build/libs/*-visual-test.jar')
p.add_argument('--suite',choices=['combat','seals','limitless'],default='combat')
args=p.parse_args()
if args.suite=='seals' and args.mode=='network':p.error('Seal suite uses one isolated client; choose plain or shader')
if os.name!='nt':p.error('This launcher harness uses a Windows Forge profile. Gradle headless checks remain cross-platform.')
assert re.fullmatch('[a-zA-Z0-9-]+',args.run_id)
project=Path(__file__).resolve().parents[1]
base=Path(args.minecraft_root).resolve();version=Path(args.version_dir).resolve() if args.version_dir else base/'versions/1.20.1-Forge';libs=base/'libraries'
d=json.loads((version/(version.name+'.json')).read_text('utf-8-sig'))
root=project/'build'/('fuga-combat-'+args.mode+'-'+args.run_id);root.mkdir(parents=True,exist_ok=True)
def permitted(rules):
 if not rules:return True
 allow=False
 for rule in rules:
  os=rule.get('os',{})
  if os.get('name','windows') not in ['windows','universal'] or os.get('arch','amd64') not in ['amd64','x86_64'] or rule.get('features'):continue
  allow=rule['action']=='allow'
 return allow
cp=[]
for lib in d['libraries']:
 if not permitted(lib.get('rules')):continue
 art=lib.get('downloads',{}).get('artifact',{})
 if art.get('path'):path=libs/art['path']
 else:
  group,artifact,v,*classifier=lib['name'].split(':');path=libs/group.replace('.','/')/artifact/v/(artifact+'-'+v+('-'+classifier[0] if classifier else '')+'.jar')
 assert path.exists(),path
 cp.append(str(path))
cp.append(str(version/'1.20.1-Forge.jar'))
candidates=list((project/'build/libs').glob('*-visual-test.jar'))
assert args.test_jar or candidates,'Build -PvisualTest=true jar first'
jar=Path(args.test_jar).resolve() if args.test_jar else max(candidates,key=lambda f:f.stat().st_mtime)
assert jar.exists(),jar
def launch(role):
 game=root/role;assert not game.exists(), 'Use a fresh QA folder to preserve evidence';(game/'mods').mkdir(parents=True)
 shutil.copy2(jar,game/'mods'/jar.name)
 if args.suite=='seals':
  (game/'config').mkdir(exist_ok=True)
  (game/'config/unlimitedvoid-common.toml').write_text('fugaDamageRadius=16.0\nfugaBaseDamage=32.0\nfugaDustDamage=80.0\nshrineTerrainDamage=false\n','utf-8')
 if args.mode=='shader':
  downloads=Path(args.compat_dir).resolve() if args.compat_dir else project/'build/compat-downloads'
  for f in downloads.glob('*.jar'):shutil.copy2(f,game/'mods'/f.name)
  (game/'shaderpacks').mkdir();shader=downloads/args.shader_pack;shutil.copy2(shader,game/'shaderpacks'/shader.name)
  (game/'config').mkdir(exist_ok=True);(game/'config/oculus.properties').write_text('enableShaders=true\nshaderPack='+shader.name+'\n','utf-8')
  (game/'shaderpacks'/(shader.name+'.txt')).write_text('SHADOW_QUALITY=0.5\nSHADOW_DISTANCE=64.0\n','utf-8')
 (game/'options.txt').write_text('lang:zh_cn\nonboardAccessibility:false\nrenderDistance:8\nsimulationDistance:5\nmaxFps:120\npauseOnLostFocus:false\nenableVsync:false\nentityShadows:false\n','utf-8')
 values={'natives_directory':str(version/'natives-windows-x86_64'),'launcher_name':'Jujutsu-isolated-QA','launcher_version':'1','classpath':';'.join(cp),'classpath_separator':';','library_directory':str(libs),'version_name':d['id'],'primary_jar_name':'1.20.1-Forge.jar','auth_player_name':'FugaObserver' if role=='observer' else 'FugaHost','game_directory':str(game),'assets_root':str(base/'assets'),'assets_index_name':str(d['assetIndex']['id']),'auth_uuid':uuid.uuid3(uuid.NAMESPACE_DNS,'OfflinePlayer:'+('FugaObserver' if role=='observer' else 'FugaHost')).hex,'auth_access_token':'0','clientid':'0','auth_xuid':'0','user_type':'mojang','version_type':'release'}
 def expand(s):return re.sub(r'\$\{([^}]+)\}',lambda m:values[m.group(1)],s)
 def arguments(items):
  out=[]
  for arg in items:
   if isinstance(arg,dict):
    if not permitted(arg.get('rules')):continue
    v=arg['value'];out.extend(expand(x) for x in (v if isinstance(v,list) else [v]))
   else:out.append(expand(arg))
  return out
 java=Path(args.java);assert java.exists(),'Pass --java pointing to a JDK17 java.exe'
 command=[str(java),'-Xmx1800M','-Duv.'+('limitlessTest' if args.suite=='limitless' else 'sealTest' if args.suite=='seals' else 'fugaCombatTest')+'=true','-Duv.fugaNetwork='+str(args.mode=='network' and role=='host').lower(),'-Duv.fugaObserver='+str(role=='observer').lower(),'-Duv.shaderTest='+str(args.mode=='shader').lower(),'-Duv.readyFile='+str(root/'ready')]+arguments(d['arguments']['jvm'])+[d['mainClass']]+arguments(d['arguments']['game'])+['--width','960','--height','540']
 log=(game/'console.log').open('w',encoding='utf-8')
 environment=os.environ.copy();environment['ALSOFT_DRIVERS']='null'
 proc=subprocess.Popen(command,cwd=game,stdout=log,stderr=subprocess.STDOUT,creationflags=0x08000000,env=environment)
 return proc,log
host,log=launch('host');observer=None
try:
 if args.mode=='network':
  deadline=time.monotonic()+180
  while not(root/'ready').exists():
   if host.poll() is not None:raise RuntimeError('Host exited before ready')
   if time.monotonic()>deadline:raise TimeoutError('Host startup timeout')
   time.sleep(.5)
  observer,observerlog=launch('observer');print('HOST_AND_OBSERVER_STARTED',flush=True)
 host.wait(timeout=240);log.close()
 if observer:observer.wait(timeout=30);observerlog.close()
 text=(root/'host/console.log').read_text('utf-8',errors='replace')
 assert ('LIMITLESS_TEST_COMPLETE' if args.suite=='limitless' else 'DOMAIN_SEAL_TEST_COMPLETE' if args.suite=='seals' else 'FUGA_COMBAT_TEST_COMPLETE') in text and 'FUGA_COMBAT_FAIL' not in text and 'DOMAIN_SEAL_FAIL' not in text and 'LIMITLESS_FAIL' not in text, 'Host test failed; inspect console.log'
 if observer:
  text=(root/'observer/console.log').read_text('utf-8',errors='replace');assert ('LIMITLESS_OBSERVER_COMPLETE' if args.suite=='limitless' else 'FUGA_NETWORK_OBSERVER_COMPLETE') in text and 'FUGA_COMBAT_FAIL' not in text and 'LIMITLESS_FAIL' not in text
 print('FUGA_QA_PASS '+args.mode,flush=True)
finally:
 # Only stop processes launched and retained by this isolated QA helper.
 for proc in [observer,host]:
  if proc is not None and proc.poll() is None:proc.terminate();proc.wait(timeout=20)
