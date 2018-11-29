// This automaton contains the specification of the category MemorySafety
// of the Competition on Software Verification.
// It queries the SMGCPA for information about memory leaks,
// i.e., forgotten allocations after leaving a scope.

CONTROL AUTOMATON SMGCPAMEMTRACK

INITIAL STATE Init;

STATE USEFIRST Init :

  // taken from 'sv-comp-terminatingfunctions.spc' without stopping at '__VERIFIER_error()'
  MATCH {abort($?)} || MATCH {exit($?)} || MATCH {__assert_fail($?)} -> STOP;

  // property MemCleanup depends on reachable heap-object at program exit
  (MATCH EXIT || MATCH {__VERIFIER_error($?)}) && CHECK(SMGCPA, "has-heap-objects") -> ERROR("valid-memcleanup");

END AUTOMATON
