// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

OBSERVER AUTOMATON linux_rwlock
LOCAL int read_lock_state = 0;
INITIAL STATE R0_W0;

// no read locks and no write locks.
STATE USEFIRST R0_W0 :
  MATCH {read_lock($?)} -> DO read_lock_state=1 GOTO R1_W0;
  MATCH {read_unlock($?)} -> ERROR("linux:rwlock::more read unlocks");
  MATCH {write_lock($?)} -> GOTO R0_W1;
  MATCH {write_unlock($?)} -> ERROR("linux:rwlock::double write unlock");

  MATCH {$1=read_trylock($?)} -> ASSUME {$1} DO read_lock_state=1 GOTO R1_W0;
  MATCH {$1=read_trylock($?)} -> ASSUME {!$1} GOTO R0_W0;
  MATCH {$1=write_trylock($?)} -> ASSUME {$1} GOTO R0_W1;
  MATCH {$1=write_trylock($?)} -> ASSUME {!$1} GOTO R0_W0;


// write lock acquired with 0 read locks -> it is forbidden to change read locks.
STATE USEFIRST R0_W1 :
  MATCH {read_lock($?)} -> ERROR("linux:rwlock::read lock on write lock");
  MATCH {read_unlock($?)} -> ERROR("linux:rwlock::more read unlocks");
  MATCH {write_lock($?)} -> ERROR("linux:rwlock::double write lock");
  MATCH {write_unlock($?)} -> GOTO R0_W0;

  MATCH {$1=read_trylock($?)} -> ASSUME {$1} ERROR("linux:rwlock::read lock with write lock");
  MATCH {$1=read_trylock($?)} -> ASSUME {!$1} GOTO R0_W1;
  MATCH {$1=write_trylock($?)} -> ASSUME {$1} ERROR("linux:rwlock::double write lock");
  MATCH {$1=write_trylock($?)} -> ASSUME {!$1} GOTO R0_W1;

  MATCH {ldv_check_final_state($?)} -> ERROR("linux:rwlock::write lock at exit");


// 1 or more read locks with no write locks.
STATE USEALL R1_W0 :
  MATCH {read_lock($?)} -> DO read_lock_state=read_lock_state+1 GOTO R1_W0;
  MATCH {read_unlock($?)} -> ASSUME {$$read_lock_state >  0;} DO read_lock_state=read_lock_state-1 GOTO R1_W0;
  MATCH {read_unlock($?)} -> ASSUME {$$read_lock_state <= 0;} DO read_lock_state=read_lock_state-1 GOTO R0_W0;
  MATCH {write_lock($?)} -> GOTO R1_W1;
  MATCH {write_unlock($?)} -> ERROR("linux:rwlock::double write unlock");

  MATCH {$1=read_trylock($?)} -> ASSUME {$1} DO read_lock_state=read_lock_state+1 GOTO R1_W0;
  MATCH {$1=read_trylock($?)} -> ASSUME {!$1} GOTO R1_W0;
  MATCH {$1=write_trylock($?)} -> ASSUME {$1} GOTO R1_W1;
  MATCH {$1=write_trylock($?)} -> ASSUME {!$1} GOTO R1_W0;

  MATCH {ldv_check_final_state($?)} -> ERROR("linux:rwlock::read lock at exit");


// write lock acquired with 1 or more read locks -> it is forbidden to change read locks.
STATE USEALL R1_W1 :
  MATCH {read_lock($?)} -> ERROR("linux:rwlock::read lock on write lock");
  MATCH {read_unlock($?)} -> ASSUME {$$read_lock_state >  0;} DO read_lock_state=read_lock_state-1 GOTO R1_W1;
  MATCH {read_unlock($?)} -> ASSUME {$$read_lock_state <= 0;} DO read_lock_state=read_lock_state-1 GOTO R0_W1;
  MATCH {write_lock($?)} -> ERROR("linux:rwlock::double write lock");
  MATCH {write_unlock($?)} -> GOTO R1_W0;

  MATCH {$1=read_trylock($?)} -> ASSUME {$1} ERROR("linux:rwlock::read lock with write lock");
  MATCH {$1=read_trylock($?)} -> ASSUME {!$1} GOTO R1_W1;
  MATCH {$1=write_trylock($?)} -> ASSUME {$1} ERROR("linux:rwlock::double write lock");
  MATCH {$1=write_trylock($?)} -> ASSUME {!$1} GOTO R1_W1;

  MATCH {ldv_check_final_state($?)} -> ERROR("linux:rwlock::read lock at exit");
  MATCH {ldv_check_final_state($?)} -> ERROR("linux:rwlock::write lock at exit");

END AUTOMATON

