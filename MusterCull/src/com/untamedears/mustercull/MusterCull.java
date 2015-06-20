package com.untamedears.mustercull;

import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.FileHandler;
import java.util.logging.Handler;

/**
 * This is the main class for the MusterCull Bukkit plug-in.
 * @author Celdecea
 *
 */
public class MusterCull extends JavaPlugin {

	/**
	 * Holds a list of entities to monitor.
	 */
	private Stack<EntityLimitPair> knownEntitiesDamage = new Stack<EntityLimitPair>();
	private Stack<EntityLimitPair> knownEntitiesMerge = new Stack<EntityLimitPair>();
	
	/**
	 * Holds a count of entities remaining for the status checker
	 */
	private int knownEntitiesRemainingDamage = 0;
	private int knownEntitiesRemainingMerge = 0;
	
	/**
	 * Flags whether or not we should clear knownEntities list next time around
	 */
	private boolean clearKnownEntitiesDamage = false;
	private boolean clearKnownEntitiesMerge = false;
	
	/**
	 * Whether or not we are returning a new entity to process (concurrency protection) 
	 */
	private boolean returningKnownEntityDamage = false;
	private boolean returningKnownEntityMerge = false;

	/**
	 * Buffer for keeping track of the parallel Laborer task for the DAMAGE method.
	 */
	private int damageLaborTask = -1;

	/**
	 * Buffer for keeping track of the parallel Laborer task for the MERGE method.
	 */
	private int mergeLaborTask = -1;
	
	/**
	 * Buffer for keeping track of the parallel Laborer task for the MERGE method.
	 */
	private int RemoveDespawnedMergedEntitiesTask = -1;
	
    /**
     * Buffer for keeping track of the parallel Laborer task for the HARDCAP method.
     */
    private int hardCapLaborTask = -1;

    /**
	 * Buffer for holding configuration information for this plug-in.
	 */
	private Configuration config = null;
	
	/**
	 * Stores any paused culling types we may have.
	 */
	private Set<CullType> pausedCullTypes = new HashSet<CullType>();

    /**
     * Whether hard cap laborer is paused.
     */
    private boolean hardCapPaused = false;
    
    private HardCapLaborer hardCapLaborerRef;
	
    /**
     * A map containing all the merged entities.
     */
    private ConcurrentHashMap<Entity, Integer> MergedEntities = new ConcurrentHashMap<Entity, Integer>();
    
	/**
	 * Called when the plug-in is enabled by Bukkit.
	 */
	public void onEnable() {
		
		this.config = new Configuration(this);
		this.config.load();
        
		this.damageLaborTask = getServer().getScheduler().scheduleSyncRepeatingTask(this, new DamageLaborer(this), this.config.getTicksBetweenDamage(), this.config.getTicksBetweenDamage());

		if (this.damageLaborTask == -1) {
			getLogger().severe("Failed to start MusterCull DAMAGE laborer.");
		}
		
		this.mergeLaborTask = getServer().getScheduler().scheduleSyncRepeatingTask(this, new MergeLaborer(this), this.config.getTicksBetweenMerge(), this.config.getTicksBetweenMerge());
		
		if (this.mergeLaborTask == -1) {
			getLogger().severe("Failed to start MusterCull MERGE laborer.");
		}
		
		this.RemoveDespawnedMergedEntitiesTask = getServer().getScheduler().scheduleSyncRepeatingTask(this, new RemoveDespawnedMergedEntitiesLaborer(this), this.config.getTicksBetweenMerge(), this.config.getTicksBetweenMerge());
		
		if (this.RemoveDespawnedMergedEntitiesTask == -1) {
			getLogger().severe("Failed to start MusterCull RemoveDespawnedMergedEntitieslaborer.");
		}
		
		hardCapLaborerRef = new HardCapLaborer(this);

        this.hardCapLaborTask = getServer().getScheduler().scheduleSyncRepeatingTask(this, hardCapLaborerRef, config.getTicksBetweenHardCap(), config.getTicksBetweenHardCap());

        if (this.hardCapLaborTask == -1) {
            getLogger().severe("Failed to start MusterCull HARDCAP laborer.");
        }

        getServer().getPluginManager().registerEvents(new EntityListener(this), this);
		Commander commander = new Commander(this);
		
		for (String command : getDescription().getCommands().keySet()) {
			getCommand(command).setExecutor(commander);
		}
		
		logging();
    }
	
	public void logging() {
		try {
			Handler fileHandler = new FileHandler(getDataFolder() + File.separator + "MusterCullLog.log.lck", true);
			getLogger().addHandler(fileHandler);
		} catch (SecurityException | IOException e) {
			System.out.println("Creating directory");
			getDataFolder().mkdirs();
			try {
				Handler fileHandler = new FileHandler(getDataFolder() + File.separator + "MusterCullLog.log.lck", true);
				getLogger().addHandler(fileHandler);
			} catch (IOException e2) {
                throw new RuntimeException("Failed to load log file", e);
			}
		}
	}
	
	public float getHardCapCullingPriorityStrategyPenaltyMobPercent()
	{
		return this.config.getHardCapCullingPriorityStrategyPenaltyMobPercent();
	}
	
	public void setHardCapCullingStrategy(String strategy)
	{
		config.setHardCapCullingStrategy(strategy);
	}
     
	/**
	 * Called when the plug-in is disabled by Bukkit.
	 */
    public void onDisable() { 
    	if (this.damageLaborTask != -1) {
    		getServer().getScheduler().cancelTask(this.damageLaborTask);
    	}

        if (this.hardCapLaborTask != -1) {
            getServer().getScheduler().cancelTask(hardCapLaborTask);
            hardCapLaborerRef = null;
        }
        
        if (this.mergeLaborTask != -1) {
            getServer().getScheduler().cancelTask(mergeLaborTask);
        }
        
        if (this.RemoveDespawnedMergedEntitiesTask != -1) {
            getServer().getScheduler().cancelTask(RemoveDespawnedMergedEntitiesTask);
        }
        
    	this.config.save();
    }
    
    public String getHardCapStatistics() {
    	return (null == hardCapLaborerRef) ? "" : hardCapLaborerRef.GetStatisticDisplayString();
    }

    /**
     * Return a limit from the config file for the provided entityType.
     * @param entityType A Bukkit entityType to return a limit for.
     * @return The ConfigurationLimit for the entityType, or null if none is defined.
     */
    public ConfigurationLimit getLimit(EntityType entityType, CullType cullType) {
    	
    	List<ConfigurationLimit> limits = this.config.getLimits(entityType);
    	
    	if (limits == null) {
    		return null;
    	}
    	
    	for (ConfigurationLimit limit : limits) {
    		if (cullType == limit.getCulling()) {
    			return limit;
    		}
    	}
    	
    	return null;
    }

	/**
	 * Sets the ConfigurationLimit for the specified mob type. Don't add 
	 * limits you don't need.
	 * 
	 * @param type The type of entity to set a ConfigurationLimit for.
	 * @param limit The limit for the entity type.
	 */
	public void setLimit(EntityType type, ConfigurationLimit limit) {
		this.config.setLimit(type, limit);
	}
	
	/**
	 * Returns whether or not we have limits with CullType DAMAGE.
	 * @return Whether or not we have limits with CullType DAMAGE.
	 */
	public boolean hasDamageLimits() {
		return this.config.hasDamageLimits();
	}
	
	public GlobalCullCullingStrategyType getGlobalCullingStrategy() {
		return this.config.getHardCapCullingStrategy();
	}
	
	/**
	 * Returns whether or not we have limits with CullType SPAWN or SPAWNER.
	 * @return Whether or not we have limits with CullType SPAWN or SPAWNER.
	 */
	public boolean hasSpawnLimits() {
		return this.config.hasSpawnLimits();
	}
	
	/**
	 * Returns whether or not we have limits with CullType MERGE.
	 * @return Whether or not we have limits with CullType MERGE.
	 */
	public boolean hasMergeLimits() {
		return this.config.hasMergeLimits();
	}
	
	/**
	 * Returns the hard mob limit.
	 * @return The hard mob limit. 
	 */
	public int getMaxMob() {
		return this.config.getMaxMob();
	}
	
	/**
	 * Sets the hard mob limit.
	 * @param limit the hard mob limit.
	 */
	public void setMaxMob(int limit) {
		this.config.setMaxMob(limit);
	}
	
	/**
	 * Returns how many mobs permitted less of the maximum, per player.
	 * @return How many mobs permitted less of the maximum, per player.
	 */
	public int getPlayerMultiplier() {
		return this.config.getPlayerMultiplier();
	}
	
	/**
	 * Sets the player multiplier for the hard mob limit.
	 * @param value the player multiplier for the hard mob limit.
	 */
	public void setPlayerMultiplier(int value) {
		this.config.setPlayerMultiplier(value);
	}

	/**
	 * Returns how much current mob count is over mob limit.
	 * @return how much current mob count is over mob limit.
	 */
	public int overHardMobLimit() {

        int playerCount = getServer().getOnlinePlayers().size();
		int hardLimit = getMaxMob();
		int lessHardLimit = getPlayerMultiplier() * playerCount;
		int currentLimit = hardLimit - lessHardLimit;
		
		if (currentLimit < 0) {
			currentLimit = 0;
		}
		
		int totalMobs = getMobCount() - playerCount;
		
		return totalMobs - currentLimit;
	}
	
	/**
	 * Returns list of all living non player entities in all worlds.
	 * @return list of all living non player entities in all worlds.
	 */
	public List<LivingEntity> getAllLivingNonPlayerMobs() {

        List<World> worlds = getServer().getWorlds();
        int mobCount = 0;

		for (World world : worlds) {
            mobCount += world.getLivingEntities().size();
		}

        List<LivingEntity> entities = new ArrayList<LivingEntity>(mobCount);

        for (World world : worlds) {
        	List<LivingEntity> mobs = world.getLivingEntities();
            for (LivingEntity mob : mobs) {
                if (   (! (mob instanceof Player))
                	   && (! (mob instanceof ArmorStand))
                	   && (! mob.isDead())) {
                    entities.add(mob);
                }
            }
        }

        return entities;
	}

	/**
	 * Returns list of entities by class in all worlds.
	 * @return Returns list of entities by class in all worlds.
	 */
	public List<Entity> getEntitiesByClass(Class<?>... classes) {

        List<World> worlds = getServer().getWorlds();
        int entityCount = 0;

		for (World world : worlds) {
			entityCount += world.getEntitiesByClasses(classes).size();
		}

        List<Entity> entities = new ArrayList<Entity>(entityCount);

        for (World world : worlds) {
        	List<Entity> entitiesInWorld = new ArrayList<Entity>(world.getEntitiesByClasses(classes));
            for (Entity entity : entitiesInWorld) {
                if (   (! (entity instanceof Player))
                	   && (! (entity instanceof ArmorStand))
                	   && (! entity.isDead())) {
                    entities.add(entity);
                }
            }
        }

        return entities;
	}
	
	/**
	 * Returns list of entities by class in a specific world.
	 * @return Returns list of entities by class in in a specific world.
	 */
	public List<Entity> getEntitiesByClass(World world, Class<?>... classes) {
		
		int entityCount = world.getEntitiesByClasses(classes).size();
		List<Entity> entities = new ArrayList<Entity>(entityCount);
		
        List<Entity> entitiesInWorld = new ArrayList<Entity>(world.getEntitiesByClasses(classes));
        
        for (Entity entity : entitiesInWorld) {
            if (   (! (entity instanceof Player))
            	   && (! (entity instanceof ArmorStand))
            	   && (! entity.isDead())) {
                entities.add(entity);
            }
        }
        return entities;
	}
	
    /**
     * Returns number of living entities in all worlds.
     * @return number of living entities in all worlds.
     */
    public int getMobCount() {

        int count = 0;

        for (World world : getServer().getWorlds()) {
            count += world.getLivingEntities().size();
        }

        return count;
    }
	
	/**
	 * Returns the next entity for monitoring.
	 * @return A reference to an EntityLimitPair.
	 */
	public EntityLimitPair getNextEntityDamage() {
		
		synchronized(this.knownEntitiesDamage) {
			if (this.returningKnownEntityDamage) {
				return null;
			}
			
			this.returningKnownEntityDamage = true;
		}
		
		EntityLimitPair entityLimitPair = null;
		
		boolean clearEntities = false;
		
		synchronized(this) {
			clearEntities = this.clearKnownEntitiesDamage;
			this.clearKnownEntitiesDamage = false;
		}
		
		if (this.knownEntitiesRemainingDamage <= 0 || clearEntities) {
			
			if (clearEntities) {
				getLogger().info("Forcing entity list to clear...");
			}
			
			this.knownEntitiesRemainingDamage = 0;
			this.knownEntitiesDamage.clear();
			
			Map<EntityType, List<Entity>> sortedEntities = new HashMap<EntityType, List<Entity>>();
			int totalEntities = 0;
			for (World world : getServer().getWorlds()) {
				
				List<LivingEntity> entities = world.getLivingEntities();
				totalEntities += entities.size();
				
				for (Entity entity : entities) {
					List<Entity> knownEntities = sortedEntities.get(entity.getType());
					
					if (knownEntities == null) {
						knownEntities = new ArrayList<Entity>();
						sortedEntities.put(entity.getType(), knownEntities);
					}
					
					knownEntities.add(entity);
				}
			}
			
			if (totalEntities < this.config.getMobLimitDamage()) {
				synchronized(this.knownEntitiesDamage) {
					this.returningKnownEntityDamage = false;
					return null;
				}
			}
			
			float LimitPercent = ((float)this.config.getMobLimitPercentDamage()) / 100.0f;
			
			Stack<EntityLimitPair> newEntities = new Stack<EntityLimitPair>();
			
			for (Map.Entry<EntityType, List<Entity>> entries : sortedEntities.entrySet()) {
				ConfigurationLimit limit = this.getLimit(entries.getKey(), CullType.DAMAGE);
				
				if (limit == null) {
					continue;
				}
				List<Entity> values = entries.getValue();
				
				if (((float)values.size()) / ((float)totalEntities) >= LimitPercent) {
					for (Entity entity : entries.getValue()) {
						newEntities.push(new EntityLimitPair(entity, limit));
					}
				}
			}
			
			this.knownEntitiesDamage = newEntities;
			this.knownEntitiesRemainingDamage = this.knownEntitiesDamage.size();
		}
		else {
			entityLimitPair = this.knownEntitiesDamage.pop();
			this.knownEntitiesRemainingDamage--;
		}
		synchronized(this.knownEntitiesDamage) {
			this.returningKnownEntityDamage = false;
			return entityLimitPair;
		}
	}
	
	/**
	 * Returns the next entity for monitoring.
	 * @return A reference to an EntityLimitPair.
	 */
	public EntityLimitPair getNextEntityMerge() {
		
		synchronized(this.knownEntitiesMerge) {
			if (this.returningKnownEntityMerge) {
				return null;
			}
			
			this.returningKnownEntityMerge = true;
		}
		
		EntityLimitPair entityLimitPair = null;
		
		boolean clearEntities = false;
		
		synchronized(this) {
			clearEntities = this.clearKnownEntitiesMerge;
			this.clearKnownEntitiesMerge = false;
		}
		
		if (this.knownEntitiesRemainingMerge <= 0 || clearEntities) {
			
			if (clearEntities) {
				getLogger().info("Forcing entity list to clear...");
			}
			
			this.knownEntitiesRemainingMerge = 0;
			this.knownEntitiesMerge.clear();
			
			Map<EntityType, List<Entity>> sortedEntities = new HashMap<EntityType, List<Entity>>();
			int totalEntities = 0;
			for (World world : getServer().getWorlds()) {
				
				List<LivingEntity> entities = world.getLivingEntities();
				totalEntities += entities.size();
				
				for (Entity entity : entities) {
					List<Entity> knownEntities = sortedEntities.get(entity.getType());
					
					if (knownEntities == null) {
						knownEntities = new ArrayList<Entity>();
						sortedEntities.put(entity.getType(), knownEntities);
					}
					
					knownEntities.add(entity);
				}
			}
			
			if (totalEntities < this.config.getMobLimitMerge()) {
				synchronized(this.knownEntitiesMerge) {
					this.returningKnownEntityMerge = false;
					return null;
				}
			}
			
			float LimitPercent = ((float)this.config.getMobLimitPercentMerge()) / 100.0f;
			
			Stack<EntityLimitPair> newEntities = new Stack<EntityLimitPair>();
			
			for (Map.Entry<EntityType, List<Entity>> entries : sortedEntities.entrySet()) {
				ConfigurationLimit limit = this.getLimit(entries.getKey(), CullType.MERGE);
				
				if (limit == null) {
					continue;
				}
				List<Entity> values = entries.getValue();
				
				if (((float)values.size()) / ((float)totalEntities) >= LimitPercent) {
					for (Entity entity : entries.getValue()) {
						newEntities.push(new EntityLimitPair(entity, limit));
					}
				}
			}
			
			this.knownEntitiesMerge = newEntities;
			this.knownEntitiesRemainingMerge = this.knownEntitiesMerge.size();
		}
		else {
			entityLimitPair = this.knownEntitiesMerge.pop();
			this.knownEntitiesRemainingMerge--;
		}
		synchronized(this.knownEntitiesMerge) {
			this.returningKnownEntityMerge = false;
			return entityLimitPair;
		}
	}
	
	/**
	 * Returns information about mobs surrounding players
	 * @return Information about mobs surrounding players
	 */
	public List<StatusItem> getStats() {
		
		List<StatusItem> stats = new ArrayList<StatusItem>();
		
		for (World world : getServer().getWorlds()) {
			for (Player player : world.getPlayers()) {
				stats.add(new StatusItem(player));
			}
		}
		
		Collections.sort(stats, new StatusItemComparator());
		Collections.reverse(stats);
		return stats;
	}
	
	/**
	 * Returns the percent chance that a mob will be damaged when crowded.
	 * @return Percent chance that a mob will be damaged when crowded.
	 */
	public int getDamageChance() {
		return this.config.getDamageChance();
	}
	
	/**
	 * Sets the percent chance that a mob will be damaged when crowded.
	 * @param damageChance Percent chance that a mob will be damaged when crowded.
	 */
	public void setDamageChance(int damageChance) {
		this.config.setDamageChance(damageChance);
	}
	
	/**
	 * Returns the number of entities to take damage each time the laborer is called.
	 * @return Number of entities to take damage each time the laborer is called.
	 */
	public int getDamageCalls() {
		return this.config.getDamageCalls();
	}
	
	/**
	 * Sets the number of entities to take damage each time the laborer is called.
	 * @param damageCalls Number of entities to take damage each time the laborer is called.
	 */
	public void setDamageCalls(int damageCalls) {
		this.config.setDamageCalls(damageCalls);
	}
	
	/**
	 * Returns the number of entities to merge each time the laborer is called.
	 * @return Number of entities to merge each time the laborer is called.
	 */
	public int getMergeCalls() {
		return this.config.getMergeCalls();
	}
	
	/**
	 * Sets the number of entities to merge each time the laborer is called.
	 * @param damageCalls Number of entities to merge each time the laborer is called.
	 */
	public void setMergeCalls(int mergeCalls) {
		this.config.setMergeCalls(mergeCalls);
	}
	
	/**
	 * Returns the amount of damage to apply to a crowded mob.
	 * @return The amount of damage to apply to a crowded mob. 
	 */
	public int getDamage() {
		return this.config.getDamage();
	}
	
	/**
	 * Sets the amount of damage to apply to a crowded mob.
	 * @param damage The amount of damage to apply to a crowded mob. 
	 */
	public void setDamage(int damage) {
		this.config.setDamage(damage);
	}
	
	/**
	 * Returns the number of entities left to check in this round.
	 * @return The size of the stack of Bukkit entities left to check.
	 */
	public int getRemainingEntitiesDamage() {
		return this.knownEntitiesRemainingDamage;
	}
	
	/**
	 * Clears the entities that may be waiting.
	 */
	public void clearRemainingEntitiesDamage() {
		getLogger().info("Flagging damage list for clearing...");
		synchronized(this) {
			this.clearKnownEntitiesDamage = true;
		}
	}

	/**
	 * Returns the number of entities left to check in this round.
	 * @return The size of the stack of Bukkit entities left to check.
	 */
	public int getRemainingEntitiesMerge() {
		return this.knownEntitiesRemainingMerge;
	}
	
	/**
	 * Clears the entities that may be waiting.
	 */
	public void clearRemainingEntitiesMerge() {
		getLogger().info("Flagging damage list for clearing...");
		synchronized(this) {
			this.clearKnownEntitiesMerge = true;
		}
	}
	
	/**
	 * Returns nearby entities to an entity.
	 * @param entity
	 * @param distance to look from the entity
	 * @return The list of entities surrounding the entity
	 */
	public List<Entity> getNearbyEntities(Entity entity, int distance, Class<?>... classes) {
		List<Entity> entities = new ArrayList<Entity>();
		
		if(entity == null){
			return null;
		}
		
		for (Entity e : getEntitiesByClass(entity.getWorld(), classes)) {
			double distanceFromEntity = entity.getLocation().distance(e.getLocation());
			if (distanceFromEntity > distance)
				continue;
			entities.add(e);
		}
		return entities;
	}

	
	/**
	 * Returns nearby entities to a player by name.
	 * @param playerName The name of a player to look up
	 * @param distance to look from player
	 * @return The list of entities surrounding the player
	 */
	public List<Entity> getNearbyEntities(String playerName, int distance, Class<?>... classes) {
		Player player = null;
		
		for (World world : getServer().getWorlds()) {
			for (Player p : world.getPlayers()) {
				if (0 == p.getName().compareToIgnoreCase(playerName)) {
					player = p;
				}
			}
		}
		
		return getNearbyEntities(player, distance, classes);
	}

	
	/**
	 * Causes damage to entities of a certain type surrounding a given player.
	 * @param playerName The name of the player to search around
	 * @param entityType The type of entity to damage around the player
	 * @param damage The amount of damage to deal to the player
	 * @param range The range from the player to check
	 * @return The number of entities damage may have been applied to
	 */
	public int damageEntitiesAroundPlayer(String playerName, EntityType entityType, int damage, int range) {
	
		int count = 0;
		
		List<Entity> nearbyEntities = getNearbyEntities(playerName, range, entityType.getEntityClass());
		
		if (nearbyEntities == null) {
			return 0;
		}		
		
		for (Entity entity : nearbyEntities) {
			if (entity.getType() == entityType) {
				this.damageEntity(entity, damage);
				count++;
			}
		}
		
		return count;
	}
	
	/**
	 * Causes damage to entities of a certain type.
	 * @param entityType The type of entity to damage.
	 * @param damage The amount of damage to deal to the entities.
	 * @return The number of entities damage may have been applied to
	 */
	public int damageEntities(EntityType entityType, int damage) {
	
		int count = 0;
		
		List<Entity> allEntities = getEntitiesByClass(entityType.getEntityClass());
		
		if (allEntities == null) {
			return 0;
		}		
		
		for (Entity entity : allEntities) {
			if (entity.getType() == entityType) {
				this.damageEntity(entity, damage);
				count++;
			}
		}
		
		return count;
	}
	
	/**
	 * Causes a specified amount of damage to an entity, doubled for baby animals.
	 * @param entity The bukkit entity to cause damage to
	 * @param damage The amount of damage to cause to the entity
	 */
	public void damageEntity(Entity entity, int damage) {
		
		if (Ageable.class.isAssignableFrom(entity.getClass())) {
			Ageable agingEntity = (Ageable)entity;
			
			if (agingEntity.isAdult()) {
				NotifyDamaged(entity, damage);
				agingEntity.damage((double)damage);
			}
			else {
				NotifyDamaged(entity, 2 * damage);
				agingEntity.damage((double)(2 * damage));
			}
		}
		else if (LivingEntity.class.isAssignableFrom(entity.getClass())) {
			NotifyDamaged(entity, damage);
			LivingEntity livingEntity = (LivingEntity)entity;
			livingEntity.damage((double)damage);
		} 
		else if (Vehicle.class.isAssignableFrom(entity.getClass())) {
			Vehicle vehicle = (Vehicle) entity;
			if(vehicle.isEmpty()){
				NotifyDamaged(entity, damage);
				vehicle.remove();
			}
		} 
		else {
			getLogger().warning("Attempt to damage entity that is not supported '" + entity.getType().toString() + "' detected.");
		}
	}
	
	/**
	 * Notifies the console that an entity has been damaged, if enabled
	 * @param entity The entity to report on.
	 * @param entity The amount of damage to report
	 */
	public void NotifyDamaged(Entity entity, int damage) {
		if (this.config.getDamageNotify()) {
			getLogger().info("Damaging " + entity.toString() + " at " + entity.getLocation().toString() + " with " + damage + " point(s).");
		}
	}
	
	/**
	 * Performs configured culling operations on the given entity.
	 * @param entity The bukkit entity to perform culling operations for.
	 * @param limit The limit to run for this entity.
	 * @return Whether the entity check was successful (i.e. we need to damage/kill something)
	 */
	public boolean runEntityChecks(Entity entity, ConfigurationLimit limit) {
		
		if (limit == null) {
			return false;
		}
		
		// If the limit is 0, prevent all of this entity type from spawning 
		if (limit.getLimit() <= 0) {
			getLogger().info("Cancelling spawn for " + entity.toString() + " at " + entity.getLocation().toString() + " (method: " + limit.getCulling().toString() + ")");
			return true;
		}
		
		// Loop through entities in range and count similar entities.
		int count = 0;
		
		for (Entity otherEntity : getNearbyEntities(entity, limit.getRange(), entity.getType().getEntityClass())) {
			if (0 == otherEntity.getType().compareTo(entity.getType())) {
				count += 1;
				
				// If we've reached a limit for this entity, prevent it from spawning.
				if (count >= limit.getLimit()) {
					getLogger().info("Cancelling spawn for " + entity.toString() + " at " + entity.getLocation().toString() + " (method: " + limit.getCulling().toString() + ")");
					return true;
				}
			}
		}
		
		return false;
	}

	/**
	 * Performs configured merging operations on the given entity.
	 * @param entity The bukkit entity to perform culling operations for.
	 * @param limit The limit to run for this entity.
	 * @return Whether the entity needs to be spawn.
	 */
	public boolean mergeEntity(Entity entity, ConfigurationLimit limit) {
		
		if (limit == null) {
			return false;
		}
		
		// Loop through entities in range and count similar entities.
		int count = 0;
		Entity e = null;
		
		for (Entity otherEntity : getNearbyEntities(entity, limit.getRange(), entity.getType().getEntityClass())) {
			if (0 == otherEntity.getType().compareTo(entity.getType())&&MergedEntities.containsKey(otherEntity)) {
				count += 1;
				
				// If we've reached a limit for this entity, merge the entity into a living entity.
				if (count >= limit.getLimit()) {
					e = addMergedEntityToMap(entity, limit, false, true);
					break;
				}
			}
		}
		
		// if we haven't reached the limit, merge the entity into a dead entity. 
		if(count < limit.getLimit()) {
			e = addMergedEntityToMap(entity, limit, true, true);
		}
		
		// if we have reached the spawn limit, spawn the entity.
		if(e != null && e == entity && MergedEntities.get(e) == limit.getSpawnDelay()){
			getLogger().info("A merged entity was spawned " + e.toString() + "multiplayer: " + MergedEntities.get(e));
			return false;
		}
		
		return true;
	}
	
	/**
	 * Merging an entity into other entities by adding it to the map.
	 * @param entity The bukkit entity to merge.
	 * @param limit The limit to run for this entity.
	 * @param toDeadEntity should the entity be merged into a dead entity or a living entity.
	 * @param newMergedEntity should a new merged entity be created if there are no other merged entities near by.
	 * @return The merged entity.
	 */
	public Entity addMergedEntityToMap(Entity entity, ConfigurationLimit limit, boolean toDeadEntity, boolean newMergedEntity){
		
		if(MergedEntities.containsKey(entity)){
			return null;
		}
		
		int distance = limit.getRange();
		Entry<Entity, Integer> min = null;
		Entity e = null;
		// Find the entity with the lowest multiplayer.
		for(Entry<Entity, Integer> entry: MergedEntities.entrySet()){
			e = entry.getKey();
			if(0 == e.getType().compareTo(entity.getType()) && e.getWorld().equals(entity.getWorld()) && e.isDead()==toDeadEntity){
				double distanceFromEntity = entity.getLocation().distance(e.getLocation());
				if (distanceFromEntity > distance)
					continue;
				if (min == null || min.getValue() > entry.getValue()) {
				        min = entry;
				}
			}
		}
		
		if(min != null){
			e = min.getKey();
			if(e.isDead()){
				MergedEntities.put(entity, MergedEntities.get(e)+limit.getMultiplayery());
				MergedEntities.remove(e);
				getLogger().info("entity " + entity.toString() + " \nwas merged into a dead entity " + e.toString() + " at " + entity.getLocation().toString() + "\nmultiplayer: " + MergedEntities.get(entity));
				return entity;
			} else {
				MergedEntities.put(e, MergedEntities.get(e)+limit.getMultiplayery());
				getLogger().info("entity " + entity.toString() + " \nwas merged into a living entity " + e.toString() + " at " + e.getLocation().toString() + "\nmultiplayer: " + MergedEntities.get(e));
				return e;
			}
		}
		
		if(newMergedEntity){
			MergedEntities.put(entity, 1);
			getLogger().info("A new entity was added to the map " + entity.toString() + " at " + entity.getLocation().toString());
			return entity;
		} else {
			return null;
		}
	}
	
	/**
	 * Adjusting the drops of a merged entity.
	 * @param liveMob The mob.
	 * @param drops The drops of the mob.
	 */
	public void AdjustDropsOfMergedEntity(LivingEntity liveMob, List<ItemStack> drops){
		if(MergedEntities.containsKey(liveMob)){
			
			EntityEquipment mobEquipment = liveMob.getEquipment();
			ItemStack[] eeItem = mobEquipment.getArmorContents();
			
			for (ItemStack item : drops) {
				boolean armor = false;
				boolean hand = false;
				for(ItemStack i : eeItem){
					if(i.isSimilar(item)){
						armor = true;
						item.setAmount(1);
					}
				}
				
				if(item.isSimilar(mobEquipment.getItemInHand())){
					hand = true;
					item.setAmount(1);
				}

				if(!hand && !armor){
					getLogger().info(item.getAmount()+"");
					Integer amount = item.getAmount() * MergedEntities.get(liveMob);
					item.setAmount(amount);
				}
				
			}
		}
	}
	
	public ConcurrentHashMap<Entity, Integer> getMergedEntities() {
		return MergedEntities;
	}

	public void setMergedEntities(ConcurrentHashMap<Entity, Integer> mergedEntities) {
		MergedEntities = mergedEntities;
	}

	/**
	 * Pauses all culling.
	 */
	public void pauseAllCulling() {
		getLogger().info("Pausing all culling types...");
		synchronized(this.pausedCullTypes) {
			for (CullType cullType : CullType.values()) {
				this.pausedCullTypes.add(cullType);
			}
		}

        hardCapPaused = true;
	}
	
	/**
	 * Resumes all culling.
	 */
	public void resumeAllCulling() {
		getLogger().info("Resuming all paused culling types...");
		synchronized(this.pausedCullTypes) {
			this.pausedCullTypes.clear();
		}

        hardCapPaused = false;
	}
	
	/**
	 * Pauses a specific CullType blocking its functionality.
	 * @param cullType The CullType to disable temporarily.
	 */
	public void pauseCulling(CullType cullType) {
		getLogger().info("Pausing culling type " + cullType.toString() + "...");
		synchronized(this.pausedCullTypes) {
			this.pausedCullTypes.add(cullType);
		}
	}

    public void pauseCulling(GlobalCullType cullType) {
        getLogger().info("Pausing culling type " + cullType.toString() + "...");
        hardCapPaused = true;
    }
	
	/**
	 * Resumes a specific CullType which was paused.
	 * @param cullType The CullType to reenable.
	 */
	public void resumeCulling(CullType cullType) {
		getLogger().info("Resuming culling type " + cullType.toString() + "...");
		synchronized(this.pausedCullTypes) {
			this.pausedCullTypes.remove(cullType);
		}
	}

    public void resumeCulling(GlobalCullType cullType) {
        getLogger().info("Resuming culling type " + cullType + "...");
        hardCapPaused = false;
    }

	/**
	 * Returns whether or not a CullType is paused.
	 * @param cullType The CullType to test the status of.
	 * @return Whether or not the CullType is paused.
	 */
	public boolean isPaused(CullType cullType) {
		synchronized(this.pausedCullTypes) {
			return this.pausedCullTypes.contains(cullType);
		}
	}

    public boolean isPaused(GlobalCullType cullType) {
        return hardCapPaused;

    }
	
	/**
	 * Forces the local configuration file to save.
	 */
	public void forceConfigSave() {
		this.config.save();
	}
	
	public Configuration getConfiguration() {
		return config;
	}

}
