package com.bergerkiller.bukkit.nolagg.lighting;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.permissions.NoPermissionException;
import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.nolagg.NoLaggComponent;
import com.bergerkiller.bukkit.nolagg.Permission;
import com.bergerkiller.reflection.net.minecraft.server.NMSRegionFileCache;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;

public class NoLaggLighting extends NoLaggComponent {
    public static NoLaggLighting plugin;
    public static long minFreeMemory = 100 * 1024 * 1024;
    int i = 0;

    @Override
    public void onReload(ConfigurationNode config) {
        config.setHeader("minFreeMemory", "The minimum amount of memory (in MB) allowed while processing");
        config.addHeader("minFreeMemory", "If the remaining free memory drops below this value, measures are taken to reduce it");
        config.addHeader("minFreeMemory", "Memory will be Garbage Collected and all worlds will be saved to free memory");
        minFreeMemory = 1024 * 1024 * config.get("minFreeMemory", 100);
    }

    @Override
    public void onEnable(ConfigurationNode config) {
        plugin = this;
        this.onReload(config);
        LightingService.loadPendingBatches();
    }

    @Override
    public void onDisable(ConfigurationNode config) {
        LightingService.abort();
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) throws NoPermissionException {
        if (args.length == 0) {
            return false;
        }
        if (args[0].equalsIgnoreCase("fixworld") || args[0].equalsIgnoreCase("fixall")) {
            Permission.LIGHTING_FIX.handle(sender);
            final World world;
            if (args.length >= 2) {
                world = Bukkit.getWorld(args[1]);
                if (world == null) {
                    sender.sendMessage(ChatColor.RED + "World '" + args[1] + "' was not found!");
                    return true;
                }
            } else if (sender instanceof Player) {
                world = ((Player) sender).getWorld();
            } else {
                sender.sendMessage("As a console you have to specify the world to fix!");
                return true;
            }
            // Obtain the region folder
            File regionFolder = WorldUtil.getWorldRegionFolder(world.getName());
            if (regionFolder == null && WorldUtil.getChunks(world).isEmpty() && NMSRegionFileCache.FILES.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "World " + world.getName() + " contains no loaded chunks neither any offline-stored regions files to read");
                sender.sendMessage(ChatColor.RED + "This could be a bug in the program, or it could be that there really are no regions generated (yet?)");
                return true;
            }
            // Fix all the chunks in this world
            sender.sendMessage(ChatColor.YELLOW + "The world is now being fixed, this may take very long!");
            sender.sendMessage(ChatColor.YELLOW + "To view the fixing status, use /lag stat");
            LightingService.addRecipient(sender);
            // Get an iterator for all the chunks to fix
            LightingService.scheduleWorld(world, regionFolder);
        } else if (args[0].equalsIgnoreCase("fix")) {
            if (sender instanceof Player) {
                Permission.LIGHTING_FIX.handle(sender);
                Player p = (Player) sender;
                int radius = Bukkit.getServer().getViewDistance();
                if (args.length == 2) {
                    radius = ParseUtil.parseInt(args[1], radius);
                }
                Location l = p.getLocation();
                LightingService.scheduleArea(p.getWorld(), l.getBlockX() >> 4, l.getBlockZ() >> 4, radius);
                p.sendMessage(ChatColor.GREEN + "A " + (radius * 2 + 1) + " X " + (radius * 2 + 1) + " chunk area around you is currently being fixed from lighting issues...");
                LightingService.addRecipient(sender);
            }
        } else if (args[0].equalsIgnoreCase("abort")) {
            Permission.LIGHTING_ABORT.handle(sender);
            if (LightingService.isProcessing()) {
                LightingService.clearTasks();
                sender.sendMessage(ChatColor.GREEN + "All pending tasks cleared, will finish current " + LightingService.getChunkFaults() + " chunks now...");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "No lighting was being processed; there was nothing to abort.");
            }
        } else {
            return false;
        }
        return true;
    }
}
