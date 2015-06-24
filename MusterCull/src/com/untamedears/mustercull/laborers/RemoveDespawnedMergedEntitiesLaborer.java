package com.untamedears.mustercull.laborers;

import org.bukkit.entity.Entity;

import com.untamedears.mustercull.MusterCull;
import com.untamedears.mustercull.configurations.ConfigurationLimit;
import com.untamedears.mustercull.culltypes.CullType;

public class RemoveDespawnedMergedEntitiesLaborer extends Laborer{
	
	/**
	 * Constructor which takes a reference to the main plug-in class.
	 * @param pluginInstance A reference to the main plug-in class.
	 */
	public RemoveDespawnedMergedEntitiesLaborer(MusterCull pluginInstance) {
		super(pluginInstance);
	}
	
	/**
	 * Repeating method that removes merged entities that despawned from the map.
	 */
	@Override
	public void run() {
		ConfigurationLimit limit = null;
		for(Entity entity: this.getPluginInstance().getMergedEntities().keySet()){
			limit = this.getPluginInstance().getLimit(entity.getType(), CullType.MERGE);
			
			if(limit == null){
				return;
			}
			
			if(entity.isDead() && this.getPluginInstance().getMergedEntities().get(entity) >= limit.getSpawnDelay()){
				this.getPluginInstance().getMergedEntities().remove(entity);
				this.getPluginInstance().getLogger().info("An "+ entity.toString() +" despawned and was removed from the map at " + entity.getLocation().toString());
			}
		}
	}

}
