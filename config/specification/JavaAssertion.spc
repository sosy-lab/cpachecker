OBSERVER AUTOMATON AssertionAutomaton
// This automaton detects assertions that may fail.

INITIAL STATE Init;

STATE USEFIRST Init :
   // matches special edge added by CPAchecker
   MATCH ASSERT -> ERROR;

END AUTOMATON
