OBSERVER AUTOMATON AlwaysValueAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  !CHECK(ValidVars,"main::flag") || CHECK(ValueAnalysis,"main::flag==1") -> GOTO Init;
  TRUE -> ASSUME {flag!=1} ERROR;
  
END AUTOMATON
