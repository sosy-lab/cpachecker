// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.lock;

import java.util.Set;

public abstract class AbstractLockStateBuilder {
  protected AbstractLockState parentState;
  protected AbstractLockState mutableToRestore;
  protected boolean isRestored;
  protected boolean isFalseState;

  protected AbstractLockStateBuilder(AbstractLockState state) {
    parentState = state;
    mutableToRestore = state.toRestore;
    isRestored = false;
    isFalseState = false;
  }

  public abstract void add(LockIdentifier lockId);

  public abstract void free(LockIdentifier lockId);

  public abstract void reset(LockIdentifier lockId);

  public abstract void set(LockIdentifier lockId, int num);

  public abstract void restore(LockIdentifier lockId);

  public abstract void restoreAll();

  public abstract AbstractLockState build();

  public AbstractLockState getOldState() {
    return parentState;
  }

  public abstract void resetAll();

  public abstract void reduce(Set<LockIdentifier> removeCounters, Set<LockIdentifier> totalRemove);

  public abstract void expand(
      AbstractLockState rootState,
      Set<LockIdentifier> expandCounters,
      Set<LockIdentifier> totalExpand);

  public void setRestoreState() {
    mutableToRestore = parentState;
  }

  public void setAsFalseState() {
    isFalseState = true;
  }
}
