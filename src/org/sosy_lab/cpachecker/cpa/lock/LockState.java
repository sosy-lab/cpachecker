// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.lock;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Comparators;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.cpa.lock.effects.AcquireLockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.LockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.ReleaseLockEffect;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleNode;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleState;

public final class LockState extends AbstractLockState {

  @SuppressWarnings("checkstyle:IllegalType") // TODO: use composition instead of inheritance
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
    public int compareTo(CompatibleState pOther) {
      Preconditions.checkArgument(pOther instanceof LockTreeNode);
      LockTreeNode o = (LockTreeNode) pOther;
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

      // empty locks do not cover all others (special case
      if (isEmpty()) {
        return o.isEmpty();
      } else {
        return o.containsAll(this);
      }
    }

    @Override
    public boolean hasEmptyLockSet() {
      return isEmpty();
    }
  }

  public class LockStateBuilder extends AbstractLockStateBuilder {
    private Map<LockIdentifier, Integer> mutableLocks;
    private boolean changed;

    public LockStateBuilder(LockState state) {
      super(state);
      mutableLocks = state.locks;
      changed = false;
    }

    public void cloneIfNecessary() {
      if (!changed) {
        changed = true;
        mutableLocks = new TreeMap<>(mutableLocks);
      }
    }

    @Override
    public void add(LockIdentifier lockId) {
      cloneIfNecessary();
      Integer a = mutableLocks.getOrDefault(lockId, 0) + 1;
      mutableLocks.put(lockId, a);
    }

    @Override
    public void free(LockIdentifier lockId) {
      if (mutableLocks.containsKey(lockId)) {
        Integer a = mutableLocks.get(lockId) - 1;
        cloneIfNecessary();
        if (a > 0) {
          mutableLocks.put(lockId, a);
        } else {
          mutableLocks.remove(lockId);
        }
      }
    }

    @Override
    public void reset(LockIdentifier lockId) {
      cloneIfNecessary();
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
      cloneIfNecessary();
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
        return LockState.this;
      } else {
        return new LockState(mutableLocks, (LockState) mutableToRestore);
      }
    }

    @Override
    public LockState getOldState() {
      return LockState.this;
    }

    @Override
    public void resetAll() {
      cloneIfNecessary();
      mutableLocks.clear();
    }

    @Override
    public void reduce(Set<LockIdentifier> removeCounters, Set<LockIdentifier> totalRemove) {
      mutableToRestore = null;
      assert Sets.intersection(removeCounters, totalRemove).isEmpty();
      cloneIfNecessary();
      removeCounters.forEach(l -> mutableLocks.replace(l, 1));
      Iterator<Entry<LockIdentifier, Integer>> iterator = mutableLocks.entrySet().iterator();
      while (iterator.hasNext()) {
        LockIdentifier lockId = iterator.next().getKey();
        if (totalRemove.contains(lockId)) {
          iterator.remove();
        }
      }
    }

    @Override
    public void expand(
        AbstractLockState rootState,
        Set<LockIdentifier> expandCounters,
        Set<LockIdentifier> totalExpand) {
      mutableToRestore = rootState.toRestore;
      assert Sets.intersection(expandCounters, totalExpand).isEmpty();
      cloneIfNecessary();
      Map<LockIdentifier, Integer> rootLocks = ((LockState) rootState).locks;
      for (LockIdentifier lock : expandCounters) {
        if (rootLocks.containsKey(lock)) {
          Integer size = mutableLocks.get(lock);
          Integer rootSize = rootLocks.get(lock);
          cloneIfNecessary();
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
      for (LockIdentifier lockId : totalExpand) {
        if (rootLocks.containsKey(lockId)) {
          mutableLocks.put(lockId, rootLocks.get(lockId));
        }
      }
    }

    @Override
    public void setRestoreState() {
      mutableToRestore = LockState.this;
    }

    @Override
    public void setAsFalseState() {
      isFalseState = true;
    }
  }

  private static final Comparator<Iterable<Entry<LockIdentifier, Integer>>> LOCKS_COMPARATOR =
      Comparators.lexicographical(
          Entry.<LockIdentifier, Integer>comparingByKey().thenComparing(Entry.comparingByValue()));

  private final ImmutableMap<LockIdentifier, Integer> locks;
  // if we need restore state, we save it here
  // Used for function annotations like annotate.function_name.restore
  public LockState() {
    locks = ImmutableMap.of();
  }

  LockState(Map<LockIdentifier, Integer> gLocks, LockState state) {
    super(state);
    locks = ImmutableMap.copyOf(gLocks);
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
    return ComparisonChain.start()
        .compare(other.getSize(), getSize()) // decreasing queue
        .compare(locks.entrySet(), other.locks.entrySet(), LOCKS_COMPARATOR) // Sizes are equal
        .result();
  }

  @Override
  public LockStateBuilder builder() {
    return new LockStateBuilder(this);
  }

  @Override
  public Multiset<LockEffect> getDifference(AbstractLockState pOther) {
    // Return the effect, which shows, what should we do to transform from this state to the other
    LockState other = (LockState) pOther;

    Multiset<LockEffect> result = HashMultiset.create();
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
  public CompatibleNode getCompatibleNode() {
    return new LockTreeNode(locks.keySet());
  }

  @Override
  protected Set<LockIdentifier> getLocks() {
    return locks.keySet();
  }

  @Override
  public boolean isLessOrEqual(AbstractLockState other) {
    // State is less, if it has the same locks as the other and may be some more

    for (LockIdentifier lock : other.getLocks()) {
      if (!locks.containsKey(lock)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public AbstractLockState join(AbstractLockState pOther) {
    Map<LockIdentifier, Integer> overlappedMap = new TreeMap<>();
    Map<LockIdentifier, Integer> otherMap = ((LockState) pOther).locks;

    for (Entry<LockIdentifier, Integer> entry : locks.entrySet()) {
      LockIdentifier id = entry.getKey();
      Integer value = entry.getValue();
      if (otherMap.containsKey(id)) {
        Integer otherVal = otherMap.get(id);
        overlappedMap.put(id, Integer.min(value, otherVal));
      }
    }
    return new LockState(overlappedMap, (LockState) toRestore);
  }
}
