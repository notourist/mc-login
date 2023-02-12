package eu.r5t.mclogin;

import org.bukkit.Bukkit;
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

public class Main extends JavaPlugin implements Listener {

    private List<String> whitelisted = new ArrayList<>();

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        whitelisted = getConfig().getStringList("whitelisted");
        getServer().getPluginManager().registerEvents(this, this);
    }

    public void onDisable() {
        getConfig().set("whitelisted", whitelisted);
        this.saveConfig();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!whitelisted.contains(getHostAddress(event))) {
            event.getPlayer().sendMessage("§6Please enter the password");
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (!whitelisted.contains(getHostAddress(player))) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!whitelisted.contains(getHostAddress(event))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!whitelisted.contains(getHostAddress(event))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryInteract(InventoryInteractEvent event) {
        for (HumanEntity viewer : event.getViewers()) {
            if (viewer instanceof Player player) {
                if (!whitelisted.contains(getHostAddress(player))) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (!whitelisted.contains(getHostAddress(player))) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!whitelisted.contains(getHostAddress(event))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        var ip = getHostAddress(event.getPlayer());
        var fails = getConfig().getStringList("fails");
        if (whitelisted.contains(ip)) {
            return;
        }
        if (!Objects.equals(event.getMessage(), this.getConfig().getString("password"))) {
            if (fails.contains(ip)) {
                kick(event.getPlayer(), 2);
                Bukkit.banIP(ip);
                Bukkit.getConsoleSender().sendMessage("Wrong password, banned " + ip);
                fails.remove(ip);
                getConfig().set("fails", fails);
                this.saveConfig();
                return;
            }
            Bukkit.getConsoleSender().sendMessage(
                    String.format("Wrong password \"%s\" (1 of 2) from %s (%s)",
                            event.getMessage(),
                            ip,
                            event.getPlayer().getName()));
            kick(event.getPlayer(), 1);
            event.setCancelled(true);
            fails.add(ip);
            getConfig().set("fails", fails);
            this.saveConfig();
            return;
        }
        if (!whitelisted.contains(ip)) {
            fails.remove(ip);
            getConfig().set("fails", fails);
            saveConfig();
            whitelisted.add(ip);
            event.getPlayer().sendMessage("Whitelisted your IP: §6" + ip);
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
