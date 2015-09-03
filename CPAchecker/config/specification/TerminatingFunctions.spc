// An automaton which tells CPAchecker about several functions which do not return.
CONTROL AUTOMATON TerminatingFunctions

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH {abort($?)} || MATCH {exit($?)}

  -> STOP;

END AUTOMATON
