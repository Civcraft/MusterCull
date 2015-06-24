package com.untamedears.mustercull.configurations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import com.untamedears.mustercull.culltypes.CullType;
import com.untamedears.mustercull.culltypes.GlobalCullCullingStrategyType;


/**
 * Manages the configuration for the plug-in.
 * 
 * @author Celdecea
 *
 */
public class Configuration {

	/**
	 * Whether or not configuration data needs to be saved.
	 */
	private boolean dirty = false;
	
	/**
	 * The amount of damage to apply to a crowded mob. 
	 */
	private int damage = 0;
	
	/**
	 * Mob limits loaded from the configuration file.
	 */
	private Map<EntityType, List<ConfigurationLimit>> mobLimits = new HashMap<EntityType, List<ConfigurationLimit>>();
	
	/**
	 * Whether or not we have limits with CullType DAMAGE.
	 * 
	 * This is used by the MusterCull class to determine if the tick laborer
	 * needs to be started.
	 */
	private boolean hasDamageLimits = false;
	
	/**
	 * Whether or not we have limits with CullTypes SPAWN or SPAWNER.
	 * 
	 * This is used by the MusterCull class to determine if the event listener
	 * needs to be registered.
	 */
	private boolean hasSpawnLimits = false;
	
	/**
	 * Whether or not we have limits with CullTypes MERGE.
	 * 
	 * This is used by the MusterCull class to determine if the tick laborer
	 * needs to be started.
	 */
	private boolean hasMergeLimits = false;
	
	/**
	 * Number of ticks between calls to the chunk damage laborer.
	 */
	private long ticksBetweenDamage = 20L;
	
	/**
	 * Number of ticks between calls to the chunk merge laborer.
	 */
	private long ticksBetweenMerge = 20L;
	
	/**
	 * Number of entities to damage every time the damage laborer runs.
	 */
	private int damageCalls = 1;
	
	/**
	 * Number of entities to damage every time the merge laborer runs.
	 */
	private int mergeCalls = 1;
	
	/**
	 * Percent chance that a mob will be damaged when crowded.
	 */
	private int damageChance = 75;
	
	/**
	 * Hard number on mobs before the damage laborer cares to run.
	 */
	private int mobLimitDamage = 1;
	
	/**
	 * Hard number on mobs before the damage laborer cares to run.
	 */
	private int mobLimitMerge = 1;
	
	/**
	 * Percentage of mobLimit each mob must be to trigger damage culling or merge culling.
	 */
	private int mobLimitPercentDamage = 1;
	
	/**
	 * Percentage of mobLimit each mob must be to trigger merge culling or merge culling.
	 */
	private int mobLimitPercentMerge = 1;
	
	/**
	 * Whether or not to notify when entities have been damaged.
	 */
	private boolean damageNotify = false;
	
	/**
	 * The hard mob limit. Also however many mobs can exist with no players.
	 */
	private int maxMob = 10000;
	
	/**
	 * How many mobs permitted less of the maximum, per player.
	 */
	private int playerMultiplier = 5;

    /**
     * Number of ticks between calls to the living entity hard cap (HardCapLaborer).
     */
    private long ticksBetweenHardCap = 40L;
	
    /**
     * Whether to perform the monster cull pass to keep them within world spawn limits.
     */
    private boolean enableMonsterCullToSpawn = true;

	/**
     * The maximum number of monsters to cull in a monster cull pass.
     */
    private int maximumMonsterCullPerPass = 30;

	/**
     * The maximum aggression factor for the monster cull.
     */
    private int minimumMonsterCullAggression = 0;

	/**
     * The maximum aggression factor for the monster cull.
     */
    private int maximumMonsterCullAggression = 5;
    
	/**
	 * Holds a reference to the Bukkit JavaPlugin for this project 
	 */
	private JavaPlugin pluginInstance = null;
	
	/**
	 * Percent that a super-chunk must contain of total server pop in order to qualify for a penalty purge.
	 */
	private int hardCapCullingPriorityStrategyPenaltyMobPercent = 100;
	
	/**
	 * Culling strategy for hard-cap culling.  RANDOM or PRIORITY
	 */
	private String hardCapCullingStrategy = "RANDOM";
	
	/**
	 * Constructor which stores a reference to the Bukkit JavaPlugin we are using.
	 * @param plugin A reference to a Bukkit JavaPlugin.
	 */
	public Configuration(JavaPlugin plugin) {
		this.pluginInstance = plugin; 
	}

	/**
	 * Loads configuration values from the supplied plug-in instance.
	 */
	public void load() {
		
		FileConfiguration config = this.pluginInstance.getConfig();
		
		this.setDamage(config.getInt("damage"));
		this.setDamageChance(config.getInt("damage_chance"));
		this.setDamageCalls(config.getInt("damage_count"));
		this.setTicksBetweenDamage(config.getInt("ticks_between_damage"));
		this.setMobLimitDamage(config.getInt("mob_limit_damage"));
		this.setMobLimitPercentDamage(config.getInt("mob_limit_percent_damage"));
		this.setDamageNotify(config.getBoolean("damage_notify"));
		this.setMergeCalls(config.getInt("merge_count"));
		this.setTicksBetweenMerge(config.getInt("ticks_between_merge"));
		this.setMobLimitMerge(config.getInt("mob_limit_merge"));
		this.setMobLimitPercentMerge(config.getInt("mob_limit_percent_merge"));
		this.setMaximumMonsterCullAggression(config.getInt("max_monster_cull_aggression"));
		this.setMinimumMonsterCullAggression(config.getInt("min_monster_cull_aggression"));
		this.setMaximumMonsterCullPerPass(config.getInt("max_monster_cull_per_pass"));
		this.setEnableMonsterCullToSpawn(config.getBoolean("enable_monster_cull_to_spawn"));
		this.setMaxMob(config.getInt("mob_max_mob"));
		this.setPlayerMultiplier(config.getInt("mob_player_multiplier"));
        this.setTicksBetweenHardCap(config.getInt("ticks_between_hard_cap"));
        this.setHardCapCullingStrategy(config.getString("hard_cap_culling_strategy"));
        this.setHardCapCullingPriorityStrategyPenaltyMobPercent(config.getInt("hard_cap_culling_priority_strategy_penalty_mob_percent"));
						
		List<?> list;
				
		list = config.getList("limits");
		
		if (list != null) {
			for (Object obj : list ) {
	
				if (obj == null) {
					this.pluginInstance.getLogger().warning("Possible bad limit in configuration file.");
					continue;
				}
				
				//TODO: Figure out how to do this without suppression.
	            @SuppressWarnings("unchecked")
	            LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) obj;
	            
	            EntityType type = EntityType.valueOf(map.get("type").toString().trim());

	            if (type == null) {
	            	this.pluginInstance.getLogger().warning("Unrecognized type '" + map.get("type").toString() + "' in configuration file.");
					continue;
	            }

	            int limit = (Integer)map.get("limit");
	            
	            CullType culling = CullType.fromName(map.get("culling").toString());

	            if (culling == null) {
	            	this.pluginInstance.getLogger().warning("Unrecognized culling '" + map.get("culling").toString() + "' in configuration file.");
					continue;
	            }
	            
	            int range = (Integer)map.get("range");
			
	            //if (range > 80) {
	            //	this.pluginInstance.getLogger().warning("range is > 80, ignoring this and setting to 80.");
	            //	range = 80;
	            //}
	            
	            int spawnDelay = 0;
	            if(map.get("spawnDelay") != null){
	            	spawnDelay = (Integer)map.get("spawnDelay");
	            }

	            double multiplier = 0;
	            if(map.get("multiplier") != null){
	            	multiplier = (Double)map.get("multiplier");
	            }
	            
	            double multiplierLimit = 0;
	            if(map.get("multiplierLimit") != null){
	            	multiplier = (Double)map.get("multiplierLimit");
	            }
	            
	            setLimit(type, new ConfigurationLimit(limit, culling, range, spawnDelay, multiplier, multiplierLimit));
	        }
		}
		
		this.dirty = false;
	}

	private void setHardCapCullingPriorityStrategyPenaltyMobPercent(int perc) {
		hardCapCullingPriorityStrategyPenaltyMobPercent = perc;
	}
	
	public float getHardCapCullingPriorityStrategyPenaltyMobPercent() {
		return hardCapCullingPriorityStrategyPenaltyMobPercent / 100.f ;
	}

	/**
	 * Saves configuration values to the supplied plug-in instance.
	 */
	public void save() {
		
		if (!this.dirty) {
			return;
		}
		
		FileConfiguration config = this.pluginInstance.getConfig();
		
		config.set("damage", this.damage);
		config.set("damage_chance", this.damageChance);
		config.set("damage_count", this.damageCalls);
		config.set("ticks_between_damage", this.ticksBetweenDamage);
		config.set("mob_limit_damage", this.mobLimitDamage);
		config.set("mob_limit_percent_damage", this.mobLimitPercentDamage);
		config.set("damage_notify", this.damageNotify);
		config.set("merge_count", this.mergeCalls);
		config.set("ticks_between_merge", this.ticksBetweenMerge);
		config.set("mob_limit_merge", this.mobLimitMerge);
		config.set("mob_limit_percent_merge", this.mobLimitPercentMerge);
		config.set("enable_monster_cull_to_spawn", this.enableMonsterCullToSpawn);
		config.set("max_monster_cull_aggression", this.maximumMonsterCullAggression);
		config.set("min_monster_cull_aggression", this.minimumMonsterCullAggression);
		config.set("max_monster_cull_per_pass", this.maximumMonsterCullPerPass);
		config.set("mob_max_mob", this.maxMob);
		config.set("mob_player_multiplier", this.playerMultiplier);
        config.set("ticks_between_hard_cap", this.ticksBetweenHardCap);
        config.set("hard_cap_culling_strategy", this.hardCapCullingStrategy);
        config.set("hard_cap_culling_priority_strategy_penalty_mob_percent", this.hardCapCullingPriorityStrategyPenaltyMobPercent);
		
        //Not working yet
        //saveLimits();
        
		this.pluginInstance.saveConfig();
		
		this.dirty = false;
	}
	
	/**
	 * Saves the ConfigurationLimits to the config file.
	 */
	public void saveLimits() {
		FileConfiguration config = this.pluginInstance.getConfig();
		Map<String, List<String>> limits = new HashMap<String, List<String>>();
		
		for(Entry<EntityType, List<ConfigurationLimit>> entry: mobLimits.entrySet()) {
			String entityType = "type: " + entry.getKey().toString().trim();
			List<String> limit = new ArrayList<>();
			for(ConfigurationLimit cl : entry.getValue()){
				limit.add("culling: " + cl.getCulling());
				limit.add("limit: " + cl.getLimit());
				limit.add("range: " + cl.getRange());
				if(cl.getCulling() == CullType.MERGE) {
					limit.add("spawnDelay: " + cl.getSpawnDelay());
					limit.add("multiplier: " + cl.getMultiplier());
					limit.add("multiplierLimit: " + cl.getMultiplierLimit());
				}
				limits.put(entityType, limit);
			}
		}
		config.set("limits", limits);
	}
	
	/**
	 * Returns the amount of damage to apply to a crowded mob.
	 * @return The amount of damage to apply to a crowded mob. 
	 */
	public int getDamage() {
		return damage;
	}

	/**
	 * Sets the amount of damage to apply to a crowded mob.
	 * @param damage The amount of damage to apply to a crowded mob.
	 */
	public void setDamage(int damage) {
		
		if (damage <= 0) {
			this.pluginInstance.getLogger().info("Warning: damage is <= 0, possibly wasting cpu cycles.");
		}
		
		this.damage = damage;
		this.dirty = true;
	}
	
	
	/**
	 * Sets the ConfigurationLimit for the specified mob type. Don't add 
	 * limits you don't need.
	 * 
	 * @param type The type of entity to set a ConfigurationLimit for.
	 * @param limit The limit for the entity type.
	 */
	public void setLimit(EntityType type, ConfigurationLimit limit) {
		
		switch (limit.getCulling()) {
		case DAMAGE:
			this.hasDamageLimits = true;
			break;
		case SPAWN:
		case SPAWNER:
			this.hasSpawnLimits = true;
			break;
		case MERGE:
			this.hasMergeLimits = true;
			break;
		}
		
		if (mobLimits.containsKey(type)) {
			List<ConfigurationLimit> otherLimits = mobLimits.get(type);
			
			boolean foundOneToEdit = false;
			
			for (ConfigurationLimit otherLimit : otherLimits) {
				if (0 == otherLimit.getCulling().compareTo(limit.getCulling())) {
					otherLimit.setLimit(limit.getLimit());
					otherLimit.setRange(limit.getRange());
					otherLimit.setSpawnDelay(limit.getSpawnDelay());
					otherLimit.setMultiplier(limit.getMultiplier());
					otherLimit.setMultiplierLimit(limit.getMultiplierLimit());
					
					foundOneToEdit = true;
					break;
				}
			}
			
			if (!foundOneToEdit) {
				otherLimits.add(limit);
			}
		}
		else {
			List<ConfigurationLimit> otherLimits = new ArrayList<ConfigurationLimit>();
			otherLimits.add(limit);
			mobLimits.put(type, otherLimits);
		}

		this.dirty = true;
		if(limit.getCulling() == CullType.MERGE) {
			this.pluginInstance.getLogger().info("Culling " + type.toString() + " using " + limit.getCulling().toString() + "; limit=" + limit.getLimit() + " range=" + limit.getRange() + " spawnDelay=" + limit.getSpawnDelay() + " multiplier=" + limit.getMultiplier() + " multiplierLimit=" + limit.getMultiplierLimit());
		} else {
			this.pluginInstance.getLogger().info("Culling " + type.toString() + " using " + limit.getCulling().toString() + "; limit=" + limit.getLimit() + " range=" + limit.getRange());
		}
	}
	
	/**
	 * Returns the ConfigurationLimits for the specified mob type. 
	 * @param type The type of entity to get a ConfigurationLimit for.
	 * @return The limits for the entity type, or null.
	 */
	public List<ConfigurationLimit> getLimits(EntityType type) {
		return mobLimits.get(type);
	}




	/**
	 * Returns whether or not we have limits with CullType SPAWN or SPAWNER.
	 * @return true if there are any mobs with SPAWN or SPAWNER CullTypes, otherwise false.
	 */
	public boolean hasSpawnLimits() {
		return hasSpawnLimits;
	}


	/**
	 * Returns whether or not we have limits with CullType DAMAGE.
	 * @return true if there are any mobs with DAMAGE CullType, otherwise false.
	 */
	public boolean hasDamageLimits() {
		return hasDamageLimits;
	}

	/**
	 * Returns whether or not we have limits with CullType MERGE.
	 * @return true if there are any mobs with MERGE CullType, otherwise false.
	 */
	public boolean hasMergeLimits() {
		return hasMergeLimits;
	}
	
	/**
	 * Returns the number of ticks between calls to the damage laborer.
	 * @return Number of ticks between calls to the damage laborer.
	 */
	public long getTicksBetweenDamage() {
		return ticksBetweenDamage;
	}

	/**
	 * Sets the number of ticks between calls to the damage laborer.
	 * @param ticksBetweenDamage Number of ticks between calls to the damage laborer.
	 */
	public void setTicksBetweenDamage(long ticksBetweenDamage) {

		this.pluginInstance.getLogger().info("MusterCull will damage something every " + ticksBetweenDamage + " ticks.");

		if (ticksBetweenDamage < 20) {
			this.pluginInstance.getLogger().info("Warning: ticks_between_damage is < 20, probably won't run that fast.");
		}

		this.ticksBetweenDamage = ticksBetweenDamage;
		this.dirty = true;
	}
	
	/**
	 * Returns the number of entities to take damage each time the laborer is called.
	 * @return Number of entities to take damage each time the laborer is called.
	 */
	public int getDamageCalls() {
		return damageCalls;
	}
	
	/**
	 * Sets the number of entities to take damage each time the laborer is called. 
	 * @param damageCalls Number of entities to take damage each time the laborer is called.
	 */
	public void setDamageCalls(int damageCalls) {
		if (damageCalls <= 0) {
			this.pluginInstance.getLogger().info("Warning: damage_count is <= 0, possibly wasting cpu cycles.");
		}
		else if (damageCalls > 5) {
			this.pluginInstance.getLogger().info("Notice: damage_count is > 5, possibly killing performance.");
		}
		
		this.damageCalls = damageCalls;
		this.dirty = true;
	}


	/**
	 * Returns the percent chance that a mob will be damaged when crowded.
	 * @return Percent chance that a mob will be damaged when crowded.
	 */
	public int getDamageChance() {
		return damageChance;
	}
	
	/**
	 * Sets the percent chance that a mob will be damaged when crowded.
	 * @param damageChance Percent chance that a mob will be damaged when crowded.
	 */
	public void setDamageChance(int damageChance) {
		if (damageChance <= 0) {
			this.pluginInstance.getLogger().info("Warning: damage_chance is <= 0, possibly wasting cpu cycles.");
		}
		else if (damageChance > 100) {
			this.pluginInstance.getLogger().info("Notice: damage_chance is > 100 when 100 is the limit. Pedantry.");
		}
		
		this.damageChance = damageChance;
		this.dirty = true;
	}
	
	/**
	 * Returns the limit on mobs before the damage laborer cares to act.
	 * @return The limit on mobs before the damage laborer cares to act.
	 */
	public int getMobLimitDamage() {
		return this.mobLimitDamage;
	}
	
	/**
	 * Sets the limit on mobs before the damage laborer cares to act.
	 * @param mobLimit The limit on mobs before the damage laborer cares to act.
	 */
	public void setMobLimitDamage(int mobLimitDamage) {

		if (mobLimitDamage < 0) {
			this.pluginInstance.getLogger().info("Warning: mob_limit_damage is < 0 when 0 is the limit. Pedantry.");
		}
		
		if (mobLimitDamage > 5000) {
			this.pluginInstance.getLogger().info("Warning: mob_limit_damage is > 5000. Damage laborer may never run.");
		}
		
		this.mobLimitDamage = mobLimitDamage;
		this.dirty = true;
	}
	
	/**
	 * Returns the percent part per total before the damage laborer queues mobs.
	 * @return The percent part per total before the damage laborer queues mobs.
	 */
	public int getMobLimitPercentDamage() {
		return this.mobLimitPercentDamage;
	}
	
	/**
	 * Sets the percent part per total before the damage laborer queues mobs.
	 * @param mobLimitPercent The percent part per total before the damage laborer queues mobs.
	 */
	public void setMobLimitPercentDamage(int mobLimitPercentDamage) {

		if (mobLimitPercentDamage < 0) {
			this.pluginInstance.getLogger().info("Warning: mob_limit_percent_damage is < 0 when 0 is the limit. Pedantry.");
		}
		
		if (mobLimitPercentDamage > 100) {
			this.pluginInstance.getLogger().info("Warning: mob_limit_percent_damage is > 100 when 100 is the limit. Pedantry.");
		}
		
		this.mobLimitPercentDamage = mobLimitPercentDamage;
		this.dirty = true;
	}
	
	/**
	 * Returns the number of ticks between calls to the damage laborer.
	 * @return Number of ticks between calls to the damage laborer.
	 */
	public long getTicksBetweenMerge() {
		return ticksBetweenMerge;
	}

	/**
	 * Sets the number of ticks between calls to the merge laborer.
	 * @param ticksBetweenDamage Number of ticks between calls to the merge laborer.
	 */
	public void setTicksBetweenMerge(long ticksBetweenMerge) {

		this.pluginInstance.getLogger().info("MusterCull will merge something every " + ticksBetweenMerge + " ticks.");

		if (ticksBetweenMerge < 20) {
			this.pluginInstance.getLogger().info("Warning: ticks_between_merge is < 20, probably won't run that fast.");
		}

		this.ticksBetweenMerge = ticksBetweenMerge;
		this.dirty = true;
	}
	
	/**
	 * Returns the number of entities to merge each time the laborer is called.
	 * @return Number of entities to merge each time the laborer is called.
	 */
	public int getMergeCalls() {
		return mergeCalls;
	}
	
	/**
	 * Sets the number of entities to take damage each time the laborer is called. 
	 * @param damageCalls Number of entities to take damage each time the laborer is called.
	 */
	public void setMergeCalls(int mergeCalls) {
		if (mergeCalls <= 0) {
			this.pluginInstance.getLogger().info("Warning: merge_count is <= 0, possibly wasting cpu cycles.");
		}
		else if (mergeCalls > 5) {
			this.pluginInstance.getLogger().info("Notice: merge_count is > 5, possibly killing performance.");
		}
		
		this.mergeCalls = mergeCalls;
		this.dirty = true;
	}
	
	/**
	 * Returns the limit on mobs before the merge laborer cares to act.
	 * @return The limit on mobs before the merge laborer cares to act.
	 */
	public int getMobLimitMerge() {
		return this.mobLimitMerge;
	}
	
	/**
	 * Sets the limit on mobs before the merge laborer cares to act.
	 * @param mobLimit The limit on mobs before the merge laborer cares to act.
	 */
	public void setMobLimitMerge(int mobLimitMerge) {

		if (mobLimitMerge < 0) {
			this.pluginInstance.getLogger().info("Warning: mob_limit_merge is < 0 when 0 is the limit. Pedantry.");
		}
		
		if (mobLimitMerge > 5000) {
			this.pluginInstance.getLogger().info("Warning: mob_limit_merge is > 5000. Damage laborer may never run.");
		}
		
		this.mobLimitMerge = mobLimitMerge;
		this.dirty = true;
	}
	
	/**
	 * Returns the percent part per total before the merge laborer queues mobs.
	 * @return The percent part per total before the merge laborer queues mobs.
	 */
	public int getMobLimitPercentMerge() {
		return this.mobLimitPercentMerge;
	}
	
	/**
	 * Sets the percent part per total before the merge laborer queues mobs.
	 * @param mobLimitPercent The percent part per total before the merge laborer queues mobs.
	 */
	public void setMobLimitPercentMerge(int mobLimitPercentMerge) {

		if (mobLimitPercentMerge < 0) {
			this.pluginInstance.getLogger().info("Warning: mob_limit_percent_merge is < 0 when 0 is the limit. Pedantry.");
		}
		
		if (mobLimitPercentMerge > 100) {
			this.pluginInstance.getLogger().info("Warning: mob_limit_percent_merge is > 100 when 100 is the limit. Pedantry.");
		}
		
		this.mobLimitPercentMerge = mobLimitPercentMerge;
		this.dirty = true;
	}
	
	/**
	 * Returns the hard mob cap.
	 * @return The hard mob cap.
	 */
	public int getMaxMob(){
		return this.maxMob;
	}
	
	/**
	 * Sets the hard mob cap.
	 * @param maxMob The hard mob cap.
	 */
	public void setMaxMob(int maxMob) {
		if (maxMob < 0) {
			this.pluginInstance.getLogger().info("Warning: maxMob is < 0 when 0 is the limit. Pedantry.");
		}
		
		this.maxMob = maxMob;
		this.dirty = true;
	}
	
	/**
	 * Returns how many mobs permitted less of the maximum, per player.
	 * @return How many mobs permitted less of the maximum, per player.
	 */
	public int getPlayerMultiplier(){
		return this.playerMultiplier;
	}
	
	/**
	 * Sets how many mobs permitted less of the maximum, per player.
	 * @param playerMultiplier How many mobs permitted less of the maximum, per player.
	 */
	public void setPlayerMultiplier(int playerMultiplier) {
		if (playerMultiplier < 0) {
			this.pluginInstance.getLogger().info("Warning: playerMultiplier is < 0 when 0 is the limit. Pedantry.");
		}
		
		this.playerMultiplier = playerMultiplier;
		this.dirty = true;
	}

    /**
     * Returns number of ticks between calls to the hard cap laborer.
     * @return number of ticks between calls to the hard cap laborer.
     */
    public long getTicksBetweenHardCap(){
        return ticksBetweenHardCap;
    }
    
    /**
     * Sets the culling strategy.
     * @param cullingStrategy is either RANDOM or PRIORITY to determine if we should randomize culling or cull items based on what is least likely to be missed vs most likely.
     */
    public void setHardCapCullingStrategy(String cullingStrategy) {
    	
    	cullingStrategy = cullingStrategy.toUpperCase();
    	
    	if (!cullingStrategy.equals("RANDOM") && !cullingStrategy.equals("PRIORITY"))
    	{
            pluginInstance.getLogger().warning("hard_cap_culling_strategy not an allowed value (needs RANDOM or PRIORITY - has " + cullingStrategy + ".");
            return;
    	}
        
    	this.hardCapCullingStrategy = cullingStrategy;
    	
		pluginInstance.getLogger().info("MusterCull hard cap culling strategy = " + this.hardCapCullingStrategy + ".");
		
		dirty = true;
    }
    
    public GlobalCullCullingStrategyType getHardCapCullingStrategy() {
    	return GlobalCullCullingStrategyType.fromName(hardCapCullingStrategy);
    }

    /**
     * Sets the number of ticks between calls to the hard cap laborer.
     * @param ticksBetween Number of ticks between calls to the damage laborer.
     */
    public void setTicksBetweenHardCap(long ticksBetween) {

        pluginInstance.getLogger().info("MusterCull will kill something every " + ticksBetween + " ticks.");

        if (ticksBetween < 200) {
            pluginInstance.getLogger().warning("ticks_between_hard_cap is < 200, ignoring this and setting to 200.");
            ticksBetween = 200;
        }

        ticksBetweenHardCap = ticksBetween;
        dirty = true;
    }

	/**
	 * Gets whether to notify when an entity is damaged by this plugin.
	 */
	public boolean getDamageNotify() {
		return this.damageNotify;
	}
	
	/**
	 * Sets whether to notify when an entity is damaged by this plugin.
	 * @param damageNotify Whether to notify when an entity is damaged by this plugin.
	 */
	public void setDamageNotify(boolean damageNotify) {
		
		this.damageNotify = damageNotify;
		this.dirty = true;
	}
	
    public boolean monsterCullToSpawnEnabled() {
		return enableMonsterCullToSpawn;
	}

	public int getMaximumMonsterCullPerPass() {
		return maximumMonsterCullPerPass;
	}

	public int getMaximumMonsterCullAggression() {
		return maximumMonsterCullAggression;
	}

	public int getMinimumMonsterCullAggression() {
		return minimumMonsterCullAggression;
	}
	
    public void setEnableMonsterCullToSpawn(boolean enableMonsterCullToSpawn) {
		this.enableMonsterCullToSpawn = enableMonsterCullToSpawn;

		if (monsterCullToSpawnEnabled()) {
			this.pluginInstance.getLogger().info("Monster cull: Up to " + getMaximumMonsterCullPerPass() + " mobs per run, aggression is " + getMinimumMonsterCullAggression() + " to " + getMaximumMonsterCullAggression() + ".");
		}
	}

	public void setMaximumMonsterCullPerPass(int maximumMonsterCullPerPass) {
		this.maximumMonsterCullPerPass = maximumMonsterCullPerPass;

		if (monsterCullToSpawnEnabled()) {
			this.pluginInstance.getLogger().info("Monster cull: Up to " + getMaximumMonsterCullPerPass() + " mobs per run, aggression is " + getMinimumMonsterCullAggression() + " to " + getMaximumMonsterCullAggression() + ".");
		}
	}

	public void setMaximumMonsterCullAggression(int maximumMonsterCullAggression) {
		this.maximumMonsterCullAggression = maximumMonsterCullAggression;

		if (monsterCullToSpawnEnabled()) {
			this.pluginInstance.getLogger().info("Monster cull: Up to " + getMaximumMonsterCullPerPass() + " mobs per run, aggression is " + getMinimumMonsterCullAggression() + " to " + getMaximumMonsterCullAggression() + ".");
		}
	}

	public void setMinimumMonsterCullAggression(int minimumMonsterCullAggression) {
		this.minimumMonsterCullAggression = minimumMonsterCullAggression;

		if (monsterCullToSpawnEnabled()) {
			this.pluginInstance.getLogger().info("Monster cull: Up to " + getMaximumMonsterCullPerPass() + " mobs per run, aggression is " + getMinimumMonsterCullAggression() + " to " + getMaximumMonsterCullAggression() + ".");
		}
	}
		
}
