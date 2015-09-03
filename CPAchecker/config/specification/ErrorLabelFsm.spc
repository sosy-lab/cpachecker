OBSERVER AUTOMATON ErrorLabelAutomaton
// This automaton detects error locations that are specified by the label "ERROR_i"

INITIAL STATE Init;

STATE USEFIRST Init :
   MATCH LABEL [[Ee\][Rr\][Rr\][Oo\][Rr\]_[0-9\]*] -> PRINT "VIOLATION at line " PRINT EVAL(location,"lineno") GOTO Init;
   MATCH LABEL [globalError] -> PRINT "VIOLATION at line " PRINT EVAL(location,"lineno") GOTO Init;

END AUTOMATON
