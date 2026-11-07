# 配置说明

配置文件：

```text
plugins/NHTotem/config.yml
```

修改后执行：

```text
/nhtotem reload
```

---

## 1. 文件结构

```yaml
settings:
  # 全局设置

totems:
  "&6图腾名":
    # 单个图腾设置
```

`totems` 下每个直接子节点就是一个图腾。配置名就是物品显示名，支持：

- `&6` 传统颜色代码；
- `&#RRGGBB` 十六进制颜色。

> 图腾名不要包含 `.`。Bukkit 的 YAML 会把 `.` 当作路径分隔符。

---

## 2. `settings` 全局设置

| 字段 | 类型 | 默认 | 说明 |
| --- | --- | --- | --- |
| `keep-level` | boolean | `true` | 死亡保留经验等级。所有图腾共用，不能单独给某个图腾关闭 |
| `totem-effect` | boolean | `true` | 播放图腾复活粒子、音效和动画 |
| `commands-as-console` | boolean | `true` | 死亡/重生命令以控制台身份执行；`false` 则伪装成玩家 |
| `drop-mode` | string | `drop` | 随机掉落的处理方式：`drop` 掉在死亡地点；`delete` 直接删除 |
| `debug` | boolean | `false` | 输出更多调试日志 |

示例：

```yaml
settings:
  keep-level: true
  totem-effect: true
  commands-as-console: true
  drop-mode: drop
  debug: false
```

---

## 3. 单个图腾字段

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| 配置名 | string | 是 | 物品显示名，支持 `&` / `&#RRGGBB` |
| `id` | string | 否 | 稳定 PDC 标识；不写则等于配置名。改名时保留它，旧图腾仍有效 |
| `material` | string | 是 | 原版材质 / `CE:命名空间:物品ID` / `IA:命名空间:物品ID` |
| `lore` | string list | 否 | 鼠标悬停文本，支持颜色代码 |
| `drop-probability` | double | 否 | 每件物品独立掉落概率，`0.0` – `1.0`，省略时为 `0.0` |
| `clear-curse` | boolean | 否 | `true` 清除带“消失诅咒”的物品（销毁，不是掉落），默认 `false` |
| `disabled-worlds` | string list | 否 | 这些世界死亡不触发 |
| `death-commands` | string list | 否 | 死亡时执行 |
| `respawn-commands` | string list | 否 | 重生时执行 |

### 关于 `drop-probability`

它是**每件物品独立判定**，不是“总共掉一件”的概率。

- `0.0`：全保留；
- `0.05`：每件物品 5% 概率掉；
- `0.1`：每件物品 10% 概率掉。

41 个槽位下，`0.1` 几乎一定会掉东西。想完全保留请设 `0.0`。

### 关于 `clear-curse`

`true` 时，带 `消失诅咒` 的物品会被**直接销毁**，和原版死亡逻辑一致；不是掉出来。

### 关于 `disabled-worlds`

填世界文件夹名，例如：

```yaml
disabled-worlds:
  - "world_nether"
  - "world_the_end"
```

如果服务器世界名是自定义的，改成实际名字。

---

## 4. 命令占位符

`death-commands` / `respawn-commands` 支持：

| 占位符 | 含义 |
| --- | --- |
| `%player%` | 玩家名 |
| `%world%` | 玩家所在世界名 |
| `%totem%` | 图腾纯文本名（去掉颜色） |
| `%totem_id%` | 图腾稳定 ID |

示例：

```yaml
death-commands:
  - "say %player% 使用 %totem% 活了下来！"
respawn-commands:
  - "say %player% 已重生"
```

命令可以带前导 `/`，插件会自动去掉；空行会忽略。

---

## 5. 完整示例

### 原版材质

```yaml
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

### CraftEngine 材质

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

### ItemsAdder 材质

```yaml
totems:
  "&6不死图腾·金":
    id: "ia_totem_gold"
    material: "IA:myitems:totem_gold"
    drop-probability: 0.0
```

### 多图腾、不同规则

```yaml
totems:
  "&6金图腾":
    id: "gold"
    material: "CE:nhtotem_tutorial:totem_gold"
    drop-probability: 0.0
    clear-curse: false

  "&c红宝石图腾":
    id: "ruby"
    material: "CE:nhtotem_tutorial:totem_ruby"
    drop-probability: 0.0
    clear-curse: true

  "&9蓝宝石图腾":
    id: "sapphire"
    material: "CE:nhtotem_tutorial:totem_sapphire"
    drop-probability: 0.05

  "&d紫水晶图腾":
    id: "amethyst"
    material: "CE:nhtotem_tutorial:totem_amethyst"
    drop-probability: 0.0
    disabled-worlds:
      - "world_nether"
```

---

## 6. 重载

| 修改内容 | 命令 |
| --- | --- |
| 只改 NHTotem `config.yml` | `nhtotem reload` |
| 改 CE `items.yml` 文字 | `ce reload config`，再 `nhtotem reload` |
| 改 CE PNG / 模型 | `ce reload all`，再 `nhtotem reload` |

---

## 7. 常见配置错误

- 用 Tab 缩进：YAML 只能空格。
- 图腾名含 `.`：会被拆成嵌套节点，插件会警告并跳过。
- `material` 写 `CE:nhtotem_tutorial:totem_gold` 时漏了引号：YAML 仍可解析，但建议加引号。
- CE 物品 ID 写错：会退化为 `PAPER`，控制台会提示等待/缺失。
- `drop-probability` 写成 `5`：会按 500% 处理，实际效果仍是必掉；正确是 `0.05`。
