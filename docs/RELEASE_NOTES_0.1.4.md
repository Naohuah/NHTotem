# NHTotem 0.1.4

死亡不掉落图腾 · NowHere Studios

带着图腾倒下，消耗 1 个图腾，保留物品栏与经验。

---

## 修复内容

- **修复经验仍然掉落**：`PlayerDeathEvent#setKeepLevel(true)` 只控制重生后保留等级，并不会阻止死亡事件掉经验。现在同时把 `droppedExp` 清零。
- **修复带空格图腾名无法发放**：`/nhtotem give Totem of Gold <player>` 现在会正确解析完整名称。
- **拒绝重复稳定 ID**：两个图腾使用同一个 `id` 时，第二个会被跳过并警告，避免死亡时触发错误图腾。
- **修复重生世界禁用导致重生命令不执行**：图腾在死亡世界已确认生效后，重生命令不再受重生世界影响。
- **修复 NaN 掉落概率导致全掉**：非法概率会被修正到 `0.0~1.0`。
- **ItemsAdder 软依赖补全**：`plugin.yml` 增加 `softdepend: ItemsAdder`，并捕获 API 链接错误。
- **CraftEngine 重载保护**：插件禁用后不再调度 CE 刷新任务。
- **帮助命令清理**：不再显示 `/gettotem`、`/qcyreload` 旧命令。
- **材质名自动 trim**：`material` 前后空格不会再影响 CE 前缀判断。

---

## 兼容性

- Paper / Purpur 1.20.1 – 26.3
- Spigot 1.20.1+（Bukkit API 理论兼容，未完整真机回归）
- CraftEngine 26.9.2（软兼容，可选）
- ItemsAdder（软兼容，可选）
- Java 17 字节码；运行 Java 17+；26.1+ 服务端需要 Java 25+
- Folia 不支持

---

## 测试

- **24 项 MockBukkit 测试**（Paper 26.3 API），全部通过。
- 新增测试：
  - 保留经验时 `droppedExp` 清零
  - 带空格图腾名发放
  - 重复 `id` 拒绝
  - `NaN` 掉落概率回退
  - 重生世界禁用时仍执行重生命令

---

## 安装

1. 停服。
2. 删除旧 NHTotem jar。
3. 把 `NHTotem-0.1.4.jar` 放进 `plugins/`。
4. 启动服务器。
5. 编辑 `plugins/NHTotem/config.yml`。
6. 执行 `/nhtotem reload`。

---

## 资源

- `NHTotem-0.1.4.jar` — 插件本体
- `NHTotem-CraftEngine-Tutorial-v1.1.zip` — 7 色图腾教程包

### SHA-256

```text
NHTotem-0.1.4.jar
6CE3CF94FA82FF67BD87B68EC8DEB00D2AEB9337FD7CA25BBA33449BB2E5E071

NHTotem-CraftEngine-Tutorial-v1.1.zip
9A465D8BAFA8FF2BAB6ABDA4F84377CB7F1C454E635238955887B88AB7E724FD
```
