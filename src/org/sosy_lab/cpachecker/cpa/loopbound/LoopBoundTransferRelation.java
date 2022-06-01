// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopbound;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.base.Predicates.not;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

@Options(prefix = "cpa.loopbound")
public class LoopBoundTransferRelation extends SingleEdgeTransferRelation {

  private final ImmutableMap<CFAEdge, Loop> loopEntryEdges;
  private final ImmutableMap<CFAEdge, Loop> loopExitEdges;
  private final ImmutableListMultimap<CFANode, Loop> loopHeads;

  @Option(
      secure = true,
      description =
          "Only checks for error after loops were unrolled at least this amount of times.")
  private int startAtBound = 0;

  @Option(
      secure = true,
      description =
          "Only checks for targets after loops were unrolled exactly a number of times that is"
              + " contained in this list. The default is an empty list, which means targets are"
              + " checked in every iteration")
  private List<Integer> checkOnlyAtBounds = ImmutableList.of();

  LoopBoundTransferRelation(Configuration pConfig, CFA pCFA)
      throws CPAException, InvalidConfigurationException {
    checkNotNull(pCFA, "CFA instance needed to create LoopBoundCPA");
    pConfig.inject(this);
    if (!pCFA.getLoopStructure().isPresent()) {
      throw new CPAException("LoopBoundCPA cannot work without loop-structure information in CFA.");
    }

    ImmutableMap.Builder<CFAEdge, Loop> entryEdges = ImmutableMap.builder();
    ImmutableMap.Builder<CFAEdge, Loop> exitEdges = ImmutableMap.builder();
    ImmutableListMultimap.Builder<CFANode, Loop> heads = ImmutableListMultimap.builder();

    for (Loop l : pCFA.getLoopStructure().orElseThrow().getAllLoops()) {
      // function edges do not count as incoming/outgoing edges
      Stream<CFAEdge> incomingEdges =
          l.getIncomingEdges().stream()
              .filter(e -> l.getLoopHeads().contains(e.getSuccessor()))
              .filter(not(instanceOf(FunctionReturnEdge.class)));
      Stream<CFAEdge> outgoingEdges =
          l.getOutgoingEdges().stream().filter(not(instanceOf(FunctionCallEdge.class)));

      incomingEdges.forEach(e -> entryEdges.put(e, l));
      outgoingEdges.forEach(e -> exitEdges.put(e, l));
      l.getLoopHeads().forEach(h -> heads.put(h, l));
    }
    loopEntryEdges = entryEdges.buildOrThrow();
    loopExitEdges = exitEdges.buildOrThrow();
    loopHeads = heads.build();
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge) throws CPATransferException {

    LoopBoundState state = (LoopBoundState) pState;
    LoopBoundPrecision precision = (LoopBoundPrecision) pPrecision;

    if (pCfaEdge instanceof FunctionCallEdge) {
      // such edges do never change loop status
      return Collections.singleton(pState);
    }

    CFANode loc = pCfaEdge.getSuccessor();

    Loop oldLoop = loopExitEdges.get(pCfaEdge);
    if (oldLoop != null) {
      state = state.exit(oldLoop);
    }

    if (pCfaEdge instanceof FunctionReturnEdge) {
      // Such edges may be real loop-exit edges "while () { return; }",
      // but never loop-entry edges.
      // Return here because they might be mis-classified as entry edges.
      return Collections.singleton(state);
    }

    Loop newLoop = null;
    if (precision.shouldTrackStack()) {
      // Push a new loop onto the stack if we enter it
      newLoop = loopEntryEdges.get(pCfaEdge);
      if (newLoop != null) {
        state = state.enter(newLoop);
      }
    }

    // Check if we need to increment the loop counter
    Collection<Loop> visitedLoops = loopHeads.get(loc);
    assert newLoop == null || visitedLoops.contains(newLoop);
    for (Loop loop : visitedLoops) {
      state = state.visitLoopHead(loop);
      // Check if the bound for unrolling has been reached;
      // this check is also performed by the precision adjustment,
      // but we need to do it here, too,
      // to ensure that states are consistent during strengthening
      if ((precision.getMaxLoopIterations() > 0)
          && state.getDeepestIteration() > precision.getMaxLoopIterations()) {
        state = state.setStop(true);
      }
      state = state.enforceAbstraction(precision.getLoopIterationsBeforeAbstraction());
    }

    return Collections.singleton(state);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state,
      Iterable<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision)
      throws CPATransferException, InterruptedException {
    int k = ((LoopBoundState) state).getDeepestIteration();
    if ((k < startAtBound || (!checkOnlyAtBounds.isEmpty() && !checkOnlyAtBounds.contains(k)))
        && Iterators.any(otherStates.iterator(), AbstractStates::isTargetState)) {
      return ImmutableList.of();
    } else {
      return Collections.singleton(state);
    }
  }
}
