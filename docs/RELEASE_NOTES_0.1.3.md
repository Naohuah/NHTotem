# NHTotem 0.1.3

死亡不掉落图腾 · NowHere Studios

带着图腾倒下，消耗 1 个图腾，保留物品栏与经验。

---

## 更新内容

- 新增 CraftEngine 自定义物品支持：`CE:命名空间:物品ID`
- 新增 CraftEngine 重载监听，CE 重载后自动刷新图腾
- 图腾触发动画自动匹配对应颜色材质（临时主手播放，结束还原）
- 修复保留物品栏后仍按 `getDrops()` 生成掉落物导致的刷物品问题
- 新增 7 色 CraftEngine 图腾教程包
- 19 项 MockBukkit 测试通过

---

## 兼容性

- Paper / Purpur 1.20.1 – 26.3
- Spigot 1.20.1+（Bukkit API 理论兼容，未完整真机回归）
- CraftEngine 26.9.2（软兼容，可选）
- ItemsAdder（反射软兼容，可选）
- Java 17 字节码；运行 Java 17+；26.1+ 服务端需要 Java 25+
- Folia 不支持

---

## 安装

1. 停服。
2. 删除旧 NHTotem jar。
3. 把 `NHTotem-0.1.3.jar` 放进 `plugins/`。
4. 启动服务器。
5. 编辑 `plugins/NHTotem/config.yml`。
6. 执行 `/nhtotem reload`。

---

## 资源

- `NHTotem-0.1.3.jar` — 插件本体
- `NHTotem-CraftEngine-Tutorial-v1.1.zip` — 7 色图腾教程包

### SHA-256

```text
NHTotem-0.1.3.jar
E0CC7004D58BEF350206EF8835924A8D82493F26F1AE90764FA4E462A68A022D

NHTotem-CraftEngine-Tutorial-v1.1.zip
9A465D8BAFA8FF2BAB6ABDA4F84377CB7F1C454E635238955887B88AB7E724FD
```

---

## 文档

- 安装与使用：`docs/USAGE.md`
- 配置说明：`docs/CONFIGURATION.md`
- CraftEngine 对接：`docs/CRAFTENGINE.md`
- 常见问题：`docs/FAQ.md`
- 教程包：`docs/craftengine-tutorial/nhtotem_tutorial/README.md`

---

**NowHere Studios** · *Still here.*
