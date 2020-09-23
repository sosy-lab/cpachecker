// This automaton contains the specification of the __VERIFIER_postpone() function used for user guided state space exploration.
// For calls to the __VERIFIER_postpone() functions it goes to Postpone states.

CONTROL AUTOMATON UserGuidedPostpone

LOCAL int traverse = 1;

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH {__VERIFIER_postpone($?)} -> DO traverse = 0 GOTO Postpone;

STATE USEFIRST Postpone :
  TRUE -> GOTO Postpone;

END AUTOMATON
