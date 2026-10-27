# 灶·开粒子安装补丁 1.5.1

从 `preview/fuga-particles` 中提取客户端粒子代码、两张程序生成纹理和隔离采集入口，合入1.5.0基线。默认启用真实ParticleEngine粒子，替换旧网格火焰表现。仅修改FugaClient的视觉和粒子生命周期，不更改服务端伤害、音效、领域、苍赫或茈。

纹理源程序：`tools/generate_fuga_particle_sprites.py`。每客户端最多4500个存活粒子/每tick新建300个；根据粒子选项与并发施法数量减少新建数量。全硬件性能、长期运行和所有光影尚未验证。

2026-10-27验证：常规build成功，250项规则检查通过。独立新世界普通客户端灶·开战斗套件通过；指定Oculus1.8.0、Embeddium0.3.31、Complementary Reimagined5.2.2实机粒子采集完成，目视检查爆燃截图，粒子纹理存在、真实Iris管线活动、峰值4500且取消后存活数0。测试没有进入用户原存档。

记录：`build/fuga-combat-shader-fuga-particles-151-final/host/console.log`（`FUGA_PARTICLE_PREVIEW_COMPLETE`和`FUGA_PARTICLE_CANCEL_OK`），`build/fuga-combat-plain-fuga-combat-151-final/host/console.log`（`FUGA_COMBAT_TEST_COMPLETE`）。这是本轮单客户端验证，没有重跑多人并发或其他领域背景光影兼容性。

正常安装包的全部生产类与assets逐项匹配上述两份实际测试包，且不含VisualTest类。与1.5.0相比，既有生产类只改变FugaClient及其嵌套类，既有资源逐项相同，新增粒子类和两张自制纹理。
