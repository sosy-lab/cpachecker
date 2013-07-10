import MinePumpSystem.Environment; 
import MinePumpSystem.MinePump; 

public   class  Actions {
	

	Environment env;

	
	MinePump p;

	
	
	Actions() {
		env = new Environment();
		p = new MinePump(env);
	}

	
	
	void waterRise() {
		env.waterRise();
	}

	
	void methaneChange() {
		env.changeMethaneLevel();
	}

	
	void stopSystem  () {
		p.stopSystem();
	}

	
	void startSystem() {
		PL_Interface_impl.executedUnimplementedAction = true;
	}

	
	
	void timeShift() {
		p.timeShift();
	}

	
	
	String getSystemState() {
		return p.toString();
	}


}
