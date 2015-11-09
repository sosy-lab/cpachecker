OBSERVER AUTOMATON AUTOMATON_147_1a
LOCAL int rcu_nested = 0;
INITIAL STATE Init;


STATE USEALL Init :
  MATCH CALL {rcu_inc($?)} -> DO rcu_nested=rcu_nested+1 GOTO Init;
  MATCH CALL {rcu_dec($?)} -> DO rcu_nested=rcu_nested-1 GOTO Init;
  MATCH CALL {check_for_read_section($?)} -> ASSERT rcu_nested == 0 GOTO Init;
  MATCH CALL {check_for_read_section($?)} -> ASSERT rcu_nested != 0 ERROR;

END AUTOMATON
