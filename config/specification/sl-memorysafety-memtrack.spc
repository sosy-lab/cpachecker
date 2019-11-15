// This automaton contains the specification of the category MemorySafety
// of the Competition on Software Verification.
// It queries the SLCPA for information about memory leaks,
// i.e., forgotten allocations after leaving a scope.

CONTROL AUTOMATON SLCPAMEMTRACK

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK(SLCPA, "has-leaks") -> ERROR("valid-memtrack");

END AUTOMATON
