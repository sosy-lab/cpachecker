OBSERVER AUTOMATON AssertionAutomaton
// This automaton detects assertions that may fail
// (i.e., a function call to __assert_fail).

INITIAL STATE Init;

STATE USEFIRST Init :
   // matches if assert_fail is called with any number of parameters
   MATCH {__assert_fail($?)}
   -> ERROR;


   MATCH {assert($?)} && !CHECK(location, "functionName==assert")
   -> PRINT "WARNING: Function assert() without body detected. Please run the C preprocessor on this file to enable assertion checking."
      GOTO Init;


END AUTOMATON
