OBSERVER AUTOMATON AUTOMATON_08_1a
LOCAL int state = 0;
INITIAL STATE Init;

STATE USEALL Init :
  MATCH CALL {ldv_module_get($?)} -> DO state=state+1 GOTO Inc;
  MATCH CALL {ldv_module_put($?)} -> ERROR;

STATE USEALL Inc :
  MATCH CALL {ldv_module_get($?)} -> DO state=state+1 GOTO Inc;
  MATCH CALL {ldv_module_put($?)} -> ASSUME {((int)$state)>0} DO state=state-1 GOTO Inc;
  MATCH CALL {ldv_module_put($?)} -> ASSUME {((int)$state)<=0} DO state=state-1 GOTO Init;
  MATCH EXIT -> ERROR;

END AUTOMATON
