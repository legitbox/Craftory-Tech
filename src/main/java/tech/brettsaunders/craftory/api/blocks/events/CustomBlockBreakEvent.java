/*******************************************************************************
 * Copyright (c) 2021. Brett Saunders & Matthew Jones - All Rights Reserved
 ******************************************************************************/

package tech.brettsaunders.craftory.api.blocks.events;

import io.sentry.Sentry;
import io.sentry.event.Breadcrumb.Type;
import io.sentry.event.BreadcrumbBuilder;
import java.util.Date;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import tech.brettsaunders.craftory.api.blocks.CustomBlock;

public class CustomBlockBreakEvent extends Event implements Cancellable {

  private static final HandlerList HANDLERS = new HandlerList();
  @Getter
  private final Location location;
  @Getter
  private final String name;
  private boolean isCancelled;
  @Getter
  private final CustomBlock customBlock;

  public CustomBlockBreakEvent(Location location, String name, CustomBlock customBlock) {
    this.location = location;
    this.name = name;
    this.isCancelled = false;
    this.customBlock = customBlock;

    Sentry.getContext().recordBreadcrumb(new BreadcrumbBuilder()
        .setCategory("customBreakEvent")
        .setTimestamp(new Date(System.currentTimeMillis()))
        .setMessage("Break Custom Block "+name + " at location: "+location)
        .setType(Type.DEFAULT)
        .build());
  }

  public static HandlerList getHandlerList() {
    return HANDLERS;
  }

  @Override
  public HandlerList getHandlers() {
    return HANDLERS;
  }

  @Override
  public boolean isCancelled() {
    return this.isCancelled;
  }

  @Override
  public void setCancelled(boolean isCancelled) {
    this.isCancelled = isCancelled;
  }
}
