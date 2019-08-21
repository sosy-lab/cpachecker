OBSERVER AUTOMATON MainEntryAutomaton
// This automaton detects the entry point of the main function

INITIAL STATE Init;

STATE USEFIRST Init :
   MATCH ENTRY -> ERROR("main entry reached");

END AUTOMATON
