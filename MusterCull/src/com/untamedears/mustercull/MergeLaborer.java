package com.untamedears.mustercull;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Entity;

public class MergeLaborer extends Laborer {

	/**
	 * Constructor which takes a reference to the main plug-in class.
	 * @param pluginInstance A reference to the main plug-in class.
	 */
	public MergeLaborer(MusterCull pluginInstance) {
		super(pluginInstance);
	}
	

	/**
	 * Repeating merge method for the class.
	 */
	@Override
	public void run() {
		if (this.getPluginInstance().isPaused(CullType.MERGE)) {
			return;
		}

		int MergeCalls = this.getPluginInstance().getMergeCalls();
		
		for (int i = 0; i < MergeCalls; i++)
		{
			EntityLimitPair entityLimitPair = this.getPluginInstance().getNextEntityMerge();
			if (entityLimitPair == null) {
				return;
			}
			
			Entity entity = entityLimitPair.getEntity();
			if (entity == null || entity.isDead()) {
				return;
			}
			
			ConfigurationLimit limit = entityLimitPair.getLimit();
			
			if (limit.getCulling() != CullType.MERGE) {
				return;
			}
			// Loop through entities in range and count similar entities.
			int MergedEntities = 0;
			List<Entity> Normalentities = new ArrayList<Entity>();
			for (Entity otherEntity : this.getPluginInstance().getNearbyEntities(entity, limit.getRange(), entity.getType().getEntityClass())) {
				if (0 == otherEntity.getType().compareTo(entity.getType())) {
					if(this.getPluginInstance().getMergedEntities().get(otherEntity) == null){
						Normalentities.add(otherEntity);
					} else {
						MergedEntities++;
					}
				}
			}
			// Loop through the normal entities in range, if we have reached the limit merge them into living entities otherwise merge them into dead entities/new entities.
			Entity e = null;
			for(Entity normalEntity : Normalentities){
				if(MergedEntities >= limit.getLimit()){
					e = this.getPluginInstance().addMergedEntityToMap(normalEntity, limit, false, false);
					normalEntity.remove();
				} else {
					e = this.getPluginInstance().addMergedEntityToMap(normalEntity, limit, true, false);
					if(e != null){
						normalEntity.remove();
					} else {
						this.getPluginInstance().addMergedEntityToMap(normalEntity, limit, true, true);
					}
					MergedEntities++;
				}
			}
		}
	}

}
