OBSERVER AUTOMATON AlwaysIntervalAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH{t2_st = 1;} && !CHECK(SignAnalysis,"t2_pc<=ZERO") -> ASSUME {t2_pc!=0} ERROR;
  MATCH{m_st = 1;} -> GOTO second;
  
STATE USEFIRST second:

END AUTOMATON
