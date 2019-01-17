// Specification-Dateien im "config"-Ordner sind aus Sicherheitsgründen in der VerifierCloud
// verboten. Daher wird als Workaround auf ein anderes Verzeichnis verwiesen:

OBSERVER AUTOMATON AssertionAutomaton
// This automaton detects assertions that may fail.

INITIAL STATE Init;

STATE USEFIRST Init :
   // matches special edge added by CPAchecker
   MATCH ASSERT -> ERROR("assertion in $location");

END AUTOMATON
