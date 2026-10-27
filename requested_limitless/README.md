# 用户要求替换的苍、赫、茈语音与效果声

检索与下载：2026-10-27。用户要求从网上查找并替换1.3.0的合成声音，包括出招语音和招式音效。

| 源文件 | 页面 | 用途 |
| --- | --- | --- |
| gojo-lapse-blue.mp3 | [Gojo: Lapse Blue](https://www.myinstants.com/en/instant/gojo-lapse-blue-19384/) | 苍的短呼号和汇聚、发射、收束声 |
| gojo-reversal-red.mp3 | [Gojo: Reversal Red](https://www.myinstants.com/en/instant/gojo-reversal-red-90179/) | 赫的短呼号和蓄力、发射、爆发声 |
| hollow-purple.mp3 | [Hollow Purple](https://www.myinstants.com/en/instant/hollow-purple-6717/) | 茈的日语招式短句 |

选择日语短呼号而非长篇台词或整段配乐，以保留原有施法速度。使用本地whisper.cpp base/small的日语识别与音节时间点定位Ao、Aka、Kyoshiki Murasaki；识别文本存在音近字，并非人工耳听校验或声源真伪认证。

每种术式拆成voice、charge、cast、impact四个事件。茈按用户明确选择，语音和所有效果声均来自`hollow-purple-6717`同一片段。茈语音在开始蓄力后约1.3秒触发，接近融合与发射前结束。进入飞行阶段仅停止蓄力声，不中断正在播放的语音；正常消散后允许短暂音频余响，取消、旁观和退出则清理声音。每个阶段最多播放一次，晚到状态不补播过期语音。

源文件SHA256、裁剪窗口、滤镜、时长与输出SHA256见[manifest.json](manifest.json)。复现命令：`python tools/import_limitless_audio.py --ffmpeg <ffmpeg路径>`。

这些是公开网站的用户上传角色片段，未确认通用再分发许可，不标为作者原创、MIT或CC0。角色及录音权利属于对应权利人。此前的合成生成器保留用于开发，音频输出已移到忽略的build目录，避免覆盖导入素材。
