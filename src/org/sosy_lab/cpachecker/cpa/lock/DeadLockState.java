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

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
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
    public boolean isCompatibleWith(CompatibleState pState) {
      Preconditions.checkArgument(pState instanceof DeadLockTreeNode);
      return true;
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
    private SortedMap<LockIdentifier, Integer> mutableLocks;
    private List<LockIdentifier> mutableLockList;

    public DeadLockStateBuilder(DeadLockState state) {
      super(state);
      mutableLocks = Maps.newTreeMap(state.locks);
      mutableLockList = Lists.newLinkedList(state.lockList);
    }

    @Override
    public void add(LockIdentifier lockId) {
      Integer a;
      if (mutableLocks.containsKey(lockId)) {
        a = mutableLocks.get(lockId) + 1;
      } else {
        a = 1;
      }
      assert (a != null);
      mutableLocks.put(lockId, a);
      if (!mutableLockList.contains(lockId)) {
        mutableLockList.add(lockId);
      }
    }

    @Override
    public void free(LockIdentifier lockId) {
      if (mutableLocks.containsKey(lockId)) {
        Integer a = mutableLocks.get(lockId) - 1;
        if (a > 0) {
          mutableLocks.put(lockId, a);
        } else {
          mutableLocks.remove(lockId);
          mutableLockList.remove(lockId);
        }
      }
    }

    @Override
    public void reset(LockIdentifier lockId) {
      mutableLocks.remove(lockId);
      mutableLockList.remove(lockId);
    }

    @Override
    public void set(LockIdentifier lockId, int num) {
      // num can be equal 0, this means, that in origin file it is 0 and we should delete locks
      assert false : "not supported";
    }

    @Override
    public void restore(LockIdentifier lockId) {
      assert false : "not supported";
    }

    @Override
    public void restoreAll() {
      mutableLocks = ((DeadLockState) mutableToRestore).locks;
    }

    @Override
    public DeadLockState build() {
      if (isFalseState) {
        return null;
      }
      if (isRestored) {
        mutableToRestore = mutableToRestore.toRestore;
      }
      if (locks.equals(mutableLocks) && mutableToRestore == toRestore) {
        return getParentLink();
      } else {
        return new DeadLockState(mutableLocks, mutableLockList, (DeadLockState) mutableToRestore);
      }
    }

    @Override
    public DeadLockState getOldState() {
      return getParentLink();
    }

    @Override
    public void resetAll() {
      mutableLocks.clear();
    }

    @Override
    public void reduce() {
      mutableToRestore = null;
    }

    @Override
    public void reduceLocks(Set<LockIdentifier> usedLocks) {
      if (usedLocks != null) {
        usedLocks.forEach(mutableLocks::remove);
      }
    }

    @Override
    public void reduceLockCounters(Set<LockIdentifier> exceptLocks) {
      assert false : "not supported";
    }

    public void expand(LockState rootState) {
      mutableToRestore = rootState.toRestore;
    }

    @Override
    public void expandLocks(LockState pRootState, Set<LockIdentifier> usedLocks) {
      assert false : "not supported";
    }

    @Override
    public void expandLockCounters(LockState pRootState, Set<LockIdentifier> pRestrictedLocks) {
      assert false : "not supported";
    }

    @Override
    public void setRestoreState() {
      mutableToRestore = getParentLink();
    }

    @Override
    public void setAsFalseState() {
      isFalseState = true;
    }
  }

  private final SortedMap<LockIdentifier, Integer> locks;
  private final List<LockIdentifier> lockList;
  // if we need restore state, we save it here
  // Used for function annotations like annotate.function_name.restore
  public DeadLockState() {
    super();
    locks = Maps.newTreeMap();
    lockList = Lists.newLinkedList();
  }

  protected DeadLockState(
      SortedMap<LockIdentifier, Integer> map, List<LockIdentifier> gLocks, DeadLockState state) {
    super(state);
    this.locks = Maps.newTreeMap(map);
    this.lockList = Lists.newLinkedList(gLocks);
  }

  @Override
  public SortedMap<LockIdentifier, Integer> getHashCodeForState() {
    // Special hash for BAM, in other cases use iterator
    return locks;
  }

  @Override
  public String toString() {
    if (locks.size() > 0) {
      StringBuilder sb = new StringBuilder();
      return Joiner.on("], ").withKeyValueSeparator("[").appendTo(sb, locks).append("]").toString();
    } else {
      return "Without locks";
    }
  }

  @Override
  public int getCounter(LockIdentifier lock) {
    return locks.getOrDefault(lock, 0);
  }

  @Override
  public int hashCode() {
    return Objects.hash(locks, lockList);
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
        && Objects.equals(locks, other.locks)
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

    Iterator<LockIdentifier> iterator1 = locks.keySet().iterator();
    Iterator<LockIdentifier> iterator2 = other.locks.keySet().iterator();
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
  public boolean isCompatibleWith(CompatibleState state) {
    return true;
  }

  @Override
  public DeadLockStateBuilder builder() {
    return new DeadLockStateBuilder(this);
  }

  private DeadLockState getParentLink() {
    return this;
  }

  @Override
  public List<LockEffect> getDifference(AbstractLockState other) {
    // Return the effect, which shows, what should we do to transform from this state to the other

    assert false : "not supported";
    return null;
  }

  @Override
  public CompatibleState prepareToStore() {
    return this;
  }

  @Override
  public CompatibleNode getTreeNode() {
    return new DeadLockTreeNode(lockList);
  }

  @Override
  protected Set<LockIdentifier> getLocks() {
    return locks.keySet();
  }
}
