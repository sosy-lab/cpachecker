// This automaton contains the specification for LDV Tools rule 32_1a.

OBSERVER AUTOMATON AUTOMATON_32_1a
INITIAL STATE Unlocked;

STATE USEFIRST Unlocked :
  MATCH {mutex_lock($?)} -> PRINT "LOCK" GOTO Locked;
  MATCH {mutex_unlock($?)} -> ERROR("double unlock");
  MATCH {ldv_check_final_state($?)} -> PRINT "CHECK" GOTO Unlocked;
  MATCH {$1=mutex_trylock($?)} && !CHECK(ValueAnalysis, "$1==0") -> PRINT "trylock=1" GOTO Locked; // if mutex_trylock returns not 0 goto Locked state
  MATCH {$1=mutex_trylock($?)} && CHECK(ValueAnalysis, "$1==0") -> PRINT "trylock=0" GOTO Unlocked; // if mutex_trylock returns 0 stay in Unlocked state

STATE USEFIRST Locked :
  MATCH {mutex_unlock($?)} -> PRINT "UNLOCK" GOTO Unlocked;
  MATCH {mutex_lock($?)}  -> ERROR("double lock");
  MATCH {ldv_check_final_state($?)} -> ERROR("lock without unlock");

END AUTOMATON
