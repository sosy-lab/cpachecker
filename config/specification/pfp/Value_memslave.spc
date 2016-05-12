OBSERVER AUTOMATON MonitorAssertAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH {kernel_st = 0;} -> GOTO STATE1;
 
STATE USEFIRST STATE1:
  MATCH {c_read_rsp_ev = 2;} && CHECK(ValueAnalysis,"c_m_lock==1")-> GOTO STATE1;
  MATCH {c_read_rsp_ev = 2;} -> ASSUME{c_m_lock==0} ERROR;
 
  
END AUTOMATON
