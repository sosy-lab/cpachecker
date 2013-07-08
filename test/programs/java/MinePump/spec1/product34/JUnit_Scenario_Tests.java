import org.junit.Before; 
import org.junit.Test; 

import MinePumpSystem.Environment; 
import MinePumpSystem.MinePump; 
import TestSpecifications.SpecificationException; 
import TestSpecifications.SpecificationManager; 

public  class  JUnit_Scenario_Tests {
	

	private static final int cleanupTimeShifts = 12;

	

	/**
	 * Hook for AbstractSpecification.aj (resets specifications at test start)
	 */
	@Before
	public void setup() {

	}

	

	/**
	 * Hook for specifications with liveness properties. (indicator for
	 * test-termination)
	 */
	public void testFinished() {

	}

	

	//@Test(expected = SpecificationException.class)
	public void Specification1() {
		SpecificationManager.checkOnlySpecification(1);

		Environment env = new Environment();
		MinePump p = new MinePump(env);
		env.waterRise();
		//p.startSystem();
		env.waterRise();
		p.timeShift();
		env.waterRise();
		p.timeShift();
		p.timeShift();
		p.timeShift();

		testFinished();
	}

	
	
	@Test(expected = SpecificationException.class)
	public void Specification4() {
		SpecificationManager.checkOnlySpecification(1);
		/*
		verificationClasses.FeatureSwitches.__SELECTED_FEATURE_lowWaterSensor = false;
		verificationClasses.FeatureSwitches.__SELECTED_FEATURE_base = true;
		verificationClasses.FeatureSwitches.__SELECTED_FEATURE_highWaterSensor = true;
		verificationClasses.FeatureSwitches.__SELECTED_FEATURE_methaneAlarm = true;
		verificationClasses.FeatureSwitches.__SELECTED_FEATURE_methaneQuery = true;
		verificationClasses.FeatureSwitches.__SELECTED_FEATURE_startCommand = true;
		verificationClasses.FeatureSwitches.__SELECTED_FEATURE_stopCommand = true;
		*/
		Environment env = new Environment();
		MinePump p = new MinePump(env);
		
		p.timeShift();
		p.timeShift();
		p.timeShift();
		env.waterRise();
		p.timeShift();

		testFinished();
	}


}
