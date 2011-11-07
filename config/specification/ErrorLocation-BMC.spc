CONTROL AUTOMATON ErrorLocationAutomaton
// This automaton detects error locations that are specified either
// by the label "ERROR" or by an assertion (function call to __assert_fail).

INITIAL STATE Init;

STATE USEFIRST Init :
       // this transition matches if the label of the successor CFA location starts with "error"
     MATCH LABEL [[Ee\][Rr\][Rr\][Oo\][Rr\].*]
       // matches if __assert_fail is called with any number of parameters
  || MATCH {__assert_fail($?)}

  -> GOTO Error;

TARGET STATE Error:
  TRUE -> STOP;

END AUTOMATON
