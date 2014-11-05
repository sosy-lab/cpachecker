OBSERVER AUTOMATON AlwaysIntervalAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  !CHECK(ValidVars,"main::x") || CHECK(IntervalAnalysis,"0<=main::x") -> GOTO Init;
  TRUE -> ASSUME {x<0} ERROR;
  
END AUTOMATON
