// This automaton contains the specification of the category MemorySafety
// of the Competition on Software Verification.
// It queries the SLCPA for information about invalid freeing of allocations.

CONTROL AUTOMATON SLCPAFREE

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK(SLCPA, "has-invalid-frees") -> ERROR("valid-free: invalid pointer free in $location");

END AUTOMATON
