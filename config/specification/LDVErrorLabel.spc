// This automaton contains the specification for
// LDV driver verification framework.
// It checks only for labels named "LDV_ERROR".

CONTROL AUTOMATON LDVErrorLabel

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH LABEL "LDV_ERROR" -> ERROR;
END AUTOMATON
