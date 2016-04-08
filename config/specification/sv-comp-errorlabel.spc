// This automaton contains the specification of the
// Competition on Software Verification.
// It checks only for "ERROR" labels,
// and also implements some functions which usually lead to a program abort.
CONTROL AUTOMATON SVCOMP

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH LABEL [ERROR] -> ERROR("error label in $location");
  MATCH {__assert_fail($?)} || MATCH {abort($?)} || MATCH {exit($?)} -> STOP;

END AUTOMATON
