package net.nowherestudios.nhtotem;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 在真实的 Paper 26.3 API 上验证图腾的死亡逻辑。
 *
 * <p>MockBukkit 4.x 的“按 Class 加载”要求插件来自 jar，因此这里把 target/classes
 * 重新打成一个临时 jar（替换 config.yml）再加载，等价于服务器里加载插件的过程。</p>
 */
class NHTotemTest {

    private static final Path CLASSES = Paths.get(System.getProperty("basedir", "."), "target", "classes");
    private static final Path TMP = Paths.get(System.getProperty("basedir", "."), "target", "test-tmp");

    private ServerMock server;
    private Plugin plugin;

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // ------------------------------------------------------------------
    // 基础功能
    // ------------------------------------------------------------------

    @Test
    void pluginLoadsAndReloads() throws IOException {
        load(defaultConfig(0.0, false, "[]"));
        assertTrue(plugin.isEnabled(), "插件应成功启用");

        assertTrue(server.dispatchCommand(server.getConsoleSender(), "qcyreload"));
        assertTrue(consoleSaid("配置重载完成，共 1 个图腾"));
    }

    @Test
    void mainCommandWorks() throws IOException {
        load(defaultConfig(0.0, false, "[]"));
        PlayerMock player = server.addPlayer("Tester");

        assertTrue(server.dispatchCommand(server.getConsoleSender(), "nhtotem list"));
        assertTrue(consoleSaid("TestTotem"));

        assertTrue(server.dispatchCommand(server.getConsoleSender(),
                "nhtotem give TestTotem " + player.getName()));
        assertNotNull(player.getInventory().getItem(0), "主命令 give 应发放图腾");

        // 旧品牌命令作为别名继续可用
        assertTrue(server.dispatchCommand(server.getConsoleSender(), "nowheretotem list"));
    }

    @Test
    void keepLevelAlsoPreventsExperienceDrops() throws IOException {
        load(defaultConfig(0.0, false, "[]"));
        PlayerMock player = server.addPlayer("Tester");
        giveTotem(player);

        PlayerDeathEvent event = new PlayerDeathEvent(
                player,
                DamageSource.builder(DamageType.GENERIC).build(),
                new ArrayList<>(),
                100,
                "Tester died");
        server.getPluginManager().callEvent(event);

        assertTrue(event.getKeepLevel());
        assertEquals(0, event.getDroppedExp(),
                "setKeepLevel 不会阻止经验掉落，必须显式把 droppedExp 清零");
    }

    @Test
    void deathClearsVanillaDropsToPreventDuplication() throws IOException {
        load(defaultConfig(0.0, false, "[]"));
        PlayerMock player = server.addPlayer("Tester");
        giveTotem(player);

        List<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(Material.DIAMOND, 5));
        PlayerDeathEvent event = new PlayerDeathEvent(
                player,
                DamageSource.builder(DamageType.GENERIC).build(),
                drops,
                0,
                "Tester died");
        server.getPluginManager().callEvent(event);

        assertTrue(event.getKeepInventory());
        assertTrue(event.getDrops().isEmpty(),
                "保留物品栏时必须清空原版掉落列表，否则会复制物品");
    }

    @Test
    void legacyQcyTotemPdcKeyStillMatches() throws IOException {
        assertLegacyPdcKeyMatches("qcytotem:totem_id");
    }

    @Test
    void legacyNowHereTotemPdcKeyStillMatches() throws IOException {
        assertLegacyPdcKeyMatches("nowheretotem:totem_id");
    }

    private void assertLegacyPdcKeyMatches(String rawKey) throws IOException {
        load(defaultConfig(0.0, false, "[]"));
        PlayerMock player = server.addPlayer("Tester");
        giveTotem(player);

        ItemStack item = player.getInventory().getItem(0);
        NamespacedKey newKey = new NamespacedKey(plugin, "totem_id");
        NamespacedKey legacyKey = NamespacedKey.fromString(rawKey);
        assertNotNull(legacyKey);

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().remove(newKey);
        meta.getPersistentDataContainer().set(legacyKey, PersistentDataType.STRING, "&eTestTotem");
        item.setItemMeta(meta);
        player.getInventory().setItem(0, item);

        PlayerDeathEvent event = death(player);

        assertTrue(event.getKeepInventory());
        assertNull(player.getInventory().getItem(0), "旧版 PDC 键 " + rawKey + " 也应该被识别");
    }

    @Test
    void getTotemCommandGivesTaggedItem() throws IOException {
        load(defaultConfig(0.0, false, "[]"));
        PlayerMock player = server.addPlayer("Tester");

        assertTrue(server.dispatchCommand(server.getConsoleSender(), "gettotem TestTotem " + player.getName()));
        ItemStack given = player.getInventory().getItem(0);
        assertNotNull(given, "图腾应进入玩家物品栏");
        assertEquals(Material.PAPER, given.getType());
        assertEquals("\u00a7eTestTotem", given.getItemMeta().getDisplayName());
        assertFalse(given.getItemMeta().getPersistentDataContainer().isEmpty(),
                "新图腾应带 PDC 标记");
    }

    // ------------------------------------------------------------------
    // 死亡逻辑
    // ------------------------------------------------------------------

    @Test
    void deathConsumesTotemAndKeepsInventory() throws IOException {
        load(defaultConfig(0.0, false, "[]"));
        PlayerMock player = server.addPlayer("Tester");
        giveTotem(player);
        player.getInventory().setItem(1, new ItemStack(Material.DIAMOND, 5));

        PlayerDeathEvent event = death(player);

        assertTrue(event.getKeepInventory());
        assertTrue(event.getKeepLevel());
        assertNull(player.getInventory().getItem(0), "图腾应被消耗");
        assertNotNull(player.getInventory().getItem(1), "其他物品应保留");
        assertEquals(5, player.getInventory().getItem(1).getAmount());
        assertTrue(player.getInventory().getItemInMainHand().getType().isAir(),
                "动画临时放到主手的图腾必须还原");
    }

    @Test
    void animationSwapRestoresMainHandWhenTotemIsInAnotherSlot() throws IOException {
        load(defaultConfig(0.0, false, "[]"));
        PlayerMock player = server.addPlayer("Tester");
        giveTotem(player);
        ItemStack totem = player.getInventory().getItem(0);
        player.getInventory().setItem(0, null);
        player.getInventory().setItem(5, totem);
        player.getInventory().setItemInMainHand(new ItemStack(Material.IRON_SWORD));

        PlayerDeathEvent event = death(player);

        assertTrue(event.getKeepInventory());
        assertNull(player.getInventory().getItem(5), "副槽图腾应被消耗");
        assertEquals(Material.IRON_SWORD, player.getInventory().getItemInMainHand().getType(),
                "动画结束后必须还原主手武器");
        assertEquals(1, player.getInventory().getItemInMainHand().getAmount());
    }

    @Test
    void giveTotemNameWithSpaces() throws IOException {
        load("""
                totems:
                  "&eTotem of Gold":
                    material: PAPER
                    lore: []
                    drop-probability: 0.0
                """);
        PlayerMock player = server.addPlayer("Tester");

        assertTrue(server.dispatchCommand(server.getConsoleSender(),
                "nhtotem give Totem of Gold Tester"));
        assertNotNull(player.getInventory().getItem(0),
                "带空格的图腾名应能完整解析，而不是只取第一个单词");
    }

    @Test
    void duplicateStableIdIsRejected() throws IOException {
        load("""
                totems:
                  "&eA":
                    id: "same_id"
                    material: PAPER
                    lore: []
                    drop-probability: 0.0
                  "&eB":
                    id: "same_id"
                    material: PAPER
                    lore: []
                    drop-probability: 0.0
                """);

        // Drain startup/config warning messages before checking the list output.
        consoleSaid("__never__");
        assertTrue(server.dispatchCommand(server.getConsoleSender(), "nhtotem list"));
        assertTrue(consoleSaid("A"), "第一个使用该 id 的图腾应保留");
        assertFalse(consoleSaid("B"), "重复 id 的图腾应被拒绝加载");
    }

    @Test
    void nanDropProbabilityFallsBackToNoDrops() throws IOException {
        load("""
                settings:
                  totem-effect: false
                totems:
                  "&eTestTotem":
                    material: PAPER
                    lore: []
                    drop-probability: .nan
                """);
        PlayerMock player = server.addPlayer("Tester");
        giveTotem(player);
        player.getInventory().setItem(1, new ItemStack(Material.DIAMOND, 3));

        death(player);

        assertNotNull(player.getInventory().getItem(1), "NaN 概率应回退为 0.0，不应随机掉落");
        assertEquals(3, player.getInventory().getItem(1).getAmount());
    }

    @Test
    void respawnCommandsRunEvenWhenRespawnWorldIsDisabled() throws IOException {
        load("""
                settings:
                  totem-effect: false
                totems:
                  "&eTestTotem":
                    material: PAPER
                    lore: []
                    drop-probability: 0.0
                    disabled-worlds:
                      - "world_nether"
                    respawn-commands:
                      - "qcyreload"
                """);
        WorldMock nether = server.addSimpleWorld("world_nether");
        WorldMock deathWorld = server.addSimpleWorld("world_death");
        PlayerMock player = server.addPlayer("Tester");
        player.teleport(deathWorld.getSpawnLocation());
        giveTotem(player);

        death(player);
        assertTrue(player.getInventory().getItem(0) == null, "图腾应在死亡世界被消耗");        PlayerRespawnEvent respawn = new PlayerRespawnEvent(player, nether.getSpawnLocation(), false);
        server.getPluginManager().callEvent(respawn);
        server.getScheduler().performOneTick();

        assertTrue(consoleSaid("配置重载完成"),
                "死亡世界生效时，即使重生世界被禁用，重生命令也应执行");
    }

    @Test
    void legacyTotemWithoutPdcStillMatches() throws IOException {
        load(defaultConfig(0.0, false, "[]"));
        PlayerMock player = server.addPlayer("Tester");

        // 模拟 1.1 旧版发出的图腾：只有显示名和 Lore，没有 PDC 标记。
        ItemStack legacy = new ItemStack(Material.PAPER);
        ItemMeta meta = legacy.getItemMeta();
        meta.setDisplayName("\u00a7eTestTotem");
        meta.setLore(List.of("\u00a77test"));
        legacy.setItemMeta(meta);

        player.getInventory().setItem(4, legacy);
        PlayerDeathEvent event = death(player);

        assertTrue(event.getKeepInventory());
        assertNull(player.getInventory().getItem(4), "旧版图腾也应被识别并消耗");
    }

    @Test
    void totemInOffHandIsRecognised() throws IOException {
        load(defaultConfig(0.0, false, "[]"));
        PlayerMock player = server.addPlayer("Tester");
        giveTotem(player);

        ItemStack totem = player.getInventory().getItem(0);
        player.getInventory().setItem(0, null);
        player.getInventory().setItemInOffHand(totem);

        PlayerDeathEvent event = death(player);

        assertTrue(event.getKeepInventory());
        assertTrue(player.getInventory().getItemInOffHand().getType().isAir(), "副手图腾应被消耗");
    }

    @Test
    void totemInDisabledWorldDoesNothing() throws IOException {
        load(defaultConfig(0.0, false, "[\"world_nether\"]"));
        WorldMock nether = server.addSimpleWorld("world_nether");
        PlayerMock player = server.addPlayer("Tester");
        player.teleport(nether.getSpawnLocation());
        giveTotem(player);

        PlayerDeathEvent event = death(player);

        assertFalse(event.getKeepInventory());
        assertNotNull(player.getInventory().getItem(0), "禁用世界不应该消耗图腾");
    }

    @Test
    void clearCurseRemovesVanishingCurseItem() throws IOException {
        load(defaultConfig(0.0, true, "[]"));
        PlayerMock player = server.addPlayer("Tester");
        giveTotem(player);

        ItemStack cursed = new ItemStack(Material.DIAMOND_SWORD);
        cursed.addUnsafeEnchantment(Enchantment.VANISHING_CURSE, 1);
        player.getInventory().setItem(1, cursed);

        PlayerDeathEvent event = death(player);

        assertTrue(event.getKeepInventory());
        assertNull(player.getInventory().getItem(1), "消失诅咒物品应被清除");
    }

    @Test
    void randomDropRemovesItemsAndDropsThem() throws IOException {
        load(defaultConfig(1.0, false, "[]"));
        PlayerMock player = server.addPlayer("Tester");
        giveTotem(player);
        player.getInventory().setItem(1, new ItemStack(Material.DIAMOND, 3));

        death(player);

        assertNull(player.getInventory().getItem(1), "100% 掉落概率下物品应离开物品栏");
        List<Item> dropped = new ArrayList<>(player.getWorld().getEntitiesByClass(Item.class));
        assertTrue(dropped.stream().anyMatch(item -> item.getItemStack().getType() == Material.DIAMOND),
                "物品应该掉落在世界上，而不是被删除");
    }

    @Test
    void randomDropDeleteModeDoesNotDropItems() throws IOException {
        load("""
                settings:
                  drop-mode: delete
                totems:
                  "&eTestTotem":
                    material: PAPER
                    lore:
                      - "&7test"
                    drop-probability: 1.0
                """);
        PlayerMock player = server.addPlayer("Tester");
        giveTotem(player);
        player.getInventory().setItem(1, new ItemStack(Material.DIAMOND, 3));

        death(player);

        assertNull(player.getInventory().getItem(1));
        assertTrue(player.getWorld().getEntitiesByClass(Item.class).isEmpty(), "delete 模式不应生成掉落物");
    }

    @Test
    void stableIdIsStoredInPdc() throws IOException {
        load("""
                totems:
                  "&eTestTotem":
                    id: "stable_test_id"
                    material: PAPER
                    lore: []
                    drop-probability: 0.0
                """);
        PlayerMock player = server.addPlayer("Tester");
        giveTotem(player);

        ItemStack item = player.getInventory().getItem(0);
        assertNotNull(item);
        NamespacedKey key = new NamespacedKey(plugin, "totem_id");
        String stored = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        assertEquals("stable_test_id", stored, "PDC 里应该写入配置的稳定 id");
    }

    @Test
    void missingCraftEngineFallsBackToPaper() throws IOException {
        load("""
                totems:
                  "&eTestTotem":
                    material: "CE:test:totem"
                    lore: []
                    drop-probability: 0.0
                """);
        PlayerMock player = server.addPlayer("Tester");
        giveTotem(player);
        ItemStack item = player.getInventory().getItem(0);
        assertNotNull(item);
        assertEquals(Material.PAPER, item.getType(), "没有 CraftEngine 时应退化为 PAPER");
    }

    @Test
    void missingItemsAdderFallsBackToPaper() throws IOException {
        load("""
                totems:
                  "&eTestTotem":
                    material: "IA:not_installed:totem"
                    lore: []
                    drop-probability: 0.0
                """);
        PlayerMock player = server.addPlayer("Tester");
        giveTotem(player);
        ItemStack item = player.getInventory().getItem(0);
        assertNotNull(item);
        assertEquals(Material.PAPER, item.getType(), "没有 ItemsAdder 时应退化为 PAPER 而不是让插件崩溃");
    }

    @Test
    void invalidMaterialFallsBackToPaper() throws IOException {
        load("""
                totems:
                  "&eTestTotem":
                    material: "NOT_A_REAL_MATERIAL"
                    lore: []
                    drop-probability: 0.0
                """);
        PlayerMock player = server.addPlayer("Tester");
        giveTotem(player);
        ItemStack item = player.getInventory().getItem(0);
        assertNotNull(item);
        assertEquals(Material.PAPER, item.getType());
    }

    @Test
    void respawnCommandsRunOnRespawn() throws IOException {        load("""
                settings:
                  totem-effect: false
                totems:
                  "&eTestTotem":
                    material: PAPER
                    lore:
                      - "&7test"
                    drop-probability: 0.0
                    death-commands:
                      - "qcyreload"
                    respawn-commands:
                      - "qcyreload"
                """);
        PlayerMock player = server.addPlayer("Tester");
        giveTotem(player);

        death(player);
        server.getScheduler().performOneTick();
        player.respawn();
        server.getScheduler().performOneTick();

        assertTrue(consoleSaid("配置重载完成"), "死亡/重生命令应该被执行");
    }

    // ------------------------------------------------------------------
    // 工具方法
    // ------------------------------------------------------------------

    private void load(String yaml) throws IOException {
        server = MockBukkit.mock();
        Files.createDirectories(TMP);
        // 插件 jar 会被 URLClassLoader 一直占用到 JVM 退出，因此不能用 @TempDir（Windows 上删不掉）。
        Path jar = TMP.resolve("NHTotem-test-" + System.nanoTime() + ".jar");
        createPluginJar(jar, yaml);
        plugin = MockBukkit.loadJar(jar.toFile());
        server.getPluginManager().enablePlugin(plugin);
    }

    private static void createPluginJar(Path jar, String configYaml) throws IOException {
        try (OutputStream fileOut = Files.newOutputStream(jar);
             JarOutputStream out = new JarOutputStream(fileOut);
             Stream<Path> paths = Files.walk(CLASSES)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                String name = CLASSES.relativize(path).toString().replace('\\', '/');
                if ("config.yml".equals(name)) {
                    continue;
                }
                out.putNextEntry(new JarEntry(name));
                Files.copy(path, out);
                out.closeEntry();
            }
            out.putNextEntry(new JarEntry("config.yml"));
            out.write(configYaml.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
    }

    private static String defaultConfig(double dropProbability, boolean clearCurse, String disabledWorlds) {
        return """
                settings:
                  keep-level: true
                  totem-effect: false
                  commands-as-console: true
                  drop-mode: drop
                totems:
                  "&eTestTotem":
                    material: PAPER
                    lore:
                      - "&7test"
                    drop-probability: %s
                    clear-curse: %s
                    disabled-worlds: %s
                """.formatted(dropProbability, clearCurse, disabledWorlds);
    }

    private void giveTotem(PlayerMock player) {
        assertTrue(server.dispatchCommand(server.getConsoleSender(),
                "gettotem TestTotem " + player.getName()), "gettotem 命令应执行成功");
    }

    private boolean consoleSaid(String needle) {
        boolean found = false;
        String message;
        while ((message = server.getConsoleSender().nextMessage()) != null) {
            if (message.contains(needle)) {
                found = true;
            }
        }
        return found;
    }

    private PlayerDeathEvent death(PlayerMock player) {
        PlayerDeathEvent event = new PlayerDeathEvent(
                player,
                DamageSource.builder(DamageType.GENERIC).build(),
                new ArrayList<>(),
                0,
                "Tester died");
        server.getPluginManager().callEvent(event);
        return event;
    }
}
