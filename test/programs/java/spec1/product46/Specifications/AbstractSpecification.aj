package Specifications;
import TestSpecifications.SpecificationException;

public abstract aspect AbstractSpecification {
	private static boolean errorFound = false;
	
	pointcut programTermination() : 
		(
				execution(public void PL_Interface_impl.test(int, int)) ||
				execution(public void JUnit_Scenario_Tests.testFinished())
		);
	pointcut programStart() : 
		(
				execution(public void PL_Interface_impl.test(int, int)) ||
				execution(public void JUnit_Scenario_Tests.setup())
		);
	before() : programStart() {
		reset();
	}
	
	protected void failure(SpecificationException specificationException) {
		// omit following errors. 
		// This might be liveness properties that cannot be fulfilled because the program 
		// terminates because of another error.
		if (errorFound) {
			//System.out.println("omit error: " +specificationException.getMessage());
			return;
		}
		
		errorFound = true;
		throw specificationException;
	}
	
	public void reset() {
		AbstractSpecification.errorFound = false;
	}
}
