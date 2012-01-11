#include ErrorLabel.spc

CONTROL AUTOMATON ExitFunction

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH {__assert_fail($?)} || MATCH {abort($?)} || MATCH {exit($?)}

  -> STOP;

END AUTOMATON
