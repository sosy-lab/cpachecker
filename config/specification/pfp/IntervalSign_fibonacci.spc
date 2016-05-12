OBSERVER AUTOMATON AlwaysIntervalAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  !CHECK(ValidVars,"flag") || (CHECK(IntervalAnalysis,"5<=flag")  && CHECK(SignAnalysis,"flag<=PLUS"))-> GOTO Init;
  TRUE -> ASSUME {flag<5} ERROR;

END AUTOMATON
