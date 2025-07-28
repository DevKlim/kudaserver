package com.kuda.tools.commands;

import com.kuda.tools.KudaTools;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExportCommand implements CommandExecutor {

    private final KudaTools plugin;

    public ExportCommand(KudaTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /kuda-export <buildName> [tag1] [tag2]...");
            return false;
        }

        String buildName = args[0];
        // Sanitize buildName to prevent directory traversal issues
        String sanitizedBuildName = buildName.replaceAll("[^a-zA-Z0-9_\\-]", "");
        if (sanitizedBuildName.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Invalid build name. Please use alphanumeric characters, dashes, or underscores.");
            return true;
        }

        List<String> tags = Arrays.stream(args).skip(1).collect(Collectors.toList());

        try {
            SessionManager manager = WorldEdit.getInstance().getSessionManager();
            LocalSession localSession = manager.get(BukkitAdapter.adapt(player));
            Region selection = localSession.getSelection(localSession.getSelectionWorld());

            // --- Define output directory and file paths ---
            File nbtOutputDir = new File(plugin.getDataFolder(), "exported_builds/nbt" + sanitizedBuildName);
            File jsonOutputDir = new File(plugin.getDataFolder(), "exported_builds/json" + sanitizedBuildName);
            
            if (!jsonOutputDir.exists()) {
                jsonOutputDir.mkdirs();
            }
            if (!nbtOutputDir.exists()) {
                nbtOutputDir.mkdirs();
            }
            File nbtFile = new File(nbtOutputDir, sanitizedBuildName + ".nbt");
            File metadataFile = new File(jsonOutputDir, "metadata.json");

            // --- Create and save the schematic ---
            BlockArrayClipboard clipboard = new BlockArrayClipboard(selection);
            clipboard.setOrigin(selection.getMinimumPoint());

            ForwardExtentCopy copy = new ForwardExtentCopy(
                localSession.getSelectionWorld(), selection, clipboard, selection.getMinimumPoint()
            );
            Operations.complete(copy);

            try (ClipboardWriter writer = BuiltInClipboardFormat.MINECRAFT_STRUCTURE.getWriter(new FileOutputStream(nbtFile))) {
                writer.write(clipboard);
            }

            Map<String,Integer> blockPalette = new HashMap<>();
            for (BlockVector3 pt : selection) {
                // Get the block state from the clipboard at the given point
                String blockId = clipboard.getBlock(pt).getBlockType().getId();
                // Add the block ID to the map, incrementing its count
                blockPalette.put(blockId, blockPalette.getOrDefault(blockId, 0) + 1);
            }
             // Format the block palette map into a JSON string ---
             String paletteJsonString = blockPalette.entrySet().stream()
             .map(entry -> String.format("    \"%s\": %d", entry.getKey(), entry.getValue()))
             .collect(Collectors.joining(",\n"));

            // --- NEW: Find all entities within the selection ---
            org.bukkit.World bukkitWorld = player.getWorld();
            List<org.bukkit.entity.Entity> entitiesInRegion = bukkitWorld.getEntities().stream()
                .filter(entity -> !(entity instanceof Player)) // Exclude players
                .filter(entity -> selection.contains(BukkitAdapter.asBlockVector(entity.getLocation())))
                .collect(Collectors.toList());
            // --- NEW: Convert entity list to a JSON array string ---
            String entitiesJsonString = entitiesInRegion.stream()
                .map(entity -> {
                    String customNameJson = "";
                    if (entity.getCustomName() != null && !entity.getCustomName().isEmpty()) {
                        // Escape quotes for JSON compatibility
                        String escapedName = entity.getCustomName().replace("\"", "\\\"");
                        customNameJson = String.format(",\n      \"customName\": \"%s\"", escapedName);
                    }
                    return String.format(
                        "    {\n" +
                        "      \"type\": \"%s\",\n" +
                        "      \"uuid\": \"%s\",\n" +
                        "      \"location\": {\"x\": %f, \"y\": %f, \"z\": %f}%s\n" +
                        "    }",
                        entity.getType().name(),
                        entity.getUniqueId().toString(),
                        entity.getLocation().getX(),
                        entity.getLocation().getY(),
                        entity.getLocation().getZ(),
                        customNameJson
                    );
                })
                .collect(Collectors.joining(",\n", "[\n", "\n  ]"));
            
            if (entitiesInRegion.isEmpty()) {
                entitiesJsonString = "[]";
            }


            // --- Create and save the metadata.json ---
            BlockVector3 dimensions = BlockVector3.at(selection.getWidth(), selection.getHeight(), selection.getLength());
            String worldName = selection.getWorld().getName();
            BlockVector3 minPoint = selection.getMinimumPoint();
            BlockVector3 maxPoint = selection.getMaximumPoint();
            int minChunkX = minPoint.getBlockX() >> 4;
            int minChunkZ = minPoint.getBlockZ() >> 4;
            int maxChunkX = maxPoint.getBlockX() >> 4;
            int maxChunkZ = maxPoint.getBlockZ() >> 4;
            String metadataJson = String.format(
                "{\n" +
                "  \"buildName\": \"%s\",\n" +
                "  \"author\": {\n" +
                "    \"name\": \"%s\",\n" +
                "    \"uuid\": \"%s\"\n" +
                "  },\n" +
                "  \"tags\": [%s],\n" +
                "  \"timestamp\": \"%s\",\n" +
                "  \"source\": \"player-submission\",\n" +
                "  \"worldName\": \"%s\",\n" +
                "  \"dimensions\": {\n" +
                "    \"x\": %d,\n" +
                "    \"y\": %d,\n" +
                "    \"z\": %d\n" +
                "  },\n" +
                "  \"jsonLocation\": \"%s\",\n" +
                "  \"nbtLocation\": \"%s\",\n" +
                "  \"blockPalette\": {\n%s\n  },\n" +
                "  \"entities\": %s,\n" +
                "  \"chunks\": {\n" +
                "    \"min\": {\"x\": %d, \"z\": %d},\n" +
                "    \"max\": {\"x\": %d, \"z\": %d}\n" +
                "  }\n" +
                "}",
                sanitizedBuildName,
                player.getName(),
                player.getUniqueId().toString(),
                tags.stream().map(t -> "\"" + t + "\"").collect(Collectors.joining(", ")),
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date()),
                worldName,
                dimensions.getX(),
                dimensions.getY(),
                dimensions.getZ(),
                metadataFile.getPath().replace("\\", "/"),
                nbtFile.getPath().replace("\\", "/"),
                paletteJsonString,
                entitiesJsonString,
                minChunkX,
                minChunkZ,
                maxChunkX,
                maxChunkZ
            );

            try (FileWriter fileWriter = new FileWriter(metadataFile)) {
                fileWriter.write(metadataJson);
            }

            player.sendMessage(ChatColor.GREEN + "Successfully exported '" + sanitizedBuildName + "'!");
            player.sendMessage(ChatColor.GRAY + "Saved to: " + nbtFile.getPath());

        } catch (IncompleteRegionException e) {
            player.sendMessage(ChatColor.RED + "Your WorldEdit selection is incomplete. Please select two points.");
        } catch (IOException e) {
            player.sendMessage(ChatColor.RED + "An error occurred while saving the file. See console for details.");
            e.printStackTrace();
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "An unexpected error occurred. See console for details.");
            e.printStackTrace();
        }

        return true;
    }
}