// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// This specification consists of 2 automata.
OBSERVER AUTOMATON linux_rculock
LOCAL int rcu_state = 0;
INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH {rcu_read_lock($?)} -> DO rcu_state=rcu_state+1 GOTO Inc;
  MATCH {rcu_read_unlock($?)} -> ERROR("linux:rculock::more unlocks");

STATE USEALL Inc :
  MATCH {rcu_read_lock($?)} -> DO rcu_state=rcu_state+1 GOTO Inc;
  MATCH {rcu_read_unlock($?)} && (rcu_state != 1) -> DO rcu_state=rcu_state-1 GOTO Inc;
  MATCH {rcu_read_unlock($?)} && (rcu_state == 1) -> DO rcu_state=rcu_state-1 GOTO Init;
  MATCH {ldv_check_final_state($?)} -> ERROR("linux:rculock::locked at exit");

END AUTOMATON

OBSERVER AUTOMATON linux_rculockbh
LOCAL int rcu_bh_state = 0;
INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH {rcu_read_lock_bh($?)} -> DO rcu_bh_state=rcu_bh_state+1 GOTO Inc;
  MATCH {rcu_read_unlock_bh($?)} -> ERROR("linux:rculockbh::more unlocks");

STATE USEALL Inc :
  MATCH {rcu_read_lock_bh($?)} -> DO rcu_bh_state=rcu_bh_state+1 GOTO Inc;
  MATCH {rcu_read_unlock_bh($?)} && (rcu_bh_state != 1) -> DO rcu_bh_state=rcu_bh_state-1 GOTO Inc;
  MATCH {rcu_read_unlock_bh($?)} && (rcu_bh_state == 1) -> DO rcu_bh_state=rcu_bh_state-1 GOTO Init;
  MATCH {ldv_check_final_state($?)} -> ERROR("linux:rculockbh::locked at exit");

END AUTOMATON

