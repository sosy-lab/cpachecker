OBSERVER AUTOMATON ErrorLabelAutomaton
// This automaton detects error locations that are specified by the label "ERROR"

INITIAL STATE Init;

STATE USEFIRST Init :
   // this transition matches if the label of the successor CFA location is "error_32"
MATCH LABEL [error_32] -> ERROR("error label in $location");

END AUTOMATON

