// This automaton contains the specification of the category MemorySafety
// of the Competition on Software Verification.
// It queries the SMGCPA for information about invalid freeing of allocations.

CONTROL AUTOMATON SMGCPAFREE

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK(SMGCPA, "has-invalid-frees") -> ERROR("valid-free: invalid pointer free in $location");

END AUTOMATON
