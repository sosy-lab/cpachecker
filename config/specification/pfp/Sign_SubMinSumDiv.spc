OBSERVER AUTOMATON SignExample

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH {z = 0;} -> GOTO STATE1;
  
STATE USEFIRST STATE1:
  !CHECK(ValidVars,"main::z") || CHECK(SignAnalysis,"main::z<=PLUS0") -> GOTO STATE1;
  TRUE -> ASSUME {z<0} ERROR;  
  
END AUTOMATON