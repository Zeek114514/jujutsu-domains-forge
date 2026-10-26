# 咒术领域 · Jujutsu Domains

> 当前源码：**1.1.0-preview.6**，新增灶·开的大范围火矢演出与分阶段音效。已发布的正式版仍为 **v1.0.0**；灶·开目前通过源码构建或 Actions 构建产物体验。

[![Build](https://github.com/Zeek114514/jujutsu-domains-forge/actions/workflows/build.yml/badge.svg)](https://github.com/Zeek114514/jujutsu-domains-forge/actions/workflows/build.yml)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-62b47a)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.4.10%2B-e5a34a)](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html)
[![Release](https://img.shields.io/github/v/release/Zeek114514/jujutsu-domains-forge)](https://github.com/Zeek114514/jujutsu-domains-forge/releases/latest)

**Zeek114514 的个人 Minecraft Java 模组作品。** 基于 Forge 1.20.1，实现《咒术回战》风格的无量空处、伏魔御厨子、领域对峙，以及灶·开火矢演出。

无量空处以纯白空间与宇宙结界封锁战场，御厨子以升起的神龛、镜面地面和开放式斩击覆盖周围。新增灶·开从聚火、拉弓到爆燃展开，提供独立的台词、射出声与命中余响。灶·开当前是**纯特效预览**，不造成伤害或地形破坏。

[灶·开指南](docs/FUGA_PREVIEW.md) · [开发进度](docs/PROGRESS.md) · [预览版验证记录](docs/FUGA_VALIDATION.md) · [Wiki](https://github.com/Zeek114514/jujutsu-domains-forge/wiki)

## 下载和安装

1. 安装 **Minecraft Java 1.20.1、Forge 47.4.10 或更高的 47.x、Java 17**。
2. 前往 [Releases](https://github.com/Zeek114514/jujutsu-domains-forge/releases/latest)，下载 `unlimited-void-1.20.1-1.0.0.jar`。
3. 放进当前游戏实例的 `mods` 文件夹。升级时移走旧版同名模组，避免重复加载，随后重启游戏。
4. 多人游戏中客户端与服务端都要安装相同版本。

体验 **灶·开预览版**：构建当前源码，或从 [Actions](https://github.com/Zeek114514/jujutsu-domains-forge/actions/workflows/build.yml) 成功运行的 `jujutsu-domains-forge` 构建产物中获取 `unlimited-void-1.20.1-1.1.0-preview.6.jar`，替换实例中的旧版同名模组后重启。v1.0.0 正式版不包含灶·开；不要混用不同版本的客户端与服务端。

开发依赖为 Forge 47.4.10；开发迭代曾在 47.4.20 客户端运行。正式发布整理不新增游戏内测试，不表示所有 47.x 或光影组合均已验证。

## 玩法

| 按键 | 功能 |
| --- | --- |
| `U` | 展开当前领域，再按一次主动解除 |
| `J` | 切换无量空处 / 伏魔御厨子 |
| `K` | 重置术式熔断，方便体验和演示 |
| `H` | 灶·开：聚火、满蓄力、吟唱后射出火矢（预览版） |
| `O` | 重置灶·开的独立冷却，不打断当前施放（预览版） |

按键可在游戏设置中修改。两种领域共用 40 秒熔断；完全成型后持续约 20 秒，解除时有约 2 秒过渡。

### 灶·开 · Fūga

- 约 1.5 秒达到最大蓄力，在「Fuga」台词期间保持，约 2.7 秒射出；独立冷却 10 秒。
- 三倍火弓与火矢、半径约 18 格的双环聚火圈，以及约 12 格高的三股旋涡。
- 火矢射程最多 64 格；命中演出包括**直径约 400 格火海、约 240 格高火柱与最远约 300 格的三重冲击波**，爆燃约 6.6 秒。
- 台词、射出破空声、命中爆燃声分别触发。音效定位在对应发射点和命中点，远近落点按实际飞行时间衔接。
- 左上角常驻显示按键与施放/冷却状态；取消施放、退出或切换世界时清理相关演出和音频。

![灶·开满蓄力：火弓与聚火旋涡](docs/images/fuga-charge-preview.png)

![灶·开大范围爆燃远景](docs/images/fuga-firestorm-preview.png)

*Forge 47.4.20 独立测试世界中的预览版实机画面。实际可见范围受视距和地形遮挡影响；尚未接入灶·开伤害、御厨子粉尘引爆和领域强化。*

### 无量空处

- 半径 16 格的封闭球形结界，外部显示黑球。
- 纯白空间从后向前推进，再逐渐转为星空、星云与流动光环。
- 完整结界持续期间限制出入；被捕获的目标定身并持续受伤。施术者、队友和受保护宠物按服务端规则处理。
- 保留迭代确认的光环造型，位置在该版基础上上移 2.4 格。

![开发过程中的无量空处实机画面](docs/images/unlimited-void-development.png)

*开发过程实机截图。展示领域氛围，正式版的位置以游戏内效果为准。*

### 伏魔御厨子

- 开放式领域，默认最大半径 200 格，允许目标自由进出。
- 咏唱中段神龛开始升起，台词结束时完成展开，天空转为血红并开始斩击。
- 神龛是带碰撞的临时实体，可站立和跳跃；结束后下沉移除，不在世界中放置永久建筑。
- 空中施术时，神龛从人物正下方最近的实体地面或屋顶升起。半砖和台阶按实际碰撞面定位；没有支撑面时取消展开。
- 屋脊达到人物脚底后托举；高于神龛最终高度的空中人物不会被突然拉下。
- 青黑色镜面地面带流动雾纹，可反射神龛及附近生物。
- 包含「解」「捌」、可配置咒力与外部结界破坏规则。地形破坏默认关闭。

### 对峙和演示指令

以下指令需要管理员权限或启用作弊：

```mcfunction
/domain clash
/domain spawn void
/domain spawn shrine
/domain clear
```

`/domain clash` 在两侧生成两个领域，方便观察一半宇宙、一半血色天空。成型后的重叠区采用同等精度的必中抵消规则；御厨子位于无量空处外部时可以持续攻击其结界。生成的演示领域用 `/domain clear` 解除。

```mcfunction
/domain energy @e[type=minecraft:zombie,distance=..20] 100
/domain terrain true
/domain terrain false
```

`/domain terrain true` 会启用真实、永久的地形破坏。默认关闭；仅在需要时手动开启。

## 配置

首次运行后生成 `config/unlimitedvoid-common.toml`。常用配置：

```toml
shrineRadius = 200
casterEnergy = 100.0
livingEntityEnergy = 100.0
shrineTerrainDamage = false
blocksDestroyedPerTick = 96
```

完整规则与兼容性限制见 [游戏说明](docs/GAMEPLAY.md)。

## 从源码构建

需要 **64 位 JDK 17**，项目已包含 Gradle Wrapper。首次构建需要联网下载依赖。

```powershell
# Windows PowerShell
.\gradlew.bat --no-daemon build
```

```sh
# Linux / macOS
./gradlew --no-daemon build
```

当前安装包在 `build/libs/unlimited-void-1.20.1-1.1.0-preview.6.jar`。开发客户端使用 `./gradlew runClient`。自动构建运行 49 项灶·开时序/边界检查及 15 项寻地几何检查，不启动游戏。

仓库已经包含运行所需的音频资源，正常构建无需重新加工音频。可选的素材处理脚本位于 `tools/`，依赖 Python 3 与 FFmpeg，来源见 [第三方素材说明](THIRD_PARTY_NOTICES.md)。

## 验证与限制

v1.1.0-preview.6 已通过本地构建、64 项无窗口检查，以及独立 Forge 47.4.20 世界中的按键映射、第一/第三人称画面、分阶段音频触发和取消测试。近落点的射出声/命中声相隔 2 tick，较远落点相隔 5 tick，均只触发一次。音频分段按识别时间轴与波形编排，尚未人工试听逐帧校准。详见 [预览版验证](docs/FUGA_VALIDATION.md)。

正式版 v1.0.0 的历史构建与基线比对记录保留在 [发布验证](docs/VALIDATION.md)。CI 通过不代表多人、光影或所有场景均已验收。

本项目没有独立领域维度。镜面只反射附近实体，不反射整片方块地形；镜面颜色覆盖不会抹平实际地形碰撞。神龛完整体积在复杂地形中的避障、低矮洞穴、其他光影模组、多人对抗与大量实体性能仍有兼容性限制。详见游戏说明。

## 作者与许可

项目作者 / 维护者：[Zeek114514](https://github.com/Zeek114514)。

项目代码以 [MIT](LICENSE) 开源。第三方角色台词、角色相关音效、Freesound 素材及其来源单独列于 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)；它们不因为收录在本仓库而自动获得 MIT 许可，也不声明为作者原创。

本项目为非官方同人作品，与 Minecraft、Mojang、Microsoft 或《咒术回战》权利方不存在官方关联。
