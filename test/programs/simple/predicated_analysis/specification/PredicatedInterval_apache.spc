OBSERVER AUTOMATON AlwaysIntervalAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  !CHECK(ValidVars,"flag") || CHECK(IntervalAnalysis,"0<=flag<=3") -> GOTO Init;
  TRUE -> ASSUME {flag<0 || flag>3} ERROR;
  
END AUTOMATON
