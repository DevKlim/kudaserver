package com.kuda.tools.logic;

import com.kuda.tools.KudaTools;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class SchematicProcessor {

    private final KudaTools plugin;

    public SchematicProcessor(KudaTools plugin) {
        this.plugin = plugin;
    }

    public void processUrl(CommandSender sender, String urlString, String buildName, List<String> tags) {
        new BukkitRunnable() {
            @Override
            public void run() {
                File schematicFile = downloadSchematic(sender, urlString, buildName);
                if (schematicFile != null) {
                    saveMetadata(sender, buildName, urlString, tags, schematicFile);
                    // The pasting logic will be called from here in the future
                    sender.sendMessage("Schematic processed: " + buildName);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private File downloadSchematic(CommandSender sender, String urlString, String buildName) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            File exportsFolder = new File(plugin.getDataFolder(), "exported_builds");
            if (!exportsFolder.exists()) {
                exportsFolder.mkdirs();
            }
            File schematicFile = new File(exportsFolder, buildName + ".schem");

            try (InputStream in = connection.getInputStream()) {
                Files.copy(in, schematicFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            sender.sendMessage("Downloaded schematic: " + buildName);
            return schematicFile;
        } catch (IOException e) {
            sender.sendMessage("Error downloading schematic: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void saveMetadata(CommandSender sender, String buildName, String urlString, List<String> tags, File schematicFile) {
        File metadataFile = new File(schematicFile.getParentFile(), buildName + ".json");
        try (FileWriter writer = new FileWriter(metadataFile)) {
            writer.write("{\n");
            writer.write("  \"buildName\": \"" + buildName + "\",\n");
            writer.write("  \"sourceUrl\": \"" + urlString + "\",\n");
            writer.write("  \"tags\": " + tags.toString() + "\n");
            writer.write("}\n");
        } catch (IOException e) {
            sender.sendMessage("Error saving metadata: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
