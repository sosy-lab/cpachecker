OBSERVER AUTOMATON AlwaysIntervalAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH{pos=0} && CHECK(IntervalAnalysis, "0<=main::i<=7") -> GOTO Init;
  MATCH{pos=0} -> ASSUME {i>7} ERROR;
  MATCH{pos=1} && CHECK(IntervalAnalysis, "0<=main::i<=2") -> GOTO Init;
  MATCH{pos=1} -> ASSUME {i>2} ERROR; 
END AUTOMATON
