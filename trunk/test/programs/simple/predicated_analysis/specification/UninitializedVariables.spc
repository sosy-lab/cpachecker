OBSERVER AUTOMATON UninitializedVariablesObservingAutomaton
/* Queries the UninitializedVariablesCPA for errors and prints them.
 * Does abort if an error is found.
 */

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK(uninitVars,"UNINITIALIZED_RETURN_VALUE") ->  ERROR;
  CHECK(uninitVars,"UNINITIALIZED_VARIABLE_USED") -> ERROR;

END AUTOMATON