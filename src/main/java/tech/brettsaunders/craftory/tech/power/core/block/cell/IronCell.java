/*******************************************************************************
 * Copyright (c) 2020. BrettSaunders & Craftory Team - All Rights Reserved
 *
 * This file is part of Craftory.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential
 *
 * File Author: Brett Saunders & Matty Jones
 ******************************************************************************/

package tech.brettsaunders.craftory.tech.power.core.block.cell;

import org.bukkit.Location;
import tech.brettsaunders.craftory.Constants.Blocks;
import tech.brettsaunders.craftory.tech.power.api.block.BaseCell;

/**
 * Energy Cell
 * <p>
 * Capacity: 400,000 Max Input: 200 Max Output: 200 Level: 0 (IRON)
 */
public class IronCell extends BaseCell {

  /* Static Constants Private */
  private static final byte C_LEVEL = 0;
  private static final int C_OUTPUT_AMOUNT = 200;

  /* Construction */
  public IronCell(Location location) {
    super(location, Blocks.IRON_CELL, C_LEVEL, C_OUTPUT_AMOUNT);
  }

  /* Saving, Setup and Loading */
  public IronCell() {
    super();
  }

}
