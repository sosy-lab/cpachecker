// This automaton contains the specification of the
// Competition on Software Verification.
// It checks only for "ERROR" labels,
// and also implements some functions which usually lead to a program abort.
CONTROL AUTOMATON SMGCPALeaks

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK(SMGCPA, "has-invalid-writes") -> ERROR;
  CHECK(SMGCPA, "has-invalid-reads") -> ERROR;
  CHECK(SMGCPA, "has-invalid-frees") -> ERROR;
  CHECK(SMGCPA, "has-leaks") -> ERROR;

END AUTOMATON
