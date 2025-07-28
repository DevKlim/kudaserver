package com.kuda.tools;

import com.kuda.tools.commands.ExportCommand;
import com.kuda.tools.commands.ProcessCsvCommand;
import com.kuda.tools.commands.ProcessUrlCommand;
import com.kuda.tools.logic.BulkProcessManager;
import com.kuda.tools.logic.PlacementManager;
import com.kuda.tools.logic.SchematicProcessor;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class KudaTools extends JavaPlugin {

    private static KudaTools instance;
    private WorldEditPlugin worldEdit;
    
    private PlacementManager placementManager;
    private SchematicProcessor schematicProcessor;
    private BulkProcessManager bulkProcessManager;

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        
        // Initialize logic managers
        String worldName = getConfig().getString("placement.world_name", "world");
        int spacing = getConfig().getInt("placement.padding", 32);
        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) {
            getLogger().severe("World '" + worldName + "' not found! Using default world.");
            world = Bukkit.getWorlds().get(0);
        }
        this.placementManager = new PlacementManager(world, spacing);
        this.schematicProcessor = new SchematicProcessor(this);
        this.bulkProcessManager = new BulkProcessManager(this);

        if (!setupWorldEdit()) {
            getLogger().severe("WorldEdit not found! This plugin is required. Disabling KudaTools...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        File dataDir = new File(getDataFolder(), "exported_builds");
        if (!dataDir.exists()) dataDir.mkdirs();
        
        // Save example CSV if it doesn't exist
        saveResource("build_list.csv", false);

        getLogger().info("KudaTools has been enabled successfully.");

        // Command Registration
        this.getCommand("kuda-export").setExecutor(new ExportCommand(this));
        this.getCommand("kuda-process-url").setExecutor(new ProcessUrlCommand(this));
        this.getCommand("kuda-process-csv").setExecutor(new ProcessCsvCommand(this));
    }

    @Override
    public void onDisable() {
        if (bulkProcessManager != null) {
            bulkProcessManager.stopProcessing();
        }
        getLogger().info("KudaTools has been disabled.");
    }

    private boolean setupWorldEdit() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
        if (plugin instanceof WorldEditPlugin) {
            this.worldEdit = (WorldEditPlugin) plugin;
            return true;
        }
        return false;
    }

    public static KudaTools getInstance() { return instance; }
    public WorldEditPlugin getWorldEdit() { return worldEdit; }
    public PlacementManager getPlacementManager() { return placementManager; }
    public SchematicProcessor getSchematicProcessor() { return schematicProcessor; }
    public BulkProcessManager getBulkProcessManager() { return bulkProcessManager; }
}