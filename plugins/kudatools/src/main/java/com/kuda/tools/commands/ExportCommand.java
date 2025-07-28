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
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.world.block.BlockState;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class ExportCommand implements CommandExecutor {

    private final KudaTools plugin;

    public ExportCommand(KudaTools plugin) {
        this.plugin = plugin;
    }

    private Map<String, Integer> analyzeBlockPalette(Region selection, com.sk89q.worldedit.world.World world) {
        Map<String, Integer> blockCounts = new HashMap<>();
        
        for (BlockVector3 point : selection) {
            BlockState block = world.getBlock(point);
            String blockId = block.getBlockType().getId();
            blockCounts.put(blockId, blockCounts.getOrDefault(blockId, 0) + 1);
        }
        
        return blockCounts;
    }

    private List<Map<String, Object>> analyzeEntities(Region selection, Player player) {
        List<Map<String, Object>> entityData = new ArrayList<>();
        World bukkitWorld = player.getWorld();
        
        BlockVector3 min = selection.getMinimumPoint();
        BlockVector3 max = selection.getMaximumPoint();
        
        Location minLoc = new Location(bukkitWorld, min.getX(), min.getY(), min.getZ());
        Location maxLoc = new Location(bukkitWorld, max.getX(), max.getY(), max.getZ());
        
        for (Entity entity : bukkitWorld.getEntities()) {
            Location entityLoc = entity.getLocation();
            
            // Check if entity is within the selection bounds
            if (entityLoc.getX() >= minLoc.getX() && entityLoc.getX() <= maxLoc.getX() &&
                entityLoc.getY() >= minLoc.getY() && entityLoc.getY() <= maxLoc.getY() &&
                entityLoc.getZ() >= minLoc.getZ() && entityLoc.getZ() <= maxLoc.getZ()) {
                
                Map<String, Object> entityInfo = new HashMap<>();
                entityInfo.put("type", entity.getType().name());
                entityInfo.put("uuid", entity.getUniqueId().toString());
                entityInfo.put("name", entity.getName());
                entityInfo.put("customName", entity.getCustomName());
                
                Map<String, Object> location = new HashMap<>();
                location.put("x", entityLoc.getX());
                location.put("y", entityLoc.getY());
                location.put("z", entityLoc.getZ());
                location.put("yaw", entityLoc.getYaw());
                location.put("pitch", entityLoc.getPitch());
                entityInfo.put("location", location);
                
                // Relative position within selection
                Map<String, Object> relativePos = new HashMap<>();
                relativePos.put("x", entityLoc.getX() - min.getX());
                relativePos.put("y", entityLoc.getY() - min.getY());
                relativePos.put("z", entityLoc.getZ() - min.getZ());
                entityInfo.put("relativePosition", relativePos);
                
                entityData.add(entityInfo);
            }
        }
        
        return entityData;
    }

    private String formatBlockPalette(Map<String, Integer> blockPalette) {
        if (blockPalette.isEmpty()) {
            return "{}";
        }
        
        return blockPalette.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .map(entry -> String.format("    \"%s\": %d", entry.getKey(), entry.getValue()))
            .collect(Collectors.joining(",\n", "{\n", "\n  }"));
    }

    private String formatEntities(List<Map<String, Object>> entities) {
        if (entities.isEmpty()) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < entities.size(); i++) {
            Map<String, Object> entity = entities.get(i);
            sb.append("    {\n");
            sb.append(String.format("      \"type\": \"%s\",\n", entity.get("type")));
            sb.append(String.format("      \"uuid\": \"%s\",\n", entity.get("uuid")));
            sb.append(String.format("      \"name\": \"%s\",\n", entity.get("name")));
            sb.append(String.format("      \"customName\": %s,\n", 
                entity.get("customName") != null ? "\"" + entity.get("customName") + "\"" : "null"));
            
            @SuppressWarnings("unchecked")
            Map<String, Object> location = (Map<String, Object>) entity.get("location");
            sb.append("      \"location\": {\n");
            sb.append(String.format("        \"x\": %.2f,\n", (Double) location.get("x")));
            sb.append(String.format("        \"y\": %.2f,\n", (Double) location.get("y")));
            sb.append(String.format("        \"z\": %.2f,\n", (Double) location.get("z")));
            sb.append(String.format("        \"yaw\": %.2f,\n", (Float) location.get("yaw")));
            sb.append(String.format("        \"pitch\": %.2f\n", (Float) location.get("pitch")));
            sb.append("      },\n");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> relativePos = (Map<String, Object>) entity.get("relativePosition");
            sb.append("      \"relativePosition\": {\n");
            sb.append(String.format("        \"x\": %.2f,\n", (Double) relativePos.get("x")));
            sb.append(String.format("        \"y\": %.2f,\n", (Double) relativePos.get("y")));
            sb.append(String.format("        \"z\": %.2f\n", (Double) relativePos.get("z")));
            sb.append("      }\n");
            sb.append("    }");
            
            if (i < entities.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  ]");
        
        return sb.toString();
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

            // Create separate directories for JSON and schematic files
            File exportedBuildsDir = new File(plugin.getDataFolder(), "exported_builds");
            File jsonDir = new File(exportedBuildsDir, "json");
            File schemDir = new File(exportedBuildsDir, "schem");
            
            if (!jsonDir.exists()) {
                jsonDir.mkdirs();
            }
            if (!schemDir.exists()) {
                schemDir.mkdirs();
            }
            
            File schematicFile = new File(schemDir, sanitizedBuildName + ".schem");
            File metadataFile = new File(jsonDir, sanitizedBuildName + ".json");

            BlockArrayClipboard clipboard = new BlockArrayClipboard(selection);
            clipboard.setOrigin(selection.getMinimumPoint());

            ForwardExtentCopy copy = new ForwardExtentCopy(
                localSession.getSelectionWorld(), selection, clipboard, selection.getMinimumPoint()
            );
            Operations.complete(copy);

            try (ClipboardWriter writer = BuiltInClipboardFormat.MINECRAFT_STRUCTURE.getWriter(new FileOutputStream(nbtFile))) {
                writer.write(clipboard);
            }

            // Analyze block palette and entities
            player.sendMessage(ChatColor.YELLOW + "Analyzing blocks and entities...");
            Map<String, Integer> blockPalette = analyzeBlockPalette(selection, localSession.getSelectionWorld());
            List<Map<String, Object>> entities = analyzeEntities(selection, player);

            BlockVector3 dimensions = selection.getMaximumPoint().subtract(selection.getMinimumPoint()).add(1, 1, 1);
            long totalBlocks = blockPalette.values().stream().mapToLong(Integer::longValue).sum();
            
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
                "  \"statistics\": {\n" +
                "    \"totalBlocks\": %d,\n" +
                "    \"uniqueBlockTypes\": %d,\n" +
                "    \"entityCount\": %d\n" +
                "  },\n" +
                "  \"blockPalette\": %s,\n" +
                "  \"entities\": %s,\n" +
                "  \"filePath\": \"%s\"\n" +
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
                totalBlocks,
                blockPalette.size(),
                entities.size(),
                formatBlockPalette(blockPalette),
                formatEntities(entities),
                schematicFile.getPath().replace("\\", "/")
            );

            try (FileWriter fileWriter = new FileWriter(metadataFile)) {
                fileWriter.write(metadataJson);
            }

            player.sendMessage(ChatColor.GREEN + "Successfully exported '" + sanitizedBuildName + "'!");
            player.sendMessage(ChatColor.GRAY + "Schematic: " + schematicFile.getPath());
            player.sendMessage(ChatColor.GRAY + "Metadata: " + metadataFile.getPath());
            player.sendMessage(ChatColor.AQUA + "Analysis Results:");
            player.sendMessage(ChatColor.AQUA + "  • Total blocks: " + ChatColor.WHITE + totalBlocks);
            player.sendMessage(ChatColor.AQUA + "  • Unique block types: " + ChatColor.WHITE + blockPalette.size());
            player.sendMessage(ChatColor.AQUA + "  • Entities found: " + ChatColor.WHITE + entities.size());
            
            if (!blockPalette.isEmpty()) {
                player.sendMessage(ChatColor.AQUA + "  • Most common blocks:");
                blockPalette.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(3)
                    .forEach(entry -> player.sendMessage(ChatColor.GRAY + "    - " + 
                        entry.getKey().replace("minecraft:", "") + ": " + entry.getValue()));
            }

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