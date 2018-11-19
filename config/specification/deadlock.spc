// This automaton contains a specification to check for deadlocks in concurrent programs.
CONTROL AUTOMATON Deadlock

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK(ThreadingCPA, "deadlock") -> ERROR("no-deadlock: deadlock detected in $location");

END AUTOMATON
