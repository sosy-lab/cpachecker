OBSERVER AUTOMATON AlwaysIntervalAutomaton

INITIAL STATE Init;

// require bitprecise analysis here
STATE USEFIRST Init :
  !CHECK(ValidVars,"flag") || CHECK(IntervalAnalysis,"0<=flag<=5") -> GOTO Init;
  CHECK(IntervalAnalysis, "0<=flag") -> ASSUME {flag>5} ERROR;
  TRUE -> ASSUME {flag<0} ERROR;

END AUTOMATON
