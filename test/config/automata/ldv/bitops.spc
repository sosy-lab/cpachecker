// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

CONTROL AUTOMATON linux_bitops
INITIAL STATE Init;

STATE USEALL Init :
  // Check if offset ($3) is not greater than the size of the array ($2).
  MATCH {$4=find_next_bit($1, $2, $3)} -> ASSUME {((unsigned long)$3) >  ((unsigned long)$2)} ERROR("linux:bitops::offset out of range");
  // Cut pathes, on which this function return value ($4) is greater than the size of the array ($2).
  MATCH {$4=find_next_bit($1, $2, $3)} -> ASSUME {((unsigned long)$4) >  ((unsigned long)$2)} STOP;
  // Allow pathes, on which this function return value ($4) is not greater than the size of the array ($2).
  MATCH {$4=find_next_bit($1, $2, $3)} -> ASSUME {((unsigned long)$4) <= ((unsigned long)$2)} GOTO Init;

  // Cut pathes, on which this function return value ($2) is greater than the size of the array ($1).
  MATCH {$2=find_first_bit($1, $3)} -> ASSUME {((unsigned long)$2) >  ((unsigned long)$1)} STOP;
  // Allow pathes, on which this function return value ($2) is not greater than the size of the array ($1).
  MATCH {$2=find_first_bit($1, $3)} -> ASSUME {((unsigned long)$2) <= ((unsigned long)$1)} GOTO Init;

END AUTOMATON

