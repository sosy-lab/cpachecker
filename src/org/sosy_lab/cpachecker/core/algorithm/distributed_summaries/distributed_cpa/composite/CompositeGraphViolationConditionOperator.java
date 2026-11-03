// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssARGPathGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssARGPathGraph.Incoming;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.MergeableViolationConditionOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Computes the violation conditions of all paths of a {@link DssARGPathGraph} at once. The
 * conditions of the components are propagated backwards through the graph together, and two paths
 * that meet at the same state are merged if every component merges them, see {@link
 * MergeableViolationConditionOperator#merge}.
 */
public final class CompositeGraphViolationConditionOperator {

  /** The condition of one component on the paths merged so far. */
  private record Part<T>(MergeableViolationConditionOperator<T> operator, T condition) {

    static <T> Part<T> initial(
        MergeableViolationConditionOperator<T> pOperator,
        ARGState pTarget,
        Optional<ARGState> pPreviousCondition)
        throws InterruptedException {
      return new Part<>(pOperator, pOperator.initialCondition(pTarget, pPreviousCondition));
    }

    Optional<Part<T>> prepend(List<CFAEdge> pEdges)
        throws CPATransferException, InterruptedException {
      return operator.prepend(condition, pEdges).map(prepended -> new Part<>(operator, prepended));
    }

    Optional<Part<T>> merge(Part<?> pOther) throws InterruptedException {
      Preconditions.checkArgument(pOther.operator() == operator);
      @SuppressWarnings("unchecked")
      T other = (T) pOther.condition();
      return operator.merge(condition, other).map(merged -> new Part<>(operator, merged));
    }

    Optional<AbstractState> finish(ARGPath pPath, Optional<ARGState> pPreviousCondition)
        throws InterruptedException, SolverException {
      return operator.finish(pPath, pPreviousCondition, condition);
    }
  }

  private final List<ConfigurableProgramAnalysis> analyses;

  /** The operators of the distributed analyses, in the order of {@link #analyses}. */
  private final ImmutableList<MergeableViolationConditionOperator<?>> operators;

  private CompositeGraphViolationConditionOperator(
      List<ConfigurableProgramAnalysis> pAnalyses,
      ImmutableList<MergeableViolationConditionOperator<?>> pOperators) {
    analyses = pAnalyses;
    operators = pOperators;
  }

  /**
   * Creates the operator for the given components, or returns empty if one of them cannot merge
   * conditions. Components that are not distributed contribute their initial state.
   */
  public static Optional<CompositeGraphViolationConditionOperator> of(
      List<ConfigurableProgramAnalysis> pAnalyses) {
    ImmutableList.Builder<MergeableViolationConditionOperator<?>> operators =
        ImmutableList.builder();
    for (ConfigurableProgramAnalysis cpa : pAnalyses) {
      if (cpa instanceof DistributedConfigurableProgramAnalysis dcpa) {
        if (!(dcpa.getViolationConditionOperator()
            instanceof MergeableViolationConditionOperator<?> operator)) {
          return Optional.empty();
        }
        operators.add(operator);
      }
    }
    return Optional.of(new CompositeGraphViolationConditionOperator(pAnalyses, operators.build()));
  }

  List<AbstractState> compute(DssARGPathGraph pGraph, Optional<ARGState> pPreviousCondition)
      throws InterruptedException, CPATransferException, SolverException {
    // The alternatives at a state are the conditions of its paths to the target that could not be
    // merged. Each alternative holds one part per component.
    Map<ARGState, List<List<Part<?>>>> alternatives = new HashMap<>();
    List<Part<?>> initial = new ArrayList<>();
    for (MergeableViolationConditionOperator<?> operator : operators) {
      initial.add(Part.initial(operator, pGraph.getLastState(), pPreviousCondition));
    }
    alternatives.put(pGraph.getLastState(), new ArrayList<>(ImmutableList.of(initial)));
    for (ARGState node : pGraph.backwardOrder()) {
      if (Thread.interrupted()) {
        throw new InterruptedException();
      }
      List<List<Part<?>>> atNode = alternatives.get(node);
      if (atNode == null) {
        continue;
      }
      for (Incoming incoming : pGraph.incoming(node)) {
        List<List<Part<?>>> atParent =
            alternatives.computeIfAbsent(incoming.parent(), unused -> new ArrayList<>());
        for (List<Part<?>> alternative : atNode) {
          Optional<List<Part<?>>> prepended = prepend(alternative, incoming.edges());
          if (prepended.isPresent()) {
            add(atParent, prepended.orElseThrow());
          }
        }
      }
    }
    ImmutableList.Builder<AbstractState> conditions = ImmutableList.builder();
    for (List<Part<?>> alternative :
        alternatives.getOrDefault(pGraph.getFirstState(), ImmutableList.of())) {
      finish(pGraph, pPreviousCondition, alternative).ifPresent(conditions::add);
    }
    return conditions.build();
  }

  private static Optional<List<Part<?>>> prepend(List<Part<?>> pAlternative, List<CFAEdge> pEdges)
      throws CPATransferException, InterruptedException {
    List<Part<?>> prepended = new ArrayList<>(pAlternative.size());
    for (Part<?> part : pAlternative) {
      Optional<? extends Part<?>> next = part.prepend(pEdges);
      if (next.isEmpty()) {
        return Optional.empty();
      }
      prepended.add(next.orElseThrow());
    }
    return Optional.of(prepended);
  }

  /** Merges {@code pAlternative} into the first alternative that allows it, or adds it. */
  private static void add(List<List<Part<?>>> pAlternatives, List<Part<?>> pAlternative)
      throws InterruptedException {
    for (int i = 0; i < pAlternatives.size(); i++) {
      Optional<List<Part<?>>> merged = merge(pAlternatives.get(i), pAlternative);
      if (merged.isPresent()) {
        pAlternatives.set(i, merged.orElseThrow());
        return;
      }
    }
    pAlternatives.add(pAlternative);
  }

  private static Optional<List<Part<?>>> merge(List<Part<?>> pFirst, List<Part<?>> pSecond)
      throws InterruptedException {
    List<Part<?>> merged = new ArrayList<>(pFirst.size());
    for (int i = 0; i < pFirst.size(); i++) {
      Optional<? extends Part<?>> part = pFirst.get(i).merge(pSecond.get(i));
      if (part.isEmpty()) {
        return Optional.empty();
      }
      merged.add(part.orElseThrow());
    }
    return Optional.of(merged);
  }

  private Optional<AbstractState> finish(
      DssARGPathGraph pGraph, Optional<ARGState> pPreviousCondition, List<Part<?>> pAlternative)
      throws InterruptedException, SolverException {
    CFANode location = AbstractStates.extractLocation(pGraph.getFirstState());
    Iterator<Part<?>> parts = pAlternative.iterator();
    List<AbstractState> states = new ArrayList<>(analyses.size());
    for (ConfigurableProgramAnalysis cpa : analyses) {
      if (cpa instanceof DistributedConfigurableProgramAnalysis) {
        Optional<AbstractState> state = parts.next().finish(pGraph, pPreviousCondition);
        if (state.isEmpty()) {
          return Optional.empty();
        }
        states.add(state.orElseThrow());
      } else {
        states.add(cpa.getInitialState(location, StateSpacePartition.getDefaultPartition()));
      }
    }
    return Optional.of(new CompositeState(ImmutableList.copyOf(states)));
  }
}
