OBSERVER AUTOMATON AlwaysIntervalAutomaton

INITIAL STATE Init;

// require bitprecise analysis here
STATE USEFIRST Init :
  !CHECK(ValidVars,"flag") || (CHECK(SignAnalysis,"flag<=PLUS0") && CHECK(IntervalAnalysis,"flag<=20")) -> GOTO Init;
  CHECK(SignAnalysis,"flag<=PLUS0")-> ASSUME {flag>20} ERROR;  
  TRUE -> ASSUME {flag<0} ERROR;
  
END AUTOMATON