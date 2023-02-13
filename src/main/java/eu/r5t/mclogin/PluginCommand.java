package eu.r5t.mclogin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PluginCommand implements TabExecutor {
    private final Main main;

    public PluginCommand(Main main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equals("ip") && sender instanceof Player player) {
            player.sendMessage("IP: " + ChatColor.GOLD + Main.getHostAddress(player));
            return true;
        }
        if ((sender instanceof Player player && !player.isOp())) {
            sender.sendMessage(ChatColor.RED + "Not allowed");
            return true;
        }
        if ("reload-login".equals(label)) {
            main.setWhitelist(main.getConfig().getStringList("whitelist"));
            main.setFails(main.getConfig().getStringList("fails"));
            main.setPassword(main.getConfig().getString("password"));
            sender.sendMessage(ChatColor.GOLD + "Reloaded all values.");
            sender.sendMessage("password: " + main.getPassword());
            sender.sendMessage("whitelisted:");
            main.getWhitelist().forEach(s -> sender.sendMessage(" - " + s));
            sender.sendMessage("fails:");
            main.getFails().forEach(s -> sender.sendMessage(" - " + s));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Wrong number of arguments");
            return true;
        }
        String type = "fails".equals(label) ? "fails" : "whitelist";
        switch (args[0]) {
            case "reload" -> {
                ("fails".equals(label) ? main.getFails() : main.getWhitelist()).clear();
                ("fails".equals(label) ? main.getFails() : main.getWhitelist()).addAll(main.getConfig().getStringList(type));
                sender.sendMessage(ChatColor.GOLD + "Reloaded " + type);
            }
            case "clear" -> {
                ("fails".equals(label) ? main.getFails() : main.getWhitelist()).clear();
                sender.sendMessage(ChatColor.GOLD + "Cleared " + type);
            }
            case "add" -> {
                ("fails".equals(label) ? main.getFails() : main.getWhitelist()).add(args[1]);
                sender.sendMessage(ChatColor.GOLD + "Added " + args[1] + " to " + type);
            }
            case "remove" -> {
                ("fails".equals(label) ? main.getFails() : main.getWhitelist()).remove(args[1]);
                sender.sendMessage(ChatColor.GOLD + "Removed " + args[1] + " from " + type);
            }
            case "list" -> {
                sender.sendMessage(type + ":");
                ("fails".equals(label) ? main.getFails() : main.getWhitelist()).forEach(s -> sender.sendMessage(" - " + ChatColor.GOLD + s));
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
            var list = new ArrayList<>(command.getName().equals("fails") ? main.getFails() : main.getWhitelist());
            var online = Bukkit.getOnlinePlayers().stream()
                    .map(Main::getHostAddress)
                    .filter(list::contains)
                    .toList();
            list.addAll(online);
            var matches = list.stream().filter(e -> e.contains(args[1])).toList();
            return matches.size() != 0 ? matches : list;
        }
        return Collections.emptyList();
    }
}
