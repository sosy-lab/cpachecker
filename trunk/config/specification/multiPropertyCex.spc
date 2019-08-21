CONTROL AUTOMATON MultiErrors
INITIAL STATE Init;
STATE USEALL Init:
  MATCH {__VERIFIER_error($?)} -> PRINTONCE "$rawstatement called in line $line" ERROR("$rawstatement called in line $line");
END AUTOMATON
