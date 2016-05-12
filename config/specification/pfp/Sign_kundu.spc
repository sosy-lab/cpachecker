OBSERVER AUTOMATON AlwaysIntervalAutomaton

INITIAL STATE Init;

// require bitprecise analysis here
STATE USEFIRST Init :
  !CHECK(ValidVars,"num") || CHECK(SignAnalysis,"num<=PLUS0") -> GOTO Init;
  TRUE -> ASSUME {num<0} ERROR;
  
END AUTOMATON
