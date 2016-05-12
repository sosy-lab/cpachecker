OBSERVER AUTOMATON AlwaysIntervalAutomaton

INITIAL STATE Init;

// require bitprecise analysis here
STATE USEFIRST Init :
  !CHECK(ValidVars,"flag") || CHECK(IntervalAnalysis,"5<=flag") -> GOTO Init;
  TRUE -> ASSUME {flag<5} ERROR;
  
END AUTOMATON
