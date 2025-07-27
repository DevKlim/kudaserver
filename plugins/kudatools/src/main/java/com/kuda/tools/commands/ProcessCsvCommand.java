package com.kuda.tools.commands;

import com.kuda.tools.KudaTools;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.File;

public class ProcessCsvCommand implements CommandExecutor {

    private final KudaTools plugin;

    public ProcessCsvCommand(KudaTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        File csvFile = new File(plugin.getDataFolder(), "build_list.csv");
        plugin.getBulkProcessManager().startBulkProcess(sender);
        return true;
    }
}