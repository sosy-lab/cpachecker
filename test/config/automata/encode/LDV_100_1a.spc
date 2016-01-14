OBSERVER AUTOMATON AUTOMATON_100_1a
INITIAL STATE Init;

STATE USEALL Init :
  MATCH ENTRY -> ENCODE {void *latest_tty = ((struct tty_struct *)1);} GOTO Init;
  //MATCH CALL {ldv_initialize($?)} -> ASSUME {((struct tty_struct *)latest_tty) == ((struct tty_struct *)0)} GOTO Stop;
  MATCH CALL {ldv_handler_precall($?)} -> ENCODE {latest_tty = ((struct tty_struct *)1);} GOTO Init;

  MATCH CALL {ldv_got_tty($1)} -> ENCODE {latest_tty = ((struct tty_struct *)$1);} GOTO Init;
  
  MATCH CALL {ldv_check_tty($1)} -> ASSUME {((struct tty_struct *)latest_tty) == ((struct tty_struct *)$1); ((struct tty_struct *)$1) == ((struct tty_struct *)0)} ERROR;
  MATCH CALL {ldv_check_tty($1)} -> ASSUME {((struct tty_struct *)latest_tty) != ((struct tty_struct *)$1)} GOTO Init;
  MATCH CALL {ldv_check_tty($1)} -> ASSUME {((struct tty_struct *)$1) != ((struct tty_struct *)0)} GOTO Init;

STATE USEFIRST Stop :
  TRUE -> GOTO Stop;

END AUTOMATON
