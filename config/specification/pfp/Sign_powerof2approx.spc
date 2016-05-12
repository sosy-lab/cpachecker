OBSERVER AUTOMATON AlwaysIntervalAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  !CHECK(ValidVars,"flag") || CHECK(SignAnalysis,"flag<=PLUS") -> GOTO Init;
  TRUE -> ASSUME {flag<=0} ERROR;
  
STATE USEFIRST second:

END AUTOMATON
