OBSERVER AUTOMATON AUTOMATON_32_1a
INITIAL STATE Unlocked;

STATE USEALL Unlocked :
  MATCH {mutex_lock($?)} -> PRINT "LOCK" GOTO Locked;
  MATCH {mutex_unlock($?)} -> ERROR("double unlock");
  MATCH {ldv_check_final_state($?)} -> PRINT "CHECK" GOTO Unlocked;
  MATCH RETURN {$1=mutex_trylock($?)} -> ASSUME { ((int)$1)!=0 } GOTO Locked; 
  MATCH RETURN {$1=mutex_trylock($?)} -> ASSUME { ((int)$1)==0 } GOTO Unlocked; 

STATE USEFIRST Locked :
  MATCH {mutex_unlock($?)} -> PRINT "UNLOCK" GOTO Unlocked;
  MATCH {mutex_lock($?)}  -> ERROR("double lock");
  MATCH {ldv_check_final_state($?)} -> ERROR("lock without unlock");

END AUTOMATON
