// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.lock;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.lock.effects.LockEffect;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleNode;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleState;

public final class DeadLockState extends AbstractLockState {

  @SuppressWarnings("checkstyle:IllegalType") // TODO: use composition instead of inheritance
  public static class DeadLockTreeNode extends ArrayList<LockIdentifier> implements CompatibleNode {

    private static final long serialVersionUID = 5757759799394605077L;

    public DeadLockTreeNode(List<LockIdentifier> locks) {
      super(locks);
    }

    @Override
    public int compareTo(CompatibleState pOther) {
      Preconditions.checkArgument(pOther instanceof DeadLockTreeNode);
      DeadLockTreeNode o = (DeadLockTreeNode) pOther;
      int result = size() - o.size();
      if (result != 0) {
        return result;
      }
      Iterator<LockIdentifier> lockIterator = iterator();
      Iterator<LockIdentifier> lockIterator2 = o.iterator();
      while (lockIterator.hasNext()) {
        result = lockIterator.next().compareTo(lockIterator2.next());
        if (result != 0) {
          return result;
        }
      }
      return 0;
    }

    @Override
    public boolean cover(CompatibleNode pNode) {
      Preconditions.checkArgument(pNode instanceof DeadLockTreeNode);
      return compareTo(pNode) == 0;
    }

    @Override
    public boolean hasEmptyLockSet() {
      return isEmpty();
    }
  }

  public class DeadLockStateBuilder extends AbstractLockStateBuilder {
    private List<LockIdentifier> mutableLockList;

    @SuppressWarnings("JdkObsolete") // TODO consider replacing this with ArrayList or ArrayDeque
    public DeadLockStateBuilder(DeadLockState state) {
      super(state);
      mutableLockList = new LinkedList<>(state.lockList);
    }

    @Override
    public void add(LockIdentifier lockId) {
      mutableLockList.add(lockId);
    }

    @Override
    public void free(LockIdentifier lockId) {
      // Remove last!
      for (int i = mutableLockList.size() - 1; i >= 0; i--) {
        LockIdentifier id = mutableLockList.get(i);
        if (id.equals(lockId)) {
          mutableLockList.remove(i);
          return;
        }
      }
    }

    @Override
    public void reset(LockIdentifier lockId) {
      while (mutableLockList.remove(lockId)) {}
    }

    @Override
    public void set(LockIdentifier lockId, int num) {
      throw new UnsupportedOperationException(
          "Set annotations are not supported for dead lock analysis");
    }

    @Override
    public void restore(LockIdentifier lockId) {
      throw new UnsupportedOperationException(
          "Restore annotations are not supported for dead lock analysis");
    }

    @Override
    public void restoreAll() {
      mutableLockList = ((DeadLockState) mutableToRestore).lockList;
    }

    @Override
    public DeadLockState build() {
      if (isFalseState) {
        return null;
      }
      if (isRestored) {
        mutableToRestore = mutableToRestore.toRestore;
      }
      if (lockList.equals(mutableLockList) && mutableToRestore == toRestore) {
        return DeadLockState.this;
      } else {
        return new DeadLockState(mutableLockList, (DeadLockState) mutableToRestore);
      }
    }

    @Override
    public DeadLockState getOldState() {
      return DeadLockState.this;
    }

    @Override
    public void resetAll() {
      mutableLockList.clear();
    }

    @Override
    public void reduce(Set<LockIdentifier> removeCounters, Set<LockIdentifier> totalRemove) {
      mutableToRestore = null;
      int num = getTailNum(mutableLockList, removeCounters, totalRemove);
      if (num < mutableLockList.size() - 1) {
        for (int i = mutableLockList.size() - 1; i > num; i--) {
          mutableLockList.remove(i);
        }
      }
    }

    @Override
    public void expand(
        AbstractLockState rootState,
        Set<LockIdentifier> expandCounters,
        Set<LockIdentifier> totalExpand) {
      mutableToRestore = rootState.toRestore;
      List<LockIdentifier> rootList = ((DeadLockState) rootState).lockList;
      int num = getTailNum(mutableLockList, expandCounters, totalExpand);
      if (num < rootList.size() - 1) {
        for (int i = num + 1; i < rootList.size(); i++) {
          mutableLockList.add(rootList.get(i));
        }
      }
    }

    private int getTailNum(
        List<LockIdentifier> pLockList,
        Set<LockIdentifier> removeCounters,
        Set<LockIdentifier> totalRemove) {
      for (int i = pLockList.size() - 1; i >= 0; i--) {
        LockIdentifier id = pLockList.get(i);
        if (!totalRemove.contains(id)
            || (pLockList.indexOf(id) == i && removeCounters.contains(id))) {
          return i;
        }
      }
      return 0;
    }

    public void expand(LockState rootState) {
      mutableToRestore = rootState.toRestore;
    }

    @Override
    public void setRestoreState() {
      mutableToRestore = DeadLockState.this;
    }

    @Override
    public void setAsFalseState() {
      isFalseState = true;
    }
  }

  private final List<LockIdentifier> lockList;
  // if we need restore state, we save it here
  // Used for function annotations like annotate.function_name.restore
  @SuppressWarnings("JdkObsolete") // TODO consider replacing this with ArrayList or ArrayDeque
  public DeadLockState() {
    lockList = new LinkedList<>();
  }

  @SuppressWarnings("JdkObsolete") // TODO consider replacing this with ArrayList or ArrayDeque
  DeadLockState(List<LockIdentifier> gLocks, DeadLockState state) {
    super(state);
    lockList = new LinkedList<>(gLocks);
  }

  @Override
  public List<LockIdentifier> getHashCodeForState() {
    // Special hash for BAM, in other cases use iterator
    return lockList;
  }

  @Override
  public String toString() {
    if (!lockList.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      return Joiner.on(",").appendTo(sb, lockList).toString();
    } else {
      return "Without locks";
    }
  }

  @Override
  public int getCounter(LockIdentifier lock) {
    return from(lockList).filter(lock::equals).size();
  }

  @Override
  public int hashCode() {
    return Objects.hash(lockList);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    DeadLockState other = (DeadLockState) obj;
    return Objects.equals(toRestore, other.toRestore) && Objects.equals(lockList, other.lockList);
  }

  /**
   * This method find the difference between two states in some metric. It is useful for
   * comparators. lock1.diff(lock2) <=> lock1 - lock2.
   *
   * @param pOther The other LockStatisticsState
   * @return Difference between two states
   */
  @Override
  public int compareTo(CompatibleState pOther) {
    DeadLockState other = (DeadLockState) pOther;

    int result = other.getSize() - getSize(); // decreasing queue

    if (result != 0) {
      return result;
    }

    Iterator<LockIdentifier> iterator1 = lockList.iterator();
    Iterator<LockIdentifier> iterator2 = other.lockList.iterator();
    // Sizes are equal
    while (iterator1.hasNext()) {
      LockIdentifier lockId1 = iterator1.next();
      LockIdentifier lockId2 = iterator2.next();
      result = lockId1.compareTo(lockId2);
      if (result != 0) {
        return result;
      }
    }
    return 0;
  }

  @Override
  public DeadLockStateBuilder builder() {
    return new DeadLockStateBuilder(this);
  }

  @Override
  public Multiset<LockEffect> getDifference(AbstractLockState other) {
    // Return the effect, which shows, what should we do to transform from this state to the other
    throw new UnsupportedOperationException("Effects are not supported for dead lock detection");
  }

  @Override
  public CompatibleNode getCompatibleNode() {
    return new DeadLockTreeNode(lockList);
  }

  @Override
  protected Set<LockIdentifier> getLocks() {
    return ImmutableSet.copyOf(lockList);
  }
}
