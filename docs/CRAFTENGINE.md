# CraftEngine 对接

NHTotem 0.1.2+ 支持 CraftEngine 自定义物品，格式：

```yaml
material: "CE:命名空间:物品ID"
```

CraftEngine 是**可选**的。没装 CE 时 NHTotem 仍然可以用原版材质，不会崩服。

---

## 1. 前置

- CraftEngine 26.9.2+（实测版本）
- NHTotem 0.1.3+
- 一个 16×16 透明 PNG（图腾贴图）

---

## 2. 安装教程包

教程包在：

```text
docs/craftengine-tutorial/nhtotem_tutorial/
```

把整个 `nhtotem_tutorial` 文件夹放到：

```text
plugins/CraftEngine/resources/nhtotem_tutorial/
```

结构：

```text
nhtotem_tutorial/
  pack.yml
  configuration/items.yml
  resourcepack/assets/nhtotem_tutorial/textures/item/
    totem_gold.png
    totem_ruby.png
    totem_sapphire.png
    totem_amethyst.png
    totem_emerald.png
    totem_rose.png
    totem_obsidian.png
  examples/nhtotem-config.yml
  README.md
```

执行：

```text
ce reload all
```

> 不要把教程包的旧备份文件夹放在 `plugins/CraftEngine/resources/` 下，CraftEngine 会把它们也当包加载，导致物品 ID 重复。备份请放到 `resources` 外面。

---

## 3. 定义 CE 物品

CE 的物品定义在：

```text
plugins/CraftEngine/resources/nhtotem_tutorial/configuration/items.yml
```

单个物品示例：

```yaml
items:
  nhtotem_tutorial:totem_gold:
    material: totem_of_undying
    data:
      item_name: "<!i><gold>不死图腾·金"
      lore:
        - "<!i><gray>NHTotem × CraftEngine"
    texture: nhtotem_tutorial:item/totem_gold
```

- `nhtotem_tutorial` 是命名空间；
- `totem_gold` 是物品 ID；
- `material` 用 `totem_of_undying`；
- `texture` 指向 `assets/nhtotem_tutorial/textures/item/totem_gold.png`，**不要带 `.png`**；
- CraftEngine 会自动生成模型和 CustomModelData，不需要手写 model JSON。

改完执行：

```text
ce reload all
```

---

## 4. 接到 NHTotem

在 `plugins/NHTotem/config.yml`：

```yaml
totems:
  "&6不死图腾·金":
    id: "ce_totem_gold"
    material: "CE:nhtotem_tutorial:totem_gold"
    lore:
      - "&7CraftEngine 自定义材质"
    drop-probability: 0.0
    clear-curse: false
    disabled-worlds: []
```

然后：

```text
nhtotem reload
/nhtotem give 不死图腾·金 <玩家名>
```

注意 `CE:` 不能漏。只写 `nhtotem_tutorial:totem_gold` 会被当成原版材质。

---

## 5. 换成自己的材质

### 5.1 放 PNG

```text
plugins/CraftEngine/resources/nhtotem_tutorial/resourcepack/assets/nhtotem_tutorial/textures/item/my_totem.png
```

要求：

- 16×16 最推荐；32×32、64×64 也可以，但必须是 2 的幂；
- 透明背景；
- 文件名只能小写字母、数字、`_`、`-`；
- 不要有空格/中文。

### 5.2 注册 CE 物品

在 `items.yml` 复制一段：

```yaml
items:
  nhtotem_tutorial:my_totem:
    material: totem_of_undying
    data:
      item_name: "<!i><gold>我的图腾"
      lore:
        - "<!i><gray>自定义材质示例"
    texture: nhtotem_tutorial:item/my_totem
```

### 5.3 重载

```text
ce reload all
/ce item get nhtotem_tutorial:my_totem
```

### 5.4 接到 NHTotem

```yaml
material: "CE:nhtotem_tutorial:my_totem"
```

```text
nhtotem reload
```

---

## 6. 教程包的 7 种配色

教程包自带：

| 物品 ID | 颜色 | 功能预设 |
| --- | --- | --- |
| `totem_gold` | 金 | 基础保命 |
| `totem_ruby` | 红宝石 | 清诅咒 |
| `totem_sapphire` | 蓝宝石 | 5% 随机掉落 |
| `totem_amethyst` | 紫水晶 | 下界禁用 |
| `totem_emerald` | 绿宝石 | 死亡/重生命令 |
| `totem_rose` | 玫瑰金 | 仅主世界 |
| `totem_obsidian` | 黑曜石 | 全功能 |

完整说明见：

[教程包 README](craftengine-tutorial/nhtotem_tutorial/README.md)

---

## 7. 关于触发动画

NHTotem 0.1.3 起会在触发瞬间把对应图腾临时放到主手，播放动画后立即还原，让动画尽量显示对应颜色。

需要同时满足：

- NHTotem 0.1.3+；
- 客户端加载了 CraftEngine 资源包；
- `material` 指向了正确的 CE 物品；
- `settings.totem-effect: true`。

如果仍然是原版金色，说明客户端没有把自定义模型用于动画，需要 ProtocolLib 伪装备包或客户端模组方案。

---

## 8. 常见问题

### 物品加载了，但 NHTotem 提示等待 CraftEngine

CE 物品是异步加载的。等 CE 加载完会自动刷新，也可以手动：

```text
nhtotem reload
```

### NHTotem 退化为 PAPER

检查：

1. `material` 是否有 `CE:`；
2. CE 物品 ID 是否和 `items.yml` 一致；
3. 是否执行了 `ce reload all`；
4. 控制台提示的具体 ID 是什么。

### 改了 PNG，游戏里没变化

```text
ce reload all
```

然后重新进服。只改 `items.yml` 文字可以用 `ce reload config`，但改贴图必须 `all`。

### 背包/副手触发时动画不对

确认服务端是 NHTotem 0.1.3+，并且客户端加载了 CE 资源包。

---

## 9. 相关文件

- [教程包 README](craftengine-tutorial/nhtotem_tutorial/README.md)
- [教程包 items.yml](craftengine-tutorial/nhtotem_tutorial/configuration/items.yml)
- [NHTotem 示例配置](craftengine-tutorial/nhtotem_tutorial/examples/nhtotem-config.yml)
- [配置说明](CONFIGURATION.md)
