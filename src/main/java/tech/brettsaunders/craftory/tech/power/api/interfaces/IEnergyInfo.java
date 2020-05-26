package tech.brettsaunders.craftory.tech.power.api.interfaces;

/**
 * Implement this interface on objects which can report information about their energy usage.
 *
 * This is used for reporting purposes - Energy transactions are handled via the RF API!
 */

public interface IEnergyInfo {

  /**
   * Returns energy usage/generation per tick (Power/t).
   */
  int getInfoEnergyPerTick();

  /**
   * Returns maximum energy usage/generation per tick (Power/t).
   */
  int getInfoMaxEnergyPerTick();

  /**
   * Returns energy stored (Power).
   */
  int getInfoEnergyStored();



}