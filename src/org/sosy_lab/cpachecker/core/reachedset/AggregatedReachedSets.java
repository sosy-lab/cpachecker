// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.reachedset;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AggregatedReachedSets {
  protected final Set<UnmodifiableReachedSet> reachedSets;

  public AggregatedReachedSets() {
    reachedSets = ImmutableSet.of();
  }

  public AggregatedReachedSets(Set<UnmodifiableReachedSet> pReachedSets) {
    reachedSets = checkNotNull(pReachedSets);
  }

  public Set<UnmodifiableReachedSet> snapShot() {
    synchronized (reachedSets) {
      return ImmutableSet.copyOf(reachedSets);
    }
  }

  private static class AggregatedThreadedReachedSets extends AggregatedReachedSets {
    private final ReentrantReadWriteLock lock;
    private final List<AggregatedThreadedReachedSets> otherAggregators = new ArrayList<>();

    private AggregatedThreadedReachedSets(
        final ReentrantReadWriteLock pLock, Set<UnmodifiableReachedSet> pReachedSets) {
      super(pReachedSets);
      lock = pLock;
    }

    @Override
    public Set<UnmodifiableReachedSet> snapShot() {
      lock.readLock().lock();
      try {
        return Sets.union(
            super.snapShot(),
            from(otherAggregators).transformAndConcat(AggregatedReachedSets::snapShot).toSet());
      } finally {
        lock.readLock().unlock();
      }
    }

    public void concat(AggregatedThreadedReachedSets other) {
      otherAggregators.add(other);
    }
  }

  public static class AggregatedReachedSetManager {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final AggregatedThreadedReachedSets reachedView;
    private final Set<UnmodifiableReachedSet> reachedSets = ConcurrentHashMap.newKeySet();

    public AggregatedReachedSetManager() {
      reachedView = new AggregatedThreadedReachedSets(lock, reachedSets);
    }

    public void addReachedSet(UnmodifiableReachedSet reached) {
      lock.writeLock().lock();
      try {
        reachedSets.add(reached);
      } finally {
        lock.writeLock().unlock();
      }
    }

    public void updateReachedSet(
        UnmodifiableReachedSet oldReached, UnmodifiableReachedSet newReached) {
      lock.writeLock().lock();
      try {
        reachedSets.remove(oldReached);
        reachedSets.add(newReached);
      } finally {
        lock.writeLock().unlock();
      }
    }

    public AggregatedReachedSets asView() {
      return reachedView;
    }

    public synchronized void addAggregated(AggregatedReachedSets pAggregatedReachedSets) {
      lock.writeLock().lock();

      try {
        if (pAggregatedReachedSets instanceof AggregatedThreadedReachedSets) {
          reachedView.concat((AggregatedThreadedReachedSets) pAggregatedReachedSets);

        } else {
          reachedSets.addAll(pAggregatedReachedSets.reachedSets);
        }

      } finally {
        lock.writeLock().unlock();
      }
    }
  }
}
