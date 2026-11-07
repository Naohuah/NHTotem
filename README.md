# NHTotem

<p align="center">
  <img src="docs/brand/totem/nhtotem-logo-green-256.png" width="180" alt="NHTotem">
</p>

NHTotem 是一个死亡保护插件。玩家死亡时，如果背包里带着配置好的图腾，就消耗一个，保留物品栏和经验。

它适合不想直接开 `keepInventory` 的服务器：死亡还是要有代价，但没必要把整包东西都爆掉。图腾可以放在主背包或副手，不需要拿在主手。你可以把它当消耗品发给玩家、放进商店，或者只作为副本和活动奖励。

## 它怎么工作

玩家死亡时，NHTotem 会扫描主背包、盔甲槽和副手。找到配置好的图腾后，扣掉一个，保留物品栏和经验。

每个图腾可以单独设置掉落概率、是否清除消失诅咒、禁用世界，以及死亡和重生时执行的命令。比如可以让金图腾全保留，让红宝石图腾的每件物品有 5% 概率掉落，或者让蓝宝石图腾在下界不生效。

死亡事件里有两个容易踩的坑：

- `setKeepInventory(true)` 不会阻止 `getDrops()` 继续生成掉落物。NHTotem 会清空这些掉落，避免“物品保留了，地上又掉一份”。
- `setKeepLevel(true)` 不会阻止经验掉落。NHTotem 会同时把死亡事件的掉落经验清零。

## 快速开始

1. 停服，把 `NHTotem-0.1.4.jar` 放进 `plugins/`。
2. 启动服务器。
3. 编辑 `plugins/NHTotem/config.yml`。

最小配置：

```yaml
totems:
  "&6不死图腾·金":
    id: "totem_gold"
    material: "CE:nhtotem_tutorial:totem_gold"
    drop-probability: 0.0
```

`id` 可以不写，但建议写。以后改图腾显示名时，已经发到玩家手里的旧物品还能继续识别。

不用 CraftEngine 的话，材质直接写原版：

```yaml
material: TOTEM_OF_UNDYING
```

4. 重载并发放：

```text
/nhtotem reload
/nhtotem give 不死图腾·金 <玩家名>
```

图腾名可以带空格。不写玩家名时，命令会发给自己。

## 兼容性

Paper 和 Purpur 的 1.20.1、1.21.4、26.3 都实际跑过。Spigot 走的是同一套 Bukkit API，理论上能用，但我没在上面做同样的真机死亡测试。如果遇到问题，带上服务端版本和配置里的对应段落开 issue。

Folia 不支持。

CraftEngine 26.9.2 测试过。ItemsAdder 用反射做软兼容，如果它的 API 大改，这边可能需要跟着更新。

Java 字节码是 17，运行 Java 17+ 即可；Minecraft 26.1+ 的服务端需要 Java 25+。

## 命令

```text
/nhtotem list
/nhtotem give <图腾> [玩家]
/nhtotem reload
```

权限默认只有 OP。玩家触发图腾不需要权限。

## 自定义物品

CraftEngine：

```yaml
material: "CE:nhtotem_tutorial:totem_gold"
```

ItemsAdder：

```yaml
material: "IA:namespace:item_id"
```

仓库里带了一个 CraftEngine 教程包，包含 7 种颜色的图腾材质、物品定义和 7 份功能配置。可以直接用，也可以把贴图换成自己的。它只是起步模板，不带经济系统或完整的成长线。

## 文档

- [安装与使用](docs/USAGE.md)
- [配置说明](docs/CONFIGURATION.md)
- [CraftEngine 对接](docs/CRAFTENGINE.md)
- [常见问题](docs/FAQ.md)
- [开发与测试](docs/DEVELOPMENT.md)
- [English introduction](docs/INTRO_EN.md)
- [CraftEngine 教程包](docs/craftengine-tutorial/nhtotem_tutorial/README.md)

## 开发

```bash
mvn package
mvn test
```

当前 0.1.4，24 项 MockBukkit 测试通过。

## License

插件代码和文档采用 MIT License，详见 [LICENSE](LICENSE)。

Minecraft 与不死图腾原始美术属于 Mojang Studios。`docs/brand/` 里的图腾衍生图仅作为本项目品牌素材，MIT 不改变其原始权利归属。
