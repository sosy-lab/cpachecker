// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// This automaton checks correct usage of mutex locks (simplified version):
// it is forbidden to acquire or release the same mutex twice in the same process and
// all acquired mutexes should be released at finalization.
// In order to differentiate mutexes a set automata variable is used.

OBSERVER AUTOMATON set_automaton_variable

// Declare automaton variable 'acquired_mutexes' of type set with string element type.
LOCAL set<string> acquired_mutexes = [];

INITIAL STATE Init;

STATE USEALL Init :
  // Check if this mutex was not acquired twice (element '$1' is not contained in the 'acquired_mutexes' set).
  MATCH {mutex_lock($1)} -> ASSUME {$$acquired_mutexes[$1]} ERROR("mutex_lock:double lock");
  // Acquire this mutex (add element '$1' to the 'acquired_mutexes' set).
  MATCH {mutex_lock($1)} -> DO acquired_mutexes[$1]=true GOTO Init;

  // Check if this mutex was acquired before (element '$1' is contained in the 'acquired_mutexes' set).
  MATCH {mutex_unlock($1)} -> ASSUME {!$$acquired_mutexes[$1]} ERROR("mutex_lock:double unlock");
  // Release this mutex (remove element '$1' from the 'acquired_mutexes' set).
  MATCH {mutex_unlock($1)} -> DO acquired_mutexes[$1]=false GOTO Init;

  // Check that all mutexes were released at finalization (the 'acquired_mutexes' set is empty).
  MATCH {check_final_state($?)} -> ASSUME {!$$acquired_mutexes.empty} ERROR("mutex_lock:locked at exit");

END AUTOMATON

