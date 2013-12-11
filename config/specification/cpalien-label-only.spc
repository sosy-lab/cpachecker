// This automaton contains the specification of the
// Competition on Software Verification.
// It checks only for "ERROR" labels,
// and also implements some functions which usually lead to a program abort.
CONTROL AUTOMATON CPAlienLeaks

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH LABEL [ERROR] -> ERROR;
  CHECK(CPAlien, "has-invalid-writes") -> STOP;
  CHECK(CPAlien, "has-invalid-reads") -> STOP;
  CHECK(CPAlien, "has-invalid-frees") -> STOP;
  CHECK(CPAlien, "has-leaks") -> STOP;

END AUTOMATON
