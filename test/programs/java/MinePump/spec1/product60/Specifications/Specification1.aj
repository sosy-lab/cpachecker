package Specifications;

import MinePumpSystem.MinePump;
import TestSpecifications.SpecificationException;
import TestSpecifications.SpecificationManager;

public privileged aspect Specification1 extends AbstractSpecification {
	// Specification 1: The pump is never on when there is methane.
	after(MinePump p) : 
		call(public void MinePumpSystem.MinePump.timeShift()) && target(p) {
		 if (SpecificationManager.checkSpecification(1)) {
			 if (p.getEnv().isMethaneLevelCritical() && p.pumpRunning) {
				 throw new SpecificationException("Spec1", "Pump is running while methane is in the Mine.");
			 }
		 }
	}
}
