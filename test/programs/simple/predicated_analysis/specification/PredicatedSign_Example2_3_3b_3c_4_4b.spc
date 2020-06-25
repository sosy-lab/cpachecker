OBSERVER AUTOMATON AlwaysSignAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  !CHECK(ValidVars,"main::x") || CHECK(SignAnalysis,"main::x<=PLUS0") -> GOTO Init;
  TRUE -> ASSUME {x<0} ERROR;
  
END AUTOMATON
