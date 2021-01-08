CONTROL AUTOMATON StopAtLeavesAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK(StopAtLeavesState, "at leaf") -> ERROR;
  TRUE -> GOTO Init;

END AUTOMATON
