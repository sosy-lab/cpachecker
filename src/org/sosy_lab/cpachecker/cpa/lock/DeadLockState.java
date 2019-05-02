/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.lock.effects.LockEffect;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleNode;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleState;

public class DeadLockState extends AbstractLockState {

  public static class DeadLockTreeNode extends ArrayList<LockIdentifier> implements CompatibleNode {

    private static final long serialVersionUID = 5757759799394605077L;

    public DeadLockTreeNode(List<LockIdentifier> locks) {
      super(locks);
    }

    @Override
    public int compareTo(CompatibleState pArg0) {
      Preconditions.checkArgument(pArg0 instanceof DeadLockTreeNode);
      DeadLockTreeNode o = (DeadLockTreeNode) pArg0;
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
      return this.compareTo(pNode) == 0;
    }

    @Override
    public boolean hasEmptyLockSet() {
      return isEmpty();
    }
  }

  public class DeadLockStateBuilder extends AbstractLockStateBuilder {
    private List<LockIdentifier> mutableLockList;

    public DeadLockStateBuilder(DeadLockState state) {
      super(state);
      mutableLockList = Lists.newLinkedList(state.lockList);
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
    public void reduce() {
      mutableToRestore = null;
    }

    @Override
    public void reduceLocks(Set<LockIdentifier> usedLocks) {

    }

    @Override
    public void reduceLockCounters(Set<LockIdentifier> exceptLocks) {
      int num = getTailNum(mutableLockList, exceptLocks);
      if (num < mutableLockList.size() - 1) {
        for (int i = mutableLockList.size() - 1; i > num; i--) {
          mutableLockList.remove(i);
        }
      }
    }

    private int getTailNum(List<LockIdentifier> pLockList, Set<LockIdentifier> exceptLocks) {
      for (int i = pLockList.size() - 1; i >= 0; i--) {
        LockIdentifier id = pLockList.get(i);
        if (pLockList.indexOf(id) == i || exceptLocks.contains(id)) {
          return i;
        }
      }
      return 0;
    }

    public void expand(LockState rootState) {
      mutableToRestore = rootState.toRestore;
    }

    @Override
    public void expandLocks(LockState pRootState, Set<LockIdentifier> usedLocks) {
      throw new UnsupportedOperationException(
          "Valueable reduce/expand operations are not supported for dead lock analysis");
    }

    @Override
    public void expandLockCounters(
        AbstractLockState pRootState,
        Set<LockIdentifier> pRestrictedLocks) {
      List<LockIdentifier> rootList = ((DeadLockState) pRootState).lockList;
      int num = getTailNum(rootList, pRestrictedLocks);
      if (num < rootList.size() - 1) {
        for (int i = num + 1; i < rootList.size(); i++) {
          mutableLockList.add(rootList.get(i));
        }
      }
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
  public DeadLockState() {
    super();
    lockList = Lists.newLinkedList();
  }

  protected DeadLockState(List<LockIdentifier> gLocks, DeadLockState state) {
    super(state);
    this.lockList = Lists.newLinkedList(gLocks);
  }

  @Override
  public List<LockIdentifier> getHashCodeForState() {
    // Special hash for BAM, in other cases use iterator
    return lockList;
  }

  @Override
  public String toString() {
    if (lockList.size() > 0) {
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
    return Objects.equals(toRestore, other.toRestore)
        && Objects.equals(lockList, other.lockList);
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
    int result = 0;

    result = other.getSize() - this.getSize(); // decreasing queue

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
    return from(lockList).toSet();
  }
}
