package MinePumpSystem; 

import MinePumpSystem.Environment; 

public   class  MinePump {
	

	boolean pumpRunning = false;

	

	boolean systemActive = true;

	

	Environment env;

	

	public MinePump(Environment env) {
		super();
		this.env = env;
	}

	

	public void timeShift() {
		if (pumpRunning)
			env.lowerWaterLevel();
		if (systemActive)
			processEnvironment();
	}

	
	private void  processEnvironment__wrappee__base  () {
		
	}

	
	public void processEnvironment() {
		if (pumpRunning && isMethaneAlarm()) {
			deactivatePump();
		} else {
			processEnvironment__wrappee__base();
		}
	}

	

	void activatePump() {
		pumpRunning = true;
	}

	

	void deactivatePump() {
		pumpRunning = false;
	}

	
	
	boolean isMethaneAlarm() {
		return env.isMethaneLevelCritical();
	}

	

	@Override
	public String toString() {
		return "Pump(System:" + (systemActive?"On":"Off") + ",Pump:" + (pumpRunning?"On":"Off") +") " + env.toString(); 
	}

	
	
	private Environment getEnv() {
		return env;
	}

	
	public void stopSystem() {
		if (pumpRunning) {
			deactivatePump();
		}
		assert !pumpRunning;
		systemActive = false;
	}

	
	public void startSystem() {
		assert !pumpRunning;
		systemActive = true;
	}


}
