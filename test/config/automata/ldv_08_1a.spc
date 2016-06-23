OBSERVER AUTOMATON 08_1a
LOCAL int state = 0;
INITIAL STATE Init;

STATE USEALL Init :
  MATCH CALL {ldv_module_get($1)} -> ASSUME {((struct module *)$1) != 0} DO state=1 GOTO Inc;
  MATCH CALL {ldv_module_get($1)} -> ASSUME {((struct module *)$1) == 0} GOTO Init;
  MATCH RETURN {$1=ldv_try_module_get($2)} -> ASSUME {((int)$1)!=0; ((struct module *)$2)!=0} DO state=1 GOTO Inc;
  MATCH RETURN {$1=ldv_try_module_get($2)} -> ASSUME {((int)$1)==0} GOTO Init;
  MATCH RETURN {$1=ldv_try_module_get($2)} -> ASSUME {((struct module *)$2)==0} GOTO Init;
  MATCH CALL {ldv_module_put($1)} -> ASSUME {((struct module *)$1) != 0} ERROR("put without get");
  MATCH CALL {ldv_module_put($1)} -> ASSUME {((struct module *)$1) == 0} GOTO Init;
  MATCH CALL {ldv_module_put_and_exit($?)} -> ERROR("put without get exit");
  MATCH RETURN {$1 = ldv_module_refcount($?)} -> SPLIT {((int)$1)==((int)$state)} GOTO Init NEGATION GOTO Stop;

STATE USEALL Inc :
  MATCH CALL {ldv_module_get($1)} -> ASSUME {((struct module *)$1) != 0} DO state=state+1 GOTO Inc;
  MATCH CALL {ldv_module_get($1)} -> ASSUME {((struct module *)$1) == 0} GOTO Inc;
  MATCH RETURN {$1=ldv_try_module_get($2)} -> ASSUME {((int)$1)!=0; ((struct module *)$2)!=0} DO state=state+1 GOTO Inc;
  MATCH RETURN {$1=ldv_try_module_get($2)} -> ASSUME {((int)$1)==0} GOTO Inc;
  MATCH RETURN {$1=ldv_try_module_get($2)} -> ASSUME {((struct module *)$2)==0} GOTO Inc;
  MATCH CALL {ldv_module_put($1)} -> ASSUME {((struct module *)$1) != 0; ((int)$state)>0} DO state=state-1 GOTO Inc;
  MATCH CALL {ldv_module_put($1)} -> ASSUME {((struct module *)$1) != 0; ((int)$state)<=0} DO state=state-1 GOTO Init;
  MATCH CALL {ldv_module_put($1)} -> ASSUME {((struct module *)$1) == 0} GOTO Inc;
  MATCH EXIT -> ERROR("get without put");
  MATCH CALL {ldv_module_put_and_exit($?)} -> GOTO Stop;
  MATCH RETURN {$1 = ldv_module_refcount($?)} -> SPLIT {((int)$1)==((int)$state)} GOTO Inc NEGATION GOTO Stop;

STATE USEFIRST Stop :
  TRUE -> GOTO Stop;

END AUTOMATON
