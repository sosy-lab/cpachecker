OBSERVER AUTOMATON AlwaysIntervalAutomaton

INITIAL STATE Init;

// require bitprecise analysis here
STATE USEFIRST Init :
  MATCH{t1_st = 1;} && !CHECK(ValueAnalysis,"t1_pc==0") -> ASSUME {t1_pc!=0} ERROR;
    
END AUTOMATON
