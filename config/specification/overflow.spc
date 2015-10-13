// This automaton contains the specification of the
// category Overflows of the
// Competition on Software Verification.
CONTROL AUTOMATON Overflows

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK(InvariantsCPA, "overflow") -> ERROR("overflow: integer overflow in $location");

END AUTOMATON
