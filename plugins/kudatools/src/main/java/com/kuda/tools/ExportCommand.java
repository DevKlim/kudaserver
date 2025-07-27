package com.kuda.tools;

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
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ExportCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("Usage: /kuda-export <buildName> [tag1] [tag2]...");
            return false;
        }

        Player player = (Player) sender;
        String buildName = args[0];
        List<String> tags = Arrays.asList(Arrays.copyOfRange(args, 1, args.length));

        // Get WorldEdit selection
        Region selection;
        try {
            SessionManager sessionManager = WorldEdit.getInstance().getSessionManager();
            LocalSession localSession = sessionManager.get(BukkitAdapter.adapt(player));
            selection = localSession.getSelection(localSession.getSelectionWorld());
        } catch (IncompleteRegionException e) {
            player.sendMessage("Please make a WorldEdit selection first.");
            return true;
        }

        if (selection == null) {
            player.sendMessage("Please make a WorldEdit selection first.");
            return true;
        }

        File pluginFolder = KudaTools.getInstance().getDataFolder();
        File exportsFolder = new File(pluginFolder, "exported_builds");
        if (!exportsFolder.exists()) {
            exportsFolder.mkdirs();
        }

        File schematicFile = new File(exportsFolder, buildName + ".schem");
        File metadataFile = new File(exportsFolder, buildName + ".json");

        try {
            // Save schematic
            BlockArrayClipboard clipboard = new BlockArrayClipboard(selection);
            ForwardExtentCopy copy = new ForwardExtentCopy(
                    selection.getWorld(), selection, clipboard, selection.getMinimumPoint()
            );
            try {
                Operations.complete(copy);
            } catch (com.sk89q.worldedit.WorldEditException e) {
                player.sendMessage("An error occurred while copying the selection.");
                e.printStackTrace();
                return true;
            }

            try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(new FileOutputStream(schematicFile))) {
                writer.write(clipboard);
            }

            // Save metadata
            try (FileWriter writer = new FileWriter(metadataFile)) {
                writer.write("{\n");
                writer.write("  \"buildName\": \"" + buildName + "\",\n");
                writer.write("  \"playerName\": \"" + player.getName() + "\",\n");
                writer.write("  \"tags\": " + tags.toString() + ",\n");
                writer.write("  \"dimensions\": {\n");
                writer.write("    \"width\": " + selection.getWidth() + ",\n");
                writer.write("    \"height\": " + selection.getHeight() + ",\n");
                writer.write("    \"length\": " + selection.getLength() + "\n");
                writer.write("  }\n");
                writer.write("}\n");
            }

            player.sendMessage("Build '" + buildName + "' exported successfully!");

        } catch (IOException e) {
            player.sendMessage("An error occurred while exporting the build.");
            e.printStackTrace();
            return true;
        }

        return true;
    }
}
