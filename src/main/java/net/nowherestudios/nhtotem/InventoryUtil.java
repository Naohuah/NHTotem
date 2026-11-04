package net.nowherestudios.nhtotem;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一包装玩家物品栏的各个槽位。
 *
 * <p>旧版用 {@code for (ItemStack stack : player.getInventory())} 和
 * {@code inventory.remove(stack)} 处理物品：前者是否包含盔甲/副手取决于服务端实现，
 * 后者按“相似物品”删除，可能删掉错误的槽位。这里显式拆分主背包、盔甲、副手。</p>
 */
final class InventoryUtil {

    private InventoryUtil() {
    }

    interface Slot {
        ItemStack get();

        void set(ItemStack stack);
    }

    /**
     * 返回玩家全部可掉落槽位：主背包 + 4 件盔甲 + 副手。
     */
    static List<Slot> slots(PlayerInventory inventory) {
        List<Slot> slots = new ArrayList<>(41);
        int storageSize = inventory.getStorageContents().length;
        for (int i = 0; i < storageSize; i++) {
            slots.add(new StorageSlot(inventory, i));
        }
        int armorSize = inventory.getArmorContents().length;
        for (int i = 0; i < armorSize; i++) {
            slots.add(new ArmorSlot(inventory, i));
        }
        slots.add(new OffHandSlot(inventory));
        return slots;
    }

    private static final class StorageSlot implements Slot {
        private final PlayerInventory inventory;
        private final int index;

        private StorageSlot(PlayerInventory inventory, int index) {
            this.inventory = inventory;
            this.index = index;
        }

        @Override
        public ItemStack get() {
            return inventory.getItem(index);
        }

        @Override
        public void set(ItemStack stack) {
            inventory.setItem(index, stack);
        }
    }

    private static final class ArmorSlot implements Slot {
        private final PlayerInventory inventory;
        private final int index;

        private ArmorSlot(PlayerInventory inventory, int index) {
            this.inventory = inventory;
            this.index = index;
        }

        @Override
        public ItemStack get() {
            ItemStack[] armor = inventory.getArmorContents();
            return index < armor.length ? armor[index] : null;
        }

        @Override
        public void set(ItemStack stack) {
            ItemStack[] armor = inventory.getArmorContents();
            if (index >= armor.length) {
                return;
            }
            armor[index] = stack;
            inventory.setArmorContents(armor);
        }
    }

    private static final class OffHandSlot implements Slot {
        private final PlayerInventory inventory;

        private OffHandSlot(PlayerInventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public ItemStack get() {
            return inventory.getItemInOffHand();
        }

        @Override
        public void set(ItemStack stack) {
            inventory.setItemInOffHand(stack);
        }
    }
}
