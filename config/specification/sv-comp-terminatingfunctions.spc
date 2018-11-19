// An automaton which tells CPAchecker about several functions which do not return.
CONTROL AUTOMATON svcompTerminatingFunctions

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH {abort($?)} || MATCH {exit($?)} || MATCH {__assert_fail($?)} || MATCH {__VERIFIER_error($?)}

  -> STOP;

END AUTOMATON
