
OBSERVER AUTOMATON ErrorLabelAutomaton
// This automaton detects error locations that are specified by the label "ERROR"

INITIAL STATE Init;

STATE USEFIRST Init :
// this transition matches if the label of the successor CFA location is "error"
MATCH LABEL "process_1:critical;process_2:critical;process_3:critical;process_4:critical" -> ERROR("error label reached");

END AUTOMATON