CONTROL AUTOMATON SplitterGuidingAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK(SplitterCPA, "isSingle") -> GOTO NextStop;
  TRUE -> GOTO Init;
  
STATE USEFIRST NextStop :
  TRUE -> STOP;  

END AUTOMATON

