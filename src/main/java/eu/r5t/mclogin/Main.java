package eu.r5t.mclogin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class Main extends JavaPlugin implements Listener {

    private List<String> whitelist;
    private List<String> fails;
    private String password;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        whitelist = getConfig().getStringList("whitelist");
        fails = getConfig().getStringList("fails");
        password = getConfig().getString("password");
        getServer().getConsoleSender().sendMessage("[mc-login] Password: " + password);
        getServer().getPluginManager().registerEvents(this, this);
        PluginCommand pluginCommand = new PluginCommand(this);
        getCommand("whitelist").setExecutor(pluginCommand);
        getCommand("fails").setExecutor(pluginCommand);
        getCommand("reload-login").setExecutor(pluginCommand);
        getCommand("ip").setExecutor(pluginCommand);
        getCommand("whitelist").setTabCompleter(pluginCommand);
        getCommand("fails").setTabCompleter(pluginCommand);
        getCommand("reload-login").setTabCompleter(pluginCommand);
    }

    public void onDisable() {
        getConfig().set("whitelist", whitelist);
        getConfig().set("fails", fails);
        getConfig().set("password", password);
        this.saveConfig();
    }

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        if (whitelist.contains(event.getAddress().getHostAddress())) {
            event.setMotd(ChatColor.GOLD + "IP is whitelisted");
            return;
        }
        while (event.iterator().hasNext()) {
            event.iterator().remove();
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandSendEvent event) {
        if (!whitelist.contains(getHostAddress(event))) {
            event.getCommands().clear();
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        var ip = getHostAddress(event.getPlayer());
        if (whitelist.contains(ip)) {
            return;
        }
        if (!password.equals(event.getMessage())) {
            if (fails.contains(ip)) {
                kick(event.getPlayer(), 2);
                Bukkit.banIP(ip);
                Bukkit.getConsoleSender().sendMessage("Wrong password, banned " + ip);
                fails.remove(ip);
            } else {
                Bukkit.getConsoleSender().sendMessage(
                        String.format("Wrong password \"%s\" (1 of 2) from %s (%s)",
                                event.getMessage(),
                                ip,
                                event.getPlayer().getName()));
                kick(event.getPlayer(), 1);
                event.setCancelled(true);
                fails.add(ip);
            }
            return;
        }
        if (!whitelist.contains(ip)) {
            fails.remove(ip);
            whitelist.add(ip);
            event.getPlayer().sendMessage("whitelisted your IP: " + ChatColor.GOLD + ip);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!whitelist.contains(getHostAddress(event))) {
            event.getPlayer().sendMessage(ChatColor.GOLD + "Please enter the password");
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (!whitelist.contains(getHostAddress(player))) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!whitelist.contains(getHostAddress(event))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!whitelist.contains(getHostAddress(event))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryInteract(InventoryInteractEvent event) {
        for (HumanEntity viewer : event.getViewers()) {
            if (viewer instanceof Player player) {
                if (!whitelist.contains(getHostAddress(player))) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (!whitelist.contains(getHostAddress(player))) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!whitelist.contains(getHostAddress(event))) {
            event.setCancelled(true);
        }
    }

    public static String getHostAddress(PlayerEvent event) {
        return getHostAddress(event.getPlayer());
    }

    public static String getHostAddress(Player player) {
        return player.getAddress().getAddress().getHostAddress();
    }

    public void kick(Player player, int tries) {
        var kickMsg = "Wrong password, " + tries + " of 2 tries";
        Bukkit.getScheduler().runTask(this, () -> player.kickPlayer(kickMsg));
    }

    public List<String> getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(List<String> whitelist) {
        this.whitelist = whitelist;
    }

    public List<String> getFails() {
        return fails;
    }

    public void setFails(List<String> fails) {
        this.fails = fails;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }
}
