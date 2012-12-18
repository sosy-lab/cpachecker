package Specifications;

import MinePumpSystem.MinePump;
import MinePumpSystem.Environment;
import TestSpecifications.SpecificationException;
import TestSpecifications.SpecificationManager;

public privileged aspect Specification5 extends AbstractSpecification {
	// Specification 5: The Pump is never switched on when the water is below the highWater sensor.
	boolean switchedOnBeforeTS = false;
	
	before(MinePump p) : 
		call(public void MinePumpSystem.MinePump.timeShift()) && target(p) {
		if (SpecificationManager.checkSpecification(5)) {
			switchedOnBeforeTS = p.pumpRunning;
		}
	}
	
	after(MinePump p) : 
		call(public void MinePumpSystem.MinePump.timeShift()) && target(p) {
		 if (SpecificationManager.checkSpecification(5)) {
			 if ((p.getEnv().getWaterLevel() != Environment.WaterLevelEnum.high) && 
					 (p.pumpRunning && !switchedOnBeforeTS)) {
			 	throw new SpecificationException("Spec5", "Pump was switched on although the water is not high");
			 }
		 }
	}
}
