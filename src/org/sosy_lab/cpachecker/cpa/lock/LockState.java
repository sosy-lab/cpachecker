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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.cpa.lock.effects.AcquireLockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.LockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.ReleaseLockEffect;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleNode;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleState;

public class LockState extends AbstractLockState {

  public static class LockTreeNode extends TreeSet<LockIdentifier> implements CompatibleNode {

    private static final long serialVersionUID = 5757759799394605077L;

    public LockTreeNode(Set<LockIdentifier> locks) {
      super(locks);
    }

    @Override
    public boolean isCompatibleWith(CompatibleState pState) {
      Preconditions.checkArgument(pState instanceof LockTreeNode);
      return Sets.intersection(this, (LockTreeNode) pState).isEmpty();
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
    public boolean cover(CompatibleNode pNode) {
      Preconditions.checkArgument(pNode instanceof LockTreeNode);
      LockTreeNode o = (LockTreeNode) pNode;
      return o.containsAll(this);
    }

    @Override
    public boolean hasEmptyLockSet() {
      return isEmpty();
    }
  }

  public class LockStateBuilder extends AbstractLockStateBuilder {
    private SortedMap<LockIdentifier, Integer> mutableLocks;

    public LockStateBuilder(LockState state) {
      super(state);
      mutableLocks = Maps.newTreeMap(state.locks);
    }

    @Override
    public void add(LockIdentifier lockId) {
      Integer a = mutableLocks.getOrDefault(lockId, 0) + 1;
      mutableLocks.put(lockId, a);
    }

    @Override
    public void free(LockIdentifier lockId) {
      if (mutableLocks.containsKey(lockId)) {
        Integer a = mutableLocks.get(lockId) - 1;
        if (a > 0) {
          mutableLocks.put(lockId, a);
        } else {
          mutableLocks.remove(lockId);
        }
      }
    }

    @Override
    public void reset(LockIdentifier lockId) {
      mutableLocks.remove(lockId);
    }

    @Override
    public void set(LockIdentifier lockId, int num) {
      // num can be equal 0, this means, that in origin file it is 0 and we should delete locks

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

    @Override
    public void restore(LockIdentifier lockId) {
      if (mutableToRestore == null) {
        return;
      }
      Integer size = ((LockState) mutableToRestore).locks.get(lockId);
      mutableLocks.remove(lockId);
      if (size != null) {
        mutableLocks.put(lockId, size);
      }
      isRestored = true;
    }

    @Override
    public void restoreAll() {
      mutableLocks = ((LockState) mutableToRestore).locks;
    }

    @Override
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
        return new LockState(mutableLocks, (LockState) mutableToRestore);
      }
    }

    @Override
    public LockState getOldState() {
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
      Set<LockIdentifier> reducableLocks =
          Sets.difference(new HashSet<>(mutableLocks.keySet()), exceptLocks);
      reducableLocks.forEach(
          l -> {
            mutableLocks.remove(l);
            add(l);
          });
    }

    public void expand(LockState rootState) {
      mutableToRestore = rootState.toRestore;
    }

    @Override
    public void expandLocks(LockState pRootState, Set<LockIdentifier> usedLocks) {
      if (usedLocks != null) {
        Set<LockIdentifier> expandableLocks = Sets.difference(pRootState.locks.keySet(), usedLocks);
        expandableLocks.forEach(l -> mutableLocks.put(l, pRootState.getCounter(l)));
      }
    }

    @Override
    public void expandLockCounters(LockState pRootState, Set<LockIdentifier> pRestrictedLocks) {
      for (LockIdentifier lock : pRootState.locks.keySet()) {
        if (!pRestrictedLocks.contains(lock)) {
          Integer size = mutableLocks.get(lock);
          Integer rootSize = pRootState.locks.get(lock);
          // null is also correct (it shows, that we've found new lock)

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
  // if we need restore state, we save it here
  // Used for function annotations like annotate.function_name.restore
  public LockState() {
    super();
    locks = Maps.newTreeMap();
  }

  protected LockState(SortedMap<LockIdentifier, Integer> gLocks, LockState state) {
    super(state);
    this.locks = Maps.newTreeMap(gLocks);
  }

  @Override
  public Map<LockIdentifier, Integer> getHashCodeForState() {
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
    return Objects.hashCode(locks);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    LockState other = (LockState) obj;
    return Objects.equals(toRestore, other.toRestore) && Objects.equals(locks, other.locks);
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
    LockState other = (LockState) pOther;
    int result = 0;

    result = other.getSize() - this.getSize(); // decreasing queue

    if (result != 0) {
      return result;
    }

    Iterator<Entry<LockIdentifier, Integer>> iterator1 = locks.entrySet().iterator();
    Iterator<Entry<LockIdentifier, Integer>> iterator2 = other.locks.entrySet().iterator();
    // Sizes are equal
    while (iterator1.hasNext()) {
      Entry<LockIdentifier, Integer> entry1 = iterator1.next();
      Entry<LockIdentifier, Integer> entry2 = iterator2.next();
      result = entry1.getKey().compareTo(entry2.getKey());
      if (result != 0) {
        return result;
      }
      Integer Result = entry1.getValue() - entry2.getValue();
      if (Result != 0) {
        return Result;
      }
    }
    return 0;
  }

  @Override
  public LockStateBuilder builder() {
    return new LockStateBuilder(this);
  }

  private LockState getParentLink() {
    return this;
  }

  @Override
  public List<LockEffect> getDifference(AbstractLockState pOther) {
    // Return the effect, which shows, what should we do to transform from this state to the other
    LockState other = (LockState) pOther;

    List<LockEffect> result = new ArrayList<>();
    Set<LockIdentifier> processedLocks = new TreeSet<>();

    for (Entry<LockIdentifier, Integer> entry : locks.entrySet()) {
      LockIdentifier lockId = entry.getKey();
      int thisCounter = entry.getValue();
      int otherCounter = other.locks.getOrDefault(lockId, 0);
      if (thisCounter > otherCounter) {
        for (int i = 0; i < thisCounter - otherCounter; i++) {
          result.add(ReleaseLockEffect.createEffectForId(lockId));
        }
      } else if (thisCounter < otherCounter) {
        for (int i = 0; i < otherCounter - thisCounter; i++) {
          result.add(AcquireLockEffect.createEffectForId(lockId));
        }
      }
      processedLocks.add(lockId);
    }
    for (Entry<LockIdentifier, Integer> entry : other.locks.entrySet()) {
      LockIdentifier lockId = entry.getKey();
      if (!processedLocks.contains(lockId)) {
        for (int i = 0; i < entry.getValue(); i++) {
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
  public CompatibleNode getTreeNode() {
    return new LockTreeNode(locks.keySet());
  }

  @Override
  protected Set<LockIdentifier> getLocks() {
    return locks.keySet();
  }
}
