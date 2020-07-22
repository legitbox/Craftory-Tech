package tech.brettsaunders.craftory;

import eu.endercentral.crazy_advancements.CrazyAdvancements;
import eu.endercentral.crazy_advancements.manager.AdvancementManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import lombok.Getter;
import lombok.Synchronized;
import org.bstats.bukkit.Metrics;
import org.bstats.bukkit.Metrics.AdvancedPie;
import org.bstats.bukkit.Metrics.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import tech.brettsaunders.craftory.CoreHolder.Blocks;
import tech.brettsaunders.craftory.api.blocks.CustomBlockFactory;
import tech.brettsaunders.craftory.api.blocks.basicBlocks.BasicBlocks;
import tech.brettsaunders.craftory.commands.CommandWrapper;
import tech.brettsaunders.craftory.external.Advancements;
import tech.brettsaunders.craftory.tech.power.core.block.cell.DiamondCell;
import tech.brettsaunders.craftory.tech.power.core.block.cell.EmeraldCell;
import tech.brettsaunders.craftory.tech.power.core.block.cell.GoldCell;
import tech.brettsaunders.craftory.tech.power.core.block.cell.IronCell;
import tech.brettsaunders.craftory.tech.power.core.block.generators.WindGenerator;
import tech.brettsaunders.craftory.tech.power.core.block.machine.magnetiser.Magnetiser;
import tech.brettsaunders.craftory.tech.power.core.block.machine.electricFurnace.DiamondElectricFurnace;
import tech.brettsaunders.craftory.tech.power.core.block.machine.electricFurnace.EmeraldElectricFurnace;
import tech.brettsaunders.craftory.tech.power.core.block.machine.electricFurnace.GoldElectricFurnace;
import tech.brettsaunders.craftory.tech.power.core.block.machine.electricFurnace.IronElectricFurnace;
import tech.brettsaunders.craftory.tech.power.core.block.machine.foundry.DiamondElectricFoundry;
import tech.brettsaunders.craftory.tech.power.core.block.machine.foundry.EmeraldElectricFoundry;
import tech.brettsaunders.craftory.tech.power.core.block.machine.foundry.GoldElectricFoundry;
import tech.brettsaunders.craftory.tech.power.core.block.machine.foundry.IronElectricFoundry;
import tech.brettsaunders.craftory.tech.power.core.block.machine.foundry.IronFoundry;
import tech.brettsaunders.craftory.tech.power.core.block.generators.GeothermalGenerator;
import tech.brettsaunders.craftory.tech.power.core.block.generators.SolidFuelGenerator;
import tech.brettsaunders.craftory.tech.power.core.block.generators.solar.BasicSolarPanel;
import tech.brettsaunders.craftory.tech.power.core.block.generators.solar.CompactedSolarPanel;
import tech.brettsaunders.craftory.tech.power.core.block.generators.solar.SolarArray;
import tech.brettsaunders.craftory.tech.power.core.block.generators.solar.SolarPanel;
import tech.brettsaunders.craftory.tech.power.core.block.machine.macerator.DiamondMacerator;
import tech.brettsaunders.craftory.tech.power.core.block.machine.macerator.EmeraldMacerator;
import tech.brettsaunders.craftory.tech.power.core.block.machine.macerator.GoldMacerator;
import tech.brettsaunders.craftory.tech.power.core.block.machine.macerator.IronMacerator;
import tech.brettsaunders.craftory.tech.power.core.block.machine.manipulators.BlockBreaker;
import tech.brettsaunders.craftory.tech.power.core.block.machine.turret.ArrowTurret;
import tech.brettsaunders.craftory.tech.power.core.block.machine.magnetiser.MagnetisingTable;
import tech.brettsaunders.craftory.tech.power.core.block.powerGrid.PowerConnector;
import tech.brettsaunders.craftory.tech.power.core.tools.ToolManager;
import tech.brettsaunders.craftory.utils.FileUtils;
import tech.brettsaunders.craftory.utils.Logger;

public class Utilities {

  public static final BlockFace[] faces = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH,
      BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};
  public final static String DATA_FOLDER;
  public final static String LANG_FOLDER;
  public static FileConfiguration config;
  public static FileConfiguration data;
  private static File configFile = new File(Craftory.plugin.getDataFolder(), "config.yml");
  private static File dataFile = new File(Craftory.plugin.getDataFolder(), "data.yml");
  private static String UNIT_ENERGY = "Re";
  private static String UNIT_FLUID = "B";
  private static DecimalFormat df = new DecimalFormat("###.###");
  public static Metrics metrics;

  public static Properties langProperties;
  @Getter
  private static HashMap<String, BasicBlocks> basicBlockRegistry;

  public static Optional<AdvancementManager> advancementManager = Optional.empty();

  static {
    config = YamlConfiguration
        .loadConfiguration(new File(Craftory.plugin.getDataFolder(), "config.yml"));
    data = YamlConfiguration
        .loadConfiguration(new File(Craftory.plugin.getDataFolder(), "data.yml"));
    DATA_FOLDER = Craftory.plugin.getDataFolder().getPath() + File.separator + "data";
    LANG_FOLDER = Craftory.plugin.getDataFolder().getPath() + File.separator + "lang";
    basicBlockRegistry = new HashMap<>();
  }

  static void pluginBanner() {
    drawBanner("&2-------------------------------------------");
    drawBanner("&2   _____            __ _                   ");
    drawBanner("&2  / ____|          / _| |                  ");
    drawBanner("&2 | |     _ __ __ _| |_| |_ ___  _ __ _   _ ");
    drawBanner("&2 | |    | '__/ _` |  _| __/ _ \\| '__| | | |");
    drawBanner("&2 | |____| | | (_| | | | || (_) | |  | |_| |");
    drawBanner("&2  \\_____|_|  \\__,_|_|  \\__\\___/|_|   \\__, |");
    drawBanner("&2                                      __/ |");
    drawBanner("&2                                     |___/ ");
    drawBanner("&2-------------------------------------------");
  }

  static void checkVersion() {
    new UpdateChecker(Craftory.plugin, Craftory.SPIGOT_ID).getVersion(version -> {
      if (Craftory.VERSION.equalsIgnoreCase(version)) {
        Logger.info("Plugin is update to date!");
      } else {
        Logger.info("There is a new update available!");
      }
    });
  }

  static void setupAdvancements() {
    if (Bukkit.getPluginManager().isPluginEnabled("CrazyAdvancementsAPI")) {
      advancementManager = Optional.ofNullable(CrazyAdvancements.getNewAdvancementManager());
      new Advancements();
      Logger.info("Advancements are Enabled");
    }
  }

  static void createConfigs() {
    config.options().header("Craftory");
    config.options().header("Debug provides extra information about errors and in most cases shouldn't be used.");
    config.addDefault("general.debug", false);
    config.addDefault("general.techEnabled", true);
    config.options().header("Sets the language for Craftory to use. See Lang folder or Plugin page for options");
    config.addDefault("language.locale", "en-GB");
    config.addDefault("generators.solarDuringStorms", true);
    config.options().header("The resource pack is required. But if you are self hosting it, you can disable this.");
    config.addDefault("resourcePack.forcePack", true);
    config.options().copyHeader(true);
    config.options().copyDefaults(true);
    saveConfigFile();
    reloadConfigFile();

    data.options().header("Do Not Touch");
    data.addDefault("reporting.serverUUID", UUID.randomUUID().toString());
    data.options().copyHeader(true);
    data.options().copyDefaults(true);
    saveDataFile();
    reloadDataFile();
  }

  static void getTranslations() {
    String locale = config.getString("language.locale");
    Logger.info("Using "+locale + " locale" );
    Properties defaultLang = new Properties();
    try {
      defaultLang.load(new InputStreamReader(new FileInputStream(new File(Craftory.plugin.getDataFolder(),
          "lang/default_lang.properties")), Charset.forName("UTF-8")));
      langProperties = new Properties(defaultLang);
      langProperties.load(new InputStreamReader(new FileInputStream(new File(LANG_FOLDER, locale+".properties")), Charset.forName("UTF-8")));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  static void createDataPath() {
    File file = new File(DATA_FOLDER);
    if (!file.exists()) {
      file.mkdirs();
    }

    File modelData = new File(Craftory.plugin.getDataFolder(), "/config/customModelData.yml");
    if (!modelData.exists()) {
      FileUtils.copyResourcesRecursively(Craftory.plugin.getClass().getResource("/config"),
          new File(Craftory.plugin.getDataFolder(), "/config"));
    }

    FileUtils.copyResourcesRecursively(Craftory.plugin.getClass().getResource("/data"),
        new File(Craftory.plugin.getDataFolder(), "/data"));

    file = new File(LANG_FOLDER);
    if (!file.exists()) {
      file.mkdirs();
      FileUtils.copyResourcesRecursively(Craftory.plugin.getClass().getResource("/lang"), file);
    }
  }

  static void startMetrics() {
    metrics = new Metrics(Craftory.plugin, 7804);
    metrics.addCustomChart(
        new Metrics.SimplePie("debug_enabled", () -> config.getString("general.debug")));
    metrics.addCustomChart(
        new Metrics.SimplePie("tech_enabled", () -> config.getString("general.techEnabled")));
    metrics.addCustomChart(new AdvancedPie("types_of_machines",
        () -> {
          Map<String, Integer> valueMap = new HashMap<>();
          //valueMap.put("totalCustomBlocks",Craftory.customBlockManager.statsContainer.getTotalCustomBlocks());
          //valueMap.put("totalPoweredBlocks",Craftory.customBlockManager.statsContainer.getTotalPoweredBlocks());
          valueMap.put("totalCells",Craftory.customBlockManager.statsContainer.getTotalCells());
          valueMap.put("totalGenerators",Craftory.customBlockManager.statsContainer.getTotalGenerators());
          valueMap.put("totalPowerConnectors",Craftory.customBlockManager.statsContainer.getTotalPowerConnectors());
          valueMap.put("totalMachines",Craftory.customBlockManager.statsContainer.getTotalMachines());
          return valueMap;
        }));
    metrics.addCustomChart(new SingleLineChart("total_custom_blocks", () -> Craftory.customBlockManager.statsContainer.getTotalCustomBlocks()));
    metrics.addCustomChart(new SingleLineChart("total_powered_blocks", () -> Craftory.customBlockManager.statsContainer.getTotalPoweredBlocks()));
  }

  static void registerCommandsAndCompletions() {
    Craftory.plugin.getCommand("craftory").setExecutor(new CommandWrapper());
    Craftory.plugin.getCommand("cr").setExecutor(new CommandWrapper());
    Craftory.plugin.getCommand("craftory").setTabCompleter(new CommandWrapper());
    Craftory.plugin.getCommand("cr").setTabCompleter(new CommandWrapper());
  }

  static void registerEvents() {
    new ToolManager();
  }

  static void registerCustomBlocks() {
    CustomBlockFactory customBlockFactory = Craftory.customBlockFactory;
    customBlockFactory.registerCustomBlock(Blocks.IRON_CELL, IronCell.class, true, false);
    customBlockFactory.registerCustomBlock(Blocks.GOLD_CELL, GoldCell.class, true, false);
    customBlockFactory.registerCustomBlock(Blocks.DIAMOND_CELL, DiamondCell.class, true, false);
    customBlockFactory.registerCustomBlock(Blocks.EMERALD_CELL, EmeraldCell.class, true, false);
    customBlockFactory.registerCustomBlock(Blocks.IRON_ELECTRIC_FURNACE, IronElectricFurnace.class, true, false);
    customBlockFactory.registerCustomBlock(Blocks.GOLD_ELECTRIC_FURNACE, GoldElectricFurnace.class, true, false);
    customBlockFactory.registerCustomBlock(Blocks.DIAMOND_ELECTRIC_FURNACE, DiamondElectricFurnace.class, true, false);
    customBlockFactory.registerCustomBlock(Blocks.EMERALD_ELECTRIC_FURNACE, EmeraldElectricFurnace.class, true, false);
    customBlockFactory.registerCustomBlock(Blocks.IRON_ELECTRIC_FOUNDRY, IronElectricFoundry.class, true, false);
    customBlockFactory.registerCustomBlock(Blocks.GOLD_ELECTRIC_FOUNDRY, GoldElectricFoundry.class, true, false);
    customBlockFactory.registerCustomBlock(Blocks.DIAMOND_ELECTRIC_FOUNDRY, DiamondElectricFoundry.class, true, false);
    customBlockFactory.registerCustomBlock(Blocks.EMERALD_ELECTRIC_FOUNDRY, EmeraldElectricFoundry.class, true, false);
    customBlockFactory.registerCustomBlock(Blocks.SOLID_FUEL_GENERATOR, SolidFuelGenerator.class, true, false);
    customBlockFactory.registerCustomBlock(Blocks.IRON_FOUNDRY, IronFoundry.class, true, false);
    customBlockFactory.registerCustomBlock(Blocks.POWER_CONNECTOR, PowerConnector.class, false, false);
    customBlockFactory.registerCustomBlock(Blocks.IRON_MACERATOR, IronMacerator.class, true, false);
    customBlockFactory.registerCustomBlock(Blocks.GOLD_MACERATOR, GoldMacerator.class, true, false);
    customBlockFactory.registerCustomBlock(Blocks.DIAMOND_MACERATOR, DiamondMacerator.class, true, false);
    customBlockFactory.registerCustomBlock(Blocks.EMERALD_MACERATOR, EmeraldMacerator.class, true, false);
    customBlockFactory.registerCustomBlock(Blocks.TURRET_PLATFORM, ArrowTurret.class, true, false);
    customBlockFactory.registerCustomBlock(Blocks.BASIC_SOLAR_PANEL, BasicSolarPanel.class, true, false);
    customBlockFactory.registerCustomBlock(Blocks.SOLAR_PANEL, SolarPanel.class, true, false);
    customBlockFactory.registerCustomBlock(Blocks.COMPACTED_SOLAR_PANEL, CompactedSolarPanel.class, true, false);
    customBlockFactory.registerCustomBlock(Blocks.SOLAR_ARRAY, SolarArray.class, true, false);
    customBlockFactory.registerCustomBlock(Blocks.GEOTHERMAL_GENERATOR, GeothermalGenerator.class, true, false);
    customBlockFactory.registerCustomBlock(Blocks.WIND_GENERATOR, WindGenerator.class, true, false);
    customBlockFactory.registerCustomBlock(Blocks.MAGNETISER, Magnetiser.class, true, false);
    customBlockFactory.registerCustomBlock(Blocks.MAGNETISING_TABLE, MagnetisingTable.class, false, false);
    customBlockFactory.registerCustomBlock(Blocks.BLOCK_BREAKER, BlockBreaker.class, true, true);
  }

  static void registerBasicBlocks() {
    basicBlockRegistry.put(Blocks.COPPER_ORE, BasicBlocks.COPPER_ORE);
    basicBlockRegistry.put(Blocks.CRYSTAL_ORE, BasicBlocks.CRYSTAL_ORE);
  }

  static void done() {
    Bukkit.getLogger().info(
        "[" + Craftory.plugin.getDescription().getPrefix() + "] " + ChatColor.GREEN
            + "Finished Loading!");
  }

  /* Helper Functions */
  public static void reloadConfigFile() {
    if (configFile == null) {
      configFile = new File(Craftory.plugin.getDataFolder(), "config.yml");
    }
    config = YamlConfiguration.loadConfiguration(configFile);
  }

  public static void reloadDataFile() {
    if (dataFile == null) {
      dataFile = new File(Craftory.plugin.getDataFolder(), "data.yml");
    }
    data = YamlConfiguration.loadConfiguration(dataFile);
  }

  public static void saveDataFile() {
    if (data == null || dataFile == null) {
      return;
    }
    try {
      data.save(dataFile);
    } catch (IOException ex) {
      Bukkit.getLogger().log(Level.SEVERE, "Could not save " + dataFile, ex);
    }
  }

  public static void saveConfigFile() {
    if (config == null || configFile == null) {
      return;
    }
    try {
      config.save(configFile);
    } catch (IOException ex) {
      Bukkit.getLogger().log(Level.SEVERE, "Could not save " + configFile, ex);
    }
  }

  public static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }

  public static void msg(final CommandSender s, String msg) {
    if (s instanceof Player) {
      msg = ChatColor.translateAlternateColorCodes('&', msg);
    } else {
      msg = ChatColor.translateAlternateColorCodes('&', "[Craftory]" + msg);
    }
    s.sendMessage(msg);
  }

  private static void drawBanner(final String message) {
    Bukkit.getServer().getConsoleSender().sendMessage(
        ChatColor.translateAlternateColorCodes('&', message));
  }

  public static String getRegionID(Chunk chunk) {
    int regionX = chunk.getX() >> 5;
    int regionZ = chunk.getZ() >> 5;
    return "r." + regionX + "," + regionZ + ".nbt";
  }

  public static String getChunkID(Chunk chunk) {
    return chunk.getX() + "," + chunk.getZ();
  }

  public static String getChunkWorldID(Chunk chunk) {
    return chunk.getWorld().getName() + "," + getChunkID(chunk);
  }

  public static String convertWorldChunkIDToChunkID(String worldChunkID) {
    return worldChunkID.replaceFirst(".*?,","");
  }

  public static String getLocationID(Location location) {
    return location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
  }

  @Synchronized
  public static Location keyToLoc(String key, World world) {
    String[] locationData = key.split(",");
    return new Location(world, Double.parseDouble(locationData[0]),
        Double.parseDouble(locationData[1]), Double.parseDouble(locationData[2]));
  }

  public static String rawEnergyToPrefixed(Integer energy) {
    String s = Integer.toString(energy);
    int length = s.length();
    if(length < 6) return s + " " + UNIT_ENERGY;
    /*if (length < 7) {
      return s + " " + UNIT;
    }*/
    if(length < 7) return df.format(energy/1000f) + " K" + UNIT_ENERGY;
    if (length < 10) {
      return df.format(energy / 1000000f) + " M" + UNIT_ENERGY;
    }
    if (length < 13) {
      return df.format(energy / 1000000000f) + " G" + UNIT_ENERGY;
    }
    if (length < 16) {
      return df.format(energy / 1000000000000f) + " T" + UNIT_ENERGY;
    }
    if (length < 19) {
      return df.format(energy / 1000000000000000f) + " P" + UNIT_ENERGY;
    }
    if (length < 22) {
      return df.format(energy / 1000000000000000000f) + " E" + UNIT_ENERGY;
    }
    return "A bukkit load";
  }

  public static String rawFluidToPrefixed(Integer amount) {
    String s = Integer.toString(amount);
    int length = s.length();
    if(length < 6) return s + " m" + UNIT_FLUID;
    if(length < 7) return s + " " + UNIT_FLUID;
    if(length < 10) return df.format(amount / 1000000f) + " K" + UNIT_FLUID;
    if(length < 13) return df.format(amount / 1000000000f) + " M" + UNIT_FLUID;
    if(length < 16) return df.format(amount / 1000000f) + " G" + UNIT_FLUID;
    if(length < 19) return df.format(amount / 1000000f) + " T" + UNIT_FLUID;
    if(length < 22) return df.format(amount / 1000000f) + " P" + UNIT_FLUID;
    return "A bukkit load";
  }
}
