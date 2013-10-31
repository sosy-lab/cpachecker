
import java.util.Random;
import MinePumpSystem.Environment; 
import MinePumpSystem.MinePump; 

public  class  Main {

	private static int cleanupTimeShifts = 2;

    	public static void main(String[] args) {
		   randomSequenceOfActions(3); 
	    }

	public static boolean getBoolean() {
	     Random random = new Random();
	     return random.nextBoolean();		
	}


    public static void randomSequenceOfActions(int maxLength) {
		Actions a = new Actions();

		int counter = 0;
		while (counter < maxLength) {
			counter++;
		
			boolean action1 = getBoolean();
			boolean action2 = getBoolean();
			boolean action3 = getBoolean();			
			if (!action3) action4 = getBoolean();

			if (action1) {
				a.waterRise();
			}

			if (action2) {
				a.methaneChange();
			}

			if (action3) {
				a.startSystem();
			} else if (action4) {
				a.stopSystem();
			}

			a.timeShift();
		}
		
		for (counter = 0; counter < cleanupTimeShifts; counter++) {
			a.timeShift();
		}
	}
}
