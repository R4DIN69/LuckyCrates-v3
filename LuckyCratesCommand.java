package com.luckycrates.command;

import com.luckycrates.LuckyCratesPlugin;
import com.luckycrates.util.ColorUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LuckyCratesCommand implements CommandExecutor, TabCompleter {
    private final LuckyCratesPlugin plugin;

    public LuckyCratesCommand(LuckyCratesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "place" -> {
                if (sender instanceof Player p) p.performCommand("place crate");
                else sender.sendMessage("§cOnly players can run this command.");
            }
            case "name", "rename" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can name crates.");
                    return true;
                }
                if (!player.hasPermission("luckycrates.admin")) {
                    player.sendMessage(ColorUtil.color(plugin.getConfig().getString("messages.no-permission", "&cNo permission.")));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ColorUtil.color(plugin.getConfig().getString("messages.prefix", "") +
                            "&cUsage: /crate " + sub + " <name> (supports & color codes)"));
                    return true;
                }

                Block target = player.getTargetBlockExact(5);
                if (target == null || !plugin.getCrateManager().isCrate(target.getLocation())) {
                    player.sendMessage(ColorUtil.color(plugin.getConfig().getString("messages.prefix", "") +
                            "&cYou must look at a Lucky Crate block within 5 blocks to name it!"));
                    return true;
                }

                // Build full name from arguments
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    sb.append(args[i]).append(i == args.length - 1 ? "" : " ");
                }
                String newName = sb.toString();

                plugin.getCrateManager().setCrateName(target.getLocation(), newName);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.8f);
                player.sendMessage(ColorUtil.color(plugin.getConfig().getString("messages.prefix", "") +
                        "&aCrate name updated to: &r" + newName));
            }
            case "remove", "delete" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can run this command.");
                    return true;
                }
                if (!player.hasPermission("luckycrates.admin")) {
                    player.sendMessage(ColorUtil.color(plugin.getConfig().getString("messages.no-permission", "&cNo permission.")));
                    return true;
                }

                Block target = player.getTargetBlockExact(5);
                if (target == null || !plugin.getCrateManager().isCrate(target.getLocation())) {
                    player.sendMessage(ColorUtil.color(plugin.getConfig().getString("messages.prefix", "") +
                            "&cLook at a Lucky Crate (Shulker Box) within 5 blocks to remove it!"));
                    return true;
                }

                Location loc = target.getLocation();
                plugin.getCrateManager().removeCrateStructure(loc);
                player.getWorld().spawnParticle(Particle.POOF, loc.clone().add(0.5, 0.5, 0.5), 25, 0.3, 0.3, 0.3, 0.05);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 0.8f, 1.2f);
                player.sendMessage(ColorUtil.color(plugin.getConfig().getString("messages.prefix", "") +
                        "&aSuccessfully removed Crate and End Rod pedestal!"));
            }
            case "key", "givekey" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /crate key <player> [amount]");
                    return true;
                }
                int amount = 1;
                if (args.length >= 3) {
                    try {
                        amount = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cInvalid amount.");
                        return true;
                    }
                }
                plugin.getGiveKeyCommand().giveKey(sender, args[1], amount);
            }
            case "reload" -> {
                if (!sender.hasPermission("luckycrates.admin")) return true;
                plugin.reloadConfig();
                plugin.getCrateManager().loadCrates();
                sender.sendMessage(ColorUtil.color(plugin.getConfig().getString("messages.prefix", "") + "&aConfiguration and crates reloaded!"));
            }
            case "list" -> {
                if (!sender.hasPermission("luckycrates.admin")) return true;
                sender.sendMessage(ColorUtil.color(plugin.getConfig().getString("messages.prefix", "") + "&6Registered Lucky Crates:"));
                for (Location loc : plugin.getCrateManager().getCrateLocations()) {
                    String name = plugin.getCrateManager().getCrateName(loc);
                    sender.sendMessage(ColorUtil.color(" §8- " + name + " &7(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")"));
                }
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.color("&8&m----------------&r &6&lLuckyCrates Commands &8&m----------------"));
        sender.sendMessage(ColorUtil.color(" &6/place crate &8- &7Place Shulker Box on End Rod pedestal"));
        sender.sendMessage(ColorUtil.color(" &6/crate name <name> &8- &7Name the targeted crate"));
        sender.sendMessage(ColorUtil.color(" &6/crate rename <name> &8- &7Rename the targeted crate"));
        sender.sendMessage(ColorUtil.color(" &6/crate remove &8- &7Remove targeted Crate and pedestal"));
        sender.sendMessage(ColorUtil.color(" &6/give <player> luckykey <amount> &8- &7Give Lucky Keys"));
        sender.sendMessage(ColorUtil.color(" &6/crate key <player> [amount] &8- &7Alternative key command"));
        sender.sendMessage(ColorUtil.color(" &6/crate reload &8- &7Reload config.yml and crates.yml"));
        sender.sendMessage(ColorUtil.color(" &6/crate list &8- &7View list of placed crates"));
        sender.sendMessage(ColorUtil.color(" &7&oNormal Player: Left-click crate to preview items & chances"));
        sender.sendMessage(ColorUtil.color(" &7&oCreative Admin: Left-click crate to edit prize pool"));
        sender.sendMessage(ColorUtil.color("&8&m----------------------------------------------------"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return Arrays.asList("place", "name", "rename", "remove", "key", "reload", "list", "help");
        return Collections.emptyList();
    }
}
