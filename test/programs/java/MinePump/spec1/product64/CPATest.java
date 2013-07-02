
import gov.nasa.jpf.jvm.Verify;

import MinePumpSystem.Environment; 
import MinePumpSystem.MinePump; 

import java.util.List; 
import java.util.ArrayList; 
import java.lang.Throwable; 

public  class  CPATest {


	private static int cleanupTimeShifts = 2;
	    
    	public static void main(String[] args) {
		   randomSequenceOfActions(3); 
	    }

	public static boolean getBoolean() {
		return Verify.getBoolean();
	}


    public static void randomSequenceOfActions(int maxLength) {
		ActionsCPA a = new ActionsCPA();

		int counter = 0;
		while (counter < maxLength) {
			counter++;
			//int action = getIntegerMinMax(0, 8);
			
			
			
			
			boolean action1 = getBoolean();
			boolean action2 = getBoolean();
			boolean action3 = getBoolean();
            //boolean action1 = false;
            //boolean action1 = true;
			//boolean action2 = false;
			//boolean action3 = counter == 0;
			//boolean action4 = false;
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
			//System.out.println(listToString(actionHistory));
			//System.out.println(a.getSystemState());
		}
		
		for (counter = 0; counter < cleanupTimeShifts; counter++) {
			a.timeShift();
		}
	}
}
