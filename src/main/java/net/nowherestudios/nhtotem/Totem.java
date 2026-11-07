package net.nowherestudios.nhtotem;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 一种图腾的配置模型。
 *
 * <p>旧版让 {@code Totem} 继承 {@link ItemStack}，把“配置模型”和“物品实例”混在一起，
 * 既无法表达没有物品的情况，也让比较逻辑变得很脆弱。2.0 把它拆开：
 * 配置只生成一个模板物品，实际发放时克隆。</p>
 */
public final class Totem {

    private final String id;
    private final String tagId;
    private final String displayName;
    private final String plainName;
    private final String materialName;
    private final ItemStack template;
    private final ItemStack tagged;
    private final NamespacedKey key;
    private final List<NamespacedKey> legacyKeys;
    private final double dropProbability;
    private final boolean clearCurse;
    private final Set<String> disabledWorlds;
    private final List<String> deathCommands;
    private final List<String> respawnCommands;

    private Totem(String id,
                  String tagId,
                  String displayName,
                  String plainName,
                  String materialName,
                  ItemStack template,
                  ItemStack tagged,
                  NamespacedKey key,
                  List<NamespacedKey> legacyKeys,
                  double dropProbability,
                  boolean clearCurse,
                  Set<String> disabledWorlds,
                  List<String> deathCommands,
                  List<String> respawnCommands) {
        this.id = id;
        this.tagId = tagId;
        this.displayName = displayName;
        this.plainName = plainName;
        this.materialName = materialName;
        this.template = template;
        this.tagged = tagged;
        this.key = key;
        this.legacyKeys = legacyKeys;
        this.dropProbability = dropProbability;
        this.clearCurse = clearCurse;
        this.disabledWorlds = disabledWorlds;
        this.deathCommands = deathCommands;
        this.respawnCommands = respawnCommands;
    }

    /**
     * 从配置段解析图腾。解析失败时抛出异常，由调用方记录并跳过该图腾。
     */
    public static Totem parse(String id,
                              ConfigurationSection section,
                              NamespacedKey key,
                              List<NamespacedKey> legacyKeys,
                              Plugin plugin) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("图腾名称为空");
        }

        String displayName = Text.color(id);
        String plainName = ChatColor.stripColor(displayName);

        // 可选 id：给图腾一个不随显示名变化的稳定标识，改名后旧物品仍能识别。
        String tagId = section.getString("id", id);
        if (tagId == null || tagId.trim().isEmpty()) {
            tagId = id;
        } else {
            tagId = tagId.trim();
        }

        String materialName = section.getString("material", "PAPER");
        if (materialName == null) {
            materialName = "PAPER";
        }
        materialName = materialName.trim();
        ItemStack base = createBaseItem(materialName, plugin);

        List<String> lore = new ArrayList<>();
        for (String line : section.getStringList("lore")) {
            lore.add(Text.color(line));
        }

        ItemMeta meta = base.getItemMeta();
        if (meta == null) {
            throw new IllegalArgumentException("材质 " + materialName + " 无法设置物品信息");
        }
        meta.setDisplayName(displayName);
        meta.setLore(lore);
        base.setItemMeta(meta);

        // 旧版图腾物品没有 PDC 标记，保留一份完全相同的模板用于向后兼容。
        ItemStack template = base.clone();

        // 新发放的图腾带 PDC 标记；配置了稳定 id 时改名/改 Lore 后仍能被识别。
        ItemStack tagged = base.clone();
        ItemMeta taggedMeta = tagged.getItemMeta();
        if (taggedMeta == null) {
            throw new IllegalArgumentException("无法给图腾写入 PersistentDataContainer");
        }
        taggedMeta.getPersistentDataContainer().set(key, PersistentDataType.STRING, tagId);
        // 同时写入旧品牌的键，升级/回滚到 NowHereTotem 2.1 或 QcyTotem 2.0 都不会丢物品。
        for (NamespacedKey legacyKey : legacyKeys) {
            taggedMeta.getPersistentDataContainer().set(legacyKey, PersistentDataType.STRING, tagId);
        }
        tagged.setItemMeta(taggedMeta);

        double probability = section.getDouble("drop-probability", 0.0D);
        if (!Double.isFinite(probability) || probability < 0.0D || probability > 1.0D) {
            plugin.getLogger().warning("图腾 '" + id + "' 的 drop-probability=" + probability
                    + " 无效，已自动修正到 0.0~1.0 范围。");
            probability = Double.isFinite(probability)
                    ? Math.max(0.0D, Math.min(1.0D, probability))
                    : 0.0D;
        }

        Set<String> worlds = new HashSet<>();
        for (String world : section.getStringList("disabled-worlds")) {
            if (world != null && !world.trim().isEmpty()) {
                worlds.add(world.trim().toLowerCase(Locale.ROOT));
            }
        }

        return new Totem(
                id,
                tagId,
                displayName,
                plainName,
                materialName,
                template,
                tagged,
                key,
                legacyKeys,
                probability,
                section.getBoolean("clear-curse", false),
                Collections.unmodifiableSet(worlds),
                Collections.unmodifiableList(new ArrayList<>(section.getStringList("death-commands"))),
                Collections.unmodifiableList(new ArrayList<>(section.getStringList("respawn-commands"))));
    }

    private static ItemStack createBaseItem(String rawMaterial, Plugin plugin) {
        String materialName = rawMaterial == null ? "" : rawMaterial.trim();
        if (materialName.isEmpty()) {
            materialName = "PAPER";
        }

        if (materialName.regionMatches(true, 0, "CE:", 0, 3)) {
            String craftEngineId = materialName.substring(3).trim();
            ItemStack custom = CraftEngineHook.getItem(craftEngineId, plugin);
            if (custom != null) {
                return custom;
            }
            CraftEngineHook.markMissing(craftEngineId);
            return new ItemStack(Material.PAPER);
        }

        if (materialName.regionMatches(true, 0, "IA:", 0, 3)) {
            String itemsAdderId = materialName.substring(3).trim();
            ItemStack custom = ItemsAdderHook.getItem(itemsAdderId, plugin);
            if (custom != null) {
                return custom;
            }
            plugin.getLogger().warning("无法读取 ItemsAdder 物品 '" + itemsAdderId
                    + "'，图腾将退化为 PAPER。");
            return new ItemStack(Material.PAPER);
        }

        Material material = Material.matchMaterial(materialName);
        if (material == null || material.isAir()) {
            plugin.getLogger().warning("未知的材质 '" + materialName + "'，图腾将退化为 PAPER。");
            material = Material.PAPER;
        }
        return new ItemStack(material);
    }

    /**
     * 判断物品是否是本图腾。
     *
     * <p>优先读取 PDC 标记；没有标记的物品（1.1 旧版发放的图腾）则按材质 + 显示名 + Lore 精确比较。</p>
     */
    public boolean matches(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String stored = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
            for (int i = 0; stored == null && i < legacyKeys.size(); i++) {
                stored = meta.getPersistentDataContainer().get(legacyKeys.get(i), PersistentDataType.STRING);
            }
            if (stored != null) {
                return stored.equals(tagId);
            }
        }
        return template.isSimilar(stack);
    }

    public boolean isDisabledIn(String worldName) {
        return worldName != null && disabledWorlds.contains(worldName.toLowerCase(Locale.ROOT));
    }

    /**
     * 生成一个带 PDC 标记的图腾物品。
     */
    public ItemStack createItem() {
        return tagged.clone();
    }

    public String getId() {
        return id;
    }

    /**
     * 写入 PDC 的稳定标识（配置里的 {@code id}，未配置时等于配置名）。
     */
    public String getTagId() {
        return tagId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPlainName() {
        return plainName;
    }

    public String getMaterialName() {
        return materialName;
    }

    public double getDropProbability() {
        return dropProbability;
    }

    public boolean shouldClearCurse() {
        return clearCurse;
    }

    public List<String> getDeathCommands() {
        return deathCommands;
    }

    public List<String> getRespawnCommands() {
        return respawnCommands;
    }
}
