OBSERVER AUTOMATON AssertionAutomaton
// This automaton detects assertions that may fail
// (i.e., a function call to __assert_fail).

INITIAL STATE Init;

STATE USEFIRST Init :
   // matches if assert_fail or assert_funct is called with any number of parameters
   MATCH {__assert_fail($1, $2, $3, $4)}
    -> ERROR("assertion in $location: Condition $1 failed in $2, line $3");

   MATCH {__assert_fail($?)} || MATCH {__assert_func($?)}
   -> ERROR("assertion in $location");


   MATCH {assert($?)} && !CHECK(location, "functionName==assert")
   -> PRINT "WARNING: Function assert() without body detected. Please run the C preprocessor on this file to enable assertion checking."
      GOTO Init;

   MATCH {__VERIFIER_error($?)} && !CHECK(location, "functionName==__VERIFIER_error")
   -> PRINT "WARNING: Function __VERIFIER_error() is ignored by this specification. If you want to check for reachability of __VERIFIER_error, pass '-spec sv-comp-reachability' as parameter."
      GOTO Init;

  MATCH {reach_error($?)}
   -> PRINT "WARNING: Function reach_error() is ignored by this specification. If you want to check for reachability of reach_error, pass '-spec sv-comp-reachability' as parameter."
      GOTO Init;

END AUTOMATON
