OBSERVER AUTOMATON AlwaysSignAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH { x=0; } -> GOTO LessEqual0;
  TRUE -> GOTO Init;
  
STATE USEFIRST LessEqual0 :
  MATCH { x=1; } -> GOTO Greater0;
  !CHECK(ValidVars,"main::x") || CHECK(SignAnalysis,"main::x<=MINUS0") -> GOTO LessEqual0;
  TRUE -> ASSUME {x>0} ERROR;

STATE USEFIRST Greater0 :
  !CHECK(ValidVars,"main::x") || CHECK(SignAnalysis,"main::x<=PLUS") -> GOTO Greater0;
  TRUE -> ASSUME {x<1} ERROR;
  
END AUTOMATON
