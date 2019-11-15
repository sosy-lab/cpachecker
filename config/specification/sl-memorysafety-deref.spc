// This automaton contains the specification of the category MemorySafety
// of the Competition on Software Verification.
// It queries the SLCPA for information about invalid derefencing of pointers.

CONTROL AUTOMATON SLCPADEREF

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK(SLCPA, "has-invalid-derefs") -> ERROR("valid-deref: invalid pointer dereference in $location");

END AUTOMATON
