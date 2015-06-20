OBSERVER AUTOMATON MonitorM_EAssignmentAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH {triangle = 1;} -> GOTO STATE1;
  MATCH {equilateral =1;}  -> GOTO STATE2;
 
STATE USEFIRST STATE1:
  MATCH {equilateral =1;} -> GOTO STATE3;
 
STATE USEFIRST STATE2:
  MATCH {triangle = 1;} -> GOTO STATE3; 
  
STATE USEFIRST STATE3:
  CHECK(SignAnalysis,"main::isoscles<=PLUS") && CHECK(SignAnalysis,"main::scalene<=ZERO")-> GOTO STATE3;
  CHECK(SignAnalysis, "main::scalene<=ZERO") -> ASSUME{isoscles<=0} ERROR;
  TRUE -> ASSUME{scalene!=0} ERROR;
  
END AUTOMATON
