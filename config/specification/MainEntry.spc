OBSERVER AUTOMATON MainEntryAutomaton
// This automaton detects the entry point of the main function

INITIAL STATE Body;

STATE USEFIRST Body :
   MATCH [.*\\s+main\\s*\\(.*\\)] -> GOTO MainEntry;

STATE USEFIRST MainEntry :
   MATCH ENTRY -> ERROR("main entry reached");

END AUTOMATON
