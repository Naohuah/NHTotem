package net.nowherestudios.nhtotem;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * 插件可选项，全部来自 config.yml 的 settings 段，缺省时使用内置默认值，
 * 因此 1.1 旧版配置文件不需要做任何修改。
 */
public final class Settings {

    public enum DropMode {
        /**
         * 随机“掉落”的物品掉落在死亡地点。
         */
        DROP,
        /**
         * 随机“掉落”的物品直接删除（旧版 1.1 的行为）。
         */
        DELETE
    }

    private final boolean keepLevel;
    private final boolean totemEffect;
    private final boolean commandsAsConsole;
    private final DropMode dropMode;
    private final boolean debug;

    private Settings(boolean keepLevel,
                     boolean totemEffect,
                     boolean commandsAsConsole,
                     DropMode dropMode,
                     boolean debug) {
        this.keepLevel = keepLevel;
        this.totemEffect = totemEffect;
        this.commandsAsConsole = commandsAsConsole;
        this.dropMode = dropMode;
        this.debug = debug;
    }

    public static Settings from(FileConfiguration config) {
        String rawMode = config.getString("settings.drop-mode", "drop");
        DropMode dropMode = "delete".equalsIgnoreCase(rawMode) ? DropMode.DELETE : DropMode.DROP;
        return new Settings(
                config.getBoolean("settings.keep-level", true),
                config.getBoolean("settings.totem-effect", true),
                config.getBoolean("settings.commands-as-console", true),
                dropMode,
                config.getBoolean("settings.debug", false));
    }

    public static Settings defaults() {
        return new Settings(true, true, true, DropMode.DROP, false);
    }

    public boolean keepLevel() {
        return keepLevel;
    }

    public boolean totemEffect() {
        return totemEffect;
    }

    public boolean commandsAsConsole() {
        return commandsAsConsole;
    }

    public DropMode dropMode() {
        return dropMode;
    }

    public boolean debug() {
        return debug;
    }
}
