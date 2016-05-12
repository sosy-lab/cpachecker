OBSERVER AUTOMATON AlwaysIntervalAutomaton

INITIAL STATE Init;

// require bitprecise analysis here
STATE USEFIRST Init :
  MATCH {c_read_rsp_ev = 2;} -> GOTO next;
  
STATE USEFIRST next:
  MATCH {c_read_rsp_ev = 2;} && !CHECK(SignAnalysis,"c_m_lock<=PLUS") -> ASSUME {c_m_lock==0} ERROR;
    
END AUTOMATON
