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

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.cpa.lock.LockIdentifier.LockType;
import org.sosy_lab.cpachecker.cpa.lock.effects.AcquireLockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.LockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.ReleaseLockEffect;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleState;
import org.sosy_lab.cpachecker.cpa.usage.UsageTreeNode;

public class LockState implements LatticeAbstractState<LockState>, Serializable, CompatibleState {

  public static class LockTreeNode extends TreeSet<LockIdentifier> implements UsageTreeNode{

    private static final long serialVersionUID = 5757759799394605077L;

    public LockTreeNode(Set<LockIdentifier> locks) {
      super(locks);
    }
    @Override
    public boolean isCompatibleWith(CompatibleState pState) {
      Preconditions.checkArgument(pState instanceof LockTreeNode);
      return Sets.intersection(this, (LockTreeNode)pState).isEmpty();
    }

    @Override
    public CompatibleState prepareToStore() {
      return this;
    }

    @Override
    public UsageTreeNode getTreeNode() {
      return this;
    }

    @Override
    public int compareTo(CompatibleState pArg0) {
      Preconditions.checkArgument(pArg0 instanceof LockTreeNode);
      LockTreeNode o = (LockTreeNode) pArg0;
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
    public boolean cover(UsageTreeNode pNode) {
      Preconditions.checkArgument(pNode instanceof LockTreeNode);
      LockTreeNode o = (LockTreeNode) pNode;
      if (o.containsAll(this)) {
        return true;
      }
      return false;
    }

  }

  public class LockStateBuilder {
    private SortedMap<LockIdentifier, Integer> mutableLocks;
    private LockState mutableToRestore;
    private boolean isRestored;
    private boolean isFalseState;

    public LockStateBuilder(LockState state) {
      mutableLocks = Maps.newTreeMap(state.locks);
      mutableToRestore = state.toRestore;
      isRestored = false;
      isFalseState = false;
    }

    public void add(LockIdentifier lockId) {
      Integer a;
      if (mutableLocks.containsKey(lockId)) {
        a = mutableLocks.get(lockId);
        a++;
      } else {
        a = 1;
      }
      assert (a != null);
      mutableLocks.put(lockId, a);
    }

    public void free(LockIdentifier lockId) {
      if (mutableLocks.containsKey(lockId)) {
        Integer a = mutableLocks.get(lockId);
        if (a != null) {
          a--;
          if (a > 0) {
            mutableLocks.put(lockId, a);
          } else {
            mutableLocks.remove(lockId);
          }
        }
      }
    }

    public void reset(LockIdentifier lockId) {
      mutableLocks.remove(lockId);
    }

    public void set(LockIdentifier lockId, int num) {
      //num can be equal 0, this means, that in origin file it is 0 and we should delete locks

      Integer size = mutableLocks.get(lockId);

      if (size == null) {
        size = 0;
      }
      if (num > size) {
        for (int i = 0; i < num - size; i++) {
          add(lockId);
        }
      } else if (num < size) {
        for (int i = 0; i < size - num; i++) {
          free(lockId);
        }
      }
    }

    public void restore(LockIdentifier lockId) {
      if (mutableToRestore == null) {
        return;
      }
      Integer size = mutableToRestore.locks.get(lockId);
      mutableLocks.remove(lockId);
      if (size != null) {
        mutableLocks.put(lockId, size);
      }
      isRestored = true;
    }

    public void restoreAll() {
      mutableLocks = mutableToRestore.locks;
    }

    public LockState build() {
      if (isFalseState) {
        return null;
      }
      if (isRestored) {
        mutableToRestore = mutableToRestore.toRestore;
      }
      if (locks.equals(mutableLocks) && mutableToRestore == toRestore) {
        return getParentLink();
      } else {
        return new LockState(mutableLocks, mutableToRestore);
      }
    }

    public LockState getOldState() {
      return getParentLink();
    }

    public void resetAll() {
      mutableLocks.clear();
    }

    public void reduce() {
      mutableToRestore = null;
    }

    public void reduceLocks(Set<LockIdentifier> usedLocks) {
      for (LockIdentifier lock : new HashSet<>(mutableLocks.keySet())) {
        if (usedLocks != null && !usedLocks.contains(lock)) {
          mutableLocks.remove(lock);
        }
      }
    }

    public void reduceLockCounters(Set<LockIdentifier> exceptLocks) {
      for (LockIdentifier lock : new HashSet<>(mutableLocks.keySet())) {
        if (!exceptLocks.contains(lock)) {
          mutableLocks.remove(lock);
          add(lock);
        }
      }
    }

    public void expand(LockState rootState) {
      mutableToRestore = rootState.toRestore;
    }

    public void expandLocks(LockState pRootState,  Set<LockIdentifier> usedLocks) {
      for (LockIdentifier lock : pRootState.locks.keySet()) {
        if (usedLocks != null && !usedLocks.contains(lock)) {
          mutableLocks.put(lock, pRootState.locks.get(lock));
        }
      }
    }

    public void expandLockCounters(LockState pRootState, Set<LockIdentifier> pRestrictedLocks) {
      for (LockIdentifier lock : pRootState.locks.keySet()) {
        if (!pRestrictedLocks.contains(lock)) {
          Integer size = mutableLocks.get(lock);
          Integer rootSize = pRootState.locks.get(lock);
          //null is also correct (it shows, that we've found new lock)

          Integer newSize;
          if (size == null) {
            newSize = rootSize - 1;
          } else {
            newSize = size + rootSize - 1;
          }
          if (newSize > 0) {
            mutableLocks.put(lock, newSize);
          } else {
            mutableLocks.remove(lock);
          }
        }
      }
    }

    public void setRestoreState() {
      mutableToRestore = getParentLink();
    }

    public void setAsFalseState() {
      isFalseState = true;
    }
  }

  private static final long serialVersionUID = -3152134511524554357L;

  private final SortedMap<LockIdentifier, Integer> locks;
  private final LockState toRestore;
  //if we need restore state, we save it here
  //Used for function annotations like annotate.function_name.restore
  public LockState() {
    locks = Maps.newTreeMap();
    toRestore = null;
  }

  private LockState(SortedMap<LockIdentifier, Integer> gLocks, LockState state) {
    this.locks  = Maps.newTreeMap(gLocks);
    toRestore = state;
  }

  public int getSize() {
    return locks.size();
  }

  public Map<LockIdentifier, Integer> getHashCodeForState() {
    //Special hash for BAM, in other cases use iterator
    return locks;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    for (LockIdentifier lock : Sets.newTreeSet(locks.keySet())) {
      sb.append(lock.toString() + "[" + locks.get(lock) + "]" + ", ");
    }
    if (locks.size() > 0) {
      sb.delete(sb.length() - 2, sb.length());
    } else {
      sb.append("Without locks");
    }
    return sb.toString();
  }

  public int getCounter(String lockName, String varName) {
    LockIdentifier lock = LockIdentifier.of(lockName, varName, LockType.GLOBAL_LOCK);
    return getCounter(lock);
  }

  public int getCounter(LockIdentifier lock) {
    Integer size = locks.get(lock);
    return (size == null ? 0 : size);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((locks == null) ? 0 : locks.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    LockState other = (LockState) obj;
    if (locks == null) {
      if (other.locks != null) {
        return false;
      }
    } else if (!locks.equals(other.locks)) {
      return false;
    }
    if (toRestore == null) {
      if (other.toRestore != null) {
        return false;
      }
    } else if (!toRestore.equals(other.toRestore)) {
      return false;
    }
    return true;
  }

  /**
   * This method decides if this element is less or equal than the other element, based on the order imposed by the lattice.
   *
   * @param other the other element
   * @return true, if this element is less or equal than the other element, based on the order imposed by the lattice
   */
  @Override
  public boolean isLessOrEqual(LockState other) {
    //State is less, if it has the same locks as the other and may be some more

    for (LockIdentifier lock : other.locks.keySet()) {
      if (!(this.locks.containsKey(lock))) {
        return false;
      }
    }
    return true;
  }

  /**
   * This method find the difference between two states in some metric.
   * It is useful for comparators. lock1.diff(lock2) <=> lock1 - lock2.
   * @param pOther The other LockStatisticsState
   * @return Difference between two states
   */
  @Override
  public int compareTo(CompatibleState pOther) {
    LockState other = (LockState) pOther;
    int result = 0;

    result = other.getSize() - this.getSize(); //decreasing queue

    if (result != 0) {
      return result;
    }

    Iterator<LockIdentifier> iterator1 = locks.keySet().iterator();
    Iterator<LockIdentifier> iterator2 = other.locks.keySet().iterator();
    //Sizes are equal
    while (iterator1.hasNext()) {
      LockIdentifier lockId1 = iterator1.next();
      LockIdentifier lockId2 = iterator2.next();
      result = lockId1.compareTo(lockId2);
      if (result != 0) {
        return result;
      }
      Integer Result = locks.get(lockId1) - other.locks.get(lockId1);
      if (Result != 0) {
        return Result;
      }
    }
    return 0;
  }

  @Override
  public boolean isCompatibleWith(CompatibleState state) {
    Preconditions.checkArgument(state.getClass() == LockState.class);
    LockState pLocks = (LockState) state;
    return !Sets.intersection(locks.keySet(), pLocks.locks.keySet()).isEmpty();
  }

  public LockStateBuilder builder() {
    return new LockStateBuilder(this);
  }

  private LockState getParentLink() {
    return this;
  }

  public List<LockEffect> getDifference(LockState other) {
    //Return the effect, which shows, what should we do to transform from this state to the other

    List<LockEffect> result = new LinkedList<>();
    Set<LockIdentifier> processedLocks = new TreeSet<>();

    for (LockIdentifier lockId : locks.keySet()) {
      int thisCounter = locks.get(lockId);
      int otherCounter = other.locks.containsKey(lockId) ? other.locks.get(lockId) : 0;
      if (thisCounter > otherCounter) {
        for (int i = 0; i < thisCounter - otherCounter; i++) {
          result.add(ReleaseLockEffect.createEffectForId(lockId));
        }
      } else if (thisCounter < otherCounter) {
        for (int i = 0; i <  otherCounter - thisCounter; i++) {
          result.add(AcquireLockEffect.createEffectForId(lockId));
        }
      }
      processedLocks.add(lockId);
    }
    for (LockIdentifier lockId : other.locks.keySet()) {
      if (!processedLocks.contains(lockId)) {
        for (int i = 0; i <  other.locks.get(lockId); i++) {
          result.add(AcquireLockEffect.createEffectForId(lockId));
        }
      }
    }
    return result;
  }

  @Override
  public CompatibleState prepareToStore() {
    return this;
  }

  @Override
  public UsageTreeNode getTreeNode() {
    return new LockTreeNode(locks.keySet());
  }

  @Override
  public LockState join(LockState pOther) {
    throw new UnsupportedOperationException("Operation join isn't supported for LockStatisticsCPA");
  }
}
