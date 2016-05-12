OBSERVER AUTOMATON AlwaysValueAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  !CHECK(ValidVars,"main::flag") || CHECK(SignAnalysis,"main::flag<=PLUS") -> GOTO Init;
  TRUE -> ASSUME {flag<=0} ERROR;
  
END AUTOMATON
