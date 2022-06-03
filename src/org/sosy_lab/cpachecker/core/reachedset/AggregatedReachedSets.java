// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.reachedset;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class AggregatedReachedSets {

  private AggregatedReachedSets() {}

  /** Return an empty immutable instance. */
  public static AggregatedReachedSets empty() {
    return new SimpleAggregatedReachedSets(ImmutableSet.of());
  }

  /** Return an immutable instance wrapping the given reached set. */
  public static AggregatedReachedSets singleton(UnmodifiableReachedSet pReachedSet) {
    return new SimpleAggregatedReachedSets(ImmutableSet.of(pReachedSet));
  }

  public abstract Set<UnmodifiableReachedSet> snapShot();

  private static class SimpleAggregatedReachedSets extends AggregatedReachedSets {
    private final ImmutableSet<UnmodifiableReachedSet> reachedSets;

    private SimpleAggregatedReachedSets(ImmutableSet<UnmodifiableReachedSet> pReachedSets) {
      reachedSets = pReachedSets;
    }

    @Override
    public Set<UnmodifiableReachedSet> snapShot() {
      return reachedSets;
    }
  }

  private static class AggregatedThreadedReachedSets extends AggregatedReachedSets {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @GuardedBy("lock")
    private final Set<UnmodifiableReachedSet> reachedSets = new LinkedHashSet<>();

    @GuardedBy("lock")
    private final List<AggregatedThreadedReachedSets> otherAggregators = new ArrayList<>();

    private AggregatedThreadedReachedSets() {}

    @Override
    public Set<UnmodifiableReachedSet> snapShot() {
      lock.readLock().lock();
      try {
        return ImmutableSet.<UnmodifiableReachedSet>builder()
            .addAll(reachedSets)
            .addAll(from(otherAggregators).transformAndConcat(AggregatedReachedSets::snapShot))
            .build();
      } finally {
        lock.readLock().unlock();
      }
    }

    private void addReachedSet(UnmodifiableReachedSet reached) {
      lock.writeLock().lock();
      try {
        reachedSets.add(reached);
      } finally {
        lock.writeLock().unlock();
      }
    }

    private void updateReachedSet(
        UnmodifiableReachedSet oldReached, UnmodifiableReachedSet newReached) {
      lock.writeLock().lock();
      try {
        reachedSets.remove(oldReached);
        reachedSets.add(newReached);
      } finally {
        lock.writeLock().unlock();
      }
    }

    private void addAggregated(AggregatedReachedSets pAggregatedReachedSets) {
      lock.writeLock().lock();

      try {
        if (pAggregatedReachedSets instanceof AggregatedThreadedReachedSets) {
          otherAggregators.add((AggregatedThreadedReachedSets) pAggregatedReachedSets);

        } else {
          reachedSets.addAll(pAggregatedReachedSets.snapShot());
        }

      } finally {
        lock.writeLock().unlock();
      }
    }
  }

  public static class AggregatedReachedSetManager {

    private final AggregatedThreadedReachedSets reachedView = new AggregatedThreadedReachedSets();

    public void addReachedSet(UnmodifiableReachedSet reached) {
      reachedView.addReachedSet(reached);
    }

    public void updateReachedSet(
        UnmodifiableReachedSet oldReached, UnmodifiableReachedSet newReached) {
      reachedView.updateReachedSet(oldReached, newReached);
    }

    public AggregatedReachedSets asView() {
      return reachedView;
    }

    public void addAggregated(AggregatedReachedSets pAggregatedReachedSets) {
      reachedView.addAggregated(pAggregatedReachedSets);
    }
  }
}
