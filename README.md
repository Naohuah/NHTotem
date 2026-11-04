# NHTotem

<p align="center">
  <img src="docs/brand/totem/nhtotem-logo-green-256.png" width="180" alt="NHTotem">
</p>

<p align="center">
  <b>死亡不是终点。</b><br>
  带着图腾倒下，消耗 1 个图腾，保留物品栏与经验。<br>
  Paper / Purpur · 1.20.1 – 26.3
</p>

NHTotem 是 **NowHere Studios** 维护的死亡掉落保护插件。它把“死亡不掉落”做成可配置的图腾系统：图腾放在主背包或副手即可，死亡时自动消耗 1 个，保留物品栏与经验。

同时处理了 Paper/Purpur 在死亡事件后仍按 `getDrops()` 生成掉落物的问题，避免“物品保留了，但又掉了一份”的刷物品漏洞。

---

## 核心功能

- **死亡保命**：消耗 1 个图腾，保留物品栏与经验。
- **图腾放哪都行**：主背包或副手，不需要拿在主手。
- **每个图腾独立配置**：随机掉落、清诅咒、禁用世界、死亡/重生命令。
- **自定义材质**：原版材质，或 CraftEngine `CE:` / ItemsAdder `IA:`。
- **配置重载**：改完配置执行 `/nhtotem reload`，不需要重启。
- **单 jar**：无 ProtocolLib / PlaceholderAPI 硬依赖。

---

## 快速开始

1. 停服，把 `NHTotem-0.1.3.jar` 放进 `plugins/`。
2. 启动服务器。
3. 编辑 `plugins/NHTotem/config.yml`：

```yaml
totems:
  "&6不死图腾·金":
    material: "CE:nhtotem_tutorial:totem_gold"
    drop-probability: 0.0
```

> 没用 CraftEngine 就写 `material: TOTEM_OF_UNDYING`。

4. 重载并发放：

```text
/nhtotem reload
/nhtotem give 不死图腾·金 <玩家名>
```

玩家带着图腾死亡时，图腾 -1，物品栏与经验保留。

---

## 兼容性

| 服务端 | 状态 |
| --- | --- |
| Paper / Purpur 1.20.1 – 26.3 | ✅ 实测 |
| Spigot 1.20.1+ | 🟡 Bukkit API 理论兼容，未完整真机回归 |
| CraftEngine 26.9.2 | ✅ 软兼容，可选 |
| ItemsAdder | 🟡 反射软兼容，可选 |
| Folia | ❌ 不支持 |

Java 字节码 17；运行 Java 17+，Minecraft 26.1+ 服务端需要 Java 25+。

---

## 命令

| 命令 | 说明 |
| --- | --- |
| `/nhtotem list` | 列出已加载图腾 |
| `/nhtotem give <图腾> [玩家]` | 发放图腾 |
| `/nhtotem reload` | 重载配置 |

权限默认仅 OP；玩家触发图腾不需要权限。

---

## 文档

| 文档 | 内容 |
| --- | --- |
| [安装与使用](docs/USAGE.md) | 安装、发放、玩家用法、测试流程 |
| [配置说明](docs/CONFIGURATION.md) | 全部字段、示例、占位符 |
| [CraftEngine 对接](docs/CRAFTENGINE.md) | CE 物品、材质、7 色教程包 |
| [常见问题](docs/FAQ.md) | 掉落、动画、随机概率、Folia 等 |
| [开发与测试](docs/DEVELOPMENT.md) | 构建、测试、兼容性记录 |
| [CraftEngine 教程包](docs/craftengine-tutorial/nhtotem_tutorial/README.md) | 7 种配色 + 功能预设 + 材质替换 |

---

## 开发

- 构建：`mvn package`
- 测试：`mvn test`（19 项 MockBukkit 测试）
- 真机：Purpur 1.20.1 / 1.21.4 / 26.3

---

## License

插件代码与文档采用 **MIT License**，详见 [LICENSE](LICENSE)。

> Minecraft 与不死图腾原始美术属于 Mojang Studios；`docs/brand/` 中的图腾衍生图仅作为本项目品牌素材，MIT 不改变其原始权利归属。

---

NowHere Studios · NHTotem
