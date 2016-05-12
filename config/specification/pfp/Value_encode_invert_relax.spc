OBSERVER AUTOMATON AlwaysValueAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  !CHECK(ValidVars,"flag") || CHECK(ValueAnalysis,"flag==0") || CHECK(ValueAnalysis,"flag==1") || CHECK(ValueAnalysis,"flag==2") || CHECK(ValueAnalysis,"flag==3") || CHECK(ValueAnalysis,"flag==4") || CHECK(ValueAnalysis,"flag==5")-> GOTO Init;
  TRUE -> ASSUME {flag<0|flag>5} ERROR;
  
END AUTOMATON
