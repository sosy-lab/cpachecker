// This automaton checks for any possible null-pointer dereference in the source code.
// To be able to use this, the following option needs to be set:
// cfa.checkNullPointers = true
CONTROL AUTOMATON NULLDEREF

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH "null-deref" -> ERROR;

END AUTOMATON

// Recognize functions such as exit() and abort() which do not return.
#include TerminatingFunctions.spc
