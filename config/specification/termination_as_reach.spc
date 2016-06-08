OBSERVER AUTOMATON NonTerminationLabelAutomaton
// Specification used by the termination algorithm to detect potential non-termination.
// This automaton detects error locations that are specified by the label
// "__CPACHECKER_NON_TERMINATION"

INITIAL STATE Init;

STATE USEFIRST Init :
   // this transition matches if the label of the successor CFA location is
   // "__CPACHECKER_NON_TERMINATION"
   MATCH LABEL __CPACHECKER_NON_TERMINATION -> ERROR("non-termination label in $location");

END AUTOMATON
