// This automaton is used to stop the exploration when reaching 
//		an error state (the cause of a possible violation).
//
// It is used in the context of partitioned state spaces 
//		in combination with a backwards analysis.
//		(in order to explore only the paths that do not get 
//		in touch with a state that violates the specification) 

CONTROL AUTOMATON NoSuccessorOnError

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH {__VERIFIER_error($?)} -> STOP;
  MATCH {__assert_fail($?)} || MATCH {abort($?)} || MATCH {exit($?)} -> STOP;
  MATCH LABEL "LDV_ERROR" -> STOP;
  MATCH LABEL [[Ee\][Rr\][Rr\][Oo\][Rr\]] -> STOP;
  TRUE -> GOTO Init;

END AUTOMATON
