package tech.brettsaunders.craftory.tech.power.core.powerGrid;

import de.tr7zw.changeme.nbtapi.NBTFile;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import tech.brettsaunders.craftory.Craftory;
import tech.brettsaunders.craftory.Utilities;
import tech.brettsaunders.craftory.api.blocks.PoweredBlockUtils;
import tech.brettsaunders.craftory.api.blocks.events.CustomBlockBreakEvent;
import tech.brettsaunders.craftory.persistence.PersistenceStorage;
import tech.brettsaunders.craftory.tech.power.api.block.BaseCell;
import tech.brettsaunders.craftory.tech.power.api.block.BaseGenerator;
import tech.brettsaunders.craftory.tech.power.api.block.BaseMachine;
import tech.brettsaunders.craftory.tech.power.api.block.PoweredBlock;
import tech.brettsaunders.craftory.utils.Logger;

public class PowerGridManager implements Listener {

  @Getter
  private final HashMap<Location, PowerGrid> powerGrids;
  private final PersistenceStorage persistenceStorage;
  private NBTFile nbtFile;

  public PowerGridManager() {
    persistenceStorage = new PersistenceStorage();
    try {
      nbtFile = new NBTFile(
          new File(Craftory.plugin.getDataFolder() + File.separator + "data", "PowerGrids.nbt"));
    } catch (Exception e) {
      e.printStackTrace();
    }
    powerGrids = new HashMap<>();
    Craftory.plugin.getServer().getPluginManager()
        .registerEvents(this, Craftory.plugin);
  }

  /* Events */
  @EventHandler
  public void onPoweredBlockBreak(CustomBlockBreakEvent event) {
    Location location = event.getLocation();
    if (powerGrids.containsKey(location)) { //GRID / Power connector stuff
      Craftory.powerConnectorManager.destroyBeams(location);
      if (powerGrids.get(location).getGridSize() > 1) {
        List<PowerGrid> newGrids = splitGrids(location,  powerGrids.get(location));
        for (Location l : powerGrids.get(location).getPowerConnectors().keySet()) {
          powerGrids.remove(l);
        }
        for (PowerGrid grid : newGrids) {
          for (Location loc : grid.getPowerConnectors().keySet()) {
            powerGrids.put(loc, grid);
          }
        }
      }
      powerGrids.remove(location);
    }
    if (PoweredBlockUtils.isPoweredBlock(location)) {
      Craftory.powerConnectorManager.destroyBeams(location); //Destroy any beams
      PoweredBlock poweredBlock = PoweredBlockUtils.getPoweredBlock(location);
      if (poweredBlock instanceof BaseMachine) {
        for (PowerGrid grid : new HashSet<>(powerGrids.values())) {
          if (grid.removeMachine(location)) {
            break;
          }
        }
      } else if (poweredBlock instanceof BaseCell) {
        for (PowerGrid grid : new HashSet<>(powerGrids.values())) {
          if (grid.removeCell(location)) {
            break;
          }
        }
      } else if (poweredBlock instanceof BaseGenerator) {
        for (PowerGrid grid : new HashSet<>(powerGrids.values())) {
          if (grid.removeGenerator(location)) {
            break;
          }
        }
      }
    }
  }

  @EventHandler
  public void onDisable(PluginDisableEvent e) {
    persistenceStorage.saveObject(powerGrids, nbtFile);
  }

  @EventHandler
  public void onEnable(PluginEnableEvent e) {
    persistenceStorage.loadObject(powerGrids, HashMap.class, nbtFile);
  }

  /* Grid Splitting */
  public void mergeGrids(PowerGrid old, PowerGrid merged) {
    for (HashMap.Entry<Location, PowerGrid> entry : powerGrids.entrySet()) {
      if (entry.getValue().equals(old)) {
        powerGrids.put(entry.getKey(), merged);
      }
    }
  }

  public void getAdjacentPowerBlocks(Location location, PowerGrid powerGrid) {
    Location blockLocation;
    for (BlockFace face : Utilities.faces) {
      blockLocation = location.getBlock().getRelative(face).getLocation();
      if (PoweredBlockUtils.isPoweredBlock(blockLocation)) {
        PoweredBlock poweredBlock = PoweredBlockUtils.getPoweredBlock(blockLocation);
        if (PoweredBlockUtils.isCell(poweredBlock)) {
          powerGrid.addPowerCell(location, blockLocation);
        } else if (PoweredBlockUtils.isGenerator(poweredBlock)) {
          powerGrid.addGenerator(location, blockLocation);
        } else if (PoweredBlockUtils.isMachine(poweredBlock)) {
          powerGrid.addMachine(location, blockLocation);
        }
      }
    }
  }

  public void addPowerGrid(Location location, PowerGrid manger) {
    powerGrids.put(location, manger);
  }

  public PowerGrid getPowerGrid(Location location) {
    return powerGrids.get(location);
  }

  public Boolean isPowerGrid(Location location) {
    return powerGrids.containsKey(location);
  }

  /**
   * Splits a power grid into the needed amount of new grids based on the power connector that broke
   * the grid
   *
   * @param breakPoint The location of the broken power connector
   * @return A list of the individual grids (could just be one)
   */
  public ArrayList<PowerGrid> splitGrids(Location breakPoint, PowerGrid powerGrid) {
    ArrayList<PowerGrid> managers = new ArrayList<>();
    powerGrid.getBlockConnections().remove(breakPoint);
    HashSet<Location> neighbours = powerGrid.getPowerConnectors().remove(breakPoint);
    Logger.info("connector had: " + neighbours.size());
    HashSet<Location> closedSet = new HashSet<>();
    neighbours.forEach(location -> { //Loop through all the neighbours of broken connector
      if (!closedSet.contains(location)) {
        closedSet.add(location);
        Logger.info("making new grid");
        PowerGrid grid = new PowerGrid();
        HashSet<Location> connections = powerGrid.getPowerConnectors().get(location);
        if (connections != null) {
          connections.remove(breakPoint);
          grid.getPowerConnectors().put(location, connections);
          grid.getBlockConnections().put(location, powerGrid.getBlockConnections().get(location));
          ArrayList<Location> openList = new ArrayList<>(connections);
          Location connection;
          while (openList.size() > 0) { //Add all its connections to the grid
            connection = openList.remove(0);
            if (closedSet.contains(connection)) {
              continue; //Skip if they are already in a grid
            }
            closedSet.add(connection);
            //Add it to the grid
            if (powerGrid.getBlockConnections().containsKey(connection)) {
              grid.getBlockConnections().put(connection, powerGrid.getBlockConnections().get(connection));
            }
            HashSet<Location> connectionConnections = powerGrid.getBlockConnections().get(connection);
            if (connectionConnections == null) {
              continue;
            }
            connectionConnections.remove(breakPoint);
            grid.getPowerConnectors().put(connection, connectionConnections);
            //Continue traversal
            connectionConnections.forEach(loc -> {
              if (!closedSet.contains(loc)) {
                openList.add(loc);
              }
            });
          }
        }
        grid.findPoweredBlocks();
        managers.add(grid);
      }

    });
    return managers;
  }



}