package com.kuda.tools.logic;

import com.kuda.tools.KudaTools;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class BulkProcessManager {

    private final KudaTools plugin;
    private final SchematicProcessor schematicProcessor;

    public BulkProcessManager(KudaTools plugin) {
        this.plugin = plugin;
        this.schematicProcessor = new SchematicProcessor(plugin);
    }

    public void startBulkProcess(CommandSender sender) {
        new BukkitRunnable() {
            @Override
            public void run() {
                File csvFile = new File(plugin.getDataFolder().getParent(), "kudatools/build_list.csv");
                if (!csvFile.exists()) {
                    sender.sendMessage("build_list.csv not found!");
                    return;
                }

                try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("#") || line.trim().isEmpty()) {
                            continue;
                        }

                        String[] values = line.split(",");
                        if (values.length < 2) {
                            sender.sendMessage("Skipping invalid line: " + line);
                            continue;
                        }

                        String buildId = values[0];
                        String buildName = values[1];
                        List<String> tags = Arrays.asList(Arrays.copyOfRange(values, 2, values.length));
                        String url = "https://buildpaste.net/blueprints/get/" + buildId;

                        sender.sendMessage("Processing build: " + buildName + " from URL: " + url);
                        schematicProcessor.processUrl(sender, url, buildName, tags);
                    }
                    sender.sendMessage("Finished processing build_list.csv.");
                } catch (IOException e) {
                    sender.sendMessage("An error occurred while reading the CSV file.");
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }
}
