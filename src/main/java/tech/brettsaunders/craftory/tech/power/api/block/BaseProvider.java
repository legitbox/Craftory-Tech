package tech.brettsaunders.craftory.tech.power.api.block;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import tech.brettsaunders.craftory.Craftory;
import tech.brettsaunders.craftory.tech.power.api.interfaces.IEnergyProvider;
import tech.brettsaunders.craftory.utils.Logger;

public abstract class BaseProvider extends PoweredBlock implements IEnergyProvider,
    Externalizable {

  /* Static Constants Protected */
  protected static final Boolean[] DEFAULT_SIDES_CONFIG = {false, false, false, false, false,
      false};  //NORTH, EAST, SOUTH, WEST, UP, DOWN
  /* Static Constants Private */
  private static final long serialVersionUID = 10008L;
  /* Per Object Variables Saved */
  protected int maxOutput;
  protected ArrayList<Boolean> sidesConfig;
  protected ArrayList<Boolean> sidesCache;

  /* Per Object Variables Not-Saved */


  /* Construction */
  public BaseProvider(Location location, byte level, int maxOutput) {
    super(location, level);
    this.maxOutput = maxOutput;
    init();
    Collections.addAll(sidesConfig, DEFAULT_SIDES_CONFIG);
    generateSideCache();
  }

  /* Saving, Setup and Loading */
  public BaseProvider() {
    super();
    init();
  }

  /* Common Load and Construction */
  private void init() {
    sidesConfig = new ArrayList<>(6);
    sidesCache = new ArrayList<>(6);
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeObject(sidesConfig);
    out.writeObject(sidesCache);
    out.writeInt(maxOutput);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    sidesConfig = (ArrayList<Boolean>) in.readObject();
    sidesCache = (ArrayList<Boolean>) in.readObject();
    maxOutput = in.readInt();
  }

  /* Update Loop */
  protected void transferEnergy() {
    int amountTransferred = 0;
    for (int i = 0; i < sidesConfig.size(); i++) {
      if (sidesConfig.get(i)) {
        if (sidesCache.get(i)) {
          amountTransferred += energyStorage
              .modifyEnergyStored(-insertEnergyIntoAdjacentEnergyReceiver(i,
                  Math.min(maxOutput, energyStorage.getEnergyStored()), false));
        }
      }
    }
  }

  @Override
  public void update(long worldTime) {
    super.update(worldTime);
    transferEnergy();
  }

  //TODO compare to energyStorage.extractEnergy
  public int retrieveEnergy(int energy) {
    int energyExtracted = Math.min(getEnergyStored(), Math.min(energy, maxOutput));
    energyStorage.modifyEnergyStored(-energyExtracted);
    return energyExtracted;
  }


  /* Internal Helper Functions */
  @Override
  public boolean updateOutputCache(BlockFace inputFrom, Boolean setTo) {
    //NORTH, EAST, SOUTH, WEST, UP, DOWN
    int side = -1;
    switch (inputFrom) {
      case NORTH:
        side = 0;
        break;
      case EAST:
        side = 1;
        break;
      case SOUTH:
        side = 2;
        break;
      case WEST:
        side = 3;
        break;
      case UP:
        side = 4;
        break;
      case DOWN:
        side = 5;
        break;
    }
    if (side != -1) {
      sidesCache.set(side, setTo);
      return true;
    }
    return false;
  }

  public int insertEnergyIntoAdjacentEnergyReceiver(int side, int energy, boolean simulate) {
    Location targetLocation = this.location.getBlock().getRelative(faces[side]).getLocation();
    if (Craftory.getBlockPoweredManager().isReceiver(targetLocation)) {
      if (Craftory.getBlockPoweredManager().isProvider(targetLocation)) {
        return ((BaseCell) Craftory.getBlockPoweredManager().getPoweredBlock(targetLocation))
            .receiveEnergy(energy, simulate);
      } else {
        return ((BaseMachine) Craftory.getBlockPoweredManager().getPoweredBlock(targetLocation))
            .receiveEnergy(energy, simulate);
      }
    } else {
      sidesCache.set(side, false);
    }
    return 0;
  }

  private void generateSideCache() {
    int i = 0;
    for (BlockFace face : faces) {
      if (Craftory.getBlockPoweredManager()
          .isReceiver(this.location.getBlock().getRelative(face).getLocation())) {
        sidesCache.add(i, true);
        Logger.info("Cached side " + i);
      } else {
        sidesCache.add(i, false);
      }
      i++;
    }
  }

  public ArrayList<Boolean> getSideConfig() {
    return sidesConfig;
  }

  public void setSidesConfig(ArrayList<Boolean> config) {
    sidesConfig.clear();
    sidesConfig.addAll(config);
  }

  /* IEnergyHandler */
  @Override
  public int getEnergyStored() {
    return energyStorage.getEnergyStored();
  }

  @Override
  public int getMaxEnergyStored() {
    return energyStorage.getMaxEnergyStored();
  }

  /* IEnergyInfo */
  @Override
  public int getInfoMaxEnergyPerTick() {
    return maxOutput;
  }

  /* IEnergyConnection */
  @Override
  public boolean canConnectEnergy() {
    return true;
  }

  /* External Methods */
  public int getMaxOutput() {
    return maxOutput;
  }

  public int getEnergyAvailable() {
    return Math.min(energyStorage.energy, maxOutput);
  }
}
