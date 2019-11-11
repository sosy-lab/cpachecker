CONTROL AUTOMATON ARG

INITIAL STATE init;

STATE USEALL init:
    MATCH "x++;" -> ASSUME {x==1} GOTO init;
    MATCH "x++;" -> ASSUME {x!=1} ERROR;
    MATCH "x--;" -> ASSUME {x==1} GOTO init;
    MATCH "x--;" -> ASSUME {x!=1} ERROR;

END AUTOMATON
