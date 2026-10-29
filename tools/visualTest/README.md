# 显式实机QA（Windows）

普通build只运行无窗口规则检查，不启动游戏。实机QA需要先构建带测试入口的专用包：

```powershell
.\gradlew.bat -PvisualTest=true jar
python tools/run_fuga_qa.py --minecraft-root '你的Minecraft根目录' --version-dir '该根目录下的Forge版本目录' --java 'JDK17的java.exe绝对路径' --mode network
```

需要已安装且可运行的Minecraft1.20.1 Forge启动器配置、完整libraries/assets/natives。使用独立离线身份、全新世界和新的build子目录，不读取原存档。联网仅监听127.0.0.1:25576，请保持该端口空闲；每次使用新的run-id。只关闭本工具创建并持有句柄的子进程。

`--mode plain`运行单客户端战斗/地形/并发检查；`--mode network`追加真实远端客户端与晚到/重复包检查。`--mode shader --compat-dir <测试素材目录>`需要其中放入Oculus、Embeddium的JAR及选定光影ZIP，默认光影名为ComplementaryReimagined_r5.2.2.zip。可用`--shader-pack`修改文件名。第三方测试组件不包含在本仓库中。

原始日志、截图和可丢弃的测试存档保存在新建的build/fuga-combat-*子目录。音频使用无输出OpenAL后端验证事件及声道，不作为人工耳听。测试专用JAR不能用于正式游戏或发布。

`--suite seals --mode plain`运行领域手势检查。`--suite limitless --mode network`运行苍、赫、茈的完整战斗、同步、地形开关和取消检查；`--suite limitless --mode shader`追加真实Iris管线断言。未指定suite时保留原灶·开套件。

`--suite slashes --mode plain|shader`采集御厨子斩痕。`--suite combo --mode plain|network|shader`检查空中自爆茈的两种融合顺序、范围/单次命中、队友与墙体保护、施术者反噬、旁观取消、空中球超时清理及音效去重；network追加真实TCP观察者，shader检查真实Iris管线。联网观察者首次握手超时可在3次尝试上限内重连；原失败运行不作为通过证据。

`--suite particles --mode plain|shader`采集灶·开粒子并检查纹理、粒子上限及取消清理；shader额外断言真实Iris管线。它不替代`--suite combat`战斗回归。

`--suite npcs --mode plain|shader`在独立新世界实际使用两种生成蛋，检查NPC同步、无游走/自动战斗、NBT保存/恢复及纹理，并采集正面/侧面截图；shader额外断言真实Iris管线。NBT恢复不是完整退出重开世界测试。

1.7.0的NPC套件追加真实右键物品变身、消耗/重复/错误物品、创造保留、实体身份保留、形态NBT往返、旧数据兼容及客户端状态同步检查，并采集变身前后截图。

1.7.1的NPC套件追加独立创造栏/原版分类移除、真实Forge分页、五条近战与箭矢防御/减速、宿傩不共享防御、间接魔法/kill绕过、配置关闭和重力标记保留检查；截图creative-tab.png。

`--suite npccombat --mode plain|shader`检查高专仅苍、受控随机种子的觉醒红/茈分支、自然AI满血觉醒/立即放茈、自爆死亡后扩散、存活后回血、贴脸茈、友方保护、玩家/NPC共用施法上限、五种血量、旧40血NBT迁移和反转延迟/恢复/满血封顶。界面套件`npcs`使用NoAI隔离变身/取蛋/保存检查，不能代替战斗套件。

## 1.10.0当前套件

`--suite npcs`覆盖九项创造栏、六种角色皮肤及NoAI隔离变身；`--suite npcdomains`覆盖领域反开/去重与真实魔虚罗召唤；`--suite sukunabalance`覆盖近身延展、领域内行动、外侧破壳与神龛追击。

`--suite mahoraga --mode plain|shader`检查专属巨型模型、轮盘转动、材质与NBT；`--suite mahoragacombat --mode plain|shader`检查逐步适应、无下限实际命中、领域内行动、召唤战斗与主人死亡退场。`--suite mahoragaowned`仅重跑召唤与退场部分。最终1.10.0验证边界见docs/MAHORAGA_VALIDATION.md，不把NBT往返表述为完整退出重开测试。
