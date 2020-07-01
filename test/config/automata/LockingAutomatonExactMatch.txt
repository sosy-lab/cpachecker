OBSERVER AUTOMATON LockingAutomaton
// Kommentar
/* Kommentar */

LOCAL int lock= 0;

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH  "init();"  -> ASSERT lock == 0 DO lock = 0  GOTO Unlocked;
  MATCH  "lock();"  -> ERROR;
  MATCH  "unlock();"  -> ERROR;


STATE USEFIRST Locked :
  MATCH  "init();"  ->  ERROR;
  MATCH  "lock();"  ->  ERROR;
  MATCH  "unlock();"  -> ASSERT lock == 1  DO  lock = 0  GOTO Unlocked;


STATE USEFIRST Unlocked :
  MATCH  "init();"  ->  ERROR;
  MATCH  "lock();"  -> ASSERT lock == 0  DO  lock = 1  GOTO Locked;
  MATCH  "unlock();"  -> ERROR;

END AUTOMATON
