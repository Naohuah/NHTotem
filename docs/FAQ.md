# 常见问题

## 玩家保留了物品，地上还有一份？

确认使用的是 **NHTotem 0.1.0+**。

NHTotem 在保留物品栏时会清空 `event.getDrops()`，避免 Paper/Purpur 在死亡事件后再次生成掉落物。只设置 `keepInventory=true` 的插件容易出现双份物品。

---

## 图腾放在副手/背包会触发吗？

会。NHTotem 遍历主背包、副手和盔甲槽，不要求拿在主手。

正常生存无法把图腾放进盔甲栏，但插件仍然会扫描盔甲槽，避免自定义插件把图腾放进装备槽时漏判。

---

## 死亡后没有保留经验？

检查：

```yaml
settings:
  keep-level: true
```

`keep-level` 是**全局设置**，所有图腾共用，不能单独给某个图腾关闭。

---

## 随机掉落为什么掉了这么多？

`drop-probability` 是**每件物品独立判定**，不是“总共掉一件”。

- `0.0`：全保留；
- `0.05`：每件 5%；
- `0.1`：每件 10%。

41 个槽位下 `0.1` 几乎必定掉东西。想全保留请设 `0.0`。

---

## `clear-curse` 会把诅咒物品掉出来吗？

不会。`true` 时会**销毁**带“消失诅咒”的物品，和原版死亡逻辑一致。

想保留诅咒物品就设 `false`。

---

## 图腾动画还是原版金色？

需要同时满足：

- 服务端 NHTotem 0.1.3+；
- 客户端加载了 CraftEngine 资源包；
- `material` 正确指向 CE 物品；
- `settings.totem-effect: true`。

如果仍然不对，说明客户端没有把自定义模型用于动画，需要 ProtocolLib 伪装备包或客户端模组方案。

---

## NHTotem 提示“等待 CraftEngine 加载”

CraftEngine 的物品是异步加载的。启动时 NHTotem 可能先于 CE 物品完成加载。

正常情况下 CE 加载完后会自动刷新；也可以手动：

```text
nhtotem reload
```

如果一直这样，检查 CE 物品 ID 是否写错。

---

## NHTotem 退化为 PAPER

常见原因：

1. `material` 漏了 `CE:`；
2. CE 物品 ID 和 `items.yml` 不一致；
3. CE 没装或没执行 `ce reload all`；
4. 使用了 `IA:` 但没装 ItemsAdder。

控制台会提示具体是哪个物品。

---

## 支持 Folia 吗？

不支持。插件使用 Bukkit 主线程调度器。

---

## 可以和墓碑/死亡保留插件一起用吗？

不建议。多个插件同时处理 `PlayerDeathEvent`、`keepInventory` 和掉落物，容易冲突。

如果必须一起用，请只保留一个负责“死亡保留”的插件，并充分测试。

---

## 配置文件报错/图腾不加载

检查：

- 是否用了 Tab 缩进（只能空格）；
- 图腾名是否包含 `.`（Bukkit YAML 会拆成嵌套节点）；
- `material` 是否是有效材质或 `CE:` / `IA:` 格式；
- 控制台具体报错。

更多字段说明见 [配置说明](CONFIGURATION.md)。
