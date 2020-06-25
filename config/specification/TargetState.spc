OBSERVER AUTOMATON TargetStateAutomaton
// This automaton detects error locations that are specified by the label "ERROR"

INITIAL STATE Init;

STATE USEFIRST Init :
   CHECK(IS_TARGET_STATE) -> ERROR;

END AUTOMATON
