# 第三方素材与许可边界

项目代码及自行实现的渲染、模型几何、玩法逻辑采用根目录 MIT 许可。该许可不涵盖第三方作品、角色、台词和第三方录音。

## 角色台词与相关音效

1.3.0的术式音频曾由`tools/generate_limitless_assets.py`程序合成。1.3.1按用户要求替换为Myinstants用户上传片段，拆成苍、赫、茈各自的语音、蓄力、发射和命中事件。其来源、裁剪和哈希见[新术式音频记录](requested_limitless/README.md)；未确认通用再分发许可，不属于MIT或CC0音频。`limitless_glow.png`和原合成生成器仍为项目MIT资产。

- 五条展开台词：Soundboards.gg 的 Gojo Domain Expansion 公开音频。
- 宿傩展开台词：Myinstants 的 Malevolent Shrine 同名公开候选片段；没有断言与此前参考站点文件完全一致。
- 灶·开台词：用户选中的 Soundboards.gg / Isuk「sukuna fire arrow audio」短片段，截取原文件 5.00–7.75 秒；来源及处理见 `requested_voice/fuga_manifest.json`。
- 灶·开射出与爆燃声：经用户要求，继续使用同一录音的 7.75–8.35 秒与 9.54–15.95 秒，分成两个随游戏阶段触发的音效；许可边界同该录音，处理记录同上。
- 三种刀风和两种命中：来自 Myinstants 的角色标签用户上传片段，并作混音加工。

以上素材未确认通用再分发许可证，不标为 CC0、MIT 或作者原创。原作品及录音的权利归相应权利人；收录、下载或混音不代表获得了新的授权。详细链接、来源及处理记录见 [台词记录](requested_voice/README.md) 与 [斩击记录](requested_slashes/README.md)。

## Freesound 素材

CC0-1.0 基础录音来自 greyfeather、Merrick079、hedonfire。来源、哈希及加工信息见 [third_party_audio/README.md](third_party_audio/README.md)。CC0 声明仅适用于对应录音和不混入角色片段的派生资源。

## 依赖与构建工具

Minecraft 与 Forge 通过开发依赖获取，不将游戏本体或完整 Forge 发行版提交进仓库。Gradle Wrapper 与构建依赖受各自许可约束；Gradle 许可见 https://github.com/gradle/gradle/blob/master/LICENSE ，Forge 许可见 https://github.com/MinecraftForge/MinecraftForge/blob/1.20.x/LICENSE.txt 。

本项目是非官方同人模组。若对素材使用有疑问，请通过仓库 Issues 联系维护者。

## 本地1.7.0起用户提供的Minecraft皮肤（1.8.0共五张）

五张64×64皮肤由用户提供，仅接入本地模组；源文件及哈希记录见docs/supplied-skins.json。作者与公开再分发许可尚未核验，不将这些图片标为项目原创或MIT素材。三张保持原样，虎皮宿傩仅定点修改服装。新增眼罩与手指功能图标为项目程序绘制。该本地版本未公开上传GitHub。

1.8.0新增觉醒五条皮肤由用户提供，源PNG及其哈希同样记录在docs/supplied-skins.json。未取得新的公开再分发许可，当前本地版未上传GitHub。

## 1.10.0魔虚罗新增资源

魔虚罗方块模型、程序绘制UV材质、透明影池及0.24秒轮盘机械短音由本项目制作。生成器为tools/create_mahoraga_assets.py；默认仅需Pillow，可选--audio重建短音需NumPy及soundfile。此说明仅适用于这些新增资源，不改变用户提供皮肤、角色录音或其他第三方素材的权利边界。
