package com.untamedears.mustercull.culltypes;

public enum GlobalCullCullingStrategyType {
	RANDOM
	, PRIORITY;
	
	public static GlobalCullCullingStrategyType fromName(String name) {
			
		if (name == null) {
			return null;
		}
		
		for (GlobalCullCullingStrategyType strategy : values()) {
			if (0 == name.compareToIgnoreCase(strategy.name().toUpperCase())) {
				return strategy;
			}
		}
		
		return null;
	}	
}
