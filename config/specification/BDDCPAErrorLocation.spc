OBSERVER AUTOMATON ErrorLocationAutomaton
// This automaton detects error locations that are specified either
// by the label "ERROR"

INITIAL STATE Init;

STATE USEFIRST Init :
       // this transition matches if the label of the successor CFA location starts with "error"
     MATCH LABEL [[Ee\][Rr\][Rr\][Oo\][Rr\].*]

  -> PRINT "Product violating in line $line: " PRINT EVAL(BDDCPA,"VALUES") ERROR;

  MATCH EXIT -> PRINT "Valid Product: " PRINT EVAL(BDDCPA,"VALUES") GOTO Init;

END AUTOMATON
