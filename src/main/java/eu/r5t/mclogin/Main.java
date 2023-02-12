package eu.r5t.mclogin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

    private List<String> whitelist;
    private List<String> fails;
    private String password;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        whitelist = getConfig().getStringList("whitelist");
        fails = getConfig().getStringList("fails");
        password = getConfig().getString("password");
        getServer().getConsoleSender().sendMessage("Password: " + password);
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("whitelist").setExecutor(this);
        getCommand("fails").setExecutor(this);
        getCommand("reload-login").setExecutor(this);
    }

    public void onDisable() {
        getConfig().set("whitelist", whitelist);
        getConfig().set("fails", fails);
        getConfig().set("password", password);
        this.saveConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ((sender instanceof Player player && !player.isOp())) {
            sender.sendMessage(ChatColor.RED + "Not allowed");
            return true;
        }
        if ("reload-login".equals(label)) {
            whitelist = getConfig().getStringList("whitelist");
            fails = getConfig().getStringList("fails");
            password = getConfig().getString("password");
            sender.sendMessage(ChatColor.GOLD + "Reloaded all values.");
            sender.sendMessage("password: " + password);
            sender.sendMessage("whitelisted:");
            whitelist.forEach(s -> sender.sendMessage(" - " + s));
            sender.sendMessage("fails:");
            fails.forEach(s -> sender.sendMessage(" - " + s));
            return true;
        }
        if (args.length != 2) {
            return false;
        }
        String type = "fails".equals(label) ? "fails" : "whitelist";
        List<String> list = "fails".equals(label) ? fails : whitelist;
        switch (args[0]) {
            case "reload" -> {
                list.clear();
                list.addAll(getConfig().getStringList(type));
                sender.sendMessage(ChatColor.GOLD + "Reloaded " + type);
            }
            case "clear" -> {
                list.clear();
                sender.sendMessage(ChatColor.GOLD + "Cleared " + type);
            }
            case "add" -> {
                list.add(args[1]);
                sender.sendMessage(ChatColor.GOLD + "Added " + args[1] + " to " + type);
            }
            case "remove" -> {
                list.remove(args[1]);
                sender.sendMessage(ChatColor.GOLD + "Removed " + args[1] + " from " + type);
            }
        }
        return true;
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

    public String getHostAddress(PlayerEvent event) {
        return getHostAddress(event.getPlayer());
    }

    public String getHostAddress(Player player) {
        return player.getAddress().getAddress().getHostAddress();
    }

    public void kick(Player player, int tries) {
        var kickMsg = "Wrong password, " + tries + " of 2 tries";
        Bukkit.getScheduler().runTask(this, () -> player.kickPlayer(kickMsg));
    }
}
