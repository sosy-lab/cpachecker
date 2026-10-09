// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.induction;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.induction.InductionPrecision.LoopPrecision;
import org.sosy_lab.cpachecker.cpa.induction.InductionState.Resets;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;

public class InductionTransferRelationTest {

  private LoopStructure loopStructure;
  private InductionTransferRelation transferRelation;
  private InductionState initial;

  /** The edge that enters the loop head from outside and the one that returns to it. */
  private CFAEdge entry;

  private CFAEdge back;

  /** An edge that leaves the loop head into the body. */
  private CFAEdge body;

  @Before
  public void setUp() throws Exception {
    CFA cfa =
        TestCfaUtils.makeCfaFromFunctionBody(
            """
            int x = 0;
            while (x < 10) {
              x++;
            }
            """);
    loopStructure = cfa.getLoopStructure().orElseThrow();
    Loop loop = getOnlyElement(loopStructure.getAllLoops());
    CFANode head = getOnlyElement(loop.getLoopHeads());
    entry = getOnlyElement(loop.getIncomingEdges());
    back =
        head.getEnteringEdges()
            .filter(e -> loop.getLoopNodes().contains(e.getPredecessor()))
            .first()
            .get();
    body =
        head.getLeavingEdges()
            .filter(e -> loop.getLoopNodes().contains(e.getSuccessor()))
            .first()
            .get();
    transferRelation = new InductionTransferRelation(loopStructure, true, false, false);
    initial =
        InductionState.initial(new Resets(ImmutableSet.of(), ImmutableSet.of(), false), false);
  }

  private InductionPrecision precision(int pK, boolean pInduction, int pDescribedAbstractions) {
    return InductionPrecision.uniform(
        loopStructure.getAllLoops(),
        new LoopPrecision(pK, pInduction, pDescribedAbstractions, false, false));
  }

  private Collection<InductionState> successors(
      InductionState pState, InductionPrecision pPrecision, CFAEdge pEdge) {
    return transferRelation.getAbstractSuccessorsForEdge(pState, pPrecision, pEdge);
  }

  private InductionState successor(
      InductionState pState, InductionPrecision pPrecision, CFAEdge pEdge) {
    return successor(pState, pPrecision, pEdge, transferRelation);
  }

  private static InductionState successor(
      InductionState pState,
      InductionPrecision pPrecision,
      CFAEdge pEdge,
      InductionTransferRelation pTransferRelation) {
    return getOnlyElement(
        pTransferRelation.getAbstractSuccessorsForEdge(pState, pPrecision, pEdge));
  }

  @Test
  public void abstractsAtEveryArrivalWithKZero() {
    InductionPrecision precision = precision(0, false, LoopPrecision.ALL_ABSTRACTIONS);
    InductionState first = successor(initial, precision, entry);
    assertThat(first.isAbstraction()).isTrue();
    assertThat(first.isInductionStart()).isFalse();
    assertThat(successor(successor(first, precision, body), precision, back).isAbstraction())
        .isTrue();
  }

  @Test
  public void keepsTheFirstArrivalConcrete() {
    InductionPrecision precision =
        InductionPrecision.uniform(
            loopStructure.getAllLoops(),
            new LoopPrecision(0, false, LoopPrecision.ALL_ABSTRACTIONS, false, true));
    InductionState first = successor(initial, precision, entry);
    assertThat(first.isAbstraction()).isFalse();
    InductionState second = successor(successor(first, precision, body), precision, back);
    assertThat(second.isAbstraction()).isTrue();
    assertThat(successor(successor(second, precision, body), precision, back).isAbstraction())
        .isTrue();
  }

  @Test
  public void keepsTheCountersOfOtherLoops() throws Exception {
    CFA cfa =
        TestCfaUtils.makeCfaFromFunctionBody(
            """
            int x = 0;
            while (x < 10) {
              int y = 0;
              while (y < 10) {
                y++;
              }
              x++;
            }
            """);
    LoopStructure loops = cfa.getLoopStructure().orElseThrow();
    Loop outer =
        from(loops.getAllLoops())
            .firstMatch(l -> from(loops.getAllLoops()).anyMatch(o -> o != l && l.isOuterLoopOf(o)))
            .get();
    Loop inner = from(loops.getAllLoops()).firstMatch(l -> l != outer).get();
    CFAEdge outerEntry = getOnlyElement(outer.getIncomingEdges());
    CFAEdge outerBack = backEdge(outer);
    CFAEdge innerEntry = getOnlyElement(inner.getIncomingEdges());
    CFAEdge innerBack = backEdge(inner);
    InductionTransferRelation transfer = new InductionTransferRelation(loops, true, false, false);
    InductionPrecision precision =
        InductionPrecision.uniform(
            loops.getAllLoops(),
            new LoopPrecision(1, true, LoopPrecision.ALL_ABSTRACTIONS, false, false));

    InductionState state = successor(initial, precision, outerEntry, transfer);
    state = successor(state, precision, innerEntry, transfer);
    InductionState start =
        ImmutableList.copyOf(transfer.getAbstractSuccessorsForEdge(state, precision, innerBack))
            .get(1);
    assertThat(start.isInductionStart()).isTrue();
    assertThat(start.getArrivals()).isEmpty();

    state = successor(start, precision, outerBack, transfer);
    state = successor(state, precision, innerEntry, transfer);
    InductionState abstraction = successor(state, precision, innerBack, transfer);
    assertThat(abstraction.isAbstraction()).isTrue();
    assertThat(abstraction.isInductionStart()).isFalse();
    // the outer loop still counts towards the arrival that checks the property
    assertThat(abstraction.getArrivals()).containsExactly(outer, 1);

    // also outside an induction step, an abstraction of the inner loop keeps the outer count
    InductionPrecision innerAbstracts =
        precision.with(
            inner, new LoopPrecision(0, false, LoopPrecision.ALL_ABSTRACTIONS, false, false));
    InductionState baseCase =
        successor(
            successor(initial, innerAbstracts, outerEntry, transfer),
            innerAbstracts,
            innerEntry,
            transfer);
    assertThat(baseCase.isAbstraction()).isTrue();
    assertThat(baseCase.isInInductionStep()).isFalse();
    assertThat(baseCase.getArrivals()).containsExactly(outer, 1);
  }

  private static CFAEdge backEdge(Loop pLoop) {
    CFANode head = getOnlyElement(pLoop.getLoopHeads());
    return head.getEnteringEdges()
        .filter(e -> pLoop.getLoopNodes().contains(e.getPredecessor()))
        .first()
        .get();
  }

  @Test
  public void splitsAtTheArrivalAfterK() {
    InductionPrecision precision = precision(1, true, LoopPrecision.ALL_ABSTRACTIONS);
    InductionState first = successor(initial, precision, entry);
    assertThat(first.isAbstraction()).isFalse();

    ImmutableList<InductionState> split =
        ImmutableList.copyOf(successors(successor(first, precision, body), precision, back));
    assertThat(split).hasSize(2);
    InductionState continuation = split.get(0);
    InductionState inductionStart = split.get(1);
    assertThat(continuation.isAbstraction()).isFalse();
    assertThat(continuation.isInInductionStep()).isFalse();
    assertThat(inductionStart.isInductionStart()).isTrue();
    assertThat(inductionStart.isInInductionStep()).isTrue();

    // the base case continues one iteration, up to the next arrival
    assertThat(successors(successor(continuation, precision, body), precision, back)).isEmpty();
  }

  @Test
  public void splitsWithoutContinuationAsNativeKInduction() {
    InductionPrecision precision = precision(1, true, LoopPrecision.ALL_ABSTRACTIONS);
    InductionState beforeSecondArrival =
        successor(successor(initial, precision, entry), precision, body);
    InductionState inductionStart =
        successor(
            beforeSecondArrival,
            precision,
            back,
            new InductionTransferRelation(loopStructure, false, false, false));
    assertThat(inductionStart.isInductionStart()).isTrue();
  }

  @Test
  public void inductionStepAbstractsWithoutSplit() {
    InductionPrecision precision = precision(1, true, LoopPrecision.ALL_ABSTRACTIONS);
    InductionState inductionStart =
        ImmutableList.copyOf(
                successors(
                    successor(successor(initial, precision, entry), precision, body),
                    precision,
                    back))
            .get(1);

    InductionState afterBody = successor(inductionStart, precision, body);
    assertThat(afterBody.reportsTargets()).isFalse();
    InductionState first = successor(afterBody, precision, back);
    assertThat(first.isAbstraction()).isFalse();
    // the violation after k arrivals is the one of the induction step
    assertThat(successor(first, precision, body).reportsTargets()).isTrue();

    InductionState second = successor(successor(first, precision, body), precision, back);
    assertThat(second.isAbstraction()).isTrue();
    assertThat(second.isInductionStart()).isFalse();
    assertThat(second.isInInductionStep()).isTrue();
  }

  @Test
  public void cutsOffWhereNoAbstractionIsDescribed() {
    InductionPrecision precision = precision(0, false, 0);
    InductionState cutOff = successor(initial, precision, entry);
    assertThat(cutOff.isCutOff()).isTrue();
    assertThat(cutOff.isTarget()).isTrue();
    assertThat(successors(cutOff, precision, body)).isEmpty();
  }

  @Test
  public void continuationsOfDifferentPrecisionsDoNotCoverEachOther() {
    InductionPrecision small = precision(1, true, LoopPrecision.ALL_ABSTRACTIONS);
    InductionPrecision large = precision(2, true, LoopPrecision.ALL_ABSTRACTIONS);
    InductionState beforeSecondArrival = successor(successor(initial, small, entry), small, body);

    InductionState continuation = successors(beforeSecondArrival, small, back).iterator().next();
    InductionState concrete = successor(beforeSecondArrival, large, back);
    assertThat(concrete.getArrivals()).isEqualTo(continuation.getArrivals());

    InductionDomain domain = new InductionDomain();
    assertThat(domain.isLessOrEqual(concrete, continuation)).isFalse();
    assertThat(
            domain.isLessOrEqual(
                successor(concrete, large, body), successor(continuation, small, body)))
        .isFalse();
    assertThat(
            domain.isLessOrEqual(
                successors(beforeSecondArrival, small, back).iterator().next(), continuation))
        .isTrue();
  }

  /** An edge into a head of a loop with two heads, as gotos create it. */
  private record ArrivalAtLoopWithTwoHeads(LoopStructure loopStructure, CFAEdge edge) {

    static ArrivalAtLoopWithTwoHeads create() throws Exception {
      CFA cfa =
          TestCfaUtils.makeCfaFromFunctionBody(
              """
              int x = 0;
              L1:
              if (x > 3) { x = 0; }
              if (x == 1) { goto L3; }
              L2:
              x++;
              if (x == 5) { goto L2; }
              L3:
              if (x == 7) { goto L1; }
              goto L2;
              """);
      LoopStructure loopStructure = cfa.getLoopStructure().orElseThrow();
      Loop loop =
          from(loopStructure.getAllLoops()).firstMatch(l -> l.getLoopHeads().size() > 1).get();
      CFANode head = loop.getLoopHeads().iterator().next();
      return new ArrivalAtLoopWithTwoHeads(loopStructure, head.getEnteringEdges().first().get());
    }

    Collection<InductionState> successors(InductionState pState, InductionPrecision pPrecision) {
      return new InductionTransferRelation(loopStructure, false, false, false)
          .getAbstractSuccessorsForEdge(pState, pPrecision, edge);
    }

    InductionPrecision precision(int pK, boolean pInduction, int pDescribedAbstractions) {
      return InductionPrecision.uniform(
          loopStructure.getAllLoops(),
          new LoopPrecision(pK, pInduction, pDescribedAbstractions, false, false));
    }
  }

  @Test
  public void startsInductionStepsBeforeTheBoundAtLoopsWithSeveralHeads() throws Exception {
    ArrivalAtLoopWithTwoHeads arrival = ArrivalAtLoopWithTwoHeads.create();
    InductionPrecision precision = arrival.precision(2, true, LoopPrecision.ALL_ABSTRACTIONS);

    ImmutableList<InductionState> split =
        ImmutableList.copyOf(arrival.successors(initial, precision));
    assertThat(split).hasSize(2);
    assertThat(split.get(0).isAbstraction()).isFalse();
    assertThat(split.get(1).isInductionStart()).isTrue();

    // in the induction step every arrival starts a further step
    ImmutableList<InductionState> inStep =
        ImmutableList.copyOf(arrival.successors(split.get(1), precision));
    assertThat(inStep).hasSize(2);
    assertThat(inStep.get(0).isAbstraction()).isFalse();
    assertThat(inStep.get(1).isAbstraction()).isTrue();
    assertThat(inStep.get(1).isInductionStart()).isFalse();
    assertThat(inStep.get(1).isInInductionStep()).isTrue();
  }

  @Test
  public void startsNoInductionStepBeforeTheBoundWithoutDescription() throws Exception {
    ArrivalAtLoopWithTwoHeads arrival = ArrivalAtLoopWithTwoHeads.create();
    assertThat(arrival.successors(initial, arrival.precision(2, true, 0))).hasSize(1);
    assertThat(
            arrival.successors(
                initial, arrival.precision(2, false, LoopPrecision.ALL_ABSTRACTIONS)))
        .hasSize(1);
  }
}
