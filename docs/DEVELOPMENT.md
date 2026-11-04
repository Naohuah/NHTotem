# 开发与测试

## 环境

- JDK 25+（项目用 JDK 26 构建）
- Maven 3.9+
- Paper API 26.3

> Paper 26.3 API / MockBukkit 的 class 文件版本是 69（Java 25），编译和测试需要 JDK 25+。  
> 生成的插件是 Java 17 字节码，运行环境 Java 17+；26.3 服务端需要 Java 25+。

---

## 构建

```bash
mvn package
```

产物：

```text
target/NHTotem-0.1.3.jar
```

首次构建需要联网下载 Paper API、MockBukkit 和 Maven 插件。

---

## 测试

```bash
mvn test
```

当前：**19 项 MockBukkit 测试**（Paper 26.3 API），全部通过。

覆盖：

1. 插件加载 / 兼容命令
2. 新主命令 / 别名
3. PDC 发放
4. 死亡消耗图腾并保留物品栏
5. 清空 `getDrops()` 防复制
6. 无 PDC 数据兜底匹配
7. 旧 PDC 键
8. 副手图腾
9. 禁用世界
10. 消失诅咒
11. 随机掉落
12. `drop-mode: delete`
13. 死亡/重生命令
14. 稳定 `id`
15. 未安装 ItemsAdder 回退 PAPER
16. 未安装 CraftEngine 回退 PAPER
17. 非法材质回退 PAPER
18. 动画临时主手还原，不复制物品
19. 图腾在副槽时动画还原主手武器

---

## 兼容性记录

| 服务端 | 版本 | 内容 |
| --- | --- | --- |
| Paper | 26.3 build 239 | 启动、迁移、命令 |
| Purpur | 26.3 build 2655 | 启动、CraftEngine、7 图腾 |
| Purpur | 1.21.4 build 2413 | 真机死亡 / 保留 / 防复制 |
| Purpur | 1.20.1 build 2062 | 真机死亡 / 保留 / 防复制 |

测试记录由维护者保留，不随公开仓库发布。仓库内只保留构建配置和测试源码。

---

## 源码结构

```text
src/main/java/net/nowherestudios/nhtotem/
  NHTotem.java         主类：生命周期、迁移、死亡/重生、命令
  Totem.java           图腾模型：解析、模板、PDC、匹配
  Settings.java        settings 段
  Text.java            颜色代码
  InventoryUtil.java   主背包/盔甲/副手槽位
  ItemsAdderHook.java  ItemsAdder 反射软兼容
  CraftEngineHook.java CraftEngine 反射软兼容 + 重载监听
```

---

## 已知限制

- 图腾名不能包含 `.`。
- `keep-level` 是全局设置。
- `drop-probability` 是每件物品独立判定。
- `clear-curse` 会销毁诅咒物品。
- 不支持 Folia。
- 动画依赖客户端模型和资源包，不能保证所有客户端都显示自定义材质。
- CraftEngine / ItemsAdder 使用反射，第三方 API 变更时可能回退 PAPER。
