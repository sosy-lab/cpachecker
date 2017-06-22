/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.loopbound;

import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

public interface LoopIterationState {

  LoopIterationState visitLoopHead(LoopEntry pLoopEntry);

  int getMaxIterationCount();

  int getLoopIterationCount(Loop pLoop);

  Set<Loop> getDeepestIterationLoops();

  boolean isEntryKnown();

  LoopEntry getLoopEntry();

  public static class UndeterminedLoopIterationState implements LoopIterationState {

    private final PersistentSortedMap<ComparableLoop, LoopIteration> iterations;

    private final int maxLoopIteration;

    private UndeterminedLoopIterationState() {
      iterations = PathCopyingPersistentTreeMap.<ComparableLoop, LoopIteration>of();
      maxLoopIteration = 0;
    }

    private UndeterminedLoopIterationState(PersistentSortedMap<ComparableLoop, LoopIteration> pIterations, int pMaxLoopIteration) {
      iterations = Objects.requireNonNull(pIterations);
      maxLoopIteration = pMaxLoopIteration;
    }

    @Override
    public LoopIterationState visitLoopHead(LoopEntry pLoopEntry) {
      ComparableLoop loop = new ComparableLoop(pLoopEntry.getLoop());
      LoopIteration storedIteration = iterations.getOrDefault(
          loop,
          new LoopIteration(pLoopEntry.getEntryPoint(), 0));
      if (storedIteration.getLoopEntryPoint().equals(pLoopEntry.getEntryPoint())) {
        storedIteration = storedIteration.increment();
        return new UndeterminedLoopIterationState(
            iterations.putAndCopy(loop, storedIteration),
            Math.max(storedIteration.getCount(), maxLoopIteration));
      }
      return this;
    }

    @Override
    public int getMaxIterationCount() {
      return maxLoopIteration;
    }

    @Override
    public String toString() {
      return String.format("Undetermined loop iteration state; at least %d iterations in some loop (%s).", maxLoopIteration, iterations);
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }
      if (pObj instanceof UndeterminedLoopIterationState) {
        UndeterminedLoopIterationState other = (UndeterminedLoopIterationState) pObj;
        return maxLoopIteration == other.maxLoopIteration
            && iterations.equals(other.iterations);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(maxLoopIteration, iterations);
    }

    @Override
    public boolean isEntryKnown() {
      return false;
    }

    @Override
    public LoopEntry getLoopEntry() {
      throw new IllegalStateException("Loop entry is unknown.");
    }

    @Override
    public int getLoopIterationCount(Loop pLoop) {
      ComparableLoop loop = new ComparableLoop(pLoop);
      LoopIteration iteration = iterations.get(loop);
      if (iteration == null) {
        return 0;
      }
      return iteration.getCount();
    }

    @Override
    public Set<Loop> getDeepestIterationLoops() {
      ImmutableSet.Builder<Loop> builder = ImmutableSet.builder();
      for (Map.Entry<ComparableLoop, LoopIteration> entry : iterations.entrySet()) {
        int count = entry.getValue().getCount();
        assert count <= maxLoopIteration;
        if (count == maxLoopIteration) {
          builder.add(entry.getKey().getLoop());
        }
      }
      return builder.build();
    }

    public static LoopIterationState newState() {
      return new UndeterminedLoopIterationState();
    }

    private static class ComparableLoop implements Comparable<ComparableLoop> {

      private final Loop loop;

      public ComparableLoop(Loop pLoop) {
        this.loop = Objects.requireNonNull(pLoop);
      }

      public Loop getLoop() {
        return loop;
      }

      @Override
      public String toString() {
        return loop.toString();
      }

      @Override
      public int hashCode() {
        return loop.hashCode();
      }

      @Override
      public boolean equals(Object pObj) {
        if (this == pObj) {
          return true;
        }
        if (pObj instanceof ComparableLoop) {
          ComparableLoop other = (ComparableLoop) pObj;
          return loop.equals(other.loop);
        }
        return false;
      }

      @Override
      public int compareTo(ComparableLoop pOther) {
        return ComparisonChain.start()
            // Compare by size
            .compare(getLoop().getLoopNodes().size(), pOther.getLoop().getLoopNodes().size())
            // Compare lexicographically by contained nodes
            .compare(getLoop().getLoopNodes(), pOther.getLoop().getLoopNodes(),
                Ordering.<CFANode>natural()
                .lexicographical()).result();
      }
    }

    private static class LoopIteration {

      private final CFANode loopEntryPoint;

      private final int iteration;

      public LoopIteration(CFANode pLoopEntryPoint, int pIteration) {
        loopEntryPoint = Objects.requireNonNull(pLoopEntryPoint);
        Preconditions.checkArgument(pIteration >= 0);
        iteration = pIteration;
      }

      public CFANode getLoopEntryPoint() {
        return loopEntryPoint;
      }

      public int getCount() {
        return iteration;
      }

      public LoopIteration increment() {
        return new LoopIteration(loopEntryPoint, getCount() + 1);
      }

      @Override
      public String toString() {
        return String.format("%d through %s", iteration, loopEntryPoint);
      }

      @Override
      public int hashCode() {
        return Objects.hash(iteration, loopEntryPoint);
      }

      @Override
      public boolean equals(Object pObj) {
        if (this == pObj) {
          return true;
        }
        if (pObj instanceof LoopIteration) {
          LoopIteration other = (LoopIteration) pObj;
          return iteration == other.iteration
              && loopEntryPoint.equals(other.loopEntryPoint);
        }
        return false;
      }

    }
  }

  public static class DeterminedLoopIterationState implements LoopIterationState {

    private final LoopEntry loopEntry;

    private final int iteration;

    private DeterminedLoopIterationState(LoopEntry pLoopEntry) {
      this(pLoopEntry, 0);
    }

    private DeterminedLoopIterationState(LoopEntry pLoopEntry, int pIteration) {
      loopEntry = Objects.requireNonNull(pLoopEntry);
      Preconditions.checkArgument(pIteration >= 0);
      iteration = pIteration;
    }

    @Override
    public String toString() {
      return loopEntry.toString() + ": " + iteration;
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }
      if (pObj instanceof DeterminedLoopIterationState) {
        DeterminedLoopIterationState other = (DeterminedLoopIterationState) pObj;
        return iteration == other.iteration
            && loopEntry.equals(other.loopEntry);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(iteration, loopEntry);
    }

    @Override
    public LoopIterationState visitLoopHead(LoopEntry pLoopEntry) {
      if (!getLoopEntry().equals(loopEntry)) {
        return this;
      }
      return new DeterminedLoopIterationState(loopEntry, iteration + 1);
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
    public LoopEntry getLoopEntry() {
      return loopEntry;
    }

    @Override
    public int getLoopIterationCount(Loop pLoop) {
      return loopEntry.getLoop().equals(pLoop) ? iteration : 0;
    }

    @Override
    public Set<Loop> getDeepestIterationLoops() {
      return Collections.singleton(loopEntry.getLoop());
    }

    public static LoopIterationState newState(LoopEntry pLoopEntry) {
      return new DeterminedLoopIterationState(pLoopEntry);
    }

  }

}
