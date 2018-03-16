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

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.cpa.lock.LockIdentifier.LockType;
import org.sosy_lab.cpachecker.cpa.lock.effects.LockEffect;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleNode;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleState;

public abstract class AbstractLockState
    implements LatticeAbstractState<AbstractLockState>, CompatibleState {

  protected final AbstractLockState toRestore;
  // if we need restore state, we save it here
  // Used for function annotations like annotate.function_name.restore
  public AbstractLockState() {
    toRestore = null;
  }

  protected AbstractLockState(AbstractLockState state) {
    toRestore = state;
  }

  public int getSize() {
    return getLocks().size();
  }

  // Special hash for BAM, in other cases use iterator
  public abstract Object getHashCodeForState();

  public int getCounter(String lockName, String varName) {
    LockIdentifier lock = LockIdentifier.of(lockName, varName, LockType.GLOBAL_LOCK);
    return getCounter(lock);
  }

  public abstract int getCounter(LockIdentifier lock);

  protected abstract Set<LockIdentifier> getLocks();

  @Override
  public boolean isCompatibleWith(CompatibleState state) {
    Preconditions.checkArgument(state instanceof AbstractLockState);
    AbstractLockState pLocks = (AbstractLockState) state;
    return !Sets.intersection(getLocks(), pLocks.getLocks()).isEmpty();
  }

  public abstract AbstractLockStateBuilder builder();

  public abstract List<LockEffect> getDifference(AbstractLockState other);

  @Override
  public boolean isLessOrEqual(AbstractLockState other) {
    // State is less, if it has the same locks as the other and may be some more

    return from(other.getLocks()).allMatch(this.getLocks()::contains);
  }

  @Override
  public abstract CompatibleNode getTreeNode();

  @Override
  public AbstractLockState join(AbstractLockState pOther) {
    throw new UnsupportedOperationException("Operation join isn't supported for LockStatisticsCPA");
  }

  protected AbstractLockState getRestoredState() {
    return toRestore;
  }
}
