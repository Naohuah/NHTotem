package net.nowherestudios.nhtotem;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * ItemsAdder 软兼容。
 *
 * <p>旧版直接把 ItemsAdder 作为编译依赖，服务器没装 ItemsAdder 时只要配置里出现
 * {@code IA:} 物品就会抛 {@link NoClassDefFoundError}，整个插件启用失败。
 * 这里改成反射调用：没装 ItemsAdder 时只打印一条警告，其余图腾照常工作。</p>
 */
final class ItemsAdderHook {

    private static final String CUSTOM_STACK_CLASS = "dev.lone.itemsadder.api.CustomStack";

    private static boolean missingWarned;

    private ItemsAdderHook() {
    }

    /**
     * 通过 ItemsAdder API 获取自定义物品，失败时返回 null。
     *
     * @param namespacedId 形如 {@code namespace:item} 的物品 ID
     */
    static ItemStack getItem(String namespacedId, Plugin plugin) {
        try {
            Class<?> customStackClass = Class.forName(CUSTOM_STACK_CLASS);
            Object customStack = customStackClass.getMethod("getInstance", String.class)
                    .invoke(null, namespacedId);
            if (customStack == null) {
                return null;
            }
            Object itemStack = customStackClass.getMethod("getItemStack").invoke(customStack);
            if (!(itemStack instanceof ItemStack)) {
                return null;
            }
            ItemStack copy = ((ItemStack) itemStack).clone();
            return copy.getType().isAir() ? null : copy;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            if (!missingWarned) {
                missingWarned = true;
                plugin.getLogger().warning("服务器未安装 ItemsAdder（或 API 不可用），配置中所有 IA: 物品都会退化为 PAPER。");
            }
            return null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            plugin.getLogger().warning("读取 ItemsAdder 物品 '" + namespacedId + "' 失败: " + e);
            return null;
        }
    }
}
