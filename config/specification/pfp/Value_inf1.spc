OBSERVER AUTOMATON AlwaysIntervalAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  !CHECK(ValidVars,"flag") || CHECK(ValueAnalysis,"flag==1") -> GOTO Init;
  TRUE -> ASSUME {flag!=1} ERROR;
  
STATE USEFIRST second:

END AUTOMATON
