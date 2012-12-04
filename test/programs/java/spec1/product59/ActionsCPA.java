import MinePumpSystem.Environment; 
import MinePumpSystem.MinePump; 

public  class  ActionsCPA {
	

	Environment env;

	
	MinePump p;

    boolean methAndRunningLastTime = false;
    boolean switchedOnBeforeTS = false;
	
	
	ActionsCPA() {
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
	    if(p.systemActive)
		p.stopSystem();
	}

	

	void startSystem  () {
	    if(!p.systemActive)
		p.startSystem();
	}

	
	
	void timeShift() {
	
	    if(p.systemActive)
	      Specification5_1();
	
		p.timeShift();
		
		if(p.systemActive) {
		  Specification1();
		  Specification2();		
	      Specification3();
		  Specification4();
		  Specification5_2();
		  SpecificationS();
		  
		  
		}
	}
	
	String getSystemState() {
		return p.toString();
	}
	
		void SpecificationS() {			
	
	        Environment e = p.getEnv();
	
		    boolean b1 = e.getWaterLevel() == Environment.WaterLevelEnum.low;
	        boolean b2 = p.isLowWaterLevel();
	        			 
			assert b1 && b2 || !b1 && !b2;   
		    //throw new SpecificationException("Spec1", "Pump is running while methane is in the Mine.");
			  			    
    }

	
	// Specification 1 methan is Critical and pumping leads to Error
	void Specification1() {			
	
	        Environment e = p.getEnv();
	
		    boolean b1 = e.isMethaneLevelCritical();
	        boolean b2 = p.pumpRunning;
	     
			 if ( b1 && b2) {
			     assert false;   
				 //throw new SpecificationException("Spec1", "Pump is running while methane is in the Mine.");
			 }		 			    
    }


	//Specification 2: When the pump is running, and there is methane, then it is in switched off at most 1 timesteps.    
    void Specification2() {			
    
        Environment e = p.getEnv();

        boolean b1 = e.isMethaneLevelCritical();
	    boolean b2 = p.pumpRunning;

        if (b1 && b2) {
	        if (methAndRunningLastTime) {
			    //throw new SpecificationException("Spec2", "Pump continued running while methane is in the Mine.");
			    assert false;
		    } else{
			    methAndRunningLastTime = true;
		    }
	    } else {
		    methAndRunningLastTime = false;
		}
    }
    
   // Specification 3: When the water is high and there is no methane, then the pump is on. 
   void Specification3() {
   
        
	    Environment e = p.getEnv();
   
        boolean b1 = e.isMethaneLevelCritical();
	    boolean b2 = p.pumpRunning;
	    boolean b3 = e.getWaterLevel() == Environment.WaterLevelEnum.high;
   
		if (!b1 && b3 && !b2) {
		    //throw new SpecificationException("Spec3", "Pump is not running although there is high water and no methane.");
		    assert false;
	    }		 
    }
    
       // Specification 4: the pump is never on when the water level is low
   void Specification4() {
   
        Environment e = p.getEnv();
      
	    boolean b2 = p.pumpRunning;
	    boolean b3 = e.getWaterLevel() == Environment.WaterLevelEnum.low;
            
			 if (b3 && b2) {
			 	//throw new SpecificationException("Spec4", "Pump is running although there is low water.");
			 	assert false;
			 }
		 	 
    }
    
           // Specification 5: The Pump is never switched on when the water is below the highWater sensor.
   void Specification5_1() {
            

			switchedOnBeforeTS = p.pumpRunning;
		
		 	 
    }
    
           // Specification 5: The Pump is never switched on when the water is below the highWater sensor.
   void Specification5_2() {
    
        Environment e = p.getEnv();
       
        boolean b1 = p.pumpRunning;
	    boolean b2 = e.getWaterLevel() != Environment.WaterLevelEnum.high;
   
            
			 if ((b2) && (b1 && !switchedOnBeforeTS)) {
			 	//throw new SpecificationException("Spec5", "Pump was switched on although the water is not high");
			 	assert false;
			 }
    }
}
