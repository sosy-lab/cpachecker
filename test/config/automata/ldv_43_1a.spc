OBSERVER AUTOMATON AUTOMATON_43_1a
INITIAL STATE Unlocked;

STATE USEFIRST Unlocked :
  MATCH CALL {spin_lock($?)} -> GOTO Locked;
  MATCH RETURN {$1=spin_trylock($?)} -> ASSUME { ((int)$1)!=0 } GOTO Locked;

STATE USEALL Locked :
  MATCH CALL {spin_unlock($?)} -> GOTO Unlocked;
  MATCH CALL {alloc_atomic($1)} -> ASSUME {((int)$1)==0 || ((int)$1)==32} ERROR; // cannot be called inside spin lock section with arguments 0 or 32
  MATCH CALL {alloc_atomic($1)} -> ASSUME {((int)$1)!=0 && ((int)$1)!=32} GOTO Locked;
  MATCH CALL {alloc_nonatomic($?)} ->  ERROR; // cannot be called inside spin lock section

END AUTOMATON
