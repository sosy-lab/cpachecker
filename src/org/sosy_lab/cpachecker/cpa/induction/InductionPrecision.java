// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.induction;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Objects;
import java.util.function.UnaryOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

/** The precision of {@link InductionCPA}: how the analysis unrolls and abstracts every loop. */
public final class InductionPrecision implements Precision {

  /**
   * How the analysis treats one loop.
   *
   * @param k the number of arrivals at a head of the loop after which the next arrival abstracts
   * @param induction whether an abstraction of the loop in the base case starts an induction step
   * @param describedAbstractions the number of abstractions of a loop head that the predicate
   *     precision describes, or {@link #ALL_ABSTRACTIONS}. The analysis cuts a path off at the
   *     first abstraction that is not described.
   * @param unrolling whether the abstractions of the loop never cover each other
   * @param concreteFirstArrival whether the first arrival at the loop before any abstraction on the
   *     path does not count, so that the initial states of the loop stay concrete, as in native IMC
   *     and ISMC
   */
  public record LoopPrecision(
      int k,
      boolean induction,
      int describedAbstractions,
      boolean unrolling,
      boolean concreteFirstArrival) {

    public static final int ALL_ABSTRACTIONS = -1;

    public LoopPrecision {
      checkArgument(k >= 0, "k must not be negative, but is %s", k);
      // with k = 0, the induction hypothesis is empty
      checkArgument(!induction || k >= 1, "an induction needs k >= 1");
      checkArgument(describedAbstractions >= ALL_ABSTRACTIONS);
      checkArgument(
          !(induction && concreteFirstArrival), "an induction counts every arrival at the loop");
    }

    /** The arrivals after which the next one abstracts, given whether no abstraction preceded. */
    public int bound(boolean pBeforeFirstAbstraction) {
      return concreteFirstArrival && pBeforeFirstAbstraction ? k + 1 : k;
    }

    boolean describes(int pAbstractionIndex) {
      return describedAbstractions == ALL_ABSTRACTIONS || pAbstractionIndex < describedAbstractions;
    }

    public LoopPrecision withK(int pK) {
      return new LoopPrecision(
          pK, induction, describedAbstractions, unrolling, concreteFirstArrival);
    }

    public LoopPrecision withInduction(boolean pInduction) {
      return new LoopPrecision(
          k, pInduction, describedAbstractions, unrolling, concreteFirstArrival);
    }

    public LoopPrecision withDescribedAbstractions(int pDescribedAbstractions) {
      return new LoopPrecision(
          k, induction, pDescribedAbstractions, unrolling, concreteFirstArrival);
    }

    public LoopPrecision withUnrolling(boolean pUnrolling) {
      return new LoopPrecision(
          k, induction, describedAbstractions, pUnrolling, concreteFirstArrival);
    }
  }

  private final ImmutableMap<Loop, LoopPrecision> loops;

  private InductionPrecision(ImmutableMap<Loop, LoopPrecision> pLoops) {
    loops = pLoops;
  }

  static InductionPrecision uniform(Collection<Loop> pLoops, LoopPrecision pPrecision) {
    return new InductionPrecision(Maps.toMap(pLoops, loop -> pPrecision));
  }

  public LoopPrecision get(Loop pLoop) {
    LoopPrecision precision = loops.get(pLoop);
    checkArgument(precision != null, "No precision for loop %s", pLoop);
    return precision;
  }

  public ImmutableMap<Loop, LoopPrecision> getLoops() {
    return loops;
  }

  public InductionPrecision with(Loop pLoop, LoopPrecision pPrecision) {
    checkArgument(loops.containsKey(pLoop), "No precision for loop %s", pLoop);
    return new InductionPrecision(
        ImmutableMap.copyOf(
            Maps.transformEntries(
                loops, (loop, precision) -> loop.equals(pLoop) ? pPrecision : precision)));
  }

  public InductionPrecision withAll(UnaryOperator<LoopPrecision> pChange) {
    return new InductionPrecision(ImmutableMap.copyOf(Maps.transformValues(loops, pChange::apply)));
  }

  @Override
  public boolean equals(Object pOther) {
    return pOther instanceof InductionPrecision other && loops.equals(other.loops);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(loops);
  }

  @Override
  public String toString() {
    return "InductionPrecision" + loops;
  }
}
