// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

CONTROL AUTOMATON linux_spinlock
LOCAL set<string> acquired_spinlocks=[];
INITIAL STATE Init;

STATE USEALL Init :
  // Check if this spinlock was not locked twice (element '$1' is not contained in the 'acquired_spinlocks' set).
  MATCH {spin_lock($1)} -> ASSUME {$$acquired_spinlocks[$1]} ERROR("linux:spinlock::one thread:double lock");
  // Lock this spinlock (add element '$1' to the 'acquired_spinlocks' set).
  MATCH {spin_lock($1)} -> DO acquired_spinlocks[$1]=true GOTO Init;

  // Check if this spinlock was locked before (element '$1' is contained in the 'acquired_spinlocks' set).
  MATCH {spin_unlock($1)} -> ASSUME {!$$acquired_spinlocks[$1]} ERROR("linux:spinlock::one thread:double unlock");
  // Unlock this spinlock (remove element '$1' from the 'acquired_spinlocks' set).
  MATCH {spin_unlock($1)} -> DO acquired_spinlocks[$1]=false GOTO Init;

  // Check if this spinlock was not locked twice (element '$1' is not contained in the 'acquired_spinlocks' set).
  MATCH {$1=spin_trylock($2)} -> ASSUME {$$acquired_spinlocks[$2]} ERROR("linux:spinlock::one thread:double lock try");
  // Lock this spinlock (add element '$2' to the 'acquired_spinlocks' set).
  MATCH {$1=spin_trylock($2)} -> ASSUME {$1} DO acquired_spinlocks[$2]=true GOTO Init;
  // Do not lock this spinlock if return value is 0.
  MATCH {$1=spin_trylock($2)} -> ASSUME {!$1} GOTO Init;

  // Cut paths, on which this function returns 0 with locked spinlock.
  MATCH {$1=spin_is_locked($2)} -> ASSUME {!$1; $$acquired_spinlocks[$2]} STOP;
  // Allow paths, on which this function returns not 0.
  MATCH {$1=spin_is_locked($2)} -> ASSUME {$1} GOTO Init;
  // Allow paths, on which this spinlock was not locked.
  MATCH {$1=spin_is_locked($2)} -> ASSUME {!$$acquired_spinlocks[$2]} GOTO Init;

  // Check if this spinlock was not locked twice (element '$3' is not contained in the 'acquired_spinlocks' set).
  MATCH {$1=_atomic_dec_and_lock($2, $3)} -> ASSUME {$$acquired_spinlocks[$3]} ERROR("linux:spinlock::one thread:double lock try");
  // Lock this spinlock (add element '$1' to the 'acquired_spinlocks' set).
  MATCH {$1=_atomic_dec_and_lock($2, $3)} -> ASSUME {$1} DO acquired_spinlocks[$3]=true GOTO Init;
  // Do not lock this spinlock if return value is 0.
  MATCH {$1=_atomic_dec_and_lock($2, $3)} -> ASSUME {!$1} GOTO Init;

  // Check that all spinlocks were unlocked (the 'acquired_spinlocks' set is empty).
  MATCH {ldv_check_final_state($?)} -> ASSUME {!$$acquired_spinlocks.empty} ERROR("linux:spinlock::one thread:locked at exit");

END AUTOMATON
