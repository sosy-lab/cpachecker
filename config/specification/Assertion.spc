OBSERVER AUTOMATON AssertionAutomaton
// This automaton detects assertions that may fail
// (i.e., a function call to __assert_fail).

INITIAL STATE Init;

STATE USEFIRST Init :
   // matches if assert_fail is called with any number of parameters
   MATCH {__assert_fail($?)}

  -> ERROR;

   MATCH {assert($?)}
  -> PRINT "WARNING: Function assert() detected. You need to run the C preprocessor or CIL on this file if you want to check the assertions!"
     GOTO Init;


END AUTOMATON
