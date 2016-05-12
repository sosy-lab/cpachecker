OBSERVER AUTOMATON Monitor3Automaton

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH  "t1_started();"  -> GOTO Start100;
  MATCH  "t2_started();"  -> GOTO Start010;
  MATCH  "t3_started();"  -> GOTO Start001;

STATE USEFIRST Start100 :
  MATCH  "t1_started();"  -> ERROR;
  MATCH  "t2_started();"  -> GOTO Start110;
  MATCH  "t3_started();"  -> GOTO Start101;

STATE USEFIRST Start010 :
  MATCH  "t1_started();"  -> GOTO Start110;
  MATCH  "t2_started();"  -> ERROR;
  MATCH  "t3_started();"  -> GOTO Start011;

STATE USEFIRST Start001 :
  MATCH  "t1_started();"  -> GOTO Start101;
  MATCH  "t2_started();"  -> GOTO Start011;
  MATCH  "t3_started();"  -> ERROR;

STATE USEFIRST Start110 :
  MATCH  "t1_started();"  -> ERROR;
  MATCH  "t2_started();"  -> ERROR;
  MATCH  "t3_started();"  -> GOTO Init;

STATE USEFIRST Start101 :
  MATCH  "t1_started();"  -> ERROR;
  MATCH  "t2_started();"  -> GOTO Init;
  MATCH  "t3_started();"  -> ERROR;

STATE USEFIRST Start011 :
  MATCH  "t1_started();"  -> GOTO Init;
  MATCH  "t2_started();"  -> ERROR;
  MATCH  "t3_started();"  -> ERROR;

END AUTOMATON
