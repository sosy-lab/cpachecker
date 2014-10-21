OBSERVER AUTOMATON AlwaysSignAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  !CHECK(ValidVars,"main::x") || CHECK(IntervalAnalysis,"main::x<=PLUSMINUS") -> GOTO Init;
  TRUE -> ERROR;
  
END AUTOMATON
