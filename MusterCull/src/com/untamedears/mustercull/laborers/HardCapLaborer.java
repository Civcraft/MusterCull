package com.untamedears.mustercull.laborers;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.DyeColor;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.entity.Wither;

import com.untamedears.mustercull.MusterCull;
import com.untamedears.mustercull.culltypes.GlobalCullCullingStrategyType;
import com.untamedears.mustercull.culltypes.GlobalCullType;

/**
 * This class performs damage to mobs using the DAMAGE CullType.
 * @author ngozi
 */
public class HardCapLaborer extends Laborer {

	// Statistics
	private int _numberOfTimesExecuted = 0;
	private int _numberOfTimesExecutedWithPenaltyPurge = 0;
	private long _averageTimePerExecutionInMs = 0;
	private long _executionTimeForLastExecutionInMs = 0;
	
	/**
	 * Constructor which takes a reference to the main plug-in class.
	 * @param pluginInstance A reference to the main plug-in class.
	 */
	public HardCapLaborer(MusterCull pluginInstance) {
		super(pluginInstance);
	}

	/**
	 * Repeating method for the class.
     * Kills N mobs where N is the number of mobs over the mob cap.
     * Recently dead mobs are counted toward the mob cap since filtering them out is costly.
     * Therefore, this should not run too quickly (set config).
     *
     * If a lot of mobs recently died when this runs then this will kill too many mobs. Possible far too many.
     * Luckily this can only happen when the cap is lowered or old chunks are loaded.
	 */
	
	private static final Object lockObj = new Object();
	private static boolean currentlyRunning = false;
	
	public void run() {

        if (this.getPluginInstance().isPaused(GlobalCullType.HARDCAP)) {
            return;
        }
        
		synchronized(lockObj)
		{
			if(!currentlyRunning)
			{
				currentlyRunning = true;
			}
			else
			{
				return;
			}
			
		}

		int overHardMobLimit = getPluginInstance().overHardMobLimit();

		if (overHardMobLimit > 0) {
			
			long purgeTimeStart = System.currentTimeMillis();
			
			// Only console-warn if the number of mobs to cull is a fairly large-ish number.
			if (overHardMobLimit > 100)
			{
				this.getPluginInstance().getLogger().warning("Hard Cap Laborer - Exceptional Culling - Required to cull " + overHardMobLimit + " mobs.");
			}
			
			GlobalCullCullingStrategyType cullStrat = this.getPluginInstance().getGlobalCullingStrategy();
            
			int toKill = overHardMobLimit;
            
			if (cullStrat == GlobalCullCullingStrategyType.RANDOM)
			{
				// this.getPluginInstance().getLogger().warning("Hard Cap Laborer - Random based culling.");
				List<LivingEntity> mobs = getPluginInstance().getAllLivingNonPlayerMobs();
	            for (LivingEntity mob : mobs) {
	                if (! mob.isDead()) {
	                    toKill--;
	                    getPluginInstance().damageEntity(mob, 100);
	                }
	                if (toKill == 0)
	                    break;
	            }
			}
			else if(cullStrat == GlobalCullCullingStrategyType.PRIORITY)
			{
				// this.getPluginInstance().getLogger().warning("Hard Cap Laborer - Priority based culling.");
				toKill = HandleCullingBasedOnChunkConcentration(getPluginInstance().getAllLivingNonPlayerMobs(), toKill);
	            toKill = HandleGlobalCulling(getPluginInstance().getAllLivingNonPlayerMobs(), toKill);
			}
			else
			{
				this.getPluginInstance().getLogger().warning("Hard Cap Laborer - Cannot determine culling strategy, no work to do.");
			}
			
			_executionTimeForLastExecutionInMs = System.currentTimeMillis() - purgeTimeStart;
			_averageTimePerExecutionInMs = 
						(long) (_averageTimePerExecutionInMs * (_numberOfTimesExecuted / (_numberOfTimesExecuted + 1.)) 
						+ _executionTimeForLastExecutionInMs / (_numberOfTimesExecuted + 1.));
			_numberOfTimesExecuted++;
		}
		
		keepHostilesWithinSpawnLimit();

		currentlyRunning = false;
		return;


	}
	
	public String GetStatisticDisplayString()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("Hard Cap Cull Statistics:  \n");
		
		sb.append("Hard Cap Cull Execution count:  ");
		sb.append(_numberOfTimesExecuted);
		sb.append("\n");
		
		sb.append("Number of culls with penalty purges:  ");
		sb.append(_numberOfTimesExecutedWithPenaltyPurge);
		sb.append("\n");
		
		sb.append("Average time per execution:  ");
		sb.append(_averageTimePerExecutionInMs);
		sb.append(" milliseconds\n");
				
		sb.append("Execution time for last execution:  ");
		sb.append(_executionTimeForLastExecutionInMs);
		sb.append(" milliseconds\n");
		
		return sb.toString();
	}
	
	public int cullPriority(LivingEntity mob) {
		// Priority 1 - Named mob.
		if (null != mob.getCustomName() || mob.isCustomNameVisible()) 
		{ 
			return 5; 
		}
		
		// Priority 2/3/4 - Persistent mobs.
		else if (!mob.getRemoveWhenFarAway())
		{
			// Priority 2 - High value persistent mobs (horses, villagers, player created iron golems).
			if (mob.getType() == EntityType.HORSE)
			{
				return 6;
			}
			else if (mob.getType() == EntityType.IRON_GOLEM && mob instanceof IronGolem && ((IronGolem) mob).isPlayerCreated()) {
				return 6;
			}
			
			//Priority 3 - tame wolves, colored sheep, tame cats.
			//else if (mob.getType() == EntityType.WOLF && ((org.bukkit.entity.Tameable)mob).isTamed())
			//{
			//	return 7;
			//}
			else if (mob.getType() == EntityType.OCELOT && ((org.bukkit.entity.Tameable)mob).isTamed())
			{
				return 7;
			}
			else if (mob.getType() == EntityType.SHEEP && ((org.bukkit.material.Colorable)mob).getColor() != DyeColor.WHITE)
			{
				return 7;
			}
			
			// Priority 4 - low value persistent mobs.
			else
			{
				return 8;
			}
		}
		else
		{
			// Non Priority - non persistent mobs.
			return 9;
		}
	}

	public void keepHostilesWithinSpawnLimit() {
		if (!this.getPluginInstance().getConfiguration().monsterCullToSpawnEnabled()) {
			return;
		}
		
		for (World world : Bukkit.getServer().getWorlds()) {
			// Find the spawn chunks around players in the world
			Set<Chunk> mobSpawnChunks = getMobSpawnChunks(world);
			int spawnChunkCount = mobSpawnChunks.size();
			
			// Find hostiles in this world
			List<LivingEntity> hostiles = new ArrayList<LivingEntity>();
			int i = 0;
			for (LivingEntity entity : world.getLivingEntities()) {
				i++;
				// Monster is hostiles except Ghast, Slime, Magma Cube (which are special case hostiles)
				if (entity instanceof Monster) {
					Monster mob = (Monster) entity;
					// Wither skeletons count as skeletons so are culled fast - exempt them
					if (mob instanceof Skeleton && ((Skeleton) mob).getSkeletonType().equals(SkeletonType.WITHER)) {
						// Do nothing
					} else if (entity instanceof Wither) {
						// Withers are rare constructs - exempt them
					} else {
						hostiles.add(mob);
					}
				}
			}
			
			int naturalLimit = world.getMonsterSpawnLimit() * spawnChunkCount / 256;
			
			// Cull in a cycle, most aggressive at full moon, to encourage turnover rather than stasis
			// Fluctuate aggression from 10% of the cap at full moon to 0 to the cap at new moon
			double days = world.getFullTime() / 24000.0;
			double phase = (days % 8);
			double cycleAmplitude = (1 + Math.cos(days * Math.PI * 0.25)) / 2.0;
			int maxAggression = getPluginInstance().getConfiguration().getMaximumMonsterCullAggression();
			int minAggression = getPluginInstance().getConfiguration().getMinimumMonsterCullAggression();
			
			int aggression = minAggression + ((int) (Math.round(cycleAmplitude * (maxAggression - minAggression))));
			int percentageLimit = 100 - aggression;

			this.getPluginInstance().getLogger().info("Hostile cull - World " + world.getName() + " contains " + hostiles.size() + " of an allowed " + percentageLimit + "% of a hostile spawn limit of " + naturalLimit + ".");
			
			int toKill = hostiles.size() - (naturalLimit * percentageLimit / 100);
			
			if (toKill >= 0 && hostiles.size() > 0) {
				int maxCullPerPass = this.getPluginInstance().getConfiguration().getMaximumMonsterCullPerPass();
				maxCullPerPass = maxCullPerPass * hostiles.size() / 100;
				if (toKill  > maxCullPerPass) {
					toKill = maxCullPerPass;
				}
				this.getPluginInstance().getLogger().info("Hostile cull in world " + world.getName() + " - culling " + toKill + " mobs.");
				GlobalCullCullingStrategyType cullStrat = this.getPluginInstance().getGlobalCullingStrategy();
				
				if (cullStrat == GlobalCullCullingStrategyType.RANDOM)
				{
					// this.getPluginInstance().getLogger().warning("Hard Cap Laborer - Random based culling.");
		            for (LivingEntity mob : hostiles) {
		                if (! mob.isDead()) {
		                    toKill--;
		                    getPluginInstance().damageEntity(mob, 100);
		                }
		                if (toKill == 0)
		                    break;
		            }
				}
				else if(cullStrat == GlobalCullCullingStrategyType.PRIORITY)
				{
					// this.getPluginInstance().getLogger().warning("Hard Cap Laborer - Priority based culling.");
		            toKill = HandleGlobalCulling(hostiles, toKill);
				}
				else
				{
					this.getPluginInstance().getLogger().warning("Hard Cap Hostile Cull - Cannot determine culling strategy, no work to do.");
				}
			}
		}
	}
	
	private Set<Chunk> getMobSpawnChunks(World world) {
		Set<Chunk> chunks = new HashSet<Chunk>();
		for (Player player : world.getPlayers()) {
			int chunkX = player.getLocation().getBlockX() / 16;
			int chunkZ = player.getLocation().getBlockZ() / 16;
			
			int spawnRadius = Bukkit.getServer().getSpawnRadius();
			int viewLimit = Bukkit.getServer().getViewDistance();
			
			int dist = 8;
			if (spawnRadius < dist) dist = spawnRadius;
			if (viewLimit < dist) dist = viewLimit;

			for (int dx = -dist; dx <= dist; ++dx) {
				for (int dz = -dist; dz <= dist; ++dz) {
					if (world.isChunkLoaded(chunkX + dx, chunkZ + dz)) {
						chunks.add(world.getChunkAt(chunkX + dx, chunkZ + dz));
					}
				}	
			}
		}
		return chunks;
	}

	/**
	 * Given an integer mob to kill count, it will attempt to find if there are any problem chunks and begin a culling 
	 * @param mobCountToCull Amount of mobs we would like to kill in total - not the amount of mobs we have to kill just from this chunk based culling.
	 * @return How many mobs are left of the mobs we would like to kill.
	 */
	private int HandleCullingBasedOnChunkConcentration(List<LivingEntity> mobs, int mobCountToCull)
	{	
        HashMap<Point, List<LivingEntity>> chunkEntities = new HashMap<Point, List<LivingEntity>>();
        int totalCullScore = 0;
        
        for (LivingEntity mob : mobs) {
        	 if ( ! mob.isDead()) {
        		 Chunk mobChunk = mob.getLocation().getChunk();
        		 Point d = new Point(mobChunk.getX(), mobChunk.getZ());
        		 
        		 if (!chunkEntities.containsKey(d))
        		 {
        			 chunkEntities.put(d, new ArrayList<LivingEntity>());
        		 }
        		 
        		 if (mob instanceof LivingEntity) {
	        		 chunkEntities.get(d).add(mob);
	        		 totalCullScore += cullPriority(mob);
        		 }
        	 }
        }
        
        Point maxCenterPoint = null;
        int mobCullScoreInLargestSuperChunk = -1;
        int maxChunksInMatchSet = 0;
                
        // For each chunk check all the chunks surrounding it.  3x3 and get mob counts.
        // Find the most populous super chunk.
        for(Point point : chunkEntities.keySet())
        {
        	int cullScore = 0;
        	int tempChunkCount = 0;
        	            	
        	for(int x = -3; x <= 3; x++)
        	{
        		for (int z = -3; z <= 3; z++)
        		{
        			if (chunkEntities.containsKey(new Point(point.x - x, point.y - z)))
        			{
        				for (Entity e : chunkEntities.get(new Point(point.x - x, point.y - z))) {
        					if (e instanceof LivingEntity) {
        						cullScore += cullPriority((LivingEntity) e);
        						tempChunkCount++;
        					}
        				}
        			}
        		}
        	}
        	
        	// If more mobs than our last super chunk, update it.
        	if (cullScore > mobCullScoreInLargestSuperChunk) 
        	{
        		maxCenterPoint = point;
        		mobCullScoreInLargestSuperChunk = cullScore;
        		maxChunksInMatchSet = tempChunkCount;
        	}
        }

        // Number of loaded chunks with mobs excepting the bad super chunk.
        int numberOfChunksToAverageOver = (chunkEntities.keySet().size()-maxChunksInMatchSet);
        
        // If the max mobs for a super chunk doesn't meet the penalty purge percent, early out.
        // Or if the 'penalty chunks' -is- all the chunks loaded.
        if ((mobCullScoreInLargestSuperChunk / ((1.0) * totalCullScore) <= this.getPluginInstance().getHardCapCullingPriorityStrategyPenaltyMobPercent())
        		|| numberOfChunksToAverageOver == 0)
		{
        	return mobCountToCull;
		}
        
        // Log out the naughty chunk.
        this.getPluginInstance().getLogger().warning("Hard Cap Laborer - Found chunk that triggered a penalty purge based on mob count.  Chunk " + maxCenterPoint.x + ", " + maxCenterPoint.y + ".");
        _numberOfTimesExecutedWithPenaltyPurge++;

        int averageScorePerChunk = (int) Math.ceil(totalCullScore / (1. * numberOfChunksToAverageOver));
        int superChunkMobScoreToCull = mobCullScoreInLargestSuperChunk - averageScorePerChunk;
        
        superChunkMobScoreToCull = superChunkMobScoreToCull > mobCountToCull ? mobCountToCull : superChunkMobScoreToCull;
        
        if (superChunkMobScoreToCull <= 0)
        {
        	// It met the limit but its not more than the average, so do nothing.
        	// Only in special cases where loaded chunks are extremely few.
        	return mobCountToCull;
        }
        
        // Consider purging every mob in the chunks surrounding it.  7x7 with the superchunk at the center. 
        List<LivingEntity> mobsToConsiderPurging = new ArrayList<LivingEntity>();	  	
    	for(int x = -3; x <= 3; x++)
    	{
    		for (int z = -3; z <= 3; z++)
    		{
    			if (chunkEntities.containsKey(new Point(maxCenterPoint.x - x, maxCenterPoint.y - z)))
    			{
    				mobsToConsiderPurging.addAll(chunkEntities.get(new Point(maxCenterPoint.x - x, maxCenterPoint.y - z)));
    			}
    		}
    	}
    	
    	// We are only going to purge enough to bring this superchunk into a good status with our other chunks.
    	// Our unculled mobs, then, after this call would be our original mobCountToCull - the superCullCount + whats left over.
    	return mobCountToCull - superChunkMobScoreToCull + PerformCullingLogic(mobsToConsiderPurging, mobCountToCull, superChunkMobScoreToCull);
	}
	
	/**
	 * For a given set of candidates, we will first bring the candidates into proportion based on entity type (up to max items to cull) and will then round-robin remove them (up to max items to cull)
	 * @param cullCandidates The list of possible items that can be culled.
	 * @param maxItemsToCull Maximum items to cull on this run.
	 * @return The number of items remaining out of the orignal items-to-cull passed in.
	 */
	private int PerformCullingLogic(List<LivingEntity> cullCandidates, int maxItemsToCull, int scoreToCull)
	{
		if (scoreToCull == 0) return scoreToCull;
		
		// Step 1 - Categorize all items on our list into categories based on priority.
		Map<Integer, List<LivingEntity>> prioritisedMobs = new HashMap<Integer, List<LivingEntity>>();
		
		for(LivingEntity mob : cullCandidates)
		{
			// TODO: add prioritised entities
			int priority = cullPriority(mob);
			if (!prioritisedMobs.containsKey(priority)) {
				prioritisedMobs.put(priority, new ArrayList<LivingEntity>());
			}
			prioritisedMobs.get(priority).add(mob);
		}
		
		List<Integer> priorities = new ArrayList<Integer>(prioritisedMobs.keySet());
		Collections.sort(priorities);
		Collections.reverse(priorities);
		
		// Step 2 - Cull based on purge priorities.
		for (Integer priority : priorities) {
			if (scoreToCull > 0) {
				int remaining = purgeToEquivalentNumbers(maxItemsToCull, getListSortedByCounts(prioritisedMobs.get(priority)));
				int culled = maxItemsToCull - remaining;
				scoreToCull -= culled * priority;
				maxItemsToCull = remaining;
			}
		}
		
		return maxItemsToCull;
	}

	/**
	 * For a given categorized entity list all of the same priority, we will attempt to bring them into proportion with one another and then round-robin remove (up to max items to cull).
	 * @param maxItemsToCull Maximum amount of items we can cull.
	 * @param orderedPurgeList 
	 * @return Of the maxItemsToCull originally passed in, how many remain to be culled.
	 */
	private int purgeToEquivalentNumbers(int maxItemsToCull, List<Entry<EntityType, List<LivingEntity>>> orderedPurgeList) {

		// Early out if we have no work to do.
		if (orderedPurgeList.size() <= 0 || maxItemsToCull <= 0)
		{
			return maxItemsToCull;
		}
		
		// Determine the average mob count per category.
		int totalItems = 0;
		for(int i = 0; i < orderedPurgeList.size(); i++)
		{
			totalItems += orderedPurgeList.get(i).getValue().size();
		}
		
		int average = totalItems / orderedPurgeList.size();
		
		// Iterate over all of the mob lists to remove (largest list first) and start removing items until it is equal to the average.
		// For each category, while we still have items left to cull.
		for(int i = 0; i < orderedPurgeList.size() && maxItemsToCull > 0; i++)
		{
			List<LivingEntity> purgeList = orderedPurgeList.get(i).getValue();

			// Even if the purgeList is already correctly-sized, we still want to always sort these lists.  Important in round-robin culling next.
			Collections.shuffle(purgeList);
			
			// Start removing mobs from this category.
			while(purgeList.size() > average && purgeList.size() > 0 && maxItemsToCull > 0)
			{
				LivingEntity mob = purgeList.remove(0);
				if (!mob.isDead())
				{
					// Cull it and remove it from our to-cull count.
					maxItemsToCull--;
					getPluginInstance().damageEntity(mob, 100);
				}
			}
		}
		
		
		// Now that we have made our categories approximately equivalent to each other, time to cull round-robin over these now-sorted lists.
		
		int ctr = 0;
		boolean removedAtLeastOneItemThisIteration = true;
		while(maxItemsToCull > 0)
		{
			// If the cycle has restarted.
			if (ctr == 0)
			{
				// Have we removed at least one item?  If not, bye!
				if (!removedAtLeastOneItemThisIteration)
				{
					// Control structure.
					break;
				}
				
				// Restart!
				removedAtLeastOneItemThisIteration = false;
			}
			
			// Kill off one item of this type.
			if (orderedPurgeList.get(ctr).getValue().size() > 0)
			{
				LivingEntity mob = orderedPurgeList.get(ctr).getValue().remove(0);
				if (!mob.isDead())
				{
					// Cull it and remove it from our to-cull count.
					maxItemsToCull--;
					getPluginInstance().damageEntity(mob, 100);

					// Note that we removed at least one mob this iteration.
					removedAtLeastOneItemThisIteration = true;
				}
			}
			
			// Move to next category and loop back if needed.
			ctr++;
			if (ctr >= orderedPurgeList.size() )
			{
				ctr = 0;
			}
			
		}
		
		return maxItemsToCull;
	}

	/**
	 * For a given unordered list of of mobs, we will attempt to categorize these and order this such that our largest mob categories come first. 
	 * @param mobList Unordered list of mobs.
	 * @return Ordered list of categorized mobs (categorized by entity types) such that the largest entity categories will come first.
	 */
	private List<Entry<EntityType, List<LivingEntity>>> getListSortedByCounts(List<LivingEntity> mobList) 
	{
		// Separate mobs by entity type.
		HashMap<EntityType, List<LivingEntity>> equivalentPurgeList = new HashMap<EntityType, List<LivingEntity>>();
		for(LivingEntity mob : mobList)
		{
			EntityType type = mob.getType();
			if(!equivalentPurgeList.containsKey(type))
			{
				equivalentPurgeList.put(type, new ArrayList<LivingEntity>());				
			}
			
			equivalentPurgeList.get(type).add(mob);
		}
		
		// Sort these entity-types by the ones with the most first.
		List<Entry<EntityType, List<LivingEntity>>> orderedPurgeList = new LinkedList<Entry<EntityType, List<LivingEntity>>> (equivalentPurgeList.entrySet());
		Collections.sort(orderedPurgeList, Collections.reverseOrder(new Comparator<Entry<EntityType, List<LivingEntity>>>()
				{
					public int compare(Entry<EntityType, List<LivingEntity>> o1, Entry<EntityType, List<LivingEntity>> o2)
					{
                        return o1.getValue().size() - o2.getValue().size();
					}
				}
		));
		
		return orderedPurgeList;
	}

	/**
	 * Will perform categorized and proportional global culling in priority order.
	 * @param overHardMobLimit Max number of items to purge.
	 * @return 
	 */
	private int HandleGlobalCulling(List<LivingEntity> mobList, int overHardMobLimit)
	{
		
		return PerformCullingLogic(mobList, overHardMobLimit, mobList.size() * 10);
	}

}
