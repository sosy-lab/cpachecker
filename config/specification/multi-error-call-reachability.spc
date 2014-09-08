// It checks only for calls to the __VERIFIER_error() functions
// and also implements some functions which usually lead to a program abort.
// It supports detecting multiple violations within one run (USEALL)
CONTROL AUTOMATON MultiErrors

INITIAL STATE Init;

STATE USEALL Init :
  MATCH {__VERIFIER_error($?)} -> ERROR("$rawstatement called in line $line");
  MATCH {__assert_fail($?)} || MATCH {abort($?)} || MATCH {exit($?)} -> STOP;
  TRUE -> GOTO Init;

END AUTOMATON
