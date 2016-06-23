OBSERVER AUTOMATON AUTOMATON_118_1a
INITIAL STATE Init;

STATE USEALL Init :
  MATCH ENTRY -> ENCODE {int rlock = 1; int wlock = 1;} GOTO Init;

  MATCH CALL {ldv_read_lock($?)} -> SPLIT {wlock == 1;} ENCODE {rlock=rlock+1;} GOTO Init NEGATION ERROR;
  MATCH CALL {ldv_read_unlock($?)} -> SPLIT {rlock > 1;} ENCODE {rlock=rlock-1;} GOTO Init NEGATION ERROR;

  MATCH CALL {ldv_write_lock($?)} -> SPLIT {wlock == 1;} ENCODE {wlock=wlock+1;} GOTO Init NEGATION ERROR;
  MATCH CALL {ldv_write_unlock($?)} -> SPLIT {wlock == 2;} ENCODE {wlock=wlock-1;} GOTO Init NEGATION ERROR;
  
  MATCH RETURN {$1=ldv_read_trylock($?)} -> ASSUME {((int)$1)==1; wlock == 1;} ENCODE {rlock=rlock+1;} GOTO Init;
  MATCH RETURN {$1=ldv_write_trylock($?)} -> ASSUME {((int)$1)==1; wlock == 2;} ENCODE {wlock=wlock+1;} GOTO Init;

  MATCH EXIT -> SPLIT {rlock == 1;} GOTO Init NEGATION ERROR;
  MATCH EXIT -> SPLIT {wlock == 1;} GOTO Init NEGATION ERROR;

END AUTOMATON
