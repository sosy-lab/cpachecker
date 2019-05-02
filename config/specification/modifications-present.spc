CONTROL AUTOMATON ModificationsAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK(ModificationsCPA, "is_modified") -> ERROR("Modification found in $location");

END AUTOMATON
