// This automaton contains the specification of the
// category MemorySafety of the
// Competition on Software Verification.
CONTROL AUTOMATON SMGCPADEREF

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK(SMGCPA, "has-invalid-writes") -> ERROR("valid-deref: invalid pointer dereference in $location");
  CHECK(SMGCPA, "has-invalid-reads") -> ERROR("valid-deref: invalid pointer dereference in $location");

END AUTOMATON
