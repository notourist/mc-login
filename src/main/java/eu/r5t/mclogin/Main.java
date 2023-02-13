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
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class Main extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

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
        getCommand("whitelist").setExecutor(this);
        getCommand("fails").setExecutor(this);
        getCommand("reload-login").setExecutor(this);
        getCommand("whitelist").setTabCompleter(this);
        getCommand("fails").setTabCompleter(this);
        getCommand("reload-login").setTabCompleter(this);
    }

    public void onDisable() {
        getConfig().set("whitelist", whitelist);
        getConfig().set("fails", fails);
        getConfig().set("password", password);
        this.saveConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ("ip".equals(label) && sender instanceof Player player) {
            player.sendMessage("IP: " + ChatColor.GOLD + getHostAddress(player));
            return true;
        }
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
        if (args.length < 1) {
            return false;
        }
        String type = "fails".equals(label) ? "fails" : "whitelist";
        switch (args[0]) {
            case "reload" -> {
                ("fails".equals(label) ? fails : whitelist).clear();
                ("fails".equals(label) ? fails : whitelist).addAll(getConfig().getStringList(type));
                sender.sendMessage(ChatColor.GOLD + "Reloaded " + type);
            }
            case "clear" -> {
                ("fails".equals(label) ? fails : whitelist).clear();
                sender.sendMessage(ChatColor.GOLD + "Cleared " + type);
            }
            case "add" -> {
                ("fails".equals(label) ? fails : whitelist).add(args[1]);
                sender.sendMessage(ChatColor.GOLD + "Added " + args[1] + " to " + type);
            }
            case "remove" -> {
                ("fails".equals(label) ? fails : whitelist).remove(args[1]);
                sender.sendMessage(ChatColor.GOLD + "Removed " + args[1] + " from " + type);
            }
            case "list" -> {
                sender.sendMessage(type + ":");
                ("fails".equals(label) ? fails : whitelist).forEach(s -> sender.sendMessage(" - " + ChatColor.GOLD + s));
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if ("ip".equals(command.getName()) || "reload-login".equals(command.getName())) {
            return Collections.emptyList();
        }
        var arg0 = Arrays.asList("reload", "clear", "list", "add", "remove");
        if (args.length == 0) {
            return arg0;
        } else if (args.length == 1) {
            var matches = arg0.stream().filter(a -> a.contains(args[0])).toList();
            return matches.size() != 0 ? matches : arg0;
        } else if (args.length == 2) {
            var list = new ArrayList<>(command.getName().equals("fails") ? fails : whitelist);
            var online = Bukkit.getOnlinePlayers().stream()
                    .map(this::getHostAddress)
                    .filter(list::contains)
                    .toList();
            list.addAll(online);
            var matches = list.stream().filter(e -> e.contains(args[1])).toList();
            return matches.size() != 0 ? matches : list;
        }
        return Collections.emptyList();
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
