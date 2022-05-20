// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.lock;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import java.util.Collection;
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
  protected AbstractLockState() {
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
    return Sets.intersection(getLocks(), pLocks.getLocks()).isEmpty();
  }

  public Collection<LockIdentifier> getIntersection(CompatibleState state) {
    Preconditions.checkArgument(state instanceof AbstractLockState);
    AbstractLockState pLocks = (AbstractLockState) state;
    return ImmutableSet.copyOf(Sets.intersection(getLocks(), pLocks.getLocks()));
  }

  public abstract AbstractLockStateBuilder builder();

  public abstract Multiset<LockEffect> getDifference(AbstractLockState other);

  @Override
  public boolean isLessOrEqual(AbstractLockState other) {
    // State is less, if it has the same locks as the other and may be some more

    return from(other.getLocks()).allMatch(getLocks()::contains);
  }

  @Override
  public abstract CompatibleNode getCompatibleNode();

  @Override
  public AbstractLockState join(AbstractLockState pOther) {
    throw new UnsupportedOperationException("Operation join isn't supported for LockStatisticsCPA");
  }

  protected AbstractLockState getRestoredState() {
    return toRestore;
  }
}
