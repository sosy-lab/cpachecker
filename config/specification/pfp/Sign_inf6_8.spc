OBSERVER AUTOMATON AlwaysSignAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  !CHECK(ValidVars,"flag") || CHECK(SignAnalysis,"flag<=ZERO") -> GOTO Init;
  TRUE -> ASSUME{flag>0} ERROR;
  
END AUTOMATON
