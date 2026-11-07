package net.nowherestudios.nhtotem;

import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * NHTotem 主类。
 *
 * <p>功能：玩家死亡时，如果物品栏里有配置好的图腾，就消耗一个图腾，
 * 保留物品栏与经验，并按配置执行命令、处理随机掉落 / 消失诅咒。</p>
 */
public class NHTotem extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private static final String PREFIX = "\u00a78[\u00a7bNH\u00a78] \u00a7r";

    private final List<Totem> totems = new ArrayList<>();
    private final Map<UUID, PendingRespawn> pendingRespawns = new HashMap<>();

    private NamespacedKey totemKey;
    private List<NamespacedKey> legacyTotemKeys = Collections.emptyList();
    private Settings settings = Settings.defaults();
    private boolean lastCraftEngineMissing;

    @Override
    @SuppressWarnings("deprecation")
    public void onEnable() {
        migrateLegacyData();
        saveDefaultConfig();
        this.totemKey = new NamespacedKey(this, "totem_id");
        // 继续识别旧品牌的 PDC 键，升级/回滚都不会丢物品。
        this.legacyTotemKeys = legacyKeys("nowheretotem:totem_id", "qcytotem:totem_id");

        try {
            loadTotems();
        } catch (Throwable throwable) {
            getLogger().severe("加载 config.yml 失败: " + throwable);
        }

        registerCommand("nhtotem");
        registerCommand("qcyreload");
        registerCommand("gettotem");
        getServer().getPluginManager().registerEvents(this, this);

        logStartup();
        setupCraftEngine();
        if (totems.isEmpty()) {
            getLogger().warning("没有加载到任何图腾，请检查 config.yml 中的 totems 配置。");
        }
    }

    /**
     * CraftEngine 的物品是在它自己的 reload 流程里加载的，启动时可能还没就绪。
     * 这里监听 CE 的 reload 事件，并在启动后补一次刷新，保证 CE: 图腾能解析到真实物品。
     */
    private void setupCraftEngine() {
        if (!CraftEngineHook.isAvailable()) {
            return;
        }
        CraftEngineHook.registerReloadListener(this, this::onCraftEngineReload);
        if (usesCraftEngine()) {
            Bukkit.getScheduler().runTaskLater(this, this::reloadTotemsFromCraftEngine, 100L);
        }
    }

    private boolean usesCraftEngine() {
        for (Totem totem : totems) {
            String material = totem.getMaterialName();
            if (material != null && material.regionMatches(true, 0, "CE:", 0, 3)) {
                return true;
            }
        }
        return false;
    }

    private void onCraftEngineReload() {
        if (!isEnabled()) {
            return;
        }
        Bukkit.getScheduler().runTask(this, this::reloadTotemsFromCraftEngine);
    }

    private void reloadTotemsFromCraftEngine() {
        if (!isEnabled()) {
            return;
        }
        try {
            boolean clean = loadTotems();
            if (lastCraftEngineMissing) {
                getLogger().info("CraftEngine 物品尚未就绪，NHTotem 将在 CE 重载后继续重试。");
            } else if (clean) {
                getLogger().info("CraftEngine 物品已就绪，NHTotem 已同步刷新 " + totems.size() + " 个图腾。");
            } else {
                getLogger().warning("CraftEngine 重载后，NHTotem 配置加载存在错误。");
            }
        } catch (Throwable throwable) {
            getLogger().warning("CraftEngine 重载后刷新图腾失败: " + throwable.getMessage());
        }
    }

    /**
     * 品牌启动信息：替代旧版“定制插件 / QQ”广告。
     */
    private void logStartup() {
        String version = getDescription().getVersion();
        getLogger().info("==================================================");
        getLogger().info("  NHTotem " + version + "  |  NowHere Studios");
        getLogger().info("  死亡不掉落图腾 · 由 NowHere Studios 开发与维护");
        getLogger().info("  Paper / Purpur / Spigot 1.20.1 - 26.3");
        getLogger().info("==================================================");
        getLogger().info("已加载 " + totems.size() + " 个图腾。");
    }

    private static List<NamespacedKey> legacyKeys(String... rawKeys) {
        List<NamespacedKey> keys = new ArrayList<>(rawKeys.length);
        for (String rawKey : rawKeys) {
            NamespacedKey key = NamespacedKey.fromString(rawKey);
            if (key != null) {
                keys.add(key);
            }
        }
        return Collections.unmodifiableList(keys);
    }

    /**
     * 从旧品牌数据目录迁移 config.yml（先找 NowHereTotem，再找 QcyTotem）。
     * 已经存在新配置时不做任何事。
     */
    private void migrateLegacyData() {
        File current = new File(getDataFolder(), "config.yml");
        if (current.isFile()) {
            return;
        }
        File pluginsDirectory = getDataFolder().getParentFile();
        if (pluginsDirectory == null) {
            return;
        }

        File legacy = null;
        String source = null;
        for (String directory : new String[]{"NowHereTotem", "QcyTotem"}) {
            File candidate = new File(new File(pluginsDirectory, directory), "config.yml");
            if (candidate.isFile()) {
                legacy = candidate;
                source = directory;
                break;
            }
        }
        if (legacy == null) {
            return;
        }

        try {
            Files.createDirectories(getDataFolder().toPath());
            Files.copy(legacy.toPath(), current.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            getLogger().info("检测到旧版 " + source + " 配置，已自动复制到 NHTotem/config.yml。");
        } catch (IOException exception) {
            getLogger().warning("迁移旧版 config.yml 失败: " + exception.getMessage());
        }
    }

    @Override
    public void onDisable() {
        pendingRespawns.clear();
        getLogger().info("NHTotem 已禁用。");
    }

    private void registerCommand(String name) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().severe("plugin.yml 中缺少命令 /" + name + "，该命令不可用。");
            return;
        }
        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    /**
     * 重新读取配置。返回 true 表示没有配置错误。
     */
    private boolean loadTotems() {
        reloadConfig();
        this.settings = Settings.from(getConfig());

        ConfigurationSection section = getConfig().getConfigurationSection("totems");
        List<Totem> loaded = new ArrayList<>();
        Set<String> tagIds = new HashSet<>();
        int errors = 0;
        CraftEngineHook.beginLoad();

        if (section == null) {
            getLogger().warning("config.yml 中缺少 totems 配置段，插件不会生效。");
        } else {
            // 读取直接子节点；如果图腾名里带 '.'，Bukkit 的 YAML 在加载时会把它拆成嵌套节点，下面会检测并警告。
            for (Map.Entry<String, Object> entry : section.getValues(false).entrySet()) {
                String id = entry.getKey();
                if (!(entry.getValue() instanceof ConfigurationSection)) {
                    errors++;
                    getLogger().warning("图腾 '" + id + "' 不是有效的配置段，已跳过。");
                    continue;
                }
                ConfigurationSection totemSection = (ConfigurationSection) entry.getValue();
                if (hasNestedSection(totemSection)) {
                    errors++;
                    getLogger().warning("图腾 '" + id + "' 下还有子节点，图腾名不能包含 '.'（Bukkit 的 YAML 路径分隔符），已跳过。");
                    continue;
                }
                try {
                    Totem totem = Totem.parse(id, totemSection, totemKey, legacyTotemKeys, this);
                    if (!tagIds.add(totem.getTagId())) {
                        errors++;
                        getLogger().warning("图腾 '" + id + "' 使用了重复的 id '" + totem.getTagId()
                                + "'，已跳过；同一个 id 会被错误地识别成前一个图腾。");
                        continue;
                    }
                    loaded.add(totem);
                    if (settings.debug()) {
                        getLogger().info("[debug] 已加载图腾 '" + id + "'，材质=" + totem.getMaterialName()
                                + "，掉落概率=" + totem.getDropProbability()
                                + "，清除诅咒=" + totem.shouldClearCurse());
                    }
                } catch (Throwable throwable) {
                    errors++;
                    getLogger().severe("加载图腾 '" + id + "' 失败: " + throwable.getMessage());
                }
            }
        }

        totems.clear();
        totems.addAll(loaded);
        this.lastCraftEngineMissing = CraftEngineHook.wasMissing();
        if (errors > 0) {
            getLogger().warning("共有 " + errors + " 个图腾配置存在错误。");
        }
        if (lastCraftEngineMissing) {
            String ids = String.join(", ", CraftEngineHook.missingIds());
            if (CraftEngineHook.isAvailable()) {
                getLogger().info("等待 CraftEngine 加载: " + ids + "（CE 重载后自动重试）");
            } else {
                getLogger().warning("未安装 CraftEngine，CE: 图腾暂不可用: " + ids);
            }
        }
        return section != null && errors == 0;
    }

    private static boolean hasNestedSection(ConfigurationSection section) {
        for (String key : section.getKeys(false)) {
            if (section.isConfigurationSection(key)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // 死亡 / 重生
    // ------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Match match = findTotem(player);
        if (match == null) {
            return;
        }

        consume(match.slot);

        event.setKeepInventory(true);
        event.setKeepLevel(settings.keepLevel());
        if (settings.keepLevel()) {
            // setKeepLevel() only controls the level kept at respawn; it does not
            // stop the death event's dropped experience. Clear it explicitly.
            event.setDroppedExp(0);
        }
        // Paper/Purpur 在事件结束后仍可能按 getDrops() 生成掉落物。
        // 保留物品栏时必须清空，否则玩家会“既保留物品又掉一份”，形成刷物品漏洞。
        event.getDrops().clear();

        if (match.totem.shouldClearCurse()) {
            clearCursedItems(player);
        }
        dropRandomItems(player, match.totem);
        playTotemEffect(player, match.totem);

        pendingRespawns.put(player.getUniqueId(), new PendingRespawn(match.totem));
        runCommands(player, match.totem, match.totem.getDeathCommands(), "死亡");

        if (settings.debug()) {
            getLogger().info("[debug] " + player.getName() + " 触发了图腾 '" + match.totem.getId()
                    + "'（世界: " + player.getWorld().getName() + "）");
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        PendingRespawn pending = pendingRespawns.remove(player.getUniqueId());
        if (pending == null) {
            return;
        }

        // The totem was already validated against the death world in onPlayerDeath().
        // The respawn world must not suppress the respawn commands (a player can
        // respawn at an anchor located in a different, disabled world).
        runCommands(player, pending.totem, pending.totem.getRespawnCommands(), "重生");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        pendingRespawns.remove(event.getPlayer().getUniqueId());
    }

    /**
     * 按配置顺序查找玩家携带的、当前世界可用的图腾。
     */
    private Match findTotem(Player player) {
        PlayerInventory inventory = player.getInventory();
        List<InventoryUtil.Slot> slots = InventoryUtil.slots(inventory);
        String worldName = player.getWorld().getName();

        for (Totem totem : totems) {
            if (totem.isDisabledIn(worldName)) {
                continue;
            }
            for (InventoryUtil.Slot slot : slots) {
                if (totem.matches(slot.get())) {
                    return new Match(totem, slot);
                }
            }
        }
        return null;
    }

    /**
     * 消耗一个图腾（数量大于 1 时只减 1）。
     */
    private void consume(InventoryUtil.Slot slot) {
        ItemStack stack = slot.get();
        if (stack == null) {
            return;
        }
        if (stack.getAmount() > 1) {
            ItemStack copy = stack.clone();
            copy.setAmount(stack.getAmount() - 1);
            slot.set(copy);
        } else {
            slot.set(null);
        }
    }

    /**
     * 清除带“消失诅咒”的物品（与原版死亡逻辑一致：直接销毁）。
     */
    private void clearCursedItems(Player player) {
        for (InventoryUtil.Slot slot : InventoryUtil.slots(player.getInventory())) {
            ItemStack stack = slot.get();
            if (stack != null && hasVanishingCurse(stack)) {
                slot.set(null);
                if (settings.debug()) {
                    getLogger().info("[debug] 已清除 " + player.getName() + " 的诅咒物品: " + stack.getType());
                }
            }
        }
    }

    private static boolean hasVanishingCurse(ItemStack stack) {
        for (Enchantment enchantment : stack.getEnchantments().keySet()) {
            if ("vanishing_curse".equals(enchantment.getKey().getKey())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按概率把物品从物品栏移除并掉落在死亡地点。
     *
     * <p>旧版只是把物品从物品栏里删掉，物品直接消失，和“随机掉落概率”的描述不符；
     * 现在默认真的掉出来，可用 settings.drop-mode=delete 恢复旧行为。</p>
     */
    private void dropRandomItems(Player player, Totem totem) {
        double probability = totem.getDropProbability();
        if (probability <= 0.0D) {
            return;
        }
        for (InventoryUtil.Slot slot : InventoryUtil.slots(player.getInventory())) {
            ItemStack stack = slot.get();
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            if (ThreadLocalRandom.current().nextDouble() >= probability) {
                continue;
            }
            slot.set(null);
            if (settings.dropMode() == Settings.DropMode.DROP) {
                dropAtDeathLocation(player, stack);
            }
        }
    }

    private void dropAtDeathLocation(Player player, ItemStack stack) {
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world != null) {
            world.dropItemNaturally(location, stack);
        }
    }

    private void playTotemEffect(Player player, Totem totem) {
        if (!settings.totemEffect()) {
            return;
        }
        ItemStack display;
        try {
            display = totem.createItem();
        } catch (Throwable throwable) {
            if (settings.debug()) {
                getLogger().warning("创建图腾动画物品失败: " + throwable);
            }
            return;
        }
        if (display == null || display.getType().isAir()) {
            return;
        }
        display.setAmount(1);

        // 原版图腾动画使用客户端主手物品的模型。NHTotem 会在触发瞬间把
        // 被消耗的图腾临时放到主手，让 CE 自定义材质也能出现在动画里；
        // playEffect 之后立刻还原，避免改动物品栏/造成复制。
        PlayerInventory inventory = player.getInventory();
        ItemStack original = inventory.getItemInMainHand().clone();
        boolean replaced = !original.isSimilar(display);
        try {
            if (replaced) {
                inventory.setItemInMainHand(display);
            }
            player.playEffect(EntityEffect.TOTEM_RESURRECT);
        } catch (Throwable throwable) {
            if (settings.debug()) {
                getLogger().warning("播放图腾特效失败: " + throwable);
            }
        } finally {
            if (replaced) {
                inventory.setItemInMainHand(original);
            }
        }
    }

    // ------------------------------------------------------------------
    // 命令执行
    // ------------------------------------------------------------------

    private void runCommands(Player player, Totem totem, List<String> commands, String phase) {
        if (commands == null || commands.isEmpty()) {
            return;
        }

        final String playerName = player.getName();
        final String totemId = totem.getTagId();
        final String totemName = totem.getPlainName();

        // 放到下一 tick 执行：死亡/重生事件里同步 dispatchCommand 容易和复活流程互相踩踏。
        Bukkit.getScheduler().runTask(this, () -> {
            if (!isEnabled()) {
                return;
            }
            CommandSender sender = settings.commandsAsConsole() ? Bukkit.getConsoleSender() : player;
            String worldName = player.getWorld().getName();

            for (String raw : commands) {
                if (raw == null) {
                    continue;
                }
                String command = raw.trim();
                if (command.startsWith("/")) {
                    command = command.substring(1);
                }
                command = command
                        .replace("%player%", playerName)
                        .replace("%world%", worldName)
                        .replace("%totem%", totemName)
                        .replace("%totem_id%", totemId);
                if (command.isEmpty()) {
                    continue;
                }
                try {
                    Bukkit.dispatchCommand(sender, command);
                } catch (Throwable throwable) {
                    getLogger().warning("执行" + phase + "命令失败: /" + command + " (" + throwable.getMessage() + ")");
                }
            }
        });
    }

    // ------------------------------------------------------------------
    // 命令
    // ------------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "nhtotem":
                return handleMainCommand(sender, args);
            case "qcyreload":
                return reload(sender);
            case "gettotem":
                return giveTotem(sender, args);
            default:
                return false;
        }
    }

    private boolean handleMainCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "reload":
                if (!hasPermission(sender, "nhtotem.command.reload",
                        "nowheretotem.command.reload", "qcytotem.command.reload")) {
                    noPermission(sender);
                    return true;
                }
                return reload(sender);
            case "give":
                if (!hasPermission(sender, "nhtotem.command.give",
                        "nowheretotem.command.give", "qcytotem.command.gettotem")) {
                    noPermission(sender);
                    return true;
                }
                return giveTotem(sender, Arrays.copyOfRange(args, 1, args.length));
            case "list":
                sendTotemList(sender);
                return true;
            case "help":
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(PREFIX + "\u00a77NHTotem \u00a78- \u00a77NowHere Studios");
        sender.sendMessage(PREFIX + "\u00a7b/nhtotem reload \u00a78- \u00a77重载配置");
        sender.sendMessage(PREFIX + "\u00a7b/nhtotem give <图腾> [玩家] \u00a78- \u00a77发放图腾");
        sender.sendMessage(PREFIX + "\u00a7b/nhtotem list \u00a78- \u00a77列出图腾");
        sender.sendMessage(PREFIX + "\u00a7b/nhtotem help \u00a78- \u00a77显示帮助");
    }

    private void noPermission(CommandSender sender) {
        sender.sendMessage(PREFIX + "\u00a7c没有权限。");
    }

    private static boolean hasPermission(CommandSender sender, String... nodes) {
        for (String node : nodes) {
            if (sender.hasPermission(node)) {
                return true;
            }
        }
        return false;
    }

    private boolean reload(CommandSender sender) {
        try {
            boolean clean = loadTotems();
            if (clean) {
                sender.sendMessage(PREFIX + "\u00a7a配置重载完成，共 " + totems.size() + " 个图腾。");
            } else {
                sender.sendMessage(PREFIX + "\u00a7e配置已重载，但存在错误，请查看控制台。");
            }
        } catch (Throwable throwable) {
            getLogger().severe("重载配置失败: " + throwable);
            sender.sendMessage(PREFIX + "\u00a7c重载失败: " + throwable.getMessage());
        }
        return true;
    }

    private boolean giveTotem(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendTotemList(sender);
            sender.sendMessage(PREFIX + "\u00a77用法: /nhtotem give <图腾名称> [玩家]");
            return true;
        }

        // Totem names may contain spaces (for example "Totem of Gold").
        // Treat the last argument as the target only when it is an online player,
        // and join the remaining arguments as the totem name.
        Player target = null;
        int nameEnd = args.length;
        if (args.length >= 2) {
            Player candidate = Bukkit.getPlayerExact(args[args.length - 1]);
            if (candidate != null) {
                target = candidate;
                nameEnd = args.length - 1;
            }
        }

        String requestedName = String.join(" ", Arrays.copyOfRange(args, 0, nameEnd));
        Totem totem = findByName(requestedName);
        if (totem == null) {
            sender.sendMessage(PREFIX + "\u00a7c找不到图腾: " + requestedName);
            sendTotemList(sender);
            return true;
        }

        if (target == null) {
            if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage(PREFIX + "\u00a7c控制台用法: /nhtotem give <图腾名称> <玩家>");
                return true;
            }
        }

        giveItem(target, totem);
        sender.sendMessage(PREFIX + "\u00a7a已给予 " + target.getName() + " 图腾: " + totem.getDisplayName());
        return true;
    }

    private void giveItem(Player player, Totem totem) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(totem.createItem());
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    private Totem findByName(String name) {
        String plainName = Text.strip(name);
        for (Totem totem : totems) {
            if (totem.getPlainName().equalsIgnoreCase(plainName)
                    || totem.getId().equalsIgnoreCase(name)
                    || totem.getTagId().equalsIgnoreCase(name)) {
                return totem;
            }
        }
        return null;
    }

    private void sendTotemList(CommandSender sender) {
        if (totems.isEmpty()) {
            sender.sendMessage(PREFIX + "\u00a77当前没有可用的图腾。");
            return;
        }
        List<String> names = new ArrayList<>(totems.size());
        for (Totem totem : totems) {
            names.add(totem.getDisplayName());
        }
        sender.sendMessage(PREFIX + "\u00a77可用图腾: " + String.join("\u00a77, ", names));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("gettotem")) {
            if (args.length == 1) {
                return totemNames(args[0]);
            }
            if (args.length == 2) {
                return playerNames(args[1]);
            }
            return Collections.emptyList();
        }
        if (name.equals("nhtotem")) {
            if (args.length == 1) {
                return filter(Arrays.asList("reload", "give", "list", "help"), args[0]);
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
                return totemNames(args[1]);
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
                return playerNames(args[2]);
            }
        }
        return Collections.emptyList();
    }

    private List<String> totemNames(String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (Totem totem : totems) {
            if (totem.getPlainName().toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                result.add(totem.getPlainName());
            }
        }
        return result;
    }

    private static List<String> playerNames(String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                result.add(player.getName());
            }
        }
        return result;
    }

    private static List<String> filter(List<String> values, String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.startsWith(lowerPrefix)) {
                result.add(value);
            }
        }
        return result;
    }

    // ------------------------------------------------------------------
    // 内部类
    // ------------------------------------------------------------------

    private static final class Match {
        private final Totem totem;
        private final InventoryUtil.Slot slot;

        private Match(Totem totem, InventoryUtil.Slot slot) {
            this.totem = totem;
            this.slot = slot;
        }
    }

    private static final class PendingRespawn {
        private final Totem totem;

        private PendingRespawn(Totem totem) {
            this.totem = totem;
        }
    }
}
