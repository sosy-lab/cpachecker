/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.lock;

import java.util.Set;

public abstract class AbstractLockStateBuilder {
  protected AbstractLockState parentState;
  protected AbstractLockState mutableToRestore;
  protected boolean isRestored;
  protected boolean isFalseState;

  public AbstractLockStateBuilder(AbstractLockState state) {
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

  public void reduce() {
    mutableToRestore = null;
  }

  public abstract void reduceLocks(Set<LockIdentifier> usedLocks);

  public abstract void reduceLockCounters(Set<LockIdentifier> exceptLocks);

  public void expand(AbstractLockState rootState) {
    mutableToRestore = rootState.getRestoredState();
  }

  public abstract void expandLocks(LockState pRootState, Set<LockIdentifier> usedLocks);

  public abstract void expandLockCounters(
      LockState pRootState, Set<LockIdentifier> pRestrictedLocks);

  public void setRestoreState() {
    mutableToRestore = parentState;
  }

  public void setAsFalseState() {
    isFalseState = true;
  }
}
