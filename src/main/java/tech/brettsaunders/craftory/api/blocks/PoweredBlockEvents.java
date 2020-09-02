/*******************************************************************************
 * Copyright (c) 2020. BrettSaunders & Craftory Team - All Rights Reserved
 *
 * This file is part of Craftory.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential
 *
 * File Author: Brett Saunders & Matty Jones
 ******************************************************************************/

package tech.brettsaunders.craftory.api.blocks;

import static tech.brettsaunders.craftory.Craftory.customBlockManager;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import tech.brettsaunders.craftory.CoreHolder;
import tech.brettsaunders.craftory.CoreHolder.Blocks;
import tech.brettsaunders.craftory.CoreHolder.Items;
import tech.brettsaunders.craftory.Craftory;
import tech.brettsaunders.craftory.Utilities;
import tech.brettsaunders.craftory.api.blocks.events.CustomBlockBreakEvent;
import tech.brettsaunders.craftory.api.blocks.events.CustomBlockInteractEvent;
import tech.brettsaunders.craftory.api.blocks.events.CustomBlockPlaceEvent;
import tech.brettsaunders.craftory.api.events.Events;
import tech.brettsaunders.craftory.api.items.CustomItemManager;
import tech.brettsaunders.craftory.tech.power.api.block.BaseProvider;
import tech.brettsaunders.craftory.tech.power.api.block.PoweredBlock;
import tech.brettsaunders.craftory.tech.power.core.block.generators.RotaryGenerator;
import tech.brettsaunders.craftory.tech.power.core.powerGrid.PowerGrid;

public class PoweredBlockEvents implements Listener {

  private final Object2ObjectOpenHashMap<UUID, Object2ObjectOpenHashMap<BlockFace, Boolean>> configuratorData = new Object2ObjectOpenHashMap<>();

  public PoweredBlockEvents() {
    Events.registerEvents(this);
  }

  @EventHandler
  public void onPoweredBlockPlace(CustomBlockPlaceEvent e) {
    if (PoweredBlockUtils.isEnergyReceiver(e.getCustomBlock())) {
      PoweredBlockUtils.updateAdjacentProviders(e.getLocation(), true, e.getCustomBlock());
    }
    if (e.getName().equals(Blocks.POWER_CONNECTOR)) {
      PowerGrid powerGrid = new PowerGrid();
      powerGrid.addPowerConnector(e.getLocation());
      Craftory.powerGridManager.getAdjacentPowerBlocks(e.getLocation(), powerGrid);
      Craftory.powerGridManager.addPowerGrid(e.getLocation(), powerGrid);
    }
  }

  @EventHandler
  public void onPoweredBlockBreak(CustomBlockBreakEvent e) {
    //Update Providers
    if (PoweredBlockUtils.isEnergyReceiver(e.getCustomBlock())) {
      PoweredBlockUtils.updateAdjacentProviders(e.getLocation(), false, e.getCustomBlock());
    }
    //Drop Inventory
    if (PoweredBlockUtils.isPoweredBlock(e.getCustomBlock())) {
      PoweredBlock poweredBlock = (PoweredBlock) e.getCustomBlock();
      World world = e.getLocation().getWorld();
      Inventory inventory = poweredBlock.getInventory();
      if (inventory != null) {
        ItemStack item;
        for (Integer i : poweredBlock.getInteractableSlots()) {
          item = inventory.getItem(i);
          if (item != null) {
            world.dropItemNaturally(e.getLocation(), item);
          }
        }
      }
    }
  }

  @EventHandler
  public void onWrenchClick(CustomBlockInteractEvent e) {
    if (e.getAction() != Action.LEFT_CLICK_BLOCK) {
      return;
    }
    if (!CustomItemManager.matchCustomItemName(e.getItemStack(), CoreHolder.Items.WRENCH)) {
      return;
    }
    e.setCancelled(true);
    //Show power levels
    if (PoweredBlockUtils.isPoweredBlock(e.getCustomBlock())) {
      PoweredBlock block = (PoweredBlock) e.getCustomBlock();
      e.getPlayer().sendMessage(
          Utilities.getTranslation("EnergyStored") + ": " + block.getInfoEnergyStored() + " RE / "
              + block.getInfoEnergyCapacity()
              + " RE");
    }
  }

  @EventHandler
  public void onWrenchRightClick(CustomBlockInteractEvent e) {
    if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    if (!CustomItemManager.matchCustomItemName(e.getItemStack(), CoreHolder.Items.WRENCH)) return;
    if (!e.getPlayer().isSneaking()) return;
    if (!PoweredBlockUtils.isPoweredBlock(e.getCustomBlock())) return;
    PoweredBlock poweredBlock = (PoweredBlock) e.getCustomBlock();

    //Break Block
    poweredBlock.blockBreak();
    CustomBlockBreakEvent customBlockBreakEvent = new CustomBlockBreakEvent(
        e.getCustomBlock().getLocation(), e.getCustomBlock().blockName, e.getCustomBlock());
    customBlockManager.removeCustomBlock(e.getCustomBlock());
    Craftory.tickManager.removeTickingBlock(e.getCustomBlock());
    Bukkit.getPluginManager().callEvent(customBlockBreakEvent);
    e.getBlockClicked().setType(Material.AIR);
    //calculateStatsDecrease(customBlock);

    //Drop Item With Data
    ItemStack itemStack = CustomItemManager.getCustomItem(e.getCustomBlock().blockName);
    NBTItem nbtItem = new NBTItem(itemStack);
    NBTCompound extraData = nbtItem.addCompound("extraData");

    //Calculate Energy Storage
    int energyStored = poweredBlock.getEnergyStorage().getEnergyStored();
    double amount = (100d - (double)Utilities.config.getInt("wrench.powerLoss"))/100d;
    double energyAfter = energyStored * amount;
    extraData.setInteger("energyStorage", (int) energyAfter);

    //Drop Item
    e.getBlockClicked().getLocation().getWorld().dropItemNaturally(e.getBlockClicked().getLocation(), nbtItem.getItem());

  }

  @EventHandler
  public void onConfiguratorClick(CustomBlockInteractEvent e) {
    if (!CustomItemManager.matchCustomItemName(e.getItemStack(), CoreHolder.Items.CONFIGURATOR)) {
      return;
    }
    e.setCancelled(true);

    final Player player = e.getPlayer();
    if (e.getAction() == Action.RIGHT_CLICK_AIR && player.isSneaking()) {
      configuratorData.remove(player.getUniqueId());
      player.sendMessage(Utilities.getTranslation("SideConfigClear"));
    }
    if (PoweredBlockUtils.isEnergyProvider(e.getCustomBlock())) {
      BaseProvider provider = (BaseProvider) e.getCustomBlock();
      if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
        configuratorData.put(player.getUniqueId(), provider.getSideConfig());
        player.sendMessage(Utilities.getTranslation("SideConfigCopied"));
      } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
        if (configuratorData.containsKey(player.getUniqueId())) {
          provider.setSidesConfig(configuratorData.get(player.getUniqueId()));
          player.sendMessage(Utilities.getTranslation("SideConfigPasted"));
        } else {
          player.sendMessage(Utilities.getTranslation("SideConfigNoData"));
        }
      }
    }
  }

  @EventHandler
  public void onRenewableClick(CustomBlockInteractEvent e) {
    if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }
    if ((e.getCustomBlock() instanceof RotaryGenerator) && (
        CustomItemManager.matchCustomItemName(e.getItemStack(), Items.WINDMILL) || CustomItemManager
            .matchCustomItemName(e.getItemStack(), Items.WATER_WHEEL))) {
      RotaryGenerator generator = (RotaryGenerator) e.getCustomBlock();
      if (generator.getWheelPlaced()) {
        return;
      }
      if (!generator.setFacing(e.getBlockFace())) {
        e.getPlayer().sendMessage(ChatColor.RED
            + "WaterWheels/WindMills require 7x7 clearance two blocks in front of them"); //TODO Brett make lang
      }
      if (generator.placeItemIn(e.getItemStack().clone())) {
        e.getItemStack().setAmount(e.getItemStack().getAmount() - 1);
      }

    }
  }

  @EventHandler
  public void onGUIBlockClick(CustomBlockInteractEvent e) {
    if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }
    if ((e.getPlayer().isSneaking() || CustomItemManager
        .matchCustomItemName(e.getItemStack(), CoreHolder.Items.CONFIGURATOR))) {
      return;
    }

    if (PoweredBlockUtils.isPoweredBlock(e.getCustomBlock())) {
      PoweredBlock poweredBlock = (PoweredBlock) e.getCustomBlock();
      //Open GUI of Powered Block
      poweredBlock.openGUI(e.getPlayer());
      e.getBaseEvent().setCancelled(true);
    }
  }

  @EventHandler
  public void onVanillaBlockBreak(BlockBreakEvent event) {
    Block block = event.getBlock();
    if (block.getType().equals(Material.HOPPER)) {
      PoweredBlockUtils.updateHopperNeighbour(block, false);
    }
  }

  @EventHandler
  public void onVanillaBlockPlace(BlockPlaceEvent event) {
    Block block = event.getBlockPlaced();
    if (block.getType().equals(Material.HOPPER)) {
      PoweredBlockUtils.updateHopperNeighbour(block, true);
    }
  }

}
