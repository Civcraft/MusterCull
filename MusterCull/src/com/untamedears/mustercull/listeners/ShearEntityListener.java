package com.untamedears.mustercull.listeners;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Colorable;

import com.untamedears.mustercull.MusterCull;

public class ShearEntityListener extends Listener{

	public ShearEntityListener(MusterCull pluginInstance) {
		super(pluginInstance);
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled=true)
	public void onPlayerShearSheep(PlayerShearEntityEvent event){
		if(event.getEntity().getType() == EntityType.SHEEP){
			Entity sheep = event.getEntity();
			if(this.getPluginInstance().getMergedEntities().containsKey(sheep)){
				DyeColor color = ((Colorable) sheep).getColor();
				int multiplier = this.getPluginInstance().getMultiplier(sheep);
				ItemStack wool = new ItemStack(Material.WOOL, multiplier ,color.getData());
				sheep.getWorld().dropItem(sheep.getLocation(), wool);
			}
		}
	}
}
