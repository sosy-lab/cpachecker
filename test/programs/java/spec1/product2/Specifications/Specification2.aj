package Specifications;

import MinePumpSystem.MinePump;
import TestSpecifications.SpecificationException;
import TestSpecifications.SpecificationManager;

public privileged aspect Specification2 extends AbstractSpecification {
	// Original : When the pump is running, and there is methane, then it is eventually switched off.
	// my Version:
	// Specification 2: When the pump is running, and there is methane, then it is in switched off at most 1 timesteps.
	boolean methAndRunningLastTime = false;
	after(MinePump p) : 
		call(public void MinePumpSystem.MinePump.timeShift()) && target(p) {
		 if (SpecificationManager.checkSpecification(2)) {
			 if (p.getEnv().isMethaneLevelCritical() && p.pumpRunning) {
				 if (methAndRunningLastTime)
					 throw new SpecificationException("Spec2", "Pump continued running while methane is in the Mine.");
				 else
					 methAndRunningLastTime = true;
			 } else
				 methAndRunningLastTime = false;
		 }
	}
}
