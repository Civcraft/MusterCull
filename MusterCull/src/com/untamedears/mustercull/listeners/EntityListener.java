package com.untamedears.mustercull.listeners;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDeathEvent;

import com.untamedears.mustercull.MusterCull;
import com.untamedears.mustercull.configurations.ConfigurationLimit;
import com.untamedears.mustercull.culltypes.CullType;

/**
 * This class provides event handlers for game entities.
 * @author Celdecea
 *
 */
public class EntityListener extends Listener {

	/**
	 * This constructor wraps the parent Listener's constructor.
	 * @param pluginInstance A reference to the plug-in instance.
	 */
	public EntityListener(MusterCull pluginInstance) {
		super(pluginInstance);
	}

	/**
	 * This handler is called when an entity is spawning.
	 * @param event A reference to the associated Bukkit event.
	 */
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
	public void onCreatureSpawn(CreatureSpawnEvent event) {

		/* If over hard mob limit, stop all mob spawning.
		 * The entity in this event isn't included in overHardMobLimit so account for this. */
		if (getPluginInstance().overHardMobLimit() + 1 > 0) {
			/* Always let a player join. */
			if (! (event.getEntity() instanceof Player)
				&& (! (event.getEntity() instanceof ArmorStand))) {
				event.setCancelled(true);
				return;
			}
		}
		
		Entity entity = event.getEntity();
		ConfigurationLimit limit = null;
		
		if (!this.getPluginInstance().isPaused(CullType.MERGE)) {
			
			limit = this.getPluginInstance().getLimit(entity.getType(), CullType.MERGE);
				
			if (limit != null) {		
				event.setCancelled(this.getPluginInstance().mergeEntity(entity, limit));
				return;
			}	
		}
		
		if (!this.getPluginInstance().isPaused(CullType.SPAWNER)) {
			if (event.getSpawnReason() == SpawnReason.SPAWNER || event.getSpawnReason() == SpawnReason.NETHER_PORTAL) {
				
				limit = this.getPluginInstance().getLimit(entity.getType(), CullType.SPAWNER);
				
				if (limit != null) {
					event.setCancelled(this.getPluginInstance().runEntityChecks(entity, limit));
					return;
				}	
			}
		}
		
		if (!this.getPluginInstance().isPaused(CullType.SPAWN)) {
			if (event.getSpawnReason() != SpawnReason.SPAWNER) {
				limit = this.getPluginInstance().getLimit(entity.getType(), CullType.SPAWN);
				
				if (limit != null) {
					event.setCancelled(this.getPluginInstance().runEntityChecks(entity, limit));
					return;
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
	public void onCreatureDeath(EntityDeathEvent event) {
		if(this.getPluginInstance().getMergedEntities().get(event.getEntity())!=null){
			this.getPluginInstance().AdjustDropsOfMergedEntity(event.getEntity(), event.getDrops());
			int multiplier = this.getPluginInstance().getMultiplier(event.getEntity());
			this.getPluginInstance().getLogger().info("A merged entity was killed " + event.getEntity().toString() + " at " + event.getEntity().getLocation().toString() + " multiplier: " + multiplier);
			this.getPluginInstance().getMergedEntities().remove(event.getEntity());
		}
	}
	
}
