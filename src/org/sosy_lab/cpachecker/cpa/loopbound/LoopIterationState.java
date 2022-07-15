// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopbound;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

interface LoopIterationState {

  LoopIterationState visitLoopHead(Loop pLoop);

  int getMaxIterationCount();

  int getLoopIterationCount(Loop pLoop);

  Set<Loop> getDeepestIterationLoops();

  boolean isEntryKnown();

  Loop getLoop();

  boolean isLoopCounterAbstracted();

  LoopIterationState enforceAbstraction(int pLoopIterationsBeforeAbstraction);

  public static class UndeterminedLoopIterationState implements LoopIterationState {

    private final PersistentSortedMap<Loop, LoopIteration> iterations;

    private final int maxLoopIteration;

    private final boolean loopCounterAbstracted;

    private UndeterminedLoopIterationState() {
      this(PathCopyingPersistentTreeMap.of(), 0, false);
    }

    private UndeterminedLoopIterationState(
        PersistentSortedMap<Loop, LoopIteration> pIterations,
        int pMaxLoopIteration,
        boolean pLoopCounterAbstracted) {
      iterations = Objects.requireNonNull(pIterations);
      Preconditions.checkArgument(pMaxLoopIteration >= 0);
      maxLoopIteration = pMaxLoopIteration;
      loopCounterAbstracted = pLoopCounterAbstracted;
    }

    @Override
    public LoopIterationState visitLoopHead(Loop pLoop) {
      Loop loop = pLoop;
      LoopIteration storedIteration = iterations.getOrDefault(loop, new LoopIteration(pLoop, 0));
      if (loop.equals(storedIteration.getLoop())) {
        storedIteration = storedIteration.increment();
        return new UndeterminedLoopIterationState(
            iterations.putAndCopy(loop, storedIteration),
            Math.max(storedIteration.getCount(), maxLoopIteration),
            loopCounterAbstracted);
      }
      return this;
    }

    @Override
    public int getMaxIterationCount() {
      return maxLoopIteration;
    }

    @Override
    public String toString() {
      return String.format(
          "Undetermined loop iteration state; at least %d iterations in some loop (%s).",
          maxLoopIteration, iterations);
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }
      if (pObj instanceof UndeterminedLoopIterationState) {
        UndeterminedLoopIterationState other = (UndeterminedLoopIterationState) pObj;
        return loopCounterAbstracted == other.loopCounterAbstracted
            && maxLoopIteration == other.maxLoopIteration
            && iterations.equals(other.iterations);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(loopCounterAbstracted, maxLoopIteration, iterations);
    }

    @Override
    public boolean isEntryKnown() {
      return false;
    }

    @Override
    public Loop getLoop() {
      throw new IllegalStateException("Loop is unknown.");
    }

    @Override
    public int getLoopIterationCount(Loop pLoop) {
      LoopIteration iteration = iterations.get(pLoop);
      if (iteration == null) {
        return 0;
      }
      return iteration.getCount();
    }

    @Override
    public Set<Loop> getDeepestIterationLoops() {
      ImmutableSet.Builder<Loop> builder = ImmutableSet.builder();
      for (Map.Entry<Loop, LoopIteration> entry : iterations.entrySet()) {
        int count = entry.getValue().getCount();
        assert count <= maxLoopIteration;
        if (count == maxLoopIteration) {
          builder.add(entry.getKey());
        }
      }
      return builder.build();
    }

    @Override
    public boolean isLoopCounterAbstracted() {
      return loopCounterAbstracted;
    }

    @Override
    public LoopIterationState enforceAbstraction(int pLoopIterationsBeforeAbstraction) {
      if (getMaxIterationCount() <= pLoopIterationsBeforeAbstraction) {
        return this;
      }
      PersistentSortedMap<Loop, LoopIteration> iters = iterations;
      for (Map.Entry<Loop, LoopIteration> entry : iters.entrySet()) {
        Loop loop = entry.getKey();
        LoopIteration oldIterationCount = entry.getValue();
        if (oldIterationCount.getCount() > pLoopIterationsBeforeAbstraction) {
          iters =
              iters.putAndCopy(
                  loop,
                  new LoopIteration(oldIterationCount.getLoop(), pLoopIterationsBeforeAbstraction));
        }
      }
      return new UndeterminedLoopIterationState(iters, pLoopIterationsBeforeAbstraction, true);
    }

    public static LoopIterationState newState() {
      return new UndeterminedLoopIterationState();
    }

    private static class LoopIteration {

      private final Loop loop;

      private final int iteration;

      public LoopIteration(Loop pLoop, int pIteration) {
        loop = Objects.requireNonNull(pLoop);
        Preconditions.checkArgument(pIteration >= 0);
        iteration = pIteration;
      }

      public Loop getLoop() {
        return loop;
      }

      public int getCount() {
        return iteration;
      }

      public LoopIteration increment() {
        return new LoopIteration(loop, getCount() + 1);
      }

      @Override
      public String toString() {
        return String.format("%d through %s", iteration, loop);
      }

      @Override
      public int hashCode() {
        return Objects.hash(iteration, loop);
      }

      @Override
      public boolean equals(Object pObj) {
        if (this == pObj) {
          return true;
        }
        if (pObj instanceof LoopIteration) {
          LoopIteration other = (LoopIteration) pObj;
          return iteration == other.iteration && loop.equals(other.loop);
        }
        return false;
      }
    }
  }

  public static class DeterminedLoopIterationState implements LoopIterationState {

    private final Loop loop;

    private final int iteration;

    private final boolean loopCounterAbstracted;

    private DeterminedLoopIterationState(Loop pLoop) {
      this(pLoop, 0, false);
    }

    private DeterminedLoopIterationState(
        Loop pLoop, int pIteration, boolean pLoopCounterAbstracted) {
      loop = Objects.requireNonNull(pLoop);
      Preconditions.checkArgument(pIteration >= 0);
      iteration = pIteration;
      loopCounterAbstracted = pLoopCounterAbstracted;
    }

    @Override
    public String toString() {
      return loop + " in iteration " + getMaxIterationCount();
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }
      if (pObj instanceof DeterminedLoopIterationState) {
        DeterminedLoopIterationState other = (DeterminedLoopIterationState) pObj;
        return loopCounterAbstracted == other.loopCounterAbstracted
            && iteration == other.iteration
            && loop.equals(other.loop);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(loopCounterAbstracted, iteration, loop);
    }

    @Override
    public LoopIterationState visitLoopHead(Loop pLoop) {
      if (!getLoop().equals(loop)) {
        return this;
      }
      return new DeterminedLoopIterationState(loop, iteration + 1, loopCounterAbstracted);
    }

    @Override
    public int getMaxIterationCount() {
      return iteration;
    }

    @Override
    public boolean isEntryKnown() {
      return true;
    }

    @Override
    public Loop getLoop() {
      return loop;
    }

    @Override
    public int getLoopIterationCount(Loop pLoop) {
      return loop.equals(pLoop) ? iteration : 0;
    }

    @Override
    public Set<Loop> getDeepestIterationLoops() {
      return Collections.singleton(loop);
    }

    public static LoopIterationState newState(Loop pLoop) {
      return new DeterminedLoopIterationState(pLoop);
    }

    @Override
    public boolean isLoopCounterAbstracted() {
      return loopCounterAbstracted;
    }

    @Override
    public LoopIterationState enforceAbstraction(int pLoopIterationsBeforeAbstraction) {
      if (getMaxIterationCount() <= pLoopIterationsBeforeAbstraction) {
        return this;
      }
      return new DeterminedLoopIterationState(loop, pLoopIterationsBeforeAbstraction, true);
    }
  }
}
