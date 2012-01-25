OBSERVER AUTOMATON LockingAutomaton
// Kommentar
/* Kommentar */

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH  "t1_started();"  -> GOTO TOStarted;
  MATCH  "t2_started();"  -> GOTO TTStarted;

STATE USEFIRST TOStarted :
  MATCH  "t1_started();"  -> ERROR;
  MATCH  "t2_started();"  -> GOTO Init;

STATE USEFIRST TTStarted :
  MATCH  "t1_started();"  -> GOTO Init;
  MATCH  "t2_started();"  -> ERROR;

END AUTOMATON
