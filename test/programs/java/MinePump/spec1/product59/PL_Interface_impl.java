import gov.nasa.jpf.annotation.FilterField; 
import gov.nasa.jpf.jvm.Verify;  //import gov.nasa.jpf.symbc.Symbolic;

import MinePumpSystem.Environment; 
import MinePumpSystem.MinePump; 
import TestSpecifications.SpecificationManager; 

import java.util.List; 
import java.util.ArrayList; 
import java.lang.Throwable; 

public  class  PL_Interface_impl  implements PL_Interface {
	

	@FilterField
	// @Symbolic("false")
	public static boolean executedUnimplementedAction = false;

	

	@FilterField
	// @Symbolic("false")
	public static List<String> actionHistory = new ArrayList<String>();

	

	// @Symbolic("false")
	private static int cleanupTimeShifts = 4;

	

	@FilterField
	// @Symbolic("false")
	private static boolean verbose = false;

	
	
	@FilterField
	// @Symbolic("false")
	private static boolean isAbortedRun = false;

	

	public static void main(String[] args) {
		try {
			PL_Interface_impl impl = new PL_Interface_impl();
			args = new String[1];
			verbose = true;
			impl.start(1, 4);
			System.out.println("no Exception");
		} catch (Throwable e) {
			System.out.println("Caught Exception: " + e.getClass() + " "
					+ e.getMessage());
			e.printStackTrace();
		}
	}

	

	public void start(int specification, int variation) throws Throwable {
		try {
			if (verbose)
				System.out.print("Started Elevator PL with Specification "
						+ specification + ", Variation: " + variation);
			test(specification, variation);
		} catch (Throwable e) {
			throw e;
		} finally {
			/*
			 * System.out.println("Penalty"); if (!isAbortedRun) { int x = 1;
			 * for (int i = 0; i < 6000000; i++) { x = i / x + 10; } }
			 */
		}
	}

	

	public void checkOnlySpecification(int specID) {
		SpecificationManager.checkOnlySpecification(specID);
	}

	

	public List<String> getExecutedActions() {
		return actionHistory;
	}

	

	public boolean isAbortedRun() {
		return isAbortedRun;
	}

	

	// this method is used as hook for the liveness properties.
	public void test(int specification, int variation) {
		if (variation == -1) {
			switch (specification) {
			case 1:
				Specification1();
				break;
			}
		} else {
			randomSequenceOfActions(variation);
		}
	}

	

	public static void randomSequenceOfActions(int maxLength) {
		Actions a = new Actions();

		int counter = 0;
		while (counter < maxLength) {
			counter++;
			//int action = getIntegerMinMax(0, 8);
			boolean action1 = getBoolean();
			boolean action2 = getBoolean();
			boolean action3 = getBoolean();
			boolean action4 = false;
			if (!action3) action4 = getBoolean();

			String actionName = "";
			
			//if (getBoolean()) {
			//if (action%2==0) {
			if (action1) {
				a.waterRise();
				actionName += "rise ";
			}
			//if (getBoolean()) {
			//action = action/2;
			//if (action%2==0) {
			if (action2) {
				a.methaneChange();
				actionName += "methChange ";
			}
			//if (getBoolean()) {
			//action = action/2;
			//if (action%2==0) {
			if (action3) {
				a.startSystem();
				actionName += "start ";
			} else if (action4) {
//			} else {//if (getBoolean()) {
//			action = action/2;
//			if (action%2==0) {
				a.stopSystem();
				actionName += "stop ";
			//}
			}

			a.timeShift();
			actionHistory.add(actionName);
			//System.out.println(listToString(actionHistory));
			//System.out.println(a.getSystemState());
		}
		for (counter = 0; counter < cleanupTimeShifts; counter++) {
			a.timeShift();
		}
	}

	

	public static void Specification1() {

	}

	

	public static int getIntegerMinMax(int min, int max) {
		return Verify.getInt(min, max);
	}

	

	public static boolean getBoolean() {
		return Verify.getBoolean();// verify true first
	}

	

	static String listToString(List<String> list) {
		String ret = "";
		for (String s : list) {
			ret = ret + " " + s;
		}
		return ret;
	}


}
