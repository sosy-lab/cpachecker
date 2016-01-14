OBSERVER AUTOMATON AUTOMATON_08_1a

INITIAL STATE Init;

STATE USEALL Init :
    MATCH ENTRY -> ENCODE {int state_module = 0;} GOTO Init;

  MATCH CALL {ldv_module_get($1)} -> ASSUME {((struct module *)$1) != 0} ENCODE {state_module=1;} GOTO Inc;
  MATCH CALL {ldv_module_get($1)} -> ASSUME {((struct module *)$1) == 0} GOTO Init;
  MATCH RETURN {$1=ldv_try_module_get($2)} -> ASSUME {((int)$1)!=0; ((struct module *)$2)!=0} ENCODE {state_module=1;} GOTO Inc;
  MATCH CALL {ldv_module_put($1)} -> ASSUME {((struct module *)$1) != 0} ERROR("put without get");
  MATCH CALL {ldv_module_put_and_exit($?)} -> ERROR("put without get exit");
  MATCH RETURN {$1 = ldv_module_refcount($?)} -> SPLIT {((int)$1)==((int)state_module)} GOTO Init NEGATION GOTO Stop;

STATE USEALL Inc :
  MATCH CALL {ldv_module_get($1)} -> ASSUME {((struct module *)$1) != 0} ENCODE {state_module=state_module+1;} GOTO Inc;
  MATCH CALL {ldv_module_get($1)} -> ASSUME {((struct module *)$1) == 0} GOTO Inc;
  MATCH RETURN {$1=ldv_try_module_get($2)} -> ASSUME {((int)$1)!=0; ((struct module *)$2)!=0} ENCODE {state_module=state_module+1;} GOTO Inc;
  MATCH CALL {ldv_module_put($1)} -> ASSUME {((struct module *)$1) != 0; ((int)state_module)>1} ENCODE {state_module=state_module-1;} GOTO Inc;
  MATCH CALL {ldv_module_put($1)} -> ASSUME {((struct module *)$1) != 0; ((int)state_module)<=1} ENCODE {state_module=state_module-1;} GOTO Init;
  MATCH EXIT -> ERROR("get without put");
  MATCH CALL {ldv_module_put_and_exit($?)} -> GOTO Stop;
  MATCH RETURN {$1 = ldv_module_refcount($?)} -> SPLIT {((int)$1)==((int)state_module)} GOTO Inc NEGATION GOTO Stop;

STATE USEFIRST Stop :
  TRUE -> GOTO Stop;

END AUTOMATON
