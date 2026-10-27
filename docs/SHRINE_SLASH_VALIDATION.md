# 1.4.1 解与捌：参考斩痕

用户确认的斩痕预览合入1.4.1；独立于灶·开粒子试作，后者仍保留在本地`preview/fuga-particles`分支。

![1.4.1斩击画面](images/shrine-slashes-1.4.1.png)

将御厨子内的旧细条改为两端收尖的斩痕：白色亮刃、红色边光与黑色裂口混合出现。解的空间斩痕用约7tick一轮的短暂闪现、掠动和淡出；捌在生物附近使用约6tick一轮的三道交叉斩痕。这里只调整视觉，服务端伤害、咒力判定、范围与声音不变。

斩痕宽度按相机与刃向计算，避免侧看消失；保留地形深度测试，透明刃光不写入深度。每领域最多36道空间斩痕和48个生物各3道近身斩痕。这个数量上限不代表完成多人并发或GPU性能验证。

2026-10-27使用Minecraft1.20.1 / Forge47.4.20 / Java17独立新世界检查普通画面与Oculus1.8.0、Embeddium0.3.31、Complementary Reimagined5.2.2组合。靶子设置高咒力，避免原有必中伤害立即杀死后无法观察近身斩痕。测试入口以`SHRINE_SLASH_PREVIEW_COMPLETE`结束，光影运行另外检查真实IrisRenderingPipeline。没有打开原存档。

普通画面与用户确认的预览一致，斩痕峰值46道；指定光影下斩痕与神龛可见，峰值47道，但原有血红天空与镜面地面覆盖未正常显示。该组合不能算完整领域画面兼容通过，本版没有扩展修改领域背景绘制管线。证据目录分别为`build/fuga-combat-plain-shrine-slashes-preview-b/host`与`build/fuga-combat-shader-shrine-slashes-141-release/host`；上述峰值不代表180道上限压力验证。

正式包通过161项既有无窗口规则检查，86个生产类逐字节匹配两份实机包（普通预览与1.4.1光影包）。与1.4.0正式包相比，仅ShrineRenderer.class改变，41项游戏资产保持一致，正式包不含测试入口或FugaParticles试作类。

本轮修改客户端斩痕，未改服务端判定与音效。视觉采集不替代完整战斗回归，本轮未重复多人联网、并发域或全硬件压力测试；规则检查与旧版记录分开列出。

复现：构建`-PvisualTest=true jar`后，调用`tools/run_fuga_qa.py --suite slashes --mode plain`并提供既有Minecraft与Java路径。正式build不含实机测试入口。
