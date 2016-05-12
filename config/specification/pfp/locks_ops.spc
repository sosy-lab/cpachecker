OBSERVER AUTOMATON MonitorM_EAssignmentAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH {init();} -> GOTO Unlocked;
  MATCH  {lock();}  -> ERROR;
  MATCH  {unlock();}  -> ERROR;
 
STATE USEFIRST Unlocked:
  MATCH {init();} -> ERROR;
  MATCH  {lock();}  -> GOTO Locked;
  MATCH  {unlock();}  -> ERROR;
 
STATE USEFIRST Locked:
  MATCH {init();} -> ERROR;
  MATCH  {lock();}  -> ERROR;
  MATCH  {unlock();}  -> GOTO Unlocked;
  
END AUTOMATON
