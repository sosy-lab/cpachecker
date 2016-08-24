OBSERVER AUTOMATON AlwaysIntervalAutomaton

INITIAL STATE Init;

// require bitprecise analysis here
STATE USEFIRST Init :
  !CHECK(ValidVars,"flag") || CHECK(IntervalAnalysis,"0<=flag<=10") -> GOTO Init;
  TRUE -> ASSUME {flag<0 | flag>10} ERROR;
  
END AUTOMATON
