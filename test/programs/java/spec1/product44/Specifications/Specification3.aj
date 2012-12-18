package Specifications;

import MinePumpSystem.MinePump;
import MinePumpSystem.Environment;
import TestSpecifications.SpecificationException;
import TestSpecifications.SpecificationManager;

public privileged aspect Specification3 extends AbstractSpecification {
	// Specification 3: When the water is high and there is no methane, then the pump is on.
	after(MinePump p) : 
		call(public void MinePumpSystem.MinePump.timeShift()) && target(p) {
		 if (SpecificationManager.checkSpecification(3)) {
			 if (!p.getEnv().isMethaneLevelCritical() && p.getEnv().getWaterLevel() == Environment.WaterLevelEnum.high && !p.pumpRunning) {
			 	throw new SpecificationException("Spec3", "Pump is not running although there is high water and no methane.");
			 }
		 }
	}
}
