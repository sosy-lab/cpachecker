OBSERVER AUTOMATON AlwaysValueAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH{cond = __VERIFIER_nondet_int();} -> GOTO STATE1;
  
STATE USEFIRST STATE1 :
  MATCH{cond = __VERIFIER_nondet_int();} 
  && CHECK(SignAnalysis,"main::lk1<=ZERO") 
  && CHECK(SignAnalysis,"main::lk2<=ZERO") 
  && CHECK(SignAnalysis,"main::lk3<=ZERO") 
  && CHECK(SignAnalysis,"main::lk4<=ZERO")
  && CHECK(SignAnalysis,"main::lk5<=ZERO")
  && CHECK(SignAnalysis,"main::lk6<=ZERO")
  && CHECK(SignAnalysis,"main::lk7<=ZERO")-> GOTO STATE1;
  MATCH{cond = __VERIFIER_nondet_int();} && !CHECK(SignAnalysis,"main::lk1<=ZERO") -> ASSUME{lk1==1} ERROR;
  MATCH{cond = __VERIFIER_nondet_int();} && !CHECK(SignAnalysis,"main::lk2<=ZERO") -> ASSUME{lk2==1} ERROR;
  MATCH{cond = __VERIFIER_nondet_int();} && !CHECK(SignAnalysis,"main::lk3<=ZERO") -> ASSUME{lk3==1} ERROR;
  MATCH{cond = __VERIFIER_nondet_int();} && !CHECK(SignAnalysis,"main::lk4<=ZERO") -> ASSUME{lk4==1} ERROR;
  MATCH{cond = __VERIFIER_nondet_int();} && !CHECK(SignAnalysis,"main::lk5<=ZERO") -> ASSUME{lk5==1} ERROR;
  MATCH{cond = __VERIFIER_nondet_int();} && !CHECK(SignAnalysis,"main::lk6<=ZERO") -> ASSUME{lk6==1} ERROR;
  MATCH{cond = __VERIFIER_nondet_int();} && !CHECK(SignAnalysis,"main::lk7<=ZERO") -> ASSUME{lk7==1} ERROR;
END AUTOMATON
