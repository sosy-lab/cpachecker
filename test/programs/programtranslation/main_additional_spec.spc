CONTROL AUTOMATON ARG

INITIAL STATE init;

STATE USEALL init:
    MATCH "[i > 0]" -> ASSUME {x<5} GOTO init;
    MATCH "[i > 0]" -> ASSUME {x>=5} ERROR;
    MATCH "[!(i > 0)]" -> ASSUME {x<7} GOTO init;
    MATCH "[!(i > 0)]" -> ASSUME {x>=7} ERROR;

END AUTOMATON
