package net.nowherestudios.nhtotem;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * CraftEngine 软兼容。
 *
 * <p>CraftEngine 的 API 只在安装了 CraftEngine 时才有意义，所以这里用反射调用，
 * 不把 CraftEngine 作为编译/运行硬依赖。服务器没装 CraftEngine 时，{@code CE:} 物品
 * 会像旧版 ItemsAdder 缺失时一样退化为 PAPER，不会导致插件加载失败。</p>
 *
 * <p>另外 CraftEngine 的物品是在它自己的 reload 流程里异步加载完成的，
 * 所以这里还负责监听 {@code CraftEngineReloadEvent}，在它加载完成后让 NHTotem 重新读取配置。</p>
 */
final class CraftEngineHook {

    private static final String ITEMS_CLASS = "net.momirealms.craftengine.bukkit.api.CraftEngineItems";
    private static final String DEFINITION_CLASS = "net.momirealms.craftengine.bukkit.item.BukkitItemDefinition";
    private static final String RELOAD_EVENT_CLASS = "net.momirealms.craftengine.bukkit.api.event.CraftEngineReloadEvent";

    private static Boolean available;
    private static boolean warnedFailure;
    private static boolean missingInLoad;
    private static final Set<String> MISSING_IDS = new LinkedHashSet<>();

    private CraftEngineHook() {
    }

    static void beginLoad() {
        missingInLoad = false;
        MISSING_IDS.clear();
    }

    static void markMissing(String id) {
        missingInLoad = true;
        MISSING_IDS.add(id);
    }

    static boolean wasMissing() {
        return missingInLoad;
    }

    static Set<String> missingIds() {
        return Collections.unmodifiableSet(MISSING_IDS);
    }

    static boolean isAvailable() {
        if (available != null) {
            return available;
        }
        if (Bukkit.getPluginManager().getPlugin("CraftEngine") == null) {
            available = false;
            return false;
        }
        try {
            Class.forName(ITEMS_CLASS);
            available = true;
        } catch (Throwable throwable) {
            available = false;
        }
        return available;
    }

    static ItemStack getItem(String id, Plugin plugin) {
        if (!isAvailable()) {
            return null;
        }
        try {
            Class<?> itemsClass = Class.forName(ITEMS_CLASS);
            Object definition = itemsClass.getMethod("byId", String.class).invoke(null, id);
            if (definition == null) {
                return null;
            }
            Object stack = Class.forName(DEFINITION_CLASS)
                    .getMethod("buildBukkitItem")
                    .invoke(definition);
            if (!(stack instanceof ItemStack)) {
                return null;
            }
            ItemStack copy = ((ItemStack) stack).clone();
            return copy.getType().isAir() ? null : copy;
        } catch (Throwable throwable) {
            if (!warnedFailure) {
                warnedFailure = true;
                plugin.getLogger().warning("读取 CraftEngine 物品失败（" + id + "）：" + throwable.getMessage());
            }
            return null;
        }
    }

    /**
     * 注册 CraftEngine 重载事件，在 CE 物品重新加载完成后回调。
     */
    @SuppressWarnings("deprecation")
    static void registerReloadListener(Plugin plugin, Runnable callback) {
        if (!isAvailable()) {
            return;
        }
        try {
            Class<?> eventClass = Class.forName(RELOAD_EVENT_CLASS);
            if (!Event.class.isAssignableFrom(eventClass)) {
                return;
            }
            @SuppressWarnings("unchecked")
            Class<? extends Event> typedEvent = (Class<? extends Event>) eventClass;
            Listener listener = new Listener() {
            };
            Bukkit.getPluginManager().registerEvent(
                    typedEvent,
                    listener,
                    EventPriority.MONITOR,
                    (ignored, event) -> callback.run(),
                    plugin);
        } catch (Throwable throwable) {
            plugin.getLogger().warning("无法注册 CraftEngine 重载监听：" + throwable.getMessage());
        }
    }
}
