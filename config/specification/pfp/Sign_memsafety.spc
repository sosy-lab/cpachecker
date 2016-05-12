OBSERVER AUTOMATON AlwaysIntervalAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK(ValidVars,"flag") && !CHECK(SignAnalysis,"flag<=PLUSMINUS") -> ASSUME {flag==0} ERROR;
  
END AUTOMATON