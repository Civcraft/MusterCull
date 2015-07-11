package com.untamedears.mustercull.laborers;

import org.bukkit.entity.Entity;

import com.untamedears.mustercull.EntityLimitPair;
import com.untamedears.mustercull.MusterCull;
import com.untamedears.mustercull.configurations.ConfigurationLimit;
import com.untamedears.mustercull.culltypes.CullType;

import java.util.Random;

/**
 * This class performs damage to mobs using the DAMAGE CullType.
 * @author Celdecea
 */
public class DamageLaborer extends Laborer {

	/**
	 * Constructor which takes a reference to the main plug-in class.
	 * @param pluginInstance A reference to the main plug-in class.
	 */
	public DamageLaborer(MusterCull pluginInstance) {
		super(pluginInstance);
	}
	
	
	/**
	 * Repeating damage method for the class.
	 */
	@Override
	public void run() {
		
		if (this.getPluginInstance().isPaused(CullType.DAMAGE)) {
			return;
		}

		int damageCalls = this.getPluginInstance().getDamageCalls();
		
		for (int i = 0; i < damageCalls; i++)
		{
			EntityLimitPair entityLimitPair = this.getPluginInstance().getNextEntityToDamage();
			
			if (entityLimitPair == null) {
				return;
			}
			
			Entity entity = entityLimitPair.getEntity();
			
			if (entity == null || entity.isDead()) {
				return;
			}
			
			ConfigurationLimit limit = entityLimitPair.getLimit();
			
			if (limit.getCulling() != CullType.DAMAGE) {
				return;
			}
			
			Random random = new Random();
			
			// Loop through entities in range and count similar entities.
			int count = 0;
			
			for (Entity otherEntity : this.getPluginInstance().getNearbyEntities(entity, limit.getRange(), entity.getType().getEntityClass())) {
				if (0 == otherEntity.getType().compareTo(entity.getType())) {
					
					count += 1;
					
					// If we've reached a limit for this entity, go ahead and damage it.
					if (count >= limit.getLimit()) {
						
						if (random.nextInt(100) < this.getPluginInstance().getDamageChance()) {
							this.getPluginInstance().damageEntity(entity, this.getPluginInstance().getDamage());
						}
						
						return;
					}
				}
			}
		}
	}
	


}
