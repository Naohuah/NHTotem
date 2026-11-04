# NHTotem 宣传图（1200 × 750）

五张 1200×750 宣传图，全部基于 `docs/brand/totem/nhtotem-logo-1024.png`（你提供的 Totem of Undying 改图）制作。

| 文件 | 用途 |
| --- | --- |
| `01-hero.png` | 主视觉：品牌、插件名、核心卖点，适合论坛头图 / 服务器宣传 |
| `02-features.png` | 功能总览：保留物品栏、经验、随机掉落、清诅咒、世界限制、旧配置迁移 |
| `03-compare.png` | 对比图：没有图腾 vs 带着图腾，一眼看懂价值 |
| `04-quickstart.png` | 升级/命令：三步安装，控制台示例 |
| `05-brand.png` | 品牌海报：大图腾 + STILL HERE.，适合公告/Teaser |

## 设计说明

- 尺寸：1200×750（16:10），PNG，2 倍渲染后 Lanczos 缩到目标尺寸
- 底色：`#070D18` → `#0C1D2A` 深海军蓝渐变 + 低透明度像素网格
- 强调色：`#22D3EE`（青）、`#41E67C`（绿）、`#F2C94C`（金）、`#F87171`（红）
- 中文：微软雅黑 Bold；英文：Arial Bold；终端：Consolas Bold
- 所有文案基于 0.1.2 实际功能；随机掉落、清诅咒、经验保留均可配置，对比图中用 `*` 标注

## 重新生成

```bash
python make_promo.py
```

需要 Pillow 和 numpy。修改 `make_promo.py` 里的文案、颜色或布局即可重新出图，不会覆盖原图腾素材。
