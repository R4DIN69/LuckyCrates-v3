package com.luckycrates.manager;

import com.luckycrates.LuckyCratesPlugin;
import com.luckycrates.util.ColorUtil;
import com.luckycrates.util.FontUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.EndRod;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class CrateManager {
    private final LuckyCratesPlugin plugin;
    private final Set<Location> crateLocations = new HashSet<>();
    private final Map<Location, String> crateNames = new HashMap<>();
    private final Map<Integer, ItemStack> slotRewards = new HashMap<>();
    private File cratesFile;
    private FileConfiguration cratesConfig;

    public CrateManager(LuckyCratesPlugin plugin) {
        this.plugin = plugin;
        loadCrates();
    }

    public void loadCrates() {
        crateLocations.clear();
        crateNames.clear();
        slotRewards.clear();

        cratesFile = new File(plugin.getDataFolder(), "crates.yml");
        if (!cratesFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                cratesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create crates.yml: " + e.getMessage());
            }
        }
        cratesConfig = YamlConfiguration.loadConfiguration(cratesFile);

        List<String> rawLocs = cratesConfig.getStringList("locations");
        for (String raw : rawLocs) {
            Location loc = parseLocation(raw);
            if (loc != null) {
                crateLocations.add(loc);
                String name = cratesConfig.getString("names." + raw, "&6&lLucky Crate");
                crateNames.put(loc, name);
            }
        }

        if (cratesConfig.isConfigurationSection("slot-rewards")) {
            for (String key : cratesConfig.getConfigurationSection("slot-rewards").getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    ItemStack item = cratesConfig.getItemStack("slot-rewards." + key);
                    if (item != null && item.getType() != Material.AIR) {
                        slotRewards.put(slot, item);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        if (slotRewards.isEmpty()) {
            populateDefaultRewards();
            saveCrates();
        }
    }

    private void populateDefaultRewards() {
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta sm = sword.getItemMeta();
        if (sm != null) {
            sm.setDisplayName(ColorUtil.color("&6&l★ " + FontUtil.toSmallCaps("Lucky Crate") + " ★ &e" + FontUtil.toSmallCaps("Netherite Sword")));
            sm.addEnchant(Enchantment.SHARPNESS, 5, true);
            sm.addEnchant(Enchantment.UNBREAKING, 3, true);
            sword.setItemMeta(sm);
        }
        slotRewards.put(0, applyRarityLore(sword, 0, 0.1, "&6&lLucky Crate"));

        slotRewards.put(1, applyRarityLore(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 3), 1, 0.5, "&6&lLucky Crate"));
        slotRewards.put(2, applyRarityLore(new ItemStack(Material.NETHERITE_INGOT, 4), 2, 1.2, "&6&lLucky Crate"));
        slotRewards.put(3, applyRarityLore(new ItemStack(Material.TOTEM_OF_UNDYING, 2), 3, 2.5, "&6&lLucky Crate"));
        slotRewards.put(4, applyRarityLore(new ItemStack(Material.DIAMOND_BLOCK, 4), 4, 4.5, "&6&lLucky Crate"));
        slotRewards.put(5, applyRarityLore(new ItemStack(Material.EMERALD_BLOCK, 8), 5, 8.0, "&6&lLucky Crate"));
        slotRewards.put(6, applyRarityLore(new ItemStack(Material.EXPERIENCE_BOTTLE, 64), 6, 15.0, "&6&lLucky Crate"));
        slotRewards.put(7, applyRarityLore(new ItemStack(Material.GOLDEN_APPLE, 16), 7, 25.0, "&6&lLucky Crate"));
        slotRewards.put(8, applyRarityLore(new ItemStack(Material.ENDER_PEARL, 16), 8, 43.2, "&6&lLucky Crate"));
    }

    public void saveCrates() {
        if (cratesConfig == null || cratesFile == null) return;

        List<String> rawLocs = new ArrayList<>();
        for (Location loc : crateLocations) {
            String raw = serializeLocation(loc);
            rawLocs.add(raw);
            cratesConfig.set("names." + raw, crateNames.getOrDefault(loc, "&6&lLucky Crate"));
        }
        cratesConfig.set("locations", rawLocs);

        cratesConfig.set("slot-rewards", null);
        for (Map.Entry<Integer, ItemStack> entry : slotRewards.entrySet()) {
            cratesConfig.set("slot-rewards." + entry.getKey(), entry.getValue());
        }

        try {
            cratesConfig.save(cratesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save crates.yml: " + e.getMessage());
        }
    }

    /**
     * Places the crate with the requested pedestal structure:
     * Bottom block = END_ROD
     * Top block = SHULKER_BOX (Crate)
     */
    public Location placeCrateStructure(Block targetBlock, String name) {
        Location rodLoc = targetBlock.getLocation().clone().add(0, 1, 0);
        Location shulkerLoc = rodLoc.clone().add(0, 1, 0);

        rodLoc.getBlock().setType(Material.END_ROD);
        shulkerLoc.getBlock().setType(Material.PURPLE_SHULKER_BOX);

        addCrate(shulkerLoc, name);
        return shulkerLoc;
    }

    /**
     * Removes the Shulker box crate and the End Rod below it.
     */
    public boolean removeCrateStructure(Location shulkerLoc) {
        if (!isCrate(shulkerLoc)) return false;

        shulkerLoc.getBlock().setType(Material.AIR);
        Location rodLoc = shulkerLoc.clone().subtract(0, 1, 0);
        if (rodLoc.getBlock().getType() == Material.END_ROD) {
            rodLoc.getBlock().setType(Material.AIR);
        }

        removeCrate(shulkerLoc);
        return true;
    }

    public void addCrate(Location loc, String name) {
        Location blockLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        crateLocations.add(blockLoc);
        crateNames.put(blockLoc, name != null ? name : "&6&lLucky Crate");
        saveCrates();
    }

    public void setCrateName(Location loc, String name) {
        Location blockLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        crateNames.put(blockLoc, name);
        saveCrates();
    }

    public String getCrateName(Location loc) {
        Location blockLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return crateNames.getOrDefault(blockLoc, "&6&lLucky Crate");
    }

    public boolean isCrate(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        Location blockLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return crateLocations.contains(blockLoc);
    }

    public boolean removeCrate(Location loc) {
        Location blockLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        boolean removed = crateLocations.remove(blockLoc);
        crateNames.remove(blockLoc);
        if (removed) saveCrates();
        return removed;
    }

    public double getSlotWeight(int slot) {
        if (slot <= 0) return 0.1; // Slot 0 is ultra rare 0.1 base weight
        return 0.1 + Math.pow(slot, 1.6) * 0.4;
    }

    public double calculatePercentage(int slot) {
        if (!slotRewards.containsKey(slot)) return 0.0;
        double total = 0.0;
        for (int s : slotRewards.keySet()) {
            total += getSlotWeight(s);
        }
        if (total <= 0) return 0.0;
        return (getSlotWeight(slot) / total) * 100.0;
    }

    public ItemStack applyRarityLore(ItemStack original, int slot, double chance, String crateName) {
        if (original == null || original.getType() == Material.AIR) return original;
        ItemStack item = original.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Automatic custom font item name based on crate name
        String rawName = meta.hasDisplayName() ? meta.getDisplayName() : original.getType().name().replace('_', ' ');
        String cleanCrateName = ColorUtil.color(crateName).replaceAll("(?i)&[0-9A-FK-OR]", "").trim();
        String fancyItemName = ColorUtil.color("&6&l★ " + FontUtil.toSmallCaps(cleanCrateName) + " ★ &e" + FontUtil.toSmallCaps(rawName));
        meta.setDisplayName(fancyItemName);

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        // Remove existing rarity line if present
        lore.removeIf(l -> l.contains("Rarity:"));

        String formattedChance = (slot == 0) ? "0.1%" : String.format(Locale.US, "%.1f%%", chance);
        String tier = (slot == 0) ? "§d§l✦ JACKPOT" : (slot < 3 ? "§c§l✦ LEGENDARY" : (slot < 8 ? "§6§l✦ RARE" : "§a§l✦ COMMON"));

        lore.add(0, ColorUtil.color("&8&m------------------------"));
        lore.add(1, ColorUtil.color(tier + " &8| &e" + formattedChance + " Chance"));
        lore.add(2, ColorUtil.color("&7Slot: &f#" + (slot + 1)));
        lore.add(3, ColorUtil.color("&8&m------------------------"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack pickWeightedReward() {
        if (slotRewards.isEmpty()) return new ItemStack(Material.DIRT);

        double totalWeight = 0.0;
        for (int slot : slotRewards.keySet()) totalWeight += getSlotWeight(slot);

        double rand = ThreadLocalRandom.current().nextDouble() * totalWeight;
        double cumulative = 0.0;

        for (Map.Entry<Integer, ItemStack> entry : slotRewards.entrySet()) {
            cumulative += getSlotWeight(entry.getKey());
            if (rand <= cumulative) return entry.getValue().clone();
        }

        return slotRewards.values().iterator().next().clone();
    }

    public List<ItemStack> getAllRewardItems() {
        return new ArrayList<>(slotRewards.values());
    }

    public Map<Integer, ItemStack> getSlotRewards() {
        return Collections.unmodifiableMap(slotRewards);
    }

    public void setSlotRewards(Map<Integer, ItemStack> newRewards) {
        slotRewards.clear();
        for (Map.Entry<Integer, ItemStack> entry : newRewards.entrySet()) {
            if (entry.getValue() != null && entry.getValue().getType() != Material.AIR) {
                slotRewards.put(entry.getKey(), entry.getValue().clone());
            }
        }
        saveCrates();
    }

    public Set<Location> getCrateLocations() {
        return Collections.unmodifiableSet(crateLocations);
    }

    private String serializeLocation(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location parseLocation(String str) {
        String[] parts = str.split(",");
        if (parts.length != 4) return null;
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        try {
            return new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
