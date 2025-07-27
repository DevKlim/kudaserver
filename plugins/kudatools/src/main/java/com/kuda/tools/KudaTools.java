package com.kuda.tools;

import com.kuda.tools.logic.BulkProcessManager;
import com.kuda.tools.logic.PlacementManager;
import com.kuda.tools.logic.SchematicProcessor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public class KudaTools extends JavaPlugin {

    private static KudaTools instance;
    private PlacementManager placementManager;
    private SchematicProcessor schematicProcessor;
    private BulkProcessManager bulkProcessManager;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("KudaTools has been enabled!");

        // Create plugin data folder if it doesn't exist
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Initialize managers
        World world = Bukkit.getWorlds().get(0); // Use the primary world
        placementManager = new PlacementManager(world, 100); // 100 block spacing
        schematicProcessor = new SchematicProcessor(this);
        bulkProcessManager = new BulkProcessManager(this);


        // Register commands
        this.getCommand("kuda-export").setExecutor(new ExportCommand());
        this.getCommand("kuda-process-url").setExecutor(new ProcessUrlCommand(this));
        this.getCommand("kuda-process-csv").setExecutor(new ProcessCsvCommand(this));

        getLogger().info("KudaTools is ready.");
    }

    @Override
    public void onDisable() {
        getLogger().info("KudaTools has been disabled.");
    }

    public static KudaTools getInstance() {
        return instance;
    }

    public PlacementManager getPlacementManager() {
        return placementManager;
    }

    public SchematicProcessor getSchematicProcessor() {
        return schematicProcessor;
    }

    public BulkProcessManager getBulkProcessManager() {
        return bulkProcessManager;
    }
}
