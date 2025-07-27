package com.kuda.tools.commands;

import com.kuda.tools.KudaTools;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ProcessUrlCommand implements CommandExecutor {

    private final KudaTools plugin;

    public ProcessUrlCommand(KudaTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /kuda-process-url <url> <buildName> [tags...]");
            return false;
        }

        String url = args[0];
        String buildName = args[1];
        List<String> tags = Arrays.stream(args).skip(2).collect(Collectors.toList());

        sender.sendMessage(ChatColor.YELLOW + "Starting processing for build '" + buildName + "'...");

        plugin.getSchematicProcessor().processUrl(sender, url, buildName, tags);

        return true;
    }
}