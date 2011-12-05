OBSERVER AUTOMATON ErrorLabelAutomaton
// This automaton detects error locations that are specified by the label "ERROR"

INITIAL STATE Init;

STATE USEFIRST Init :
   MATCH { __MONITOR_START_TRANSITION\(\) } -> GOTO Monitor;
   MATCH LABEL [[Ee\][Rr\][Rr\][Oo\][Rr\]] -> ERROR;

STATE USEFIRST Monitor :
  MATCH  { __MONITOR_END_TRANSITION\(\) } -> GOTO Init; 
  MATCH LABEL [[Ee\][Rr\][Rr\][Oo\][Rr\]] -> ERROR;

END AUTOMATON
