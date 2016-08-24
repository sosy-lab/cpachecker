// This automaton contains the specification of the
// Competition on Software Verification.
// It checks only for "ERROR" labels,
// and also implements some functions which usually lead to a program abort.
CONTROL AUTOMATON SMGCPALeaks

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH LABEL [ERROR] -> ERROR("error label in $location");
  CHECK(SMGCPA, "has-invalid-writes") -> STOP;
  CHECK(SMGCPA, "has-invalid-reads") -> STOP;
  CHECK(SMGCPA, "has-invalid-frees") -> STOP;
  CHECK(SMGCPA, "has-leaks") -> STOP;

END AUTOMATON
