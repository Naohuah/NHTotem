# 安装与使用

本文只讲“怎么装、怎么发、玩家怎么用”。字段解释见 [配置说明](CONFIGURATION.md)，CraftEngine 见 [CraftEngine 对接](CRAFTENGINE.md)。

---

## 1. 环境要求

- 服务端：Paper / Purpur / Spigot 1.20.1 – 26.3
- Java：17+；Minecraft 26.1+ 服务端需要 Java 25+
- 可选：CraftEngine 26.9.2+ 或 ItemsAdder

---

## 2. 安装

1. 停服。
2. 删除同名旧 NHTotem jar：
   - `NHTotem-*.jar`
3. 把 `NHTotem-0.1.3.jar` 放进 `plugins/`。
4. 启动服务器。
5. 打开 `plugins/NHTotem/config.yml`，按 [配置说明](CONFIGURATION.md) 修改。
6. 执行 `/nhtotem reload`。

> 第一次启动会自动生成默认配置。

---

## 3. 最小配置

```yaml
settings:
  keep-level: true
  totem-effect: true
  commands-as-console: true
  drop-mode: drop
  debug: false

totems:
  "&6不死图腾·金":
    id: "totem_gold"
    material: TOTEM_OF_UNDYING
    lore:
      - "&7死亡时消耗，保留物品栏与经验"
    drop-probability: 0.0
    clear-curse: false
    disabled-worlds: []
```

保存后：

```text
/nhtotem reload
```

---

## 4. 给玩家图腾

### 管理员命令

```text
/nhtotem give 不死图腾·金 <玩家名>
```

不写玩家名时默认给自己（需要游戏内执行）。

### CraftEngine 物品

如果你用 CE 自定义材质：

```text
/ce item get nhtotem_tutorial:totem_gold
```

或让 NHTotem 发放：

```yaml
material: "CE:nhtotem_tutorial:totem_gold"
```

### 正式服获取方式

教程包只负责物品与材质，不包含经济系统。常见方式：

- 商店插件购买；
- 点券 / 礼包 / 抽奖；
- 副本、活动奖励；
- CraftEngine 合成配方（需要另外写 `recipes`）。

---

## 5. 玩家怎么用

1. 把图腾放在**主背包或副手**，不需要拿在主手。
2. 死亡时自动消耗 **1 个**图腾。
3. 消耗后：
   - 物品栏保留；
   - 经验等级保留（由 `settings.keep-level` 控制）；
   - 图腾消失；
   - 如果配置了随机掉落 / 清诅咒 / 命令，会按配置执行。
4. 触发图腾**不需要权限**。

> 多个图腾时只消耗 1 个，不会一次清空。

---

## 6. 测试清单

建议开服后按顺序测：

1. `/nhtotem list` 能看到配置的图腾。
2. 给自己一个图腾。
3. 带着图腾死亡：
   - 图腾 -1；
   - 物品栏保留；
   - 经验保留；
   - 地上没有额外的复制掉落物。
4. 把图腾放副手再死一次，确认仍然触发。
5. 不带图腾死亡，确认正常掉落。
6. 如果图腾配置了 `disabled-worlds`，去对应世界死亡，确认不触发。

---

## 7. 命令与权限

| 命令 | 说明 | 权限 |
| --- | --- | --- |
| `/nhtotem` | 主命令 | `nhtotem.command.use` |
| `/nht` | 别名 | 同上 |
| `/nhtotem list` | 列出图腾 | `nhtotem.command.use` |
| `/nhtotem give <图腾> [玩家]` | 发放图腾 | `nhtotem.command.give` |
| `/nhtotem reload` | 重载配置 | `nhtotem.command.reload` |

默认仅 OP；控制台拥有全部权限。

---

## 8. 重载说明

| 改了什么 | 执行 |
| --- | --- |
| NHTotem 的 `config.yml` | `nhtotem reload` |
| CE 的 `items.yml` 文字 | `ce reload config`，再 `nhtotem reload` |
| CE 的 PNG 贴图 / 模型 | `ce reload all`，再 `nhtotem reload` |
| 新增/删除图腾 | `nhtotem reload` |

> CE 物品是异步加载的。启动时如果 NHTotem 提示“等待 CraftEngine 加载”，等 CE 加载完会自动刷新；也可以手动 `/nhtotem reload`。

---

## 9. 遇到问题

先看：

- [常见问题](FAQ.md)
- [配置说明](CONFIGURATION.md)
- [CraftEngine 对接](CRAFTENGINE.md)

如果还有问题，把以下信息发出来：

```text
服务端版本：
NHTotem 版本：
config.yml 对应段落：
控制台报错：
```
