// This automaton contains the specification of the
// Competition on Software Verification.
// It checks only for calls to the __VERIFIER_error() functions
// and also implements some functions which usually lead to a program abort.
CONTROL AUTOMATON SVCOMP

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH {__VERIFIER_error($?)} -> ERROR("$rawstatement called in $location");
  MATCH {__assert_fail($?)} || MATCH {abort($?)} || MATCH {exit($?)} -> STOP;

END AUTOMATON
