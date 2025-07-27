# Minecraft Server for Data Collection

## 1. Purpose

This directory contains the configuration and plugins for a dedicated Minecraft server with two primary goals:

1.  **BuildPaste Automation:** The server will be equipped with tools to automatically interface with services like [BuildPaste](https://buildpaste.net/). It will programmatically import new schematics into a dedicated world for processing and data collection.

2.  **Player/Agent Data Collection & Partitioning:** The server will host a creative world where whitelisted builders and AI agents can create structures. We are developing a server-side plugin, **KudaTools**, to use existing tools (like WorldEdit) for a streamlined data pipeline:
    - Allow players to select a region containing their build.
    - Execute a command (e.g., `/kuda-export <build_name> [tags...]`) to export the selection.
    - The plugin saves the selection as a standardized `.schem` file.
    - The plugin also generates a `metadata.json` file containing the player's name, the build name, dimensions, and the tags they provided.
    - This data is then ready for ingestion into the main `ai.kuda` data pipeline.

### Automation Goals:

    - **Bulk Processing from CSV:** A single command, `/kuda-process-csv`, reads a `build_list.csv` file from the plugin's directory. For each entry, it automatically downloads the schematic from BuildPaste, pastes it into a designated grid location, and saves the build data and metadata. This is the primary method for bulk data collection.
    - **Single URL Ingestion:** A command, `/kuda-process-url`, remains available for processing individual builds on-the-fly.
    - **Grid-Based Placement:** Automatically place pasted builds into an organized grid system to avoid overlaps, managed via the `config.yml` file.

This component is crucial for gathering unique, high-quality, and well-contextualized data that may not be available on public schematic-hosting websites.

## 2. Custom Plugins


This server uses custom plugins developed specifically for the `ai.kuda` project.

### KudaTools

This is the primary data collection and automation plugin.

- **Source Code:** `mc_server/plugins/kudatools/`
- **Purpose:** Allows players and the server console to export and manage builds as `.schem` files with rich `metadata.json`. It is the foundation for our BuildPaste automation.
- **Dependencies:** [WorldEdit](https://dev.bukkit.org/projects/worldedit) (must be installed on the server).
- **Commands:**
  - `/kuda-export <buildName> <tag1>...` - Exports the player's current WorldEdit selection.
  - `/kuda-process-url <url> <buildName> <tag1>...` - Downloads a schematic from a URL and processes it. **BuildPaste URL format:** `https://buildpaste.net/blueprints/get/<id>`
  - `/kuda-process-csv` - Starts the bulk processing of all builds listed in `plugins/KudaTools/build_list.csv`. Can be run from the console.
- **Building the Plugin:**
  1.  Navigate to the `mc_server/plugins/kudatools/` directory.
  2.  Make sure you have Java (JDK 17+) and Maven installed.
  3.  Run the command: `mvn clean package`.
  4.  The compiled plugin JAR will be in the `target/` sub-directory. Copy this JAR file into the main `mc_server/plugins/` folder to be loaded by the server.
- **Output:** Exported builds are saved in `mc_server/plugins/KudaTools/exported_builds/`.

## 3. Server Info

In this folder are all the server assets, running Purpur which is a fork of Paper (CraftBukkit).
