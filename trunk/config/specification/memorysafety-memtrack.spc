// This automaton contains the specification of the
// category MemorySafety of the
// Competition on Software Verification.
CONTROL AUTOMATON SMGCPAMEMTRACK

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK(SMGCPA, "has-leaks") -> ERROR("valid-memtrack");

END AUTOMATON
