package dev.murk.antiesp.command;

import dev.murk.antiesp.MAntiESP;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

public class AntiESPCommand implements CommandExecutor, TabCompleter {
    private final MAntiESP plugin;

    public AntiESPCommand(MAntiESP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("mantiesp.admin")) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав для выполнения этой команды.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reload();
            sender.sendMessage(ChatColor.GREEN + "[mAntiESP] Конфигурация успешно перезагружена! Для корректной работы рекомендуется перезапустить сервер");
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Использование: /" + label + " reload");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("mantiesp.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            if ("reload".startsWith(args[0].toLowerCase())) {
                return List.of("reload");
            }
        }

        return Collections.emptyList();
    }
}
