// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

CONTROL AUTOMATON linux_mutex
LOCAL set<string> acquired_mutexes;
INITIAL STATE Init;

STATE USEALL Init :
  // Check if this mutex was not locked twice (element '$1' is not contained in the 'acquired_mutexes' set).
  MATCH {mutex_lock($1)} -> ASSUME {$$acquired_mutexes[$1]} ERROR("linux:mutex::one thread:double lock");
  // Lock this mutex (add element '$1' to the 'acquired_mutexes' set).
  MATCH {mutex_lock($1)} -> DO acquired_mutexes[$1]=true GOTO Init;

  // Check if this mutex was not locked twice (element '$1' is not contained in the 'acquired_mutexes' set).
  MATCH {mutex_lock_nested($1, $2)} -> ASSUME {$$acquired_mutexes[$1]} ERROR("linux:mutex::one thread:double lock");
  // Lock this mutex (add element '$1' to the 'acquired_mutexes' set).
  MATCH {mutex_lock_nested($1, $2)} -> DO acquired_mutexes[$1]=true GOTO Init;

  // Check if this mutex was locked before (element '$1' is contained in the 'acquired_mutexes' set).
  MATCH {mutex_unlock($1)} -> ASSUME {!$$acquired_mutexes[$1]} ERROR("linux:mutex::one thread:double unlock");
  // Unlock this mutex (remove element '$1' from the 'acquired_mutexes' set).
  MATCH {mutex_unlock($1)} -> DO acquired_mutexes[$1]=false GOTO Init;

  // Check if this mutex was not locked twice (element '$1' is not contained in the 'acquired_mutexes' set).
  MATCH {$1 = mutex_trylock($2)} -> ASSUME {$$acquired_mutexes[$2]} ERROR("linux:mutex::one thread:double lock try");
  // Lock this mutex if return value is not 0 (add element '$1' to the 'acquired_mutexes' set).
  MATCH {$1 = mutex_trylock($2)} -> ASSUME {((int)$1)} DO acquired_mutexes[$2]=true GOTO Init;
  // Do not lock this mutex if return value is 0.
  MATCH {$1 = mutex_trylock($2)} -> ASSUME {!((int)$1)} GOTO Init;

  // Check if this mutex was not locked twice (element '$1' is not contained in the 'acquired_mutexes' set).
  MATCH {$1 = mutex_lock_interruptible($2)} -> ASSUME {$$acquired_mutexes[$2]} ERROR("linux:mutex::one thread:double lock");
  // Lock this mutex if return value is 0 (add element '$1' to the 'acquired_mutexes' set).
  MATCH {$1 = mutex_lock_interruptible($2)} -> ASSUME {!((int)$1)} DO acquired_mutexes[$2]=true GOTO Init;
  // Do not lock this mutex if return value is not 0.
  MATCH {$1 = mutex_lock_interruptible($2)} -> ASSUME {((int)$1)} GOTO Init;

  // Check if this mutex was not locked twice (element $1 is not contained in set mutex).
  MATCH {$1 = mutex_lock_killable($2)} -> ASSUME {$$acquired_mutexes[$2]} ERROR("linux:mutex::one thread:double lock");
  // Lock this mutex if return value is 0 (add element $1 to set mutex).
  MATCH {$1 = mutex_lock_killable($2)} -> ASSUME {!((int)$1)} DO acquired_mutexes[$2]=true GOTO Init;
  // Do not lock this mutex if return value is not 0.
  MATCH {$1 = mutex_lock_killable($2)} -> ASSUME {((int)$1)} GOTO Init;

  // Check if this mutex was not locked twice (element '$1' is not contained in the 'acquired_mutexes' set).
  MATCH {$1 = mutex_lock_interruptible_nested($2, $3)} -> ASSUME {$$acquired_mutexes[$2]} ERROR("linux:mutex::one thread:double lock");
  // Lock this mutex if return value is 0 (add element '$1' to the 'acquired_mutexes' set).
  MATCH {$1 = mutex_lock_interruptible_nested($2, $3)} -> ASSUME {!((int)$1)} DO acquired_mutexes[$2]=true GOTO Init;
  // Do not lock this mutex if return value is not 0.
  MATCH {$1 = mutex_lock_interruptible_nested($2, $3)} -> ASSUME {((int)$1)} GOTO Init;

  // Check if this mutex was not locked twice (element '$1' is not contained in the 'acquired_mutexes' set).
  MATCH {$1 = mutex_lock_killable_nested($2, $3)} -> ASSUME {$$acquired_mutexes[$2]} ERROR("linux:mutex::one thread:double lock");
  // Lock this mutex if return value is 0 (add element '$1' to the 'acquired_mutexes' set).
  MATCH {$1 = mutex_lock_killable_nested($2, $3)} -> ASSUME {!((int)$1)} DO acquired_mutexes[$2]=true GOTO Init;
  // Do not lock this mutex if return value is not 0.
  MATCH {$1 = mutex_lock_killable_nested($2, $3)} -> ASSUME {((int)$1)} GOTO Init;

  // Check if this mutex was not locked twice (element '$1' is not contained in the 'acquired_mutexes' set).
  MATCH {$1 = atomic_dec_and_mutex_lock($2, $3)} -> ASSUME {$$acquired_mutexes[$3]} ERROR("linux:mutex::one thread:double lock");
  // Lock this mutex if return value is not 0 (add element '$1' to the 'acquired_mutexes' set).
  MATCH {$1 = atomic_dec_and_mutex_lock($2, $3)} -> ASSUME {((int)$1)} DO acquired_mutexes[$3]=true GOTO Init;
  // Do not lock this mutex if return value is 0.
  MATCH {$1 = atomic_dec_and_mutex_lock($2, $3)} -> ASSUME {!((int)$1)} GOTO Init;

  // Cut pathes, on which this function returns 0 with locked mutex.
  MATCH {$1 = mutex_is_locked($2)} -> ASSUME {!((int)$1); $$acquired_mutexes[$2]} STOP;
  // Allow pathes, on which this function returns not 0.
  MATCH {$1 = mutex_is_locked($2)} -> ASSUME {((int)$1)} GOTO Init;
  // Allow pathes, on which this mutex was not locked.
  MATCH {$1 = mutex_is_locked($2)} -> ASSUME {!$$acquired_mutexes[$2]} GOTO Init;

  // Check if this mutex was not locked twice (element '$4' is not contained in the 'acquired_mutexes' set).
  MATCH {$1 = kref_put_mutex($2, $3, $4)} -> ASSUME {$$acquired_mutexes[$4]} ERROR("linux:mutex::one thread:double lock");
  // Lock this mutex if return value is not 0 (add element '$4' to the 'acquired_mutexes' set).
  MATCH {$1 = kref_put_mutex($2, $3, $4)} -> ASSUME {$1} DO acquired_mutexes[$4]=true GOTO Init;
  // Do not lock this mutex if return value is 0.
  MATCH {$1 = kref_put_mutex($2, $3, $4)} -> ASSUME {!$1} GOTO Init;

  // Check that all mutexes were unlocked (the 'acquired_mutexes' set is empty).
  MATCH {ldv_check_final_state($?)} -> ASSUME {!$$acquired_mutexes.empty} ERROR("linux:mutex::one thread:locked at exit");

END AUTOMATON
