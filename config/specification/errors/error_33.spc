OBSERVER AUTOMATON ErrorLabelAutomaton
// This automaton detects error locations that are specified by the label "ERROR"

INITIAL STATE Init;

STATE USEFIRST Init :
   // this transition matches if the label of the successor CFA location is "error_33"
MATCH LABEL [error_33] -> ERROR("error label in $location");

END AUTOMATON

