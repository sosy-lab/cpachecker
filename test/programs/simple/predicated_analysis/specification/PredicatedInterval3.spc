OBSERVER AUTOMATON AlwaysIntervalAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  !CHECK(ValidVars,"flag") || CHECK(IntervalAnalysis,"0<=flag<=10") -> GOTO Init;
  TRUE -> ERROR;
  
END AUTOMATON
