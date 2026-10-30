package com.luckycrates.gui;

import com.luckycrates.LuckyCratesPlugin;
import com.luckycrates.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class CrateEditorGUI implements InventoryHolder {
    private final LuckyCratesPlugin plugin;
    private final Inventory inventory;
    private final String crateName;

    public CrateEditorGUI(LuckyCratesPlugin plugin, String crateName) {
        this.plugin = plugin;
        this.crateName = crateName;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtil.color("&8» &6&lEditor: " + crateName));

        Map<Integer, ItemStack> rewards = plugin.getCrateManager().getSlotRewards();
        for (Map.Entry<Integer, ItemStack> entry : rewards.entrySet()) {
            if (entry.getKey() < 54 && entry.getValue() != null) {
                inventory.setItem(entry.getKey(), entry.getValue().clone());
            }
        }
    }

    public void open(Player player) {
        player.openInventory(inventory);
        player.sendMessage(ColorUtil.color(plugin.getConfig().getString("messages.prefix", "") +
                "&6Opened Crate Editor for &e" + crateName + "&6. Items will automatically format with fancy fonts & slot rarity!"));
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.0f);
    }

    public void handleClose(Player player) {
        Map<Integer, ItemStack> newRewards = new HashMap<>();
        for (int i = 0; i < 54; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                double chance = plugin.getCrateManager().calculatePercentage(i);
                ItemStack styled = plugin.getCrateManager().applyRarityLore(item, i, chance, crateName);
                newRewards.put(i, styled);
            }
        }

        plugin.getCrateManager().setSlotRewards(newRewards);

        player.sendMessage(ColorUtil.color(plugin.getConfig().getString("messages.prefix", "") +
                "&aSaved &e" + newRewards.size() + " &arewards for &e" + crateName + "&a! Slot 0 is set to 0.1% Jackpot."));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.2f);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
