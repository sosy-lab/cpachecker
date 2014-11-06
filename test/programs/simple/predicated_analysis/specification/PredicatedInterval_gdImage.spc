OBSERVER AUTOMATON AlwaysIntervalAutomaton

INITIAL STATE Init;

// require bitprecise analysis here
STATE USEFIRST Init :
  !CHECK(ValidVars,"flag") || CHECK(IntervalAnalysis,"0<=flag<=2") -> GOTO Init;
  TRUE -> ASSUME {flag<0 | flag>2} ERROR;
  
END AUTOMATON
