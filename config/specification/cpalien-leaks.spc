// This automaton contains the specification of the
// category MemorySafety of the
// Competition on Software Verification.
CONTROL AUTOMATON SMGCPALeaks

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK(SMGCPA, "has-invalid-writes") -> ERROR;
  CHECK(SMGCPA, "has-invalid-reads") -> ERROR;
  CHECK(SMGCPA, "has-invalid-frees") -> ERROR;
  CHECK(SMGCPA, "has-leaks") -> ERROR;

END AUTOMATON
