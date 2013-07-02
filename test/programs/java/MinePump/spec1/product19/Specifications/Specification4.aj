package Specifications;

import MinePumpSystem.MinePump;
import MinePumpSystem.Environment;
import TestSpecifications.SpecificationException;
import TestSpecifications.SpecificationManager;

public privileged aspect Specification4 extends AbstractSpecification {
	// Specification 4: the pump is never on when the water level is low
	after(MinePump p) : 
		call(public void MinePumpSystem.MinePump.timeShift()) && target(p) {
		 if (SpecificationManager.checkSpecification(4)) {
			 if (p.getEnv().getWaterLevel() == Environment.WaterLevelEnum.low && p.pumpRunning) {
			 	throw new SpecificationException("Spec4", "Pump is running although there is low water.");
			 }
		 }
	}
}
