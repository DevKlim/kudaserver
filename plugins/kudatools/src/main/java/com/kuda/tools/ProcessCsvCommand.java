package com.kuda.tools;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import java.util.Arrays;

public class ProcessCsvCommand implements CommandExecutor {

    private final KudaTools plugin;

    public ProcessCsvCommand(KudaTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        plugin.getBulkProcessManager().startBulkProcess(sender);
        return true;
    }
}
