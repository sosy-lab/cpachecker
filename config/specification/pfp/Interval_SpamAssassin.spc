OBSERVER AUTOMATON AlwaysIntervalAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  !CHECK(ValidVars,"flag1") || !CHECK(ValidVars,"flag2") || (CHECK(IntervalAnalysis,"0<=flag1<=5") && CHECK(IntervalAnalysis,"0<=flag2<=10"))-> GOTO Init;
  !CHECK(IntervalAnalysis,"0<=flag1<=5") -> ASSUME {flag1>5} ERROR;
  TRUE -> ASSUME {flag2>10} ERROR;
  
END AUTOMATON
