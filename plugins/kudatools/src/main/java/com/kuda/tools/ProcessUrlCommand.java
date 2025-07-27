package com.kuda.tools;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import java.util.Arrays;
import java.util.List;

public class ProcessUrlCommand implements CommandExecutor {

    private final KudaTools plugin;

    public ProcessUrlCommand(KudaTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /kuda-process-url <url> <buildName> [tag1] [tag2]...");
            return false;
        }

        String urlString = args[0];
        String buildName = args[1];
        List<String> tags = Arrays.asList(Arrays.copyOfRange(args, 2, args.length));

        plugin.getSchematicProcessor().processUrl(sender, urlString, buildName, tags);

        return true;
    }
}
