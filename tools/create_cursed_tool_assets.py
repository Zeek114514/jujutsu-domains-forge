"""Generate original OBJ weapons, dedicated materials, localization and damage tags."""
from pathlib import Path
import json
from create_cursed_tool_meshes import generate
ROOT=Path(__file__).resolve().parents[1]/"src/main/resources"
def write(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
generate()
translations={
 'zh_cn':{
 'item.unlimitedvoid.inverted_spear_of_heaven':'天逆鉾','item.unlimitedvoid.split_soul_katana':'释魂刀','item.unlimitedvoid.playful_cloud':'游云',
 'tooltip.unlimitedvoid.special_grade':'特级咒具',
 'effect.unlimitedvoid.soul_sight':'灵魂感知',
 'tooltip.unlimitedvoid.cloud_form.0':'形态：三节棍',
 'tooltip.unlimitedvoid.cloud_form.1':'形态：整棍',
 'tooltip.unlimitedvoid.cloud_form.2':'形态：削尖双刀',
 'tooltip.unlimitedvoid.cloud_controls':'右键切换三节/整棍；潜行右键削尖/收回双刀',
 'message.unlimitedvoid.cloud_offhand':'腾空副手后才能使用双刀',
 'tooltip.unlimitedvoid.cursed_tool.spear':'接触强制解除目标正在发动的术式；压制2秒',
 'tooltip.unlimitedvoid.cursed_tool.soul':'潜行右键开启灵魂感知30秒；感知时斩击无视护甲与抗性',
 'tooltip.unlimitedvoid.cursed_tool.cloud':'纯物理咒具；威力随攻击蓄力、力量属性提升',
 'death.attack.unlimitedvoid.soul_cut':'%1$s的灵魂被%2$s斩断了',
 'death.attack.unlimitedvoid.soul_cut.item':'%1$s的灵魂被%2$s用%3$s斩断了'},
 'en_us':{
 'item.unlimitedvoid.inverted_spear_of_heaven':'Inverted Spear of Heaven','item.unlimitedvoid.split_soul_katana':'Split Soul Katana','item.unlimitedvoid.playful_cloud':'Playful Cloud',
 'tooltip.unlimitedvoid.special_grade':'Special Grade Cursed Tool',
 'effect.unlimitedvoid.soul_sight':'Soul Sight',
 'tooltip.unlimitedvoid.cloud_form.0':'Form: three-section staff',
 'tooltip.unlimitedvoid.cloud_form.1':'Form: long staff',
 'tooltip.unlimitedvoid.cloud_form.2':'Form: sharpened dual blades',
 'tooltip.unlimitedvoid.cloud_controls':'Right-click: staff forms; sneak-right-click: sharpen/retract blades',
 'message.unlimitedvoid.cloud_offhand':'Empty your off hand before using dual blades',
 'tooltip.unlimitedvoid.cursed_tool.spear':'Contact cancels active techniques and suppresses recasting for 2s',
 'tooltip.unlimitedvoid.cursed_tool.soul':'Sneak-right-click: Soul Sight for 30s; perceived souls bypass armor and resistance',
 'tooltip.unlimitedvoid.cursed_tool.cloud':'Pure physical force; power scales with attack charge and Strength',
 'death.attack.unlimitedvoid.soul_cut':"%1$s's soul was cut by %2$s",
 'death.attack.unlimitedvoid.soul_cut.item':"%1$s's soul was cut by %2$s using %3$s"}}
for lang,entries in translations.items():
    path=ROOT/'assets/unlimitedvoid/lang'/f'{lang}.json';data=json.loads(path.read_text(encoding='utf-8-sig'));data.update(entries);write(path,data)
for tag in ('bypasses_armor','bypasses_resistance','bypasses_enchantments'):
    path=ROOT/'data/minecraft/tags/damage_type'/f'{tag}.json';data=json.loads(path.read_text(encoding='utf-8-sig'))
    if 'unlimitedvoid:soul_cut' not in data['values']:data['values'].append('unlimitedvoid:soul_cut')
    write(path,data)
from PIL import Image,ImageDraw
icon=Image.new('RGBA',(18,18));d=ImageDraw.Draw(icon)
d.polygon([(1,9),(5,4),(12,4),(16,9),(12,14),(5,14)],outline=(155,232,232,255))
d.ellipse((6,5,11,13),fill=(182,246,246,255));d.ellipse((8,7,9,11),fill=(20,50,64,255))
path=ROOT/'assets/unlimitedvoid/textures/mob_effect/soul_sight.png';path.parent.mkdir(parents=True,exist_ok=True);icon.save(path)
print('Generated OBJ weapons, custom materials, soul perception icon and translations')
