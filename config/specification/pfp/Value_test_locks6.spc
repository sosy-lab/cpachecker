OBSERVER AUTOMATON AlwaysValueAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH{cond = __VERIFIER_nondet_int();} -> GOTO STATE1;
  
STATE USEFIRST STATE1 :
  MATCH{cond = __VERIFIER_nondet_int();} 
  && CHECK(ValueAnalysis,"main::lk1==0") 
  && CHECK(ValueAnalysis,"main::lk2==0") 
  && CHECK(ValueAnalysis,"main::lk3==0") 
  && CHECK(ValueAnalysis,"main::lk4==0")
  && CHECK(ValueAnalysis,"main::lk5==0")
  && CHECK(ValueAnalysis,"main::lk6==0")-> GOTO STATE1;
  MATCH{cond = __VERIFIER_nondet_int();} && !CHECK(ValueAnalysis,"main::lk1==0") -> ASSUME{lk1==1} ERROR;
  MATCH{cond = __VERIFIER_nondet_int();} && !CHECK(ValueAnalysis,"main::lk2==0") -> ASSUME{lk2==1} ERROR;
  MATCH{cond = __VERIFIER_nondet_int();} && !CHECK(ValueAnalysis,"main::lk3==0") -> ASSUME{lk3==1} ERROR;
  MATCH{cond = __VERIFIER_nondet_int();} && !CHECK(ValueAnalysis,"main::lk4==0") -> ASSUME{lk4==1} ERROR;
  MATCH{cond = __VERIFIER_nondet_int();} && !CHECK(ValueAnalysis,"main::lk5==0") -> ASSUME{lk5==1} ERROR;
  MATCH{cond = __VERIFIER_nondet_int();} && !CHECK(ValueAnalysis,"main::lk6==0") -> ASSUME{lk6==1} ERROR;
END AUTOMATON
