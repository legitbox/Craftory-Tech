/*******************************************************************************
 * Copyright (c) 2020. BrettSaunders & Craftory Team - All Rights Reserved
 *
 * This file is part of Craftory.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential
 *
 * File Author: Brett Saunders & Matty Jones
 ******************************************************************************/

package tech.brettsaunders.craftory.commands;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import tech.brettsaunders.craftory.Craftory;
import tech.brettsaunders.craftory.Utilities;

public class Command_Main implements CommandExecutor, TabCompleter {

  public boolean onCommand(final CommandSender sender, final Command command, final String label,
      final String[] args) {
    Utilities.msg(sender, Utilities.getTranslation("MainCommandLineOne") + Craftory.VERSION);
    Utilities.msg(sender, Utilities.getTranslation("MainCommandLineTwo") + " ©");
    return true;
  }

  public List<String> onTabComplete(CommandSender s, Command c, String label, String[] args) {
    ArrayList<String> tabs = new ArrayList<>();
    tabs.add("help");
    tabs.add("toggleDebug");
    tabs.add("give");
    tabs.add("fixGraphics");
    tabs.add("recipeBook");
    return CommandWrapper.filterTabs(tabs, args);
  }
}
