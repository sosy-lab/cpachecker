OBSERVER AUTOMATON AlwaysValueAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  !CHECK(ValidVars,"flag") || CHECK(ValueAnalysis,"flag==0") -> GOTO Init;
  TRUE -> ASSUME {flag>0} ERROR;
  
END AUTOMATON
