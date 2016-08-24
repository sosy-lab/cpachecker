// This automaton waits for the end of the program and prints the last statement.
// It is used to test the function of the MATCH EXIT keywords.


OBSERVER AUTOMATON PrintLastStatementAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH EXIT -> PRINT "Last statement is \"$rawstatement\"" GOTO Init;

END AUTOMATON