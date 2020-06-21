
OBSERVER AUTOMATON ErrorLabelAutomaton
// This automaton detects error locations that are specified by the label "ERROR"

INITIAL STATE Init;

STATE USEFIRST Init :
// this transition matches if the label of the successor CFA location is "error"
MATCH LABEL "process_1:critical;process_2:critical;process_3:critical;process_4:critical;process_5:critical;process_6:critical;process_7:critical;process_8:critical" -> ERROR("error label reached");

END AUTOMATON