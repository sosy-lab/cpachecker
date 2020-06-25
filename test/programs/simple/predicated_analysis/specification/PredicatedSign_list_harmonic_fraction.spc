OBSERVER AUTOMATON AlwaysSignAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  !CHECK(ValidVars,"flag") || CHECK(SignAnalysis,"flag<=PLUSMINUS") -> GOTO Init;
  TRUE -> ASSUME {flag==0} ERROR;
  
END AUTOMATON
