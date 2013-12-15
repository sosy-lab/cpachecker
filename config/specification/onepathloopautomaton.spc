CONTROL AUTOMATON OnePathLoopAutomaton

INITIAL STATE entry;

STATE USEFIRST entry :
    MATCH "" -> GOTO loopstate;
    
STATE USEFIRST loopstate :
//MATCH {abort($?)} || MATCH {exit($?)}  -> STOP;
	//MATCH EXIT -> GOTO targetstate;
	MATCH EXIT -> ERROR;
	TRUE -> GOTO loopstate;

TARGET STATE USEFIRST targetstate :
	TRUE -> STOP;
	
END AUTOMATON