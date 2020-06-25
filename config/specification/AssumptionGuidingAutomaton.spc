CONTROL AUTOMATON AssumptionGuidingAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK(AutomatonAnalysis_AssumptionAutomaton, "state == __TRUE") -> STOP;
  TRUE -> GOTO Init;

END AUTOMATON
