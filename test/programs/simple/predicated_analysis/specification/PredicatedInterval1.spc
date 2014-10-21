OBSERVER AUTOMATON AlwaysIntervalAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  !CHECK(ValidVars,"main::x") || CHECK(IntervalAnalysis,"0<=main::x") -> GOTO Init;
  TRUE -> ERROR;
  
END AUTOMATON
