CONTROL AUTOMATON EvalOnlyOnePathAutomaton
// This automaton detects error locations that are specified by the label "ERROR"

INITIAL STATE Init;

STATE USEFIRST Init :
   // this transition matches if the label of the successor CFA location is "error"
   MATCH EXIT -> STOP;
   TRUE -> GOTO Init;

END AUTOMATON
