// An automaton which tells CPAchecker about several statements after which execution ends.
CONTROL AUTOMATON TerminatingFunctions

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH {abort($?)} || MATCH {exit($?)} || MATCH {__assert_fail($?)} -> BREAK;

END AUTOMATON
