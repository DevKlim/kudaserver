package com.kuda.tools.logic;

import org.bukkit.Location;
import org.bukkit.World;

public class PlacementManager {

    private final World world;
    private int currentX = 0;
    private int currentZ = 0;
    private final int spacing;

    public PlacementManager(World world, int spacing) {
        this.world = world;
        this.spacing = spacing;
    }

    public synchronized Location getNextPlacementLocation() {
        Location loc = new Location(world, currentX, 100, currentZ);
        currentX += spacing;
        // Simple grid, move to next row if we exceed a certain width
        if (currentX > (spacing * 10)) {
            currentX = 0;
            currentZ += spacing;
        }
        return loc;
    }
}
