// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

CONTROL AUTOMATON linux_alloc_spinlock
LOCAL set<string> acquired_spinlocks_alloc;
INITIAL STATE Init;

STATE USEALL Init :
  // Cut paths, on which spinlock is locked twice.
  MATCH {spin_lock($1)} -> ASSUME {$$acquired_spinlocks_alloc[$1]} STOP;
  // Lock this spinlock (add element '$1' to the 'acquired_spinlocks_alloc' set).
  MATCH {spin_lock($1)} -> DO acquired_spinlocks_alloc[$1]=true GOTO Init;

  // Cut paths, on which spinlock is unlocked twice.
  MATCH {spin_unlock($1)} -> ASSUME {!$$acquired_spinlocks_alloc[$1]} STOP;
  // Unlock this spinlock (remove element '$1' from the 'acquired_spinlocks_alloc' set).
  MATCH {spin_unlock($1)} -> DO acquired_spinlocks_alloc[$1]=false GOTO Init;

  // Cut paths, on which spinlock is locked twice.
  MATCH {$1=spin_trylock($2)} -> ASSUME {$$acquired_spinlocks_alloc[$2]} STOP;
  // Lock this spinlock (add element '$2' to the 'acquired_spinlocks_alloc' set).
  MATCH {$1=spin_trylock($2)} -> ASSUME {$1} DO acquired_spinlocks_alloc[$2]=true GOTO Init;
  // Do not lock this spinlock if return value is 0.
  MATCH {$1=spin_trylock($2)} -> ASSUME {!$1} GOTO Init;

  // Cut paths, on which this function returns 0 with locked spinlock.
  MATCH {$1=spin_is_locked($2)} -> ASSUME {!$1; $$acquired_spinlocks_alloc[$2]} STOP;
  // Allow paths, on which this function returns not 0.
  MATCH {$1=spin_is_locked($2)} -> ASSUME {$1} GOTO Init;
  // Allow paths, on which this spinlock was not locked.
  MATCH {$1=spin_is_locked($2)} -> ASSUME {!$$acquired_spinlocks_alloc[$2]} GOTO Init;

  // Cut paths, on which spinlock is locked twice.
  MATCH {$1=_atomic_dec_and_lock($2, $3)} -> ASSUME {$$acquired_spinlocks_alloc[$3]} STOP;
  // Lock this spinlock (add element '$1' to the 'acquired_spinlocks_alloc' set).
  MATCH {$1=_atomic_dec_and_lock($2, $3)} -> ASSUME {$1} DO acquired_spinlocks_alloc[$3]=true GOTO Init;
  // Do not lock this spinlock if return value is 0.
  MATCH {$1=_atomic_dec_and_lock($2, $3)} -> ASSUME {!$1} GOTO Init;

  // Check if flags value ($3) satisfies spinlock section.
  MATCH {$1 = kmalloc($2, $3)} -> ASSUME {((int)$3)!=32; ((int)$3)!=0; !$$acquired_spinlocks_alloc.empty} ERROR("linux:alloc:spin lock::wrong flags");
  // Allow paths, on which flags value ($3) satisfies spinlock section.
  MATCH {$1 = kmalloc($2, $3)} -> ASSUME {((int)$3)==0} GOTO Init;
  MATCH {$1 = kmalloc($2, $3)} -> ASSUME {((int)$3)==32} GOTO Init;
 // Allow paths, on which any spinlock was unlocked.
  MATCH {$1 = kmalloc($2, $3)} -> ASSUME {$$acquired_spinlocks_alloc.empty} GOTO Init;

  // Check for calls, which are forbidden with locked spinlocks.
  MATCH {$1 = vmalloc($2)} -> ASSUME {!$$acquired_spinlocks_alloc.empty} ERROR("linux:alloc:spin lock::nonatomic");
  // Allow paths, on which any spinlock was unlocked.
  MATCH {$1 = vmalloc($2)} -> GOTO Init;

END AUTOMATON

