OBSERVER AUTOMATON AlwaysIntervalAutomaton

INITIAL STATE Init;

// require bitprecise analysis here
STATE USEFIRST Init :
  !CHECK(ValidVars,"flag") || CHECK(IntervalAnalysis,"0<=flag<=3") -> GOTO Init;
  TRUE -> ASSUME {flag>3 | flag<0} ERROR;
  
END AUTOMATON
