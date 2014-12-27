OBSERVER AUTOMATON PartitioningAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :

   CHECK(location, "locationClass==TARGET")
   -> PRINT "TARGET MATCH"
      GOTO Violating;

   !CHECK(location, "locationClass==TARGET")
   -> PRINT "NO TARGET MATCH"
      GOTO Valid;

STATE Valid:
    TRUE -> GOTO Valid; 

STATE Violating:
    TRUE -> GOTO Violating; 

END AUTOMATON
