// This automaton contains the specification of the category MemorySafety
// of the Competition on Software Verification.
// It queries the SMGCPA for information about memory leaks,
// i.e., forgotten allocations after leaving a scope.

CONTROL AUTOMATON SMGCPAMEMTRACK

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK(SMGCPA, "has-leaks") -> ERROR("valid-memtrack");

END AUTOMATON
