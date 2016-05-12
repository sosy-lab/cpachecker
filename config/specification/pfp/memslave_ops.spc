OBSERVER AUTOMATON LockAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH  {c_m_lock = 0;}  -> GOTO STATE1;

 
STATE USEFIRST STATE1:
  MATCH  {kernel_st = 0;} ->  GOTO STATE3; 
  
STATE USEFIRST STATE2:
  MATCH  {c_m_lock = 0;}  -> GOTO STATE3;
  MATCH  {c_m_lock = 1;}  -> ERROR;
  
STATE USEFIRST STATE3 :
  MATCH  {c_m_lock = 0;}  -> ERROR;
  MATCH  {c_m_lock = 1;}  -> GOTO STATE2;
  
END AUTOMATON
