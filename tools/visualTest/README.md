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
