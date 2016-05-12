OBSERVER AUTOMATON AlwaysSignAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  !CHECK(ValidVars,"main::y") || CHECK(SignAnalysis,"main::y<=PLUS") -> GOTO Init;
  TRUE -> ASSUME {y<=0} ERROR;
  
END AUTOMATON
